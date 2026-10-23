// Fixture for no-dangerously-set-inner-html (TS-10, M0-16b): `semgrep --test .semgrep` checks every annotated line.
import { createElement } from "react";

export function RawHtml({ html }: { html: string }) {
  // ruleid: no-dangerously-set-inner-html
  return <div dangerouslySetInnerHTML={{ __html: html }} />;
}

export function RawHtmlInsideMarkup({ html }: { html: string }) {
  const note = (
    // ruleid: no-dangerously-set-inner-html
    <p className="note" dangerouslySetInnerHTML={{ __html: html }}></p>
  );
  return <section>{note}</section>;
}

export function RawHtmlWithoutJsx({ html }: { html: string }) {
  // ruleid: no-dangerously-set-inner-html
  return createElement("div", { dangerouslySetInnerHTML: { __html: html } });
}

export function Text({ text }: { text: string }) {
  // ok: no-dangerously-set-inner-html
  return <div className="note">{text}</div>;
}
