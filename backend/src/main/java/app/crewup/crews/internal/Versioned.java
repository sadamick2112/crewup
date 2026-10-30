package app.crewup.crews.internal;

/** A response body with the entity version its {@code ETag} header carries (API-7). */
record Versioned<T>(T value, long version) {}
