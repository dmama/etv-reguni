package ch.vd.unireg.fors;

import ch.vd.unireg.type.MotifFor;

@SuppressWarnings("UnusedDeclaration")
public class MotifForView {
	private MotifFor type;
	private String label;

	public MotifForView(MotifFor type, String label) {
		this.type = type;
		this.label = label;
	}

	public MotifFor getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}
}
