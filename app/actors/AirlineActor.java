package actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import actors.AirlineActorProtocol.Confirm;
import actors.AirlineActorProtocol.DebugFlag;
import actors.AirlineActorProtocol.Hold;
import akka.actor.AbstractActor;
import akka.actor.Props;
import play.Logger;
import play.Logger.ALogger;
import services.DatabaseService;
import util.Strings;

/**
 * @author srivassumit
 *
 */
public class AirlineActor extends AbstractActor {

	private static final ALogger LOG = Logger.of(AirlineActor.class);
	final String airline;
	private String debugFlag;
	private List<String> waitingForConfirm;
	final DatabaseService databaseService;
	public static final String NORMAL = Strings.NORMAL;
	public static final String FAIL = Strings.FAIL;
	public static final String NO_REPLY = Strings.NO_REPLY;

	public AirlineActor(String airline, String debugFlag, DatabaseService databaseService) {
		this.airline = airline;
		this.debugFlag = debugFlag;
		this.databaseService = databaseService;
		waitingForConfirm = new ArrayList<String>();
	}

	public static Props getProps(String airline, String debugFlag, DatabaseService databaseService) {
		return Props.create(AirlineActor.class, () -> new AirlineActor(airline, debugFlag, databaseService));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Hold.class, hold -> {
			LOG.debug("Hold request recieved by " + airline);
			String tripId = doHoldAction(hold.flight);
			if (tripId == null) {
				tripId = Strings.BLANK;
			}
			checkPreviousHold(hold.flight);
			sender().tell(tripId, self());
		}).match(Confirm.class, confirm -> {
			LOG.debug("Confirm request recieved by " + airline);
			if (NO_REPLY.equals(this.debugFlag)) {
				LOG.debug("Confirm request handled by NO_REPLY flag for " + airline);
			} else if (FAIL.equals(this.debugFlag)) {
				LOG.debug("Confirm request handled by FAIL flag for " + airline);
				sender().tell(Strings.FAIL, self());
			} else {
				LOG.debug("Confirm request handled by NORMAL flag for " + airline);
				waitingForConfirm.remove(confirm.flight);
				String tripId = doConfirmationAction(confirm.flight, confirm.tripId);
				if (tripId == null) {
					tripId = Strings.BLANK;
				}
				sender().tell(tripId, self());
			}
		}).match(DebugFlag.class, debugFlag -> {
			LOG.debug("Debug API request:" + debugFlag.message + " received by " + airline);
			this.debugFlag = debugFlag.message;
			String reply = airline + ": completed debug request for: " + debugFlag.message;
			LOG.debug(reply);
			sender().tell(reply, self());
		}).build();
	}

	private void checkPreviousHold(String flight) {
		LOG.debug(airline + " Waiting for Confirm Request for " + flight + " for next 3 seconds.");
		waitingForConfirm.add(flight);
		// If hold is still there in DB after 3 seconds and that hold is older than 3
		// seconds, then release that hold.
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				checkAfter3Seconds(flight);
			}
		}, 3000);
	}

	private void checkAfter3Seconds(String flight) {
		LOG.debug(airline + " waited for 3 seconds for Confirm request for " + flight + ". Checking the hold now.");
		if (waitingForConfirm.contains(flight)) {
			LOG.debug(airline + " did not receive confirm request for: " + flight
					+ " => hold was not confirmed. Checking in database");
			long currentTime = System.currentTimeMillis();
			// check in db now
			String dbHoldTime = databaseService.getHoldTime(airline, flight);
			if (dbHoldTime != null) {
				long holdTime = Long.valueOf(dbHoldTime);
				double seconds = (currentTime - holdTime) / 1000.0;
				if (seconds >= 3) {
					// release hold
					LOG.debug("Hold found in database that is older than 3 seconds. releasing hold now.");
					databaseService.releaseHold(airline, flight);
				}
			} else {
				LOG.debug("No Hold time in database. No need to release hold.");
			}
			waitingForConfirm.remove(flight);
		} else {// if it is not waiting for confirm that means confirm request was received.
			LOG.debug(airline + " is not waiting for Confirm request for: " + flight + " => hold was confirmed.");
		}

	}

	private String doHoldAction(String flight) {
		LOG.debug("Performing Hold action by " + airline + " actor for flight: " + flight);
		String tripId = Strings.BLANK;
		try {
			tripId = databaseService.tryHoldSeat(this.airline, flight);
		} catch (Exception e) {
			LOG.error("Error while performing Hold action by " + airline + ", for flight " + flight);
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}

	private String doConfirmationAction(String flight, String tripId) {
		LOG.debug("Performing Confirm action by " + airline + " actor for flight: " + flight + ". Transaction ID: "
				+ tripId);
		try {
			tripId = databaseService.tryConfirmSeat(this.airline, flight, tripId);
		} catch (Exception e) {
			LOG.error("Error while performing Confirm action by " + airline + ", for flight " + flight
					+ ". Transaction ID: " + tripId);
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}
}
