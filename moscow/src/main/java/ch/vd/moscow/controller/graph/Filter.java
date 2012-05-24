package ch.vd.moscow.controller.graph;

public class Filter {
	private CallDimension dimension;
	private String value;

	public Filter() {
	}

	public Filter(CallDimension dimension, String value) {
		this.dimension = dimension;
		this.value = value;
	}

	public CallDimension getDimension() {
		return dimension;
	}

	public void setDimension(CallDimension dimension) {
		this.dimension = dimension;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(dimension).append(':').append(value).toString();
	}
}
