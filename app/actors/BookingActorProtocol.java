package actors;

/**
 * @author srivassumit
 *
 */
public class BookingActorProtocol {
	public static class BookFlight {
		public final String from;
		public final String to;

		public BookFlight(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}
}
