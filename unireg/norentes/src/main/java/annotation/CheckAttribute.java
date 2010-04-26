package annotation;

public class CheckAttribute {

	private final int index;
	private final String description;

	public CheckAttribute(Check check) {
		index= check.id();
		description = check.descr();
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
