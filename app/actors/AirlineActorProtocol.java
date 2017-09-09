package actors;

/**
 * @author srivassumit
 *
 */
public class AirlineActorProtocol {

	public static class Hold {
		public final String flight;

		public Hold(String flight) {
			this.flight = flight;
		}
	}

	public static class Confirm {
		public final String flight;
		public final String tripId;

		public Confirm(String flight, String tripId) {
			this.flight = flight;
			this.tripId = tripId;
		}
	}

	public static class DebugFlag {
		public final String message;

		public DebugFlag(String message) {
			this.message = message;
		}
	}
}
