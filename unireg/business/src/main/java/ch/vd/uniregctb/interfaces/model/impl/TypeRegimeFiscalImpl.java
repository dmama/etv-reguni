package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

public class TypeRegimeFiscalImpl implements TypeRegimeFiscal {

	private String code;
	private String libelle;
	private String libelleAbrege;

	public TypeRegimeFiscalImpl() {
	}

	public TypeRegimeFiscalImpl(String code, String libelle, String libelleAbrege) {
		this.code = code;
		this.libelle = libelle;
		this.libelleAbrege = libelleAbrege;
	}

	public static TypeRegimeFiscal get(ch.vd.infrastructure.fiscal.model.TypeRegimeFiscal type) {
		if (type == null) {
			return null;
		}
		return new TypeRegimeFiscalImpl(type.getCode(), type.getLibelle(), type.getLibelleAbrege());
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

	public String getLibelleAbrege() {
		return libelleAbrege;
	}

	public void setLibelleAbrege(String libelleAbrege) {
		this.libelleAbrege = libelleAbrege;
	}

}
