package college.mediator;

import java.util.Collection;

/**
 * MEDIATOR pattern - the Mediator interface.
 * Every method returns how many participants actually received the message.
 */
public interface NotificationMediator {
    void register(Participant participant);

    int sendTo(Participant sender, Participant recipient, String message);

    int sendToRole(Participant sender, String role, String message);

    int sendToGroup(Participant sender, Collection<? extends Participant> group, String message);

    int broadcast(Participant sender, String message);
}
