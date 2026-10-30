package college.model;

import college.mediator.Participant;

public class Administrator extends Participant {
    public Administrator(String id, String name) {
        super(id, name, Participant.ADMIN);
    }
}
