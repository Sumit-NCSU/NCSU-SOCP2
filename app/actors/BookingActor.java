package actors;

import actors.AirlineActorProtocol.Confirm;
import actors.AirlineActorProtocol.Hold;
import actors.BookingActorProtocol.BookFlight;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import play.Logger;
import play.Logger.ALogger;
import scala.compat.java8.FutureConverters;
import util.Strings;

/**
 * @author srivassumit
 *
 */
public class BookingActor extends AbstractActor {

	private static final ALogger LOG = Logger.of(BookingActor.class);
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
			LOG.debug("The booking actor has recieved a book flight request from " + bookFlight.from + ", to "
					+ bookFlight.to);
			// Try to book CA001
			String tripId = tryBookCA001(bookFlight.from, bookFlight.to);
			if (Strings.BLANK.equals(tripId)) {
				// If CA001 is unavailable then, try to book AA001+BA001
				tripId = tryBookAA001BA001(bookFlight.from, bookFlight.to);
				if (Strings.BLANK.equals(tripId)) {
					// If AA001+BA001 is unavailable then try to book AA001+CA002+AA002
					tripId = tryBookAA001CA002AA002(bookFlight.from, bookFlight.to);
					if (Strings.BLANK.equals(tripId)) {
						tripId = "No seats available in any of the flights from " + bookFlight.from + " to "
								+ bookFlight.to;
						LOG.debug(tripId);
					} else {
						LOG.debug("Flight Booking Confirmed on Schedule 3. Transaction ID: " + tripId);
					}
				} else {
					LOG.debug("Flight Booking Confirmed on Schedule 2. Transaction ID: " + tripId);
				}
			} else {
				LOG.debug("Flight Booking Confirmed on Schedule 1. Transaction ID: " + tripId);
			}
			sender().tell(tripId, self());
		}).build();
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private String tryBookCA001(String from, String to) {
		LOG.debug("Trying to Book Schedule1: CA001");
		String tripId = Strings.BLANK;
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(caActor, new Hold("CA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!Strings.BLANK.equals(tripId)) {
				LOG.debug("Hold seat successful for schedule1: CA001. Transaction ID: " + tripId);
				// If the Hold request is successful, then send Confirm request.
				tripId = FutureConverters.toJava(Patterns.ask(caActor, new Confirm("CA001", tripId), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
				if (!Strings.BLANK.equals(tripId) && !Strings.FAIL.equals(tripId)) {
					LOG.debug("Confirm seat successful for schedule1: CA001. Transaction ID: " + tripId);
				} else {
					LOG.debug("Could not confirm seat for schedule1: CA001. Try Schedule 2.");
					tripId = Strings.BLANK;
					// now hold should be released after some time and adjust available seat
				}
			} else {
				LOG.debug("Could not hold seat for schedule1: CA001. Try Schedule 2.");
				tripId = Strings.BLANK;
			}
		} catch (Exception e) {
			LOG.debug("Error while trying to Book CA001");
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private String tryBookAA001BA001(String from, String to) {
		LOG.debug("Trying to Book Schedule2: AA001+BA001");
		String tripId = Strings.BLANK;
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!Strings.BLANK.equals(tripId)) {
				LOG.debug("Hold seat successful for schedule2-part1: AA001. Transaction ID: " + tripId);
				tripId = FutureConverters.toJava(Patterns.ask(baActor, new Hold("BA001"), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
				if (!Strings.BLANK.equals(tripId)) {
					LOG.debug("Hold seat successful for schedule2-part2: BA001. Transaction ID: " + tripId);
					// If the Hold request is successful, then send Confirm request.
					tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Confirm("AA001", tripId), 1000))
							.thenApply(response -> ((String) response)).toCompletableFuture().get();
					if (!Strings.BLANK.equals(tripId) && !Strings.FAIL.equals(tripId)) {
						LOG.debug("Confirm seat successful for schedule2-part1: AA001. Transaction ID: " + tripId);
						tripId = FutureConverters.toJava(Patterns.ask(baActor, new Confirm("BA001", tripId), 1000))
								.thenApply(response -> ((String) response)).toCompletableFuture().get();
						if (Strings.BLANK.equals(tripId) || Strings.FAIL.equals(tripId)) {
							LOG.debug("Could not confirm seat for schedule2-part2: BA001. Try Schedule 3.");
							tripId = Strings.BLANK;
							// now both holds should be released after some time.
						} else {
							LOG.debug("Confirm seat successful for schedule2-part2: BA001. Transaction ID: " + tripId);
						}
					} else {
						LOG.debug("Could not confirm seat for schedule2-part1: AA001. Try Schedule 3.");
						tripId = Strings.BLANK;
						// now both holds should be released after some time and adjust available seat
					}
				} else {
					LOG.debug("Could not hold seat for schedule2-part2: BA001. Try Schedule 3.");
					// now schedule2-part1 hold should be released after some time and adjust
					// available seat
				}
			} else {
				LOG.debug("Could not hold seat for schedule2-part1: AA001. Try Schedule 3.");
			}
		} catch (Exception e) {
			LOG.debug("Error while trying to Book AA001+BA001");
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private String tryBookAA001CA002AA002(String from, String to) {
		LOG.debug("Trying to Book Schedule3: AA001+CA002+AA002");
		String tripId = Strings.BLANK;
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!Strings.BLANK.equals(tripId)) {
				LOG.debug("Hold seat successful for schedule3-part1: AA001. Transaction ID: " + tripId);
				tripId = FutureConverters.toJava(Patterns.ask(caActor, new Hold("CA002"), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
				if (!Strings.BLANK.equals(tripId)) {
					LOG.debug("Hold seat successful for schedule3-part2: CA002. Transaction ID: " + tripId);
					tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA002"), 1000))
							.thenApply(response -> ((String) response)).toCompletableFuture().get();
					if (!Strings.BLANK.equals(tripId)) {
						LOG.debug("Hold seat successful for schedule3-part3: AA002. Transaction ID: " + tripId);
						// If the Hold request is successful, then send Confirm request.
						tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Confirm("AA001", tripId), 1000))
								.thenApply(response -> ((String) response)).toCompletableFuture().get();
						if (!Strings.BLANK.equals(tripId) && !Strings.FAIL.equals(tripId)) {
							LOG.debug("Confirm seat successful for schedule3-part1: AA001. Transaction ID: " + tripId);
							tripId = FutureConverters.toJava(Patterns.ask(caActor, new Confirm("CA002", tripId), 1000))
									.thenApply(response -> ((String) response)).toCompletableFuture().get();
							if (!Strings.BLANK.equals(tripId) && !Strings.FAIL.equals(tripId)) {
								LOG.debug("Confirm seat successful for schedule3-part2: CA002. Transaction ID: "
										+ tripId);
								tripId = FutureConverters
										.toJava(Patterns.ask(aaActor, new Confirm("AA002", tripId), 1000))
										.thenApply(response -> ((String) response)).toCompletableFuture().get();
								if (!Strings.BLANK.equals(tripId) && !Strings.FAIL.equals(tripId)) {
									LOG.debug("Confirm seat successful for schedule3-part3: AA002. Transaction ID: "
											+ tripId);
								} else {
									LOG.debug("Could not confirm seat for schedule3-part3: AA003. All schedules full.");
									tripId = Strings.BLANK;
									// now all holds should be released after some time and seats should be
									// adjusted.
								}
							} else {
								LOG.debug("Could not confirm seat for schedule3-part2: CA002. All schedules full.");
								tripId = Strings.BLANK;
								// now all holds should be released after some time and seats should be
								// adjusted.
							}
						} else {
							LOG.debug("Could not confirm seat for schedule3-part1: AA001. All schedules full.");
							tripId = Strings.BLANK;
							// now all holds should be released after some time and seats should be
							// adjusted.
						}
					} else {
						LOG.debug("Could not hold seat for schedule3-part3: AA002. All schedules full.");
						// now hold should be released for part 1 and 2 after some time and seats should
						// be adjusted.
					}
				} else {
					LOG.debug("Could not hold seat for schedule3-part2: CA002. All schedules full.");
					// now hold should be released for part 1 after some time and seats should be
					// adjusted.
				}
			} else {
				LOG.debug("Could not hold seat for schedule3-part1: AA001. All schedules full.");
			}
		} catch (Exception e) {
			LOG.error("Error while trying to Book AA001+CA002+AA002");
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}

	/**
	 * Method to send a hold request to the given airline actor or the given flight
	 * 
	 * @param actor
	 *            the airline actor
	 * @param flight
	 *            the flight name
	 * @return
	 * @throws Exception
	 */
	private String sendHoldRequest(ActorRef actor, String flight) throws Exception {
		// catch the timeout exception here to do error handling for the hold request
		// timeout
		return FutureConverters.toJava(Patterns.ask(actor, new Hold(flight), 1000))
				.thenApply(response -> ((String) response)).toCompletableFuture().get();
	}

	/**
	 * Method to send a confirm request to the given airline actor or the given
	 * flight
	 * 
	 * @param actor
	 *            the airline actor
	 * @param flight
	 *            the flight name
	 * @param tripId
	 *            the tripId from the successful hold request.
	 * @return
	 * @throws Exception
	 */
	private String sendConfirmRequest(ActorRef actor, String flight, String tripId) throws Exception {
		// catch the timeout exception here to do error handling for the confirm request
		// timeout
		return FutureConverters.toJava(Patterns.ask(actor, new Confirm(flight, tripId), 1000))
				.thenApply(response -> ((String) response)).toCompletableFuture().get();
	}

	/**
	 * This method tries to book a schedule based on 2PC protocol. This is an
	 * optimized method to avoid the multiple if-else blocks when we are trying to
	 * book each flight manually separately. Currently this method is not being
	 * used, because it has not been tested properly.
	 * 
	 * @param schedule
	 *            the schedule of flights to be booked.
	 * @return
	 */
	private String tryBookSchedule(String[] schedule) {
		LOG.debug("Trying to book schedule: " + printArray(schedule));
		String tripId = Strings.BLANK;
		try {
			// try hold for all flights
			for (String flight : schedule) {
				tripId = sendHoldRequest(getActor(flight), flight);
				if (Strings.BLANK.equals(tripId)) {
					LOG.debug("Could not Hold flight: " + flight);
					break;
					// at this points, all previous holds can be released after a while.
				} else {
					LOG.debug("Hold successful for flight: " + flight + ". Transaction ID: " + tripId);
				}
			}
			if (!Strings.BLANK.equals(tripId)) {
				// try confirm for all flights
				for (String flight : schedule) {
					tripId = sendConfirmRequest(getActor(flight), flight, tripId);
					if (Strings.BLANK.equals(tripId) || Strings.FAIL.equals(tripId)) {
						LOG.debug("Could not Confirm flight: " + flight);
						break;
						// at this points, all previous holds can be released after a while.
					} else {
						LOG.debug("Confirm successful for flight: " + flight + ". Transaction ID: " + tripId);
					}
				}
			}
		} catch (Exception e) {
			LOG.debug("Error while booking schedule: " + printArray(schedule));
			e.printStackTrace();
			tripId = Strings.BLANK;
		}
		return tripId;
	}

	/**
	 * helper method to get the actor from the flight name. Currently unused.
	 * 
	 * @param flight
	 *            the flight name
	 * @return
	 */
	private ActorRef getActor(String flight) {
		if (flight.startsWith(Strings.AA)) {
			return this.aaActor;
		}
		if (flight.startsWith(Strings.BA)) {
			return this.baActor;
		}
		if (flight.startsWith(Strings.CA)) {
			return this.caActor;
		}
		return null;
	}

	/**
	 * Helper method to print the array of schedule. Currently unused.
	 * 
	 * @param array
	 *            the array to be printed
	 * @return
	 */
	private String printArray(String[] array) {
		String stringValue = Strings.BLANK;
		if (array != null && array.length > 0) {
			for (String item : array) {
				stringValue += Strings.SPACE + item;
			}
		}
		return stringValue.trim();
	}

}
