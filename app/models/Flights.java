package models;

/**
 * @author srivassumit
 *
 */
public class Flights {

	private int id;
	private String name;
	private int operatorId;
	private int totalSeats;
	private int availableSeats;
	private String route;

	public Flights(int id, String name, int operatorId, int totalSeats, int availableSeats, String route) {
		this.id = id;
		this.name = name;
		this.operatorId = operatorId;
		this.totalSeats = totalSeats;
		this.availableSeats = availableSeats;
		this.route = route;
	}

	public Flights() {
	}

	public int getId() {
		return id;
	}

	public void setId(int Id) {
		this.id = Id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(int operatorId) {
		this.operatorId = operatorId;
	}

	public int getTotalSeats() {
		return totalSeats;
	}

	public void setTotalSeats(int totalSeats) {
		this.totalSeats = totalSeats;
	}

	public int getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(int availableSeats) {
		this.availableSeats = availableSeats;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

}
