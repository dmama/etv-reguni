package ch.vd.moscow.controller.graph;

import java.util.List;

public class DimensionInfo {
	private CallDimension type;
	private String label;
	private List<Object> values;

	public DimensionInfo() {
	}

	public DimensionInfo(CallDimension type, String label, List<Object> values) {
		this.type = type;
		this.label = label;
		this.values = values;
	}

	public CallDimension getType() {
		return type;
	}

	public void setType(CallDimension type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<Object> getValues() {
		return values;
	}

	public void setValues(List<Object> values) {
		this.values = values;
	}
}
