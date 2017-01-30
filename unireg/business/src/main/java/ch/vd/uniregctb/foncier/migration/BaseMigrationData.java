package ch.vd.uniregctb.foncier.migration;

import ch.vd.registre.base.date.RegDate;

/**
 * Classe de base des données d'extraction de SIMPA : entreprise et localisation de immeuble
 */
public class BaseMigrationData {

	private long numeroEntreprise;

	private String nomEntreprise;

	private long noAciCommune;

	private int noOfsCommune;

	private String nomCommune;

	private String noBaseParcelle;

	private String noParcelle;

	private String noLotPPE;

	private RegDate dateDebutRattachement;

	private RegDate dateFinRattachement;

	public long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	public String getNomEntreprise() {
		return nomEntreprise;
	}

	public void setNomEntreprise(String nomEntreprise) {
		this.nomEntreprise = nomEntreprise;
	}

	public long getNoAciCommune() {
		return noAciCommune;
	}

	public void setNoAciCommune(long noAciCommune) {
		this.noAciCommune = noAciCommune;
	}

	public int getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(int noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}

	public String getNomCommune() {
		return nomCommune;
	}

	public void setNomCommune(String nomCommune) {
		this.nomCommune = nomCommune;
	}

	public String getNoBaseParcelle() {
		return noBaseParcelle;
	}

	public void setNoBaseParcelle(String noBaseParcelle) {
		this.noBaseParcelle = noBaseParcelle;
	}

	public String getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(String noParcelle) {
		this.noParcelle = noParcelle;
	}

	public String getNoLotPPE() {
		return noLotPPE;
	}

	public void setNoLotPPE(String noLotPPE) {
		this.noLotPPE = noLotPPE;
	}

	public RegDate getDateDebutRattachement() {
		return dateDebutRattachement;
	}

	public void setDateDebutRattachement(RegDate dateDebutRattachement) {
		this.dateDebutRattachement = dateDebutRattachement;
	}

	public RegDate getDateFinRattachement() {
		return dateFinRattachement;
	}

	public void setDateFinRattachement(RegDate dateFinRattachement) {
		this.dateFinRattachement = dateFinRattachement;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '{' + getAttributesToString() + '}';
	}

	protected String getAttributesToString() {
		return "numeroEntreprise=" + numeroEntreprise +
				", nomEntreprise='" + nomEntreprise + '\'' +
				", noAciCommune=" + noAciCommune +
				", noOfsCommune=" + noOfsCommune +
				", nomCommune='" + nomCommune + '\'' +
				", noBaseParcelle='" + noBaseParcelle + '\'' +
				", noParcelle='" + noParcelle + '\'' +
				", noLotPPE='" + noLotPPE + '\'' +
				", dateDebutRattachement=" + dateDebutRattachement +
				", dateFinRattachement=" + dateFinRattachement;
	}
}
