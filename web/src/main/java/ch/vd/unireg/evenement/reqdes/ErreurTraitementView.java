package ch.vd.unireg.evenement.reqdes;

import java.io.Serializable;

import ch.vd.unireg.reqdes.ErreurTraitement;

/**
 * Classe de visualisation des données d'une erreur / d'un warning
 */
public class ErreurTraitementView implements Serializable {

	private static final long serialVersionUID = 5139504300559480032L;

	private final String message;
	private final String callstack;
	private final String cssClass;

	public ErreurTraitementView(ErreurTraitement source) {
		this.message = source.getMessage();
		this.callstack = source.getCallstack();
		this.cssClass = source.getType() == ErreurTraitement.TypeErreur.WARNING ? "warning" : "error";
	}

	public String getMessage() {
		return message;
	}

	public String getCallstack() {
		return callstack;
	}

	public String getCssClass() {
		return cssClass;
	}
}
