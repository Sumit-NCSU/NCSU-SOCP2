package actors;

import actors.AirlineActorProtocol.Hold;
import actors.BookingActorProtocol.BookFlight;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import scala.compat.java8.FutureConverters;

/**
 * @author srivassumit
 *
 */
public class BookingActor extends AbstractActor {

	final ActorRef aaActor, baActor, caActor;

	public BookingActor(ActorRef aaActor, ActorRef baActor, ActorRef caActor) {
		this.aaActor = aaActor;
		this.baActor = baActor;
		this.caActor = caActor;
	}

	public static Props getProps(ActorRef aaActor, ActorRef baActor, ActorRef caActor) {
		return Props.create(BookingActor.class, () -> new BookingActor(aaActor, baActor, caActor));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(BookFlight.class, bookFlight -> {
			String reply = "The booking actor has recieved a book flight request";
			// Try to book CA001
			String tripId = tryBookCA001(bookFlight.from, bookFlight.to);
			System.out.println("Got message from CA: " + tripId);
			if (tripId == null) {
				// If CA001 is unavailable then, try to book AA001+BA001
				tripId = tryBookAA001BA001(bookFlight.from, bookFlight.to);
				if (tripId == null) {
					// If AA001+BA001 is unavailable then try to book AA001+CA002+AA002
					tripId = tryBookAA001CA002AA002(bookFlight.from, bookFlight.to);
				}
			}
			if (tripId == null) {
				reply = "Erorr, no seats available in any of the flights from " + bookFlight.from + " to "
						+ bookFlight.to;
			} else {
				reply = tripId;
			}
			sender().tell(reply, self());
		}).build();
	}

	private String tryBookCA001(String from, String to) {
		String tripId = null;
		// Use 2PC for booking.
		// Send Hold request for all segments.
		try {
			String result = FutureConverters.toJava(Patterns.ask(caActor, new Hold(), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			return result;
		} catch (Exception e) {
			System.out.println("Error while sending hold request to CA");
			e.printStackTrace();
		}
		return null;
		// Send Confirm request for all segments.
		// caActor.tell(new Confirm(), getSelf());
		// return tripId;
	}

	private String tryBookAA001BA001(String from, String to) {
		String tripId = null;
		// Use 2PC for booking.
		// Send Hold request for all segments.

		// Send Confirm request for all segments.

		return tripId;
	}

	private String tryBookAA001CA002AA002(String from, String to) {
		String tripId = null;
		// Use 2PC for booking.
		// Send Hold request for all segments.

		// Send Confirm request for all segments.

		return tripId;
	}

}
