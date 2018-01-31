package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.fidor.xml.impotspecial.v1.ImpotSpecial;

public class GenreImpotMandataireImpl implements GenreImpotMandataire, Serializable {

	private static final long serialVersionUID = 4244483157655414533L;

	private final String code;
	private final String libelle;

	public static GenreImpotMandataireImpl get(ImpotSpecial is) {
		if (is == null) {
			return null;
		}
		return new GenreImpotMandataireImpl(is);
	}

	private GenreImpotMandataireImpl(ImpotSpecial is) {
		this.code = is.getCode();
		this.libelle = is.getLibelle();
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getLibelle() {
		return libelle;
	}
}
