package ch.vd.moscow.controller.graph;

import ch.vd.moscow.data.Caller;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.Method;
import ch.vd.moscow.data.Service;

public class DimensionValue {

	private Long id;
	private String name;

	public DimensionValue() {
	}

	public DimensionValue(Environment value) {
		this.id = value.getId();
		this.name = value.getName();
	}

	public DimensionValue(Method method) {
		this.id = method.getId();
		this.name = method.getName();
	}

	public DimensionValue(Caller caller) {
		this.id = caller.getId();
		this.name = caller.getName();
	}

	public DimensionValue(Service service) {
		this.id = service.getId();
		this.name = service.getName();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
