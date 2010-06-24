package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import ch.vd.uniregctb.interfaces.model.CompteBancaire;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CompteBancaireWrapper implements CompteBancaire {

	private final String numero;
	private final Format format;
	private final String nomInstitution;

	public static CompteBancaire get(ch.vd.registre.pm.model.CompteBancaire target) {
		if (target == null) {
			return null;
		}
		return new CompteBancaireWrapper(target);
	}

	private CompteBancaireWrapper(ch.vd.registre.pm.model.CompteBancaire target) {
		this.numero = target.getNumero();
		this.format = Format.valueOf(target.getFormat().name());
		this.nomInstitution = target.getNomInstitution();
	}

	public String getNumero() {
		return numero;
	}

	public Format getFormat() {
		return format;
	}

	public String getNomInstitution() {
		return nomInstitution;
	}
}
