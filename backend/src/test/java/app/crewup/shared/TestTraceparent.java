package app.crewup.shared;

/** A W3C {@code traceparent} with a fresh trace ID, so a test can find its own log lines and responses. */
record TestTraceparent(String traceId, String parentSpanId) {

    static TestTraceparent random() {
        String traceId = Ids.newId().toString().replace("-", "");
        String parentSpanId = Ids.newId().toString().replace("-", "").substring(16);
        return new TestTraceparent(traceId, parentSpanId);
    }

    String header() {
        return "00-" + traceId + "-" + parentSpanId + "-01";
    }
}
