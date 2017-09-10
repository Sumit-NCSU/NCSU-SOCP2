package controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;

import actors.AirlineActor;
import actors.AirlineActorProtocol.DebugFlag;
import actors.BookingActor;
import actors.BookingActorProtocol.BookFlight;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.compat.java8.FutureConverters;
import services.DatabaseService;
import util.Strings;

/**
 * @author srivassumit
 *
 */
@Singleton
public class BookingController extends Controller {

	private static final ALogger LOG = Logger.of(BookingController.class);
	final ActorSystem system;
	final DatabaseService databaseService;
	final ActorRef bookingActor;
	final ActorRef aaActor, baActor, caActor;

	@Inject
	public BookingController(ActorSystem system, DatabaseService databaseService) {
		LOG.debug("Initializing Booking Controller");
		this.system = system;
		this.databaseService = databaseService;
		aaActor = system.actorOf(AirlineActor.getProps("AA", AirlineActor.NORMAL, databaseService));
		baActor = system.actorOf(AirlineActor.getProps("BA", AirlineActor.NORMAL, databaseService));
		caActor = system.actorOf(AirlineActor.getProps("CA", AirlineActor.NORMAL, databaseService));
		bookingActor = system.actorOf(BookingActor.getProps(aaActor, baActor, caActor));
		LOG.debug("Initialized Booking Controller");
	}

	/**
	 * Get a list of trips booked.
	 * 
	 * @return Result
	 */
	public Result getTrips() {
		LOG.debug("Received request for get trips.");
		List<String> tripIds = databaseService.fetchTrips();
		return ok(createSuccessResponse("trips", new Gson().toJson(tripIds)));
	}

	/**
	 * Get a list of segments of a trip. A segment is represented by its flight.
	 * 
	 * @param tripID
	 *            the Trip ID
	 * @return Result
	 */
	public Result getSegments(String tripID) {
		LOG.debug("Received request for get segments.");
		String segments = databaseService.fetchSegments(tripID);
		if (segments == null) {
			return ok(createErrorResponse("No segments found for Trip ID: " + tripID));
		} else {
			String[] segmentArray = segments.trim().split(" ");
			return ok(createSuccessResponse("segments", new Gson().toJson(segmentArray)));
		}
	}

	/**
	 * Get a list of airline operators
	 * 
	 * @return
	 */
	public Result getOperators() {
		LOG.debug("Received request for get operators.");
		List<String> operators = databaseService.fetchOperators();
		return ok(createSuccessResponse("operators", new Gson().toJson(operators)));
	}

	/**
	 * Get a list of flights operated by an airline operator
	 * 
	 * @param operator
	 * @return
	 */
	public Result getFlights(String operator) {
		LOG.debug("Received request for get flights for operator: " + operator);
		List<String> operatorFlights = databaseService.fetchOperatorFlights(operator);
		if (operatorFlights.size() == 0) {
			return ok(createErrorResponse("No Flights operated by operator: " + operator));
		} else {
			return ok(createSuccessResponse("flights", new Gson().toJson(operatorFlights)));
		}
	}

	/**
	 * Get the number of available seats on a flight.
	 * 
	 * @param operator
	 *            the airline operator
	 * @param flight
	 *            the flight
	 * @return Result
	 */
	public Result getSeats(String operator, String flight) {
		LOG.debug("Received request for get seats in flight: " + flight + " by operator: " + operator);
		int availableSeats = databaseService.fetchAvailableSeats(operator, flight);
		if (availableSeats == 0) {
			return ok(createErrorResponse("No seat available on: " + flight + ", operated by: " + operator));
		} else {
			return ok(createSuccessResponse("seats", String.valueOf(availableSeats)));
		}
	}

	/**
	 * Book a trip. Currently, the $from and $to should always be X and Y. If not,
	 * return an error
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public CompletionStage<Result> bookTrip(String from, String to) {
		LOG.debug("Received request for book trip from: " + from + " to: " + to);
		if (!"X".equals(from) || !"Y".equals(to)) {
			return CompletableFuture
					.completedFuture(ok(createErrorResponse("From and To should be 'X' and 'Y' respectively")));
		} else {
			return FutureConverters.toJava(Patterns.ask(bookingActor, new BookFlight(from, to), 15000))
					.thenApply(response -> parseResponse((String) response));
		}
	}

	/**
	 * After this request is posted, corresponding airline actor will reply fail to
	 * subsequent Confirm requests without actual processing
	 * 
	 * @param airline
	 *            the airline
	 * @return
	 */
	public Result confirmFail(String airline) {
		LOG.debug("Request recieved: 'DebugAPI:Fail' for airline: " + airline);
		return processDebugAPI(airline, new DebugFlag(AirlineActor.FAIL));
	}

	/**
	 * After this request is posted, corresponding airline actor will not reply to
	 * subsequent Confirm requests without actual processing
	 * 
	 * @param airline
	 *            the airline
	 * @return
	 */
	public Result confirmNoResponse(String airline) {
		LOG.debug("Request recieved: 'DebugAPI:No_Response' for airline: " + airline);
		return processDebugAPI(airline, new DebugFlag(AirlineActor.NO_REPLY));
	}

	/**
	 * After this request is posted, the actor will reset to normal
	 * 
	 * @param airline
	 *            the airline
	 * @return
	 */
	public Result reset(String airline) {
		LOG.debug("Request recieved: 'DebugAPI:Reset' for airline: " + airline);
		return processDebugAPI(airline, new DebugFlag(AirlineActor.NORMAL));
	}

	/**
	 * This methods sends a request to reset the database
	 * 
	 * @return
	 */
	public Result resetdb() {
		LOG.debug("Resetting database.");
		if (databaseService.resetDatabase()) {
			LOG.debug("Database Reset.");
			return ok(createSuccessResponse(Strings.MESSAGE, "Database reset to initial state"));
		} else {
			LOG.debug("Failed to reset Database.");
			return ok(createErrorResponse("Failed to reset the database"));
		}
	}

	/**
	 * This method parses the response received from the booking actor.
	 * 
	 * @param response
	 *            the response received from the booking actor.
	 * @return
	 */
	private Result parseResponse(String response) {
		if (response.startsWith("No")) {
			return ok(createErrorResponse(response));
		} else {
			return ok(createSuccessResponse("tripID", response));
		}
	}

	/**
	 * This method processes the debug API and sends the request to the
	 * corresponding actor
	 * 
	 * @param airline
	 * @param flag
	 * @return
	 */
	private Result processDebugAPI(String airline, DebugFlag flag) {
		switch (airline) {
		case Strings.AA:
			LOG.debug("Processing Debug API request for: " + Strings.AA);
			aaActor.tell(flag, ActorRef.noSender());
			break;
		case Strings.BA:
			LOG.debug("Processing Debug API request for: " + Strings.BA);
			baActor.tell(flag, ActorRef.noSender());
			break;
		case Strings.CA:
			LOG.debug("Processing Debug API request for: " + Strings.CA);
			caActor.tell(flag, ActorRef.noSender());
			break;
		default:
			LOG.debug("Invalid Airline: " + airline);
			return ok(createErrorResponse("Invalid Airline: " + airline));
		}
		return ok(createSuccessResponse(null, null));
	}

	/**
	 * This method returns the following JSON response:
	 * 
	 * <pre>
	 * {
	 *   "status":"success",
	 *   "&lt;key&gt;":"&lt;message&gt;"
	 * </pre>
	 * 
	 * @param key
	 *            the key
	 * @param message
	 *            the message
	 * @return
	 */
	private ObjectNode createSuccessResponse(String key, Object message) {
		ObjectNode result = Json.newObject();
		result.put(Strings.STATUS, Strings.SUCCESS);
		if (key != null && message != null) {
			result.put(key, (String) message);
		}
		return result;
	}

	/**
	 * This method returns the following JSON response:
	 * 
	 * <pre>
	 * {
	 *   "status":"error",
	 *   "message":"&lt;message&gt;"
	 * </pre>
	 * 
	 * @param message
	 *            the message
	 * @return
	 */
	private ObjectNode createErrorResponse(String message) {
		ObjectNode result = Json.newObject();
		result.put(Strings.STATUS, Strings.ERROR);
		result.put(Strings.MESSAGE, message);
		return result;
	}

}
