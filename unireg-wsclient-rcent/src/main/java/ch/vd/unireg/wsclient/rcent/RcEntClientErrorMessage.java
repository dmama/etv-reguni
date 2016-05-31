package ch.vd.unireg.wsclient.rcent;

import java.io.Serializable;

import ch.vd.evd0004.v3.Error;

/**
 * Structure sérialisable qui représente une erreur (avec code spécifique) remontée par RCEnt
 */
public class RcEntClientErrorMessage implements Serializable {

	private static final long serialVersionUID = 2268704405324719865L;

	private final Integer code;
	private final String message;

	public RcEntClientErrorMessage(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public RcEntClientErrorMessage(Error erreur) {
		this.code = erreur.getCode();
		this.message = erreur.getMessage();
	}

	public Integer getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
