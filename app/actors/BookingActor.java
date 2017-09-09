package actors;

import actors.AirlineActorProtocol.Confirm;
import actors.AirlineActorProtocol.Hold;
import actors.BookingActorProtocol.BookFlight;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import scala.compat.java8.FutureConverters;
import services.DatabaseService;

/**
 * @author srivassumit
 *
 */
public class BookingActor extends AbstractActor {

	final ActorRef aaActor, baActor, caActor;
	final DatabaseService databaseService;

	public BookingActor(ActorRef aaActor, ActorRef baActor, ActorRef caActor, DatabaseService databaseService) {
		this.aaActor = aaActor;
		this.baActor = baActor;
		this.caActor = caActor;
		this.databaseService = databaseService;
	}

	public static Props getProps(ActorRef aaActor, ActorRef baActor, ActorRef caActor,
			DatabaseService databaseService) {
		return Props.create(BookingActor.class, () -> new BookingActor(aaActor, baActor, caActor, databaseService));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(BookFlight.class, bookFlight -> {
			String reply = "The booking actor has recieved a book flight request from " + bookFlight.from + ", to "
					+ bookFlight.to;
			// Try to book CA001
			String tripId = tryBookCA001(bookFlight.from, bookFlight.to);
			if ("".equals(tripId)) {
				// If CA001 is unavailable then, try to book AA001+BA001
				tripId = tryBookAA001BA001(bookFlight.from, bookFlight.to);
				if ("".equals(tripId)) {
					// If AA001+BA001 is unavailable then try to book AA001+CA002+AA002
					tripId = tryBookAA001CA002AA002(bookFlight.from, bookFlight.to);
				}
			}
			if ("".equals(tripId)) {
				reply = "Erorr, no seats available in any of the flights from " + bookFlight.from + " to "
						+ bookFlight.to;
			} else {
				reply = tripId;
			}
			sender().tell(reply, self());
		}).build();
	}

	/**
	 * This method tries to book a schedule based on 2PC protocol
	 * 
	 * @param schedule
	 *            the schedule of flights to be booked.
	 * @return
	 */
	private String tryBookSchedule(String[] schedule) {
		System.out.println("Trying to book schedule: " + printArray(schedule));
		String tripId = "";
		try {
			// try hold for all flights
			for (String flight : schedule) {
				tripId = sendHoldRequest(getActor(flight), flight);
				if ("".equals(tripId)) {
					System.out.println("Could not Hold flight: " + flight);
					break;
					// at this points, all previous holds can be released after a while.
				} else {
					System.out.println("Hold successful for flight: " + flight + ", the transaction ID is: " + tripId);
				}
			}
			if (!"".equals(tripId)) {
				// try confirm for all flights
				for (String flight : schedule) {
					tripId = sendConfirmRequest(getActor(flight), flight, tripId);
					if ("".equals(tripId)) {
						System.out.println("Could not Hold flight: " + flight);
						break;
						// at this points, all previous holds can be released after a while.
					} else {
						System.out.println(
								"Confirm successful for flight: " + flight + ", the transaction ID is: " + tripId);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error while booking schedule: " + printArray(schedule));
			e.printStackTrace();
			tripId = "";
		}
		return tripId;
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	private String tryBookCA001(String from, String to) {
		System.out.println("Trying to Book Schedule1: CA001");
		String tripId = "";
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(caActor, new Hold("CA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!"".equals(tripId)) {
				System.out.println("Hold seat successful for schedule1: CA001. The transaction ID is: " + tripId);
				// If the Hold request is successful, then send Confirm request.
				tripId = FutureConverters.toJava(Patterns.ask(caActor, new Confirm("CA001", tripId), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
			} else {
				System.out.println("Could not hold seat for schedule1: CA001. Try Schedule 2.");
			}
		} catch (Exception e) {
			System.out.println("Error while trying to Book CA001");
			e.printStackTrace();
			tripId = "";
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
		System.out.println("Trying to Book Schedule2: AA001+BA001");
		String tripId = "";
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!"".equals(tripId)) {
				System.out.println("Hold seat successful for schedule2-part1: AA001. The transaction ID is: " + tripId);
				tripId = FutureConverters.toJava(Patterns.ask(baActor, new Hold("BA001"), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
				if (!"".equals(tripId)) {
					System.out.println(
							"Hold seat successful for schedule2-part2: BA001. The transaction ID is: " + tripId);
					// If the Hold request is successful, then send Confirm request.
					tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Confirm("AA001", tripId), 1000))
							.thenApply(response -> ((String) response)).toCompletableFuture().get();
					if (!"".equals(tripId)) {
						System.out.println(
								"Confirm seat successful for schedule2-part1: AA001. The transaction ID is: " + tripId);
						tripId = FutureConverters.toJava(Patterns.ask(baActor, new Confirm("BA001", tripId), 1000))
								.thenApply(response -> ((String) response)).toCompletableFuture().get();
						if ("".equals(tripId)) {
							System.out.println("Could not confirm seat for schedule2-part2: BA001. Try Schedule 3.");
							tripId = "";
							// now both holds should be released after some time.
						} else {
							System.out.println(
									"Confirm seat successful for schedule2-part2: BA001. The transaction ID is: "
											+ tripId);
						}
					} else {
						System.out.println("Could not confirm seat for schedule2-part1: AA001. Try Schedule 3.");
						tripId = "";
						// now both holds should be released after some time.
					}
				} else {
					System.out.println("Could not hold seat for schedule2-part2: BA001. Try Schedule 3.");
					// now schedule2-part1 hold should be released after some time.
				}
			} else {
				System.out.println("Could not hold seat for schedule2-part1: AA001. Try Schedule 3.");
			}
		} catch (Exception e) {
			System.out.println("Error while trying to Book AA001+BA001");
			e.printStackTrace();
			tripId = "";
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
		System.out.println("Trying to Book Schedule3: AA001+CA002+AA002");
		String tripId = "";
		// Use 2PC for booking.
		try {
			// Sending Hold request for all segments.
			tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA001"), 1000))
					.thenApply(response -> ((String) response)).toCompletableFuture().get();
			if (!"".equals(tripId)) {
				System.out.println("Hold seat successful for schedule3-part1: AA001. The transaction ID is: " + tripId);
				tripId = FutureConverters.toJava(Patterns.ask(caActor, new Hold("CA002"), 1000))
						.thenApply(response -> ((String) response)).toCompletableFuture().get();
				if (!"".equals(tripId)) {
					System.out.println(
							"Hold seat successful for schedule3-part2: CA002. The transaction ID is: " + tripId);
					tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Hold("AA002"), 1000))
							.thenApply(response -> ((String) response)).toCompletableFuture().get();
					if (!"".equals(tripId)) {
						System.out.println(
								"Hold seat successful for schedule3-part3: AA002. The transaction ID is: " + tripId);
						// If the Hold request is successful, then send Confirm request.
						tripId = FutureConverters.toJava(Patterns.ask(aaActor, new Confirm("AA001", tripId), 1000))
								.thenApply(response -> ((String) response)).toCompletableFuture().get();
						if (!"".equals(tripId)) {
							System.out.println(
									"Confirm seat successful for schedule3-part1: AA001. The transaction ID is: "
											+ tripId);
							tripId = FutureConverters.toJava(Patterns.ask(caActor, new Confirm("CA002", tripId), 1000))
									.thenApply(response -> ((String) response)).toCompletableFuture().get();
							if (!"".equals(tripId)) {
								System.out.println(
										"Confirm seat successful for schedule3-part2: CA002. The transaction ID is: "
												+ tripId);
								tripId = FutureConverters
										.toJava(Patterns.ask(aaActor, new Confirm("AA002", tripId), 1000))
										.thenApply(response -> ((String) response)).toCompletableFuture().get();
								if (!"".equals(tripId)) {
									System.out.println(
											"Confirm seat successful for schedule3-part3: AA002. The transaction ID is: "
													+ tripId);
								} else {
									System.out.println(
											"Could not confirm seat for schedule3-part3: AA003. All schedules full.");
									tripId = "";
								}
							} else {
								System.out.println(
										"Could not confirm seat for schedule3-part2: CA002. All schedules full.");
							}
						} else {
							System.out
									.println("Could not confirm seat for schedule3-part1: AA001. All schedules full.");
						}
					} else {
						System.out.println("Could not hold seat for schedule3-part3: AA002. All schedules full.");
					}
				} else {
					System.out.println("Could not hold seat for schedule3-part3: CA002. All schedules full.");
				}
			} else {
				System.out.println("Could not hold seat for schedule3-part1: AA001. All schedules full.");
			}
		} catch (Exception e) {
			System.out.println("Error while trying to Book AA001+CA002+AA002");
			e.printStackTrace();
			tripId = "";
		}
		return tripId;
	}

	private String sendHoldRequest(ActorRef actor, String flight) throws Exception {
		return FutureConverters.toJava(Patterns.ask(actor, new Hold(flight), 1000))
				.thenApply(response -> ((String) response)).toCompletableFuture().get();
	}

	private String sendConfirmRequest(ActorRef actor, String flight, String tripId) throws Exception {
		return FutureConverters.toJava(Patterns.ask(actor, new Confirm(flight, tripId), 1000))
				.thenApply(response -> ((String) response)).toCompletableFuture().get();
	}

	private ActorRef getActor(String flight) {
		if (flight.startsWith("AA")) {
			return this.aaActor;
		}
		if (flight.startsWith("BA")) {
			return this.baActor;
		}
		if (flight.startsWith("CA")) {
			return this.caActor;
		}
		return null;
	}

	private String printArray(String[] array) {
		String stringValue = "";
		if (array != null && array.length > 0) {
			for (String item : array) {
				stringValue += " " + item;
			}
		}
		return stringValue.trim();
	}

}
