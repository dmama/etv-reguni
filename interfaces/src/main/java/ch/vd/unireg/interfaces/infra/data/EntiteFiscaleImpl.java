package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public abstract class EntiteFiscaleImpl implements EntiteFiscale, Serializable {

	private static final long serialVersionUID = 9087929252816681359L;

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

	protected EntiteFiscaleImpl() {
	}

	protected EntiteFiscaleImpl(Integer code, String designation) {
		this.code = code;
		this.designation = designation;
	}

	@Override
	public final String toString() {
		return String.format("%s{%s}", getClass().getSimpleName(), getMemberString());
	}

	protected String getMemberString() {
		return String.format("code=%s, designation='%s'", code, designation);
	}
}

