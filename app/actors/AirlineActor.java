package actors;

import actors.AirlineActorProtocol.Confirm;
import actors.AirlineActorProtocol.DebugFlag;
import actors.AirlineActorProtocol.Hold;
import akka.actor.AbstractActor;
import akka.actor.Props;
import services.DatabaseService;

/**
 * @author srivassumit
 *
 */
public class AirlineActor extends AbstractActor {
	final String airline;
	final String debugFlag;
	final DatabaseService databaseService;
	public static final String NORMAL = "N";
	public static final String FAIL = "F";
	public static final String NO_REPLY = "NR";

	public AirlineActor(String airline, String debugFlag, DatabaseService databaseService) {
		this.airline = airline;
		this.debugFlag = debugFlag;
		this.databaseService = databaseService;
	}

	// Should track number of available seats.
	// Should accept and respond to hold/confirm requests
	public static Props getProps(String airline, String debugFlag, DatabaseService databaseService) {
		return Props.create(AirlineActor.class, () -> new AirlineActor(airline, debugFlag, databaseService));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Hold.class, hold -> {
			// String reply = this.airline + " Hold for Flight: " + hold.flight;
			String tripId = doHoldAction(hold.flight);
			if (tripId == null) {
				tripId = "";
			}
			sender().tell(tripId, self());
		}).match(Confirm.class, confirm -> {
			// String reply = this.airline + " Confirm for Flight: " + confirm.flight;
			String tripId = doConfirmationAction(confirm.flight, confirm.tripId);
			if (tripId == null) {
				tripId = "";
			}
			sender().tell(tripId, self());
		}).match(DebugFlag.class, debugFlag -> {
			String reply = "Debug Request recieved: " + debugFlag.message;
			sender().tell(reply, self());
		}).build();
	}

	private String doHoldAction(String flight) {
		String tripId = "";
		try {
			tripId = databaseService.tryHoldSeat(this.airline, flight);
		} catch (Exception e) {
			e.printStackTrace();
			tripId = "";
		}
		return tripId;
	}

	private String doConfirmationAction(String flight, String tripId) {
		try {
			tripId = databaseService.tryConfirmSeat(this.airline, flight, tripId);
		} catch (Exception e) {
			e.printStackTrace();
			tripId = "";
		}
		return tripId;
	}
}
