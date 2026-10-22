/**
 * This file was hand-edited by openapi-typescript.
 * Do not make direct changes to the file.
 */

export interface paths {
    "/api/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the signed-in user's profile
         * @description The first call creates the user from the access token; later calls return the same user.
         */
        get: operations["me"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description An error, as RFC 9457 Problem Details */
        ProblemDetail: {
            /**
             * Format: uri
             * @description Identifies the kind of error, e.g. urn:crewup:problem:unauthenticated
             */
            type?: string;
            title: string;
            /** Format: int32 */
            status: number;
            detail?: string;
            /** Format: uri */
            instance?: string;
            /** @description The request's trace ID; shown to users as the error's reference */
            traceId: string;
        };
        UserProfile: {
            /** Format: uuid */
            id: string;
            /** @description Name shown to other members */
            displayName: string;
            /** @description Null if the identity provider has no email for the user */
            email: string | null;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    me: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description The signed-in user's profile */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["UserProfile"];
                };
            };
            /** @description Missing or invalid access token */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
}
