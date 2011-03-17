package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.CompteBancaire;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockCompteBancaire implements CompteBancaire {

	private String numero;
	private Format format;
	private String nomInstitution;

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public String getNomInstitution() {
		return nomInstitution;
	}

	public void setNomInstitution(String nomInstitution) {
		this.nomInstitution = nomInstitution;
	}
}
