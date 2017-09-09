package models;

/**
 * @author srivassumit
 *
 */
public class Flights {

	private int id;
	private String name;
	private String operator;
	private int totalSeats;
	private int availableSeats;
	private String route;
	private String holdTime;

	public Flights(int id, String name, String operator, int totalSeats, int availableSeats, String route,
			String holdTime) {
		this.id = id;
		this.name = name;
		this.operator = operator;
		this.totalSeats = totalSeats;
		this.availableSeats = availableSeats;
		this.route = route;
		this.holdTime = holdTime;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the operator
	 */
	public String getOperator() {
		return operator;
	}

	/**
	 * @param operator
	 *            the operator to set
	 */
	public void setOperator(String operator) {
		this.operator = operator;
	}

	/**
	 * @return the totalSeats
	 */
	public int getTotalSeats() {
		return totalSeats;
	}

	/**
	 * @param totalSeats
	 *            the totalSeats to set
	 */
	public void setTotalSeats(int totalSeats) {
		this.totalSeats = totalSeats;
	}

	/**
	 * @return the availableSeats
	 */
	public int getAvailableSeats() {
		return availableSeats;
	}

	/**
	 * @param availableSeats
	 *            the availableSeats to set
	 */
	public void setAvailableSeats(int availableSeats) {
		this.availableSeats = availableSeats;
	}

	/**
	 * @return the route
	 */
	public String getRoute() {
		return route;
	}

	/**
	 * @param route
	 *            the route to set
	 */
	public void setRoute(String route) {
		this.route = route;
	}

	/**
	 * @return the holdTime
	 */
	public String getHoldTime() {
		return holdTime;
	}

	/**
	 * @param holdTime
	 *            the holdTime to set
	 */
	public void setHoldTime(String holdTime) {
		this.holdTime = holdTime;
	}
}
