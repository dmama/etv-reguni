package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.TypeEtatPM;

public class TypeEtatPMImpl implements TypeEtatPM {

	private String code;
	private String libelle;

	public TypeEtatPMImpl() {
	}

	public TypeEtatPMImpl(String code, String libelle) {
		this.code = code;
		this.libelle = libelle;
	}

	public static TypeEtatPMImpl get(ch.vd.infrastructure.fiscal.model.TypeEtatPM type) {
		if (type == null) {
			return null;
		}
		return new TypeEtatPMImpl(type.getCode(), type.getLibelle());
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
}
