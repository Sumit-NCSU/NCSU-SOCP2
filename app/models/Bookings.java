package models;

/**
 * @author srivassumit
 *
 */
public class Bookings {

	private long id;
	private String schedule;

	public Bookings(long id, String schedule) {
		this.id = id;
		this.schedule = schedule;
	}

	public Bookings() {
	}

	public long getId() {
		return id;
	}

	public void setId(long Id) {
		this.id = Id;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
}
