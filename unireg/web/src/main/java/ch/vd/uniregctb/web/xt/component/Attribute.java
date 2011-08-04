/**
 *
 */
package ch.vd.uniregctb.web.xt.component;

/**
 * @author xcicfh
 *
 */
public class Attribute {

	private final String name;
	private final String value;

	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}


}
