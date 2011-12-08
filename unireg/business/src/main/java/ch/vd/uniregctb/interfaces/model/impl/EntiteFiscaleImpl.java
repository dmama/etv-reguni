package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.EntiteFiscale;

public abstract class EntiteFiscaleImpl implements EntiteFiscale {

	private Integer code;

	private String designation;

	@Override
	public Integer getCode() {
		return code;
	}

	@Override
	public String getDesignation() {
		return designation;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	protected EntiteFiscaleImpl(Integer code, String designation) {
		this.code = code;
		this.designation = designation;
	}
}

