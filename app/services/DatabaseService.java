package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import play.db.Database;

/**
 * @author srivassumit
 *
 */
public class DatabaseService {

	private Database db;
	private DatabaseExecutionContext executionContext;

	@Inject
	public DatabaseService(Database db, DatabaseExecutionContext executionContext) {
		this.db = db;
		this.executionContext = executionContext;
	}

	/**
	 * This method initializes the database if it doesn't already exist.
	 */
	public void initializeDatabase() {
		Connection con = null;
		try {
			con = db.getConnection();
			if (createFlights(con, flightsExists(con))) {
				System.out.println("Successfully initialized flights");
			}
			if (createBookings(con)) {
				System.out.println("Successfully initialized bookings");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the database connection from initialize database method");
					e.printStackTrace();
				}
			}
		}
	}

	public List<String> fetchTrips() {
		List<String> tripIds = new ArrayList<String>();
		String fetchTripIdQuery = "SELECT id from bookings";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement pstmt = con.prepareStatement(fetchTripIdQuery)) {
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					tripIds.add(rs.getString("id"));
				}
			} catch (Exception e) {
				System.out.println("Error while fetching trips.");
				e.printStackTrace();
				return tripIds;
			}
		} catch (Exception e) {
			System.out.println("Error while fetching trips.");
			e.printStackTrace();
			return tripIds;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from fetch trips method");
					e.printStackTrace();
				}
			}
		}
		return tripIds;
	}

	public String fetchSegments(String tripId) {
		String segments = null;
		String fetchScheduleQuery = "SELECT schedule FROM bookings WHERE id = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement pstmt = con.prepareStatement(fetchScheduleQuery)) {
				pstmt.setString(1, tripId);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					segments = rs.getString("schedule");
				}
			} catch (Exception e) {
				System.out.println("Error while fetching segments.");
				e.printStackTrace();
				return null;
			}
		} catch (Exception e) {
			System.out.println("Error while fetching segments.");
			e.printStackTrace();
			return null;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from fetch segments method");
					e.printStackTrace();
				}
			}
		}
		return segments;
	}

	public List<String> fetchOperators() {
		List<String> operators = new ArrayList<String>();
		String fetchOperatorsQuery = "SELECT DISTINCT operator as name from flights";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement pstmt = con.prepareStatement(fetchOperatorsQuery)) {
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					operators.add(rs.getString("name"));
				}
			} catch (Exception e) {
				System.out.println("Error while fetching operators.");
				e.printStackTrace();
				return operators;
			}
		} catch (Exception e) {
			System.out.println("Error while fetching operators.");
			e.printStackTrace();
			return operators;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from fetch operators method");
					e.printStackTrace();
				}
			}
		}
		return operators;
	}

	public List<String> fetchOperatorFlights(String operator) {
		List<String> operatorFlights = new ArrayList<String>();
		String fetchOperatorFlightsQuery = "SELECT flights.name name from flights where operator = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement pstmt = con.prepareStatement(fetchOperatorFlightsQuery)) {
				pstmt.setString(1, operator);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					operatorFlights.add(rs.getString("name"));
				}
			} catch (Exception e) {
				System.out.println("Error while fetching flights operated by operator: " + operator);
				e.printStackTrace();
				return operatorFlights;
			}
		} catch (Exception e) {
			System.out.println("Error while fetching flights operated by operator: " + operator);
			e.printStackTrace();
			return operatorFlights;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from fetch operator flights method");
					e.printStackTrace();
				}
			}
		}
		return operatorFlights;
	}

	public int fetchAvailableSeats(String operator, String flight) {
		int availableSeats = 0;
		String fetchAvailableSeatsQuery = "SELECT available_seats from flights where operator = ? and name = ?";
		Connection con = null;
		try {
			con = db.getConnection();
			try (PreparedStatement pstmt = con.prepareStatement(fetchAvailableSeatsQuery)) {
				pstmt.setString(1, operator);
				pstmt.setString(2, flight);
				ResultSet rs = pstmt.executeQuery();
				while (rs.next()) {
					availableSeats = rs.getInt("available_seats");
				}
			} catch (Exception e) {
				System.out.println(
						"Error while fetching seats for flight: " + flight + ", operated by operator: " + operator);
				e.printStackTrace();
				return availableSeats;
			}
		} catch (Exception e) {
			System.out.println(
					"Error while fetching seats for flight: " + flight + ", operated by operator: " + operator);
			e.printStackTrace();
			return availableSeats;
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from fetch available seats method");
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
		System.out.println("Trying to hold a seat for flight: " + flight + ", operated by: " + airline);
		String tripId = String.valueOf(System.currentTimeMillis());
		Connection con = null;
		try {
			con = db.getConnection();
			// check if seat is available
			String checkSeats = "SELECT available_seats FROM flights WHERE operator=? AND name=?";
			PreparedStatement selectStatement = con.prepareStatement(checkSeats);
			selectStatement.setString(1, airline);
			selectStatement.setString(2, flight);
			ResultSet rs = selectStatement.executeQuery();
			rs.next();
			int availableSeats = rs.getInt("available_seats");
			System.out.println(
					"There are " + availableSeats + " available for flight: " + flight + ", operated by: " + airline);
			if (availableSeats > 0) {
				// hold a seat
				String holdSeat = "UPDATE flights set available_seats = available_seats-1, hold_time=? where operator=? AND name=?";
				PreparedStatement updateStatement = con.prepareStatement(holdSeat);
				updateStatement.setString(1, tripId);
				updateStatement.setString(2, airline);
				updateStatement.setString(3, flight);
				updateStatement.executeUpdate();
			} else {
				System.out.println("No seats available for flight: " + flight + ", operated by: " + airline);
				return "";
			}
		} catch (Exception e) {
			System.out.println("Error while trying to hold a seat for flight: " + flight + ", operated by: " + airline);
			e.printStackTrace();
			return "";
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from try hold seat method");
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
		Connection con = null;
		try {
			con = db.getConnection();
			String selectQuery = "SELECT count(*) as booked from bookings where id = ?";
			PreparedStatement selectStatement = con.prepareStatement(selectQuery);
			selectStatement.setLong(1, Long.valueOf(tripId));
			ResultSet rs = selectStatement.executeQuery();
			rs.next();
			boolean entryExists = (rs.getInt("booked") > 0);
			int status = 0;
			if (entryExists) {
				// String concatenation in Sqlite is ||
				String updateQuery = "UPDATE bookings set schedule=schedule || ? where id = ?";
				PreparedStatement updateStatement = con.prepareStatement(updateQuery);
				updateStatement.setString(1, flight.concat(" "));
				updateStatement.setLong(2, Long.valueOf(tripId));
				status = updateStatement.executeUpdate();
			} else {
				String insertQuery = "INSERT INTO bookings (id, schedule) values (?, ?)";
				PreparedStatement insertStatement = con.prepareStatement(insertQuery);
				insertStatement.setLong(1, Long.valueOf(tripId));
				insertStatement.setString(2, flight.concat(" "));
				status = insertStatement.executeUpdate();
			}
			if (status > 0) {
				System.out.println("Confirm seat successfully");
				// reset the hold flag of the flight.
				String updateQuery = "UPDATE flights set hold_time=null where name = ?";
				PreparedStatement updateStatement = con.prepareStatement(updateQuery);
				updateStatement.setString(1, flight);
				status = updateStatement.executeUpdate();
				System.out.println("The previous hold flag was released");
			} else {
				System.out.println("Could not confirm seat for flight: " + flight + ", with transactionID: " + tripId);
				tripId = "";
			}
		} catch (Exception e) {
			System.out.println(
					"Error while trying to confirm a seat for flight: " + flight + ", with transactionID: " + tripId);
			e.printStackTrace();
			tripId = "";
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					System.out.println("Error while closing the connection from try confirm seat method");
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
					System.out.println("Error while closing the database connection from reset database method");
					e.printStackTrace();
				}
			}
		}
		return status;
	}

	/**
	 * This method checks if flights table already exists.
	 * 
	 * @param con
	 *            the database connection
	 * @return
	 */
	private boolean flightsExists(Connection con) {
		String checkTables = "SELECT name FROM sqlite_master WHERE type='table' AND name = 'flights'";
		try (PreparedStatement pstmt = con.prepareStatement(checkTables)) {
			ResultSet rs = pstmt.executeQuery();
			rs.next();
			return !"flights".equalsIgnoreCase(rs.getString("name"));
		} catch (Exception e) {
			System.out.println("Error while fetching table list.");
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
		String createTable = "CREATE TABLE IF NOT EXISTS flights (id integer PRIMARY KEY, name text NOT NULL, operator text, total_seats integer, available_seats integer, route text, hold_time text)";
		boolean status = false;
		try {
			Statement stmt = con.createStatement();
			stmt.execute(createTable);
			status = true;
			if (insertRecords) {
				System.out.println("Created Table Flights");
				System.out.println("Inserting Data into Flights");
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
						System.out.println(
								"Error while inserting data for flights, for Insert statement: " + insertStatement);
						e.printStackTrace();
					}
				}
			} else {
				System.out.println("Flights table is already created");
			}
		} catch (SQLException e) {
			System.out.println("Error while creating flights");
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
		System.out.println("Creating Table Bookings if not already created.");
		String createTable = "CREATE TABLE IF NOT EXISTS bookings (id integer PRIMARY KEY, schedule text)";
		try (Statement stmt = con.createStatement()) {
			stmt.execute(createTable);
			return true;
		} catch (SQLException e) {
			System.out.println("Error while creating bookings");
			e.printStackTrace();
		}
		return false;
	}

}
