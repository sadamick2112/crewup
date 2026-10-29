package app.crewup.crews.internal;

import app.crewup.shared.NotFoundException;

/** The same answer for an unknown crew, a deleted one, and one the caller isn't in, so none can be told apart. */
class CrewNotFoundException extends NotFoundException {

    CrewNotFoundException() {
        super("Crew not found");
    }
}
