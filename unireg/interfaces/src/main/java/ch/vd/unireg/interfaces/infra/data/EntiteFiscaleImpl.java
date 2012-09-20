package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public abstract class EntiteFiscaleImpl implements EntiteFiscale, Serializable {

	private static final long serialVersionUID = -8674294582132866017L;

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
}

