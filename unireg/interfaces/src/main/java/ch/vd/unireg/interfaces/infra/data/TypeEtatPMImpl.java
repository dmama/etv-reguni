package ch.vd.unireg.interfaces.infra.data;

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

	@Override
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
}
