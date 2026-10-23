#!/usr/bin/env python3
"""Get a real access token from the local DEV Keycloak, for calling the API by hand.

DEV ONLY. The realm disables the password grant (unsafe for browser apps), so this
script performs the same Authorization Code + PKCE sign-in the web app does.

Usage (from the repo root, with `docker compose up -d` running):
    TOKEN=$(python3 infra/keycloak/get-dev-token.py)
    curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/me

Options:
    --user / --password   another account (default: the dev user)
    --claims              print the token's decoded claims instead of the token
    --check               check the realm still behaves as the M0 spec requires

Uses only the Python standard library.
"""
import argparse
import base64
import hashlib
import html
import http.cookiejar
import json
import re
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request

REALM_URL = "http://localhost:8180/realms/crewup/protocol/openid-connect"
CLIENT_ID = "crewup-web"
REDIRECT_URI = "http://localhost:5173/auth/callback"


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Browser:
    """Just enough of a browser: a cookie jar, and redirects returned instead of followed."""

    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies), _NoRedirect)

    def request(self, url, form=None):
        """Return (redirect location or None, response body)."""
        # Keycloak marks its cookies Secure. Browsers still send them to http://localhost
        # (a secure context); Python's cookie jar doesn't, so clear the flag.
        for cookie in self.cookies:
            cookie.secure = False
        data = urllib.parse.urlencode(form).encode() if form else None
        try:
            response = self.opener.open(urllib.request.Request(url, data=data))
            return None, response.read().decode()
        except urllib.error.HTTPError as error:
            return error.headers.get("Location"), error.read().decode(errors="replace")


def b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def auth_url(**overrides):
    params = {
        "client_id": CLIENT_ID,
        "response_type": "code",
        "redirect_uri": REDIRECT_URI,
        "scope": "openid",
        "state": secrets.token_urlsafe(8),
    }
    params.update(overrides)
    return f"{REALM_URL}/auth?{urllib.parse.urlencode(params)}"


def sign_in(user, password):
    """Run Authorization Code + PKCE and return (access token, login page HTML)."""
    verifier = b64url(secrets.token_bytes(32))
    challenge = b64url(hashlib.sha256(verifier.encode()).digest())
    browser = Browser()

    _, page = browser.request(auth_url(code_challenge=challenge, code_challenge_method="S256"))
    form_action = re.search(r'<form[^>]+id="kc-form-login"[^>]+action="([^"]+)"', page)
    if not form_action:
        sys.exit("Keycloak login page not found. Is `docker compose up -d` running?")

    location, _ = browser.request(html.unescape(form_action.group(1)), {"username": user, "password": password})
    code = urllib.parse.parse_qs(urllib.parse.urlparse(location or "").query).get("code")
    if not code:
        sys.exit(f"Sign-in failed for {user} (wrong password, or the account doesn't exist).")

    token_form = {
        "grant_type": "authorization_code",
        "code": code[0],
        "redirect_uri": REDIRECT_URI,
        "client_id": CLIENT_ID,
        "code_verifier": verifier,
    }
    with urllib.request.urlopen(f"{REALM_URL}/token", urllib.parse.urlencode(token_form).encode()) as response:
        return json.load(response)["access_token"], page


def claims_of(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))


def check_realm(user, password):
    """The T2 checks: PKCE required, foreign redirects refused, registration on, crewup-api audience."""
    results = []

    location, body = Browser().request(auth_url())
    results.append(("PKCE is required", bool(location and "error=" in location) or "code_challenge" in body.lower()))

    _, body = Browser().request(
        auth_url(redirect_uri="https://evil.example/cb", code_challenge="x" * 43, code_challenge_method="S256")
    )
    results.append(("Foreign redirect URIs are refused", "Invalid parameter: redirect_uri" in body))

    token, login_page = sign_in(user, password)
    results.append(("Self-registration is offered", "kc-registration" in login_page or "registrations?" in login_page))

    audience = claims_of(token).get("aud")
    audiences = audience if isinstance(audience, list) else [audience]
    results.append(("Access token audience includes crewup-api", "crewup-api" in audiences))

    for name, passed in results:
        print(f"{'PASS' if passed else 'FAIL'}  {name}")
    return all(passed for _, passed in results)


def main():
    parser = argparse.ArgumentParser(description="Get an access token from the local DEV Keycloak.")
    parser.add_argument("--user", default="dev@crewup.local")
    parser.add_argument("--password", default="dev")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--claims", action="store_true", help="print the decoded claims instead of the token")
    mode.add_argument("--check", action="store_true", help="check the realm configuration")
    args = parser.parse_args()

    if args.check:
        sys.exit(0 if check_realm(args.user, args.password) else 1)

    token, _ = sign_in(args.user, args.password)
    print(json.dumps(claims_of(token), indent=2) if args.claims else token)


if __name__ == "__main__":
    main()
