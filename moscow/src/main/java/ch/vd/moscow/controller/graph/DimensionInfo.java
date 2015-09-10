package ch.vd.moscow.controller.graph;

import java.util.List;

public class DimensionInfo {
	private CallDimension type;
	private String label;
	private List<DimensionValue> values;

	public DimensionInfo() {
	}

	public DimensionInfo(CallDimension type, List<DimensionValue> values) {
		this.type = type;
		this.label = type.getDisplayName();
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

	public List<DimensionValue> getValues() {
		return values;
	}

	public void setValues(List<DimensionValue> values) {
		this.values = values;
	}
}
