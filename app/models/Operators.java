package models;

/**
 * @author srivassumit
 *
 */
public class Operators {

	private int id;
	private String name;
	private String debugFlags;

	public Operators(int id, String name, String debugFlags) {
		this.id = id;
		this.name = name;
		this.debugFlags = debugFlags;
	}

	public Operators() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDebugFlags() {
		return debugFlags;
	}

	public void setDebugFlags(String debugFlags) {
		this.debugFlags = debugFlags;
	}
}
