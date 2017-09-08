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

			List<String> tableList = getTables(con);
			boolean insertOperators = true, insertFlights = true;
			for (String item : tableList) {
				System.out.println("Table Entry : " + item);
			}
			if (tableList.contains("operators")) {
				insertOperators = false;
			} else {
				System.out.println("Will insert operators");
			}
			if (tableList.contains("flights")) {
				insertFlights = false;
			} else {
				System.out.println("Will insert flights");
			}
			if (createOperators(con, insertOperators)) {
				System.out.println("Successfully initialized operators");
			}
			if (createFlights(con, insertFlights)) {
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
		String fetchOperatorsQuery = "SELECT name from operators";
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
		String fetchOperatorFlightsQuery = "SELECT flights.name name from flights where operator_id in (select id from operators where name = ?)";
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
		String fetchAvailableSeatsQuery = "SELECT available_seats from flights where operator_id in (select id from operators where name = ?) and flights.name = ?";
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

	private List<String> getTables(Connection con) {
		List<String> tableList = new ArrayList<String>();
		String checkTables = "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('operators','flights')";
		try (PreparedStatement pstmt = con.prepareStatement(checkTables)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				tableList.add(rs.getString("name"));
			}
		} catch (Exception e) {
			System.out.println("Error while fetching table list.");
			e.printStackTrace();
		}
		return tableList;
	}

	/**
	 * This method creates the table for Possible values for debug_flag are:
	 * <li>'Normal' for Normal (i.e. normal operation for the actor of the
	 * corresponding airline operator if this flag value is set.)
	 * <li>'Fail' for Fail Reply (i.e. the actor of the corresponding airline
	 * operator will reply Fail without processing if this flag value is set.)
	 * <li>'NoReply' for No Reply (i.e. the actor of the corresponding airline
	 * operator will not reply if this flag value is set.)
	 * 
	 * @param con
	 *            the database connection.
	 * @param insertRecords
	 * @return
	 */
	private boolean createOperators(Connection con, boolean insertRecords) {
		String createTable = "CREATE TABLE IF NOT EXISTS operators (id integer PRIMARY KEY, name text NOT NULL, debug_flag text)";
		boolean status = false;
		try {
			Statement stmt = con.createStatement();
			stmt.execute(createTable);
			status = true;
			if (insertRecords) {
				System.out.println("Created Table Operators");
				System.out.println("Inserting Data into Operators");
				List<String> insertStatements = new ArrayList<String>();
				insertStatements.add("INSERT INTO operators (id, name, debug_flag) values (1, 'AA','Normal')");
				insertStatements.add("INSERT INTO operators (id, name, debug_flag) values (2, 'BA','Normal')");
				insertStatements.add("INSERT INTO operators (id, name, debug_flag) values (3, 'CA','Normal')");
				for (String insertStatement : insertStatements) {
					try (PreparedStatement pstmt = con.prepareStatement(insertStatement)) {
						int recordsInserted = pstmt.executeUpdate();
						status = status && (recordsInserted > 0);
					} catch (Exception e) {
						System.out.println(
								"Error while inserting daat for operators, for Insert statement: " + insertStatement);
						e.printStackTrace();
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Error while creating operators");
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * 
	 * @param con
	 * @param insertRecords
	 * @return
	 */
	private boolean createFlights(Connection con, boolean insertRecords) {
		String createTable = "CREATE TABLE IF NOT EXISTS flights (id integer PRIMARY KEY, name text NOT NULL, operator_id integer, total_seats integer, available_seats integer, route text)";
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
						"INSERT INTO flights (name, operator_id, total_seats, available_seats, route) values ('AA001',1,3,3,'X-Z')");
				insertStatements.add(
						"INSERT INTO flights (name, operator_id, total_seats, available_seats, route) values ('AA002',1,1,1,'W-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator_id, total_seats, available_seats, route) values ('BA001',2,1,1,'Z-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator_id, total_seats, available_seats, route) values ('CA001',3,1,1,'X-Y')");
				insertStatements.add(
						"INSERT INTO flights (name, operator_id, total_seats, available_seats, route) values ('CA001',3,1,1,'Z-W')");
				for (String insertStatement : insertStatements) {
					try (PreparedStatement pstmt = con.prepareStatement(insertStatement)) {
						int recordsInserted = pstmt.executeUpdate();
						status = status && (recordsInserted > 0);
					} catch (Exception e) {
						System.out.println(
								"Error while inserting daat for flights, for Insert statement: " + insertStatement);
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
	 * @param insertRecords
	 * @return
	 */
	private boolean createBookings(Connection con) {
		System.out.println("Creating Table Bookings");
		// from and to are keywords in sqlite use '' to escape
		String createTable = "CREATE TABLE IF NOT EXISTS bookings (id integer PRIMARY KEY, 'from' text NOT NULL, 'to' text NOT NULL, schedule text)";
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
