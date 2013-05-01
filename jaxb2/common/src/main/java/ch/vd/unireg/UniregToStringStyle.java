package ch.vd.unireg;

import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Style de formatting spécifique à Unireg
 */
public class UniregToStringStyle extends ToStringStyle {

	@SuppressWarnings({"UnusedDeclaration"})
	public static final UniregToStringStyle STYLE = new UniregToStringStyle();

	public UniregToStringStyle() {
		this.setContentStart("{");
		this.setContentEnd("}");
		this.setArrayStart("[");
		this.setArrayStart("]");
		this.setUseShortClassName(true);
		this.setUseIdentityHashCode(false);
	}
}
