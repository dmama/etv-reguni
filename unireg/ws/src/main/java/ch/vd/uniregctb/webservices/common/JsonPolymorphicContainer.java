package ch.vd.uniregctb.webservices.common;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class JsonPolymorphicContainer<T> {

	private final T data;

	protected JsonPolymorphicContainer(T data) {
		this.data = data;
	}

	@JsonProperty(value = "data")
	public T getData() {
		return data;
	}

	@JsonProperty(value = "type")
	public abstract String getDataType();
}
