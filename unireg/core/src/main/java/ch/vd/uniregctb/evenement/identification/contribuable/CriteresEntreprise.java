package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * Contient les critères d'identification utilisés pour une entreprise
 */
@Embeddable
public class CriteresEntreprise {

	/**
	 * Le numéro IDE est optionnel.
	 */
	private String ide;

	/**
	 * La raison sociale est optionnelle
	 */
	private String raisonSociale;

	/**
	 * L'adresse de l'entreprise
	 */
	private CriteresAdresse adresse;

	@Column(name = "IDE")
	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}

	@Column(name = "RAISON_SOCIALE")
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Embedded
	public CriteresAdresse getAdresse() {
		return adresse;
	}

	public void setAdresse(CriteresAdresse adresse) {
		this.adresse = adresse;
	}

	@Override
	public String toString() {
		return String.format("IDE: %s, Raison sociale: %s", ide, raisonSociale);
	}
}
