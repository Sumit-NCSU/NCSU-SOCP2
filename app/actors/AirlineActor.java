package actors;

import actors.AirlineActorProtocol.Confirm;
import actors.AirlineActorProtocol.Hold;
import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * @author srivassumit
 *
 */
public class AirlineActor extends AbstractActor {
	final String airline;

	public AirlineActor(String airline) {
		this.airline = airline;
	}

	// Should track number of available seats.
	// Should accept and respond to hold/confirm requests
	public static Props getProps(String airline) {
		return Props.create(AirlineActor.class, () -> new AirlineActor(airline));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Hold.class, hold -> {
			String reply = this.airline + " has recieved a hold request";
			sender().tell(reply, self());
		}).match(Confirm.class, confirm -> {
			String reply = this.airline + " has recieved a confirm request";
			sender().tell(reply, self());
		}).build();
	}

}
