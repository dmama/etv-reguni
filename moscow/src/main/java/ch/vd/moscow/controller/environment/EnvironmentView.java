package ch.vd.moscow.controller.environment;

import ch.vd.moscow.data.Environment;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EnvironmentView {

	private Long id;
	private String name;

	public EnvironmentView() {
	}

	public EnvironmentView(Environment environment) {
		this.id = environment.getId();
		this.name = environment.getName();
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
