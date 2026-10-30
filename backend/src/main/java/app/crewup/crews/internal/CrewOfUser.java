package app.crewup.crews.internal;

/** A row of My crews: the crew, the caller's role in it, and its sort key as the database computed it. */
record CrewOfUser(Crew crew, MemberRole myRole, String sortName) {}
