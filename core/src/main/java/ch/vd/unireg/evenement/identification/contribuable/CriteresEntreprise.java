package ch.vd.unireg.evenement.identification.contribuable;

import java.util.Set;

import ch.vd.unireg.tiers.TypeTiers;

/**
 * Contient les critères d'identification utilisés pour une entreprise
 */
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

	/**
	 * Le numéro RC/FOSC
	 */
	private String numeroRC;

	private Set<TypeTiers> typesTiers;

	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public CriteresAdresse getAdresse() {
		return adresse;
	}

	public void setAdresse(CriteresAdresse adresse) {
		this.adresse = adresse;
	}

	public String getNumeroRC() {
		return numeroRC;
	}

	public void setNumeroRC(String numeroRC) {
		this.numeroRC = numeroRC;
	}

	@Override
	public String toString() {
		return String.format("IDE: %s, RC: %s, Raison sociale: %s", ide, numeroRC, raisonSociale);
	}

	public Set<TypeTiers> getTypesTiers() {
		return typesTiers;
	}

	public void setTypesTiers(Set<TypeTiers> typesTiers) {
		this.typesTiers = typesTiers;
	}
}
