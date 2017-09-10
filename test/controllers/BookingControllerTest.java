package controllers;

import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.route;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.DatabaseService;

/**
 * @author srivassumit
 *
 */
public class BookingControllerTest extends WithApplication {

	DatabaseService databaseService = Mockito.mock(DatabaseService.class);

	@Override
	protected Application provideApplication() {
		return new GuiceApplicationBuilder().build();
	}

	@Test
	public void testGetTrips() {
		Http.RequestBuilder request = new Http.RequestBuilder().method(GET).uri("/trips");
		List<String> returnList = new ArrayList<String>();
		Mockito.when(databaseService.fetchTrips()).thenReturn(returnList);

		Result result = route(app, request);
		assertEquals(OK, result.status());
	}

	@Test
	public void testGetSegments() {
		Http.RequestBuilder request = new Http.RequestBuilder().method(GET).uri("/trips/1234");

		Mockito.when(databaseService.fetchSegments(Mockito.anyString())).thenReturn("segments");

		Result result = route(app, request);
		assertEquals(OK, result.status());
	}

	@Test
	public void testGetOperators() {
		Http.RequestBuilder request = new Http.RequestBuilder().method(GET).uri("/operators");
		List<String> returnList = new ArrayList<String>();
		Mockito.when(databaseService.fetchOperators()).thenReturn(returnList);

		Result result = route(app, request);
		assertEquals(OK, result.status());
	}

	@Test
	public void testGetFlights() {
		Http.RequestBuilder request = new Http.RequestBuilder().method(GET).uri("/operators/AA/flights");
		List<String> returnList = new ArrayList<String>();
		Mockito.when(databaseService.fetchOperatorFlights(Mockito.anyString())).thenReturn(returnList);

		Result result = route(app, request);
		assertEquals(OK, result.status());
	}

	@Test
	public void testGetSeats() {
		Http.RequestBuilder request = new Http.RequestBuilder().method(GET).uri("/operators/AA/flights/AA001");

		Mockito.when(databaseService.fetchAvailableSeats(Mockito.anyString(), Mockito.anyString())).thenReturn(1);

		Result result = route(app, request);
		assertEquals(OK, result.status());
	}
}
