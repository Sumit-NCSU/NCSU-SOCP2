package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import play.Logger;
import play.Logger.ALogger;
import play.db.Database;
import util.Strings;

/**
 * @author srivassumit
 *
 */
public class DatabaseService {

	private static final ALogger LOG = Logger.of(DatabaseService.class);
	private Database db;
	private DatabaseExecutionContext executionContext;

	@Inject
	public DatabaseService(Database db, DatabaseExecutionContext executionContext) {
		LOG.debug("Initializing Database service");
		this.db = db;
		this.executionContext = executionContext;
		initializeDatabase();
		LOG.debug("Initialized Database service");
	}

	/**
	 * This method initializes the database if it doesn't already exist.
	 */
	public void initializeDatabase() {
		LOG.debug("Initializing database");
		Connection con = null;
		try {
			con = db.getConnection();
			if (createFlights(con, flightsExists(con))) {
				LOG.debug("Successfully initialized flights");
			}
			if (createBookings(con)) {
				LOG.debug("Successfully initialized bookings");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Error while initializing database");
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the database connection from initialize database method");
					e.printStackTrace();
				}
			}
		}
		LOG.debug("Initialized database");
	}

	public List<String> fetchTrips() {
		LOG.debug("Fetching Trips");
		List<String> tripIds = new ArrayList<String>();
		String selectQuery = "SELECT id from bookings";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement selectStatement = con.prepareStatement(selectQuery)) {
				ResultSet rs = selectStatement.executeQuery();
				while (rs.next()) {
					tripIds.add(rs.getString("id"));
				}
			} catch (Exception e) {
				LOG.error("Error while fetching trips.");
				e.printStackTrace();
				return tripIds;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching trips.");
			e.printStackTrace();
			return tripIds;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from fetch trips method");
					e.printStackTrace();
				}
			}
		}
		return tripIds;
	}

	public String fetchSegments(String tripId) {
		LOG.debug("Fetching Segments for Trip: " + tripId);
		String segments = null;
		String selectQuery = "SELECT schedule FROM bookings WHERE id = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement selectStatement = con.prepareStatement(selectQuery)) {
				selectStatement.setString(1, tripId);
				ResultSet rs = selectStatement.executeQuery();
				if (rs.next()) {
					segments = rs.getString("schedule");
				} else {
					LOG.debug("No segments founds for trip: " + tripId);
				}
			} catch (Exception e) {
				LOG.error("Error while fetching segments.");
				e.printStackTrace();
				return null;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching segments.");
			e.printStackTrace();
			return null;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from fetch segments method");
					e.printStackTrace();
				}
			}
		}
		return segments;
	}

	public List<String> fetchOperators() {
		LOG.debug("Fetching Operators");
		List<String> operators = new ArrayList<String>();
		String selectQuery = "SELECT DISTINCT operator as name from flights";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement selectStatement = con.prepareStatement(selectQuery)) {
				ResultSet rs = selectStatement.executeQuery();
				while (rs.next()) {
					operators.add(rs.getString("name"));
				}
			} catch (Exception e) {
				LOG.error("Error while fetching operators.");
				e.printStackTrace();
				return operators;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching operators.");
			e.printStackTrace();
			return operators;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from fetch operators method");
					e.printStackTrace();
				}
			}
		}
		return operators;
	}

	public List<String> fetchOperatorFlights(String operator) {
		LOG.debug("Fetching Flights for operator: " + operator);
		List<String> operatorFlights = new ArrayList<String>();
		String selectQuery = "SELECT flights.name name from flights where operator = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement selectStatement = con.prepareStatement(selectQuery)) {
				selectStatement.setString(1, operator);
				ResultSet rs = selectStatement.executeQuery();
				while (rs.next()) {
					operatorFlights.add(rs.getString("name"));
				}
			} catch (Exception e) {
				LOG.error("Error while fetching flights operated by operator: " + operator);
				e.printStackTrace();
				return operatorFlights;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching flights operated by operator: " + operator);
			e.printStackTrace();
			return operatorFlights;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from fetch operator flights method");
					e.printStackTrace();
				}
			}
		}
		return operatorFlights;
	}

	public int fetchAvailableSeats(String operator, String flight) {
		LOG.debug("Fetching available seats for flight: " + flight + " operated by: " + operator);
		int availableSeats = 0;
		String selectQuery = "SELECT available_seats from flights where operator = ? and name = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement selectStatement = con.prepareStatement(selectQuery)) {
				selectStatement.setString(1, operator);
				selectStatement.setString(2, flight);
				ResultSet rs = selectStatement.executeQuery();
				if (rs.next()) {
					availableSeats = rs.getInt("available_seats");
				} else {
					LOG.debug("No seats available for flight: " + flight + ", operated by: " + operator);
					return 0;
				}
			} catch (Exception e) {
				LOG.error("Error while fetching seats for flight: " + flight + ", operated by operator: " + operator);
				e.printStackTrace();
				return availableSeats;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching seats for flight: " + flight + ", operated by operator: " + operator);
			e.printStackTrace();
			return availableSeats;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from fetch available seats method");
					e.printStackTrace();
				}
			}
		}
		return availableSeats;
	}

	/**
	 * This method will try to hold a seat in the database for a given flight,
	 * operated by a given airline. If no such flight is found, or if there are no
	 * available seats, the method will return false.
	 * 
	 * @param airline
	 *            the airline
	 * @param flight
	 *            the flight name
	 * @param holdTime
	 *            the time at which the hold request is received.
	 * @return
	 */
	public String tryHoldSeat(String airline, String flight) {
		LOG.debug("Trying to hold a seat for flight: " + flight + ", operated by: " + airline);
		String tripId = String.valueOf(System.currentTimeMillis());
		Connection con = null;
		try {
			con = db.getConnection();
			// check if seat is available
			String selectQuery = "SELECT available_seats FROM flights WHERE operator=? AND name=?";
			PreparedStatement selectStatement = con.prepareStatement(selectQuery);
			selectStatement.setString(1, airline);
			selectStatement.setString(2, flight);
			ResultSet rs = selectStatement.executeQuery();
			int availableSeats = 0;
			if (rs.next()) {
				availableSeats = rs.getInt("available_seats");
			}
			LOG.debug("There are " + availableSeats + " available for flight: " + flight + ", operated by: " + airline);
			if (availableSeats > 0) {
				// hold a seat
				String holdSeat = "UPDATE flights set available_seats = available_seats-1, hold_time=? where operator=? AND name=?";
				PreparedStatement updateStatement = con.prepareStatement(holdSeat);
				updateStatement.setString(1, tripId);
				updateStatement.setString(2, airline);
				updateStatement.setString(3, flight);
				updateStatement.executeUpdate();
			} else {
				LOG.debug("No seats available for flight: " + flight + ", operated by: " + airline);
				return Strings.BLANK;
			}
		} catch (Exception e) {
			LOG.error("Error while trying to hold a seat for flight: " + flight + ", operated by: " + airline);
			e.printStackTrace();
			return Strings.BLANK;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from try hold seat method");
					e.printStackTrace();
				}
			}
		}
		return tripId;
	}

	/**
	 * This method confirms the seats in the database. It also clears the hold flag
	 * in the database once the confirm is complete.
	 * 
	 * @param airline
	 *            the airline operator
	 * @param flight
	 *            the flight name
	 * @param tripId
	 *            the trip ID same as the transaction ID which was used to hold the
	 *            seats.
	 * @return
	 */
	public String tryConfirmSeat(String airline, String flight, String tripId) {
		LOG.debug("Confirming a seat for flight " + flight + " by airline: " + airline + " for transaction ID: "
				+ tripId);
		Connection con = null;
		try {
			con = db.getConnection();
			String selectQuery = "SELECT count(*) as booked from bookings where id = ?";
			PreparedStatement selectStatement = con.prepareStatement(selectQuery);
			selectStatement.setLong(1, Long.valueOf(tripId));
			ResultSet rs = selectStatement.executeQuery();
			boolean entryExists = false;
			if (rs.next()) {
				entryExists = (rs.getInt("booked") > 0);
			}
			int status = 0;
			if (entryExists) {
				// String concatenation in Sqlite is ||
				String updateQuery = "UPDATE bookings set schedule=schedule || ? where id = ?";
				PreparedStatement updateStatement = con.prepareStatement(updateQuery);
				updateStatement.setString(1, flight.concat(Strings.SPACE));
				updateStatement.setLong(2, Long.valueOf(tripId));
				status = updateStatement.executeUpdate();
			} else {
				String insertQuery = "INSERT INTO bookings (id, schedule) values (?, ?)";
				PreparedStatement insertStatement = con.prepareStatement(insertQuery);
				insertStatement.setLong(1, Long.valueOf(tripId));
				insertStatement.setString(2, flight.concat(Strings.SPACE));
				status = insertStatement.executeUpdate();
			}
			if (status > 0) {
				LOG.debug("Confirm seat successfully");
				// reset the hold flag of the flight.
				String updateQuery = "UPDATE flights set hold_time=null where name = ?";
				PreparedStatement updateStatement = con.prepareStatement(updateQuery);
				updateStatement.setString(1, flight);
				status = updateStatement.executeUpdate();
				LOG.debug("The previous hold flag was released");
			} else {
				LOG.debug("Could not confirm seat for flight: " + flight + ", with transactionID: " + tripId);
				tripId = Strings.BLANK;
			}
		} catch (Exception e) {
			LOG.error("Error while trying to confirm a seat for flight: " + flight + ", with transactionID: " + tripId);
			e.printStackTrace();
			tripId = Strings.BLANK;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the connection from try confirm seat method");
					e.printStackTrace();
				}
			}
		}
		return tripId;
	}

	/**
	 * This method is used to reset the database back to its original configuration
	 * 
	 * @return
	 */
	public boolean resetDatabase() {
		LOG.debug("Resetting the database");
		boolean status = true;
		String dropBookings = "DROP TABLE bookings";
		String dropFlights = "DROP TABLE flights";
		Connection con = null;
		try {
			con = db.getConnection();
			Statement st = con.createStatement();
			st.executeUpdate(dropBookings);
			st.executeUpdate(dropFlights);
			createFlights(con, true);
			createBookings(con);
		} catch (Exception e) {
			e.printStackTrace();
			status = false;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the database connection from reset database method");
					e.printStackTrace();
				}
			}
		}
		return status;
	}

	public String getHoldTime(String operator, String flight) {
		LOG.debug("Fetching the hold time from database for flight: " + flight);
		Connection con = null;
		try {
			con = db.getConnection();
			String selectQuery = "SELECT hold_time from flights where name = ? and operator=?";
			PreparedStatement selectStatement = con.prepareStatement(selectQuery);
			selectStatement.setString(1, flight);
			selectStatement.setString(2, operator);
			ResultSet rs = selectStatement.executeQuery();
			if (rs.next()) {
				return rs.getString("hold_time");
			} else {
				LOG.debug("No records found in database");
				return null;
			}
		} catch (Exception e) {
			LOG.error("Error while fetching the hold time from database for flight: " + flight);
			e.printStackTrace();
			return null;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the database connection from get hold time method");
					e.printStackTrace();
				}
			}
		}
	}

	public void releaseHold(String operator, String flight) {
		LOG.debug("Releasing hold for flight: " + flight);
		Connection con = null;
		try {
			con = db.getConnection();
			String holdSeat = "UPDATE flights set available_seats = available_seats+1, hold_time=null where operator=? AND name=?";
			PreparedStatement updateStatement = con.prepareStatement(holdSeat);
			updateStatement.setString(1, operator);
			updateStatement.setString(2, flight);
			updateStatement.executeUpdate();
		} catch (Exception e) {
			LOG.error("Error while releasing the hold for flight: " + flight);
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					LOG.error("Error while closing the database connection from release hold method");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This method checks if flights table already exists.
	 * 
	 * @param con
	 *            the database connection
	 * @return
	 */
	private boolean flightsExists(Connection con) {
		LOG.debug("Checking existing tables");
		String checkTables = "SELECT name FROM sqlite_master WHERE type='table' AND name = 'flights'";
		try (PreparedStatement pstmt = con.prepareStatement(checkTables)) {
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return !"flights".equalsIgnoreCase(rs.getString("name"));
			} else {
				LOG.debug("No records found in database.");
				return false;
			}
		} catch (Exception e) {
			LOG.error("Error while checking existing tables.");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @param con
	 * @param insertRecords
	 * @return
	 */
	private boolean createFlights(Connection con, boolean insertRecords) {
		LOG.debug("Creating Flights table if it does not exist");
		String createTable = "CREATE TABLE IF NOT EXISTS flights (id integer PRIMARY KEY, name text NOT NULL, operator text, total_seats integer, available_seats integer, route text, hold_time text)";
		boolean status = false;
		try {
			Statement stmt = con.createStatement();
			stmt.execute(createTable);
			status = true;
			if (insertRecords) {
				LOG.debug("Created Flights table");
				LOG.debug("Inserting Data into Flights");
				List<String> insertStatements = new ArrayList<String>();
				insertStatements.add(
						"INSERT INTO flights (name, operator, total_seats, available_seats, route) values ('AA001','AA',3,3,'X-Z')");
				insertStatements.add(
						"INSERT INTO flights (name, operator, total_seats, available_seats, route) values ('AA002','AA',1,1,'W-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator, total_seats, available_seats, route) values ('BA001','BA',1,1,'Z-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator, total_seats, available_seats, route) values ('CA001','CA',1,1,'X-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator, total_seats, available_seats, route) values ('CA002','CA',1,1,'Z-W')");
				for (String insertStatement : insertStatements) {
					try (PreparedStatement pstmt = con.prepareStatement(insertStatement)) {
						int recordsInserted = pstmt.executeUpdate();
						status = status && (recordsInserted > 0);
					} catch (Exception e) {
						LOG.error("Error while inserting data for flights, for Insert statement: " + insertStatement);
						e.printStackTrace();
					}
				}
			} else {
				LOG.debug("Flights table is already created");
			}
		} catch (SQLException e) {
			LOG.error("Error while creating flights");
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * 
	 * @param con
	 * @return
	 */
	private boolean createBookings(Connection con) {
		LOG.debug("Creating Table Bookings if it does not exist.");
		String createTable = "CREATE TABLE IF NOT EXISTS bookings (id integer PRIMARY KEY, schedule text)";
		try (Statement stmt = con.createStatement()) {
			stmt.execute(createTable);
			return true;
		} catch (SQLException e) {
			LOG.error("Error while creating bookings");
			e.printStackTrace();
		}
		return false;
	}

}
