package controllers;

import play.mvc.Controller;
import play.mvc.Result;

/**
 * This controller contains an action to handle HTTP requests to the
 * application's home page.
 */
public class HomeController extends Controller {

	/**
	 * An action that renders an HTML page with a welcome message. The configuration
	 * in the <code>routes</code> file means that this method will be called when
	 * the application receives a <code>GET</code> request with a path of
	 * <code>/</code>.
	 */
	public Result index() {
		return ok(views.html.index.render());
	}

	public Result getSeats(String operator, String flight) {
		return ok("operator: " + operator + ", flight: " + flight);
	}

	public Result getTrips() {
		return ok("trips: a, b, c, d, e");
	}

	public Result getSegments(String tripID) {
		return ok("tripID: " + tripID);
	}

	public Result getOperators() {
		return ok("operators: a, b, c, d, e");
	}

	public Result getFlights(String operator) {
		return ok("operator: " + operator);
	}

	public Result bookTrip(String from, String to) {
		return ok("from: " + from + ", to: " + to);
	}

}
