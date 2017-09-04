package actors;

import akka.actor.AbstractActor;
import akka.actor.Props;

/**
 * @author sriva
 *
 */
public class BookingActor extends AbstractActor {

	private final String message;

	public static Props props(String message) {
		return Props.create(BookingActor.class, () -> new BookingActor(message));
	}

	public BookingActor(String message) {
		this.message = message;
	}

	@Override
	public Receive createReceive() {
		return null;
	}

}
