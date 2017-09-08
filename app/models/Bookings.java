package models;

/**
 * @author srivassumit
 *
 */
public class Bookings {

	private long id;
	private String from;
	private String to;
	private String schedule;

	public Bookings(long id, String from, String to, String schedule) {
		this.id = id;
		this.from = from;
		this.to = to;
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

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
}
