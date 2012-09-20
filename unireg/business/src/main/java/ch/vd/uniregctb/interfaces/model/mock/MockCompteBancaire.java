package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.uniregctb.interfaces.model.CompteBancaire;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockCompteBancaire implements CompteBancaire {

	private String numero;
	private Format format;
	private String nomInstitution;

	@Override
	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	@Override
	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	@Override
	public String getNomInstitution() {
		return nomInstitution;
	}

	public void setNomInstitution(String nomInstitution) {
		this.nomInstitution = nomInstitution;
	}
}
