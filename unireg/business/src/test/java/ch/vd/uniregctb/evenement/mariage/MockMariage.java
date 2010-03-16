package ch.vd.uniregctb.evenement.mariage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockMariage implements Mariage {


	private Individu nouveauConjoint;
	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private Individu conjoint;
	private RegDate date;
	private Individu individu;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private TypeEvenementCivil type;


	/**
	 * @return the nouveauConjoint
	 */
	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}


	/**
	 * @param nouveauConjoint the nouveauConjoint to set
	 */
	public void setNouveauConjoint(Individu nouveauConjoint) {
		this.nouveauConjoint = nouveauConjoint;
	}


	/**
	 * @return the adresseCourrier
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}


	/**
	 * @param adresseCourrier the adresseCourrier to set
	 */
	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}


	/**
	 * @return the adressePrincipale
	 */
	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}


	/**
	 * @param adressePrincipale the adressePrincipale to set
	 */
	public void setAdressePrincipale(Adresse adressePrincipale) {
		this.adressePrincipale = adressePrincipale;
	}


	/**
	 * @return the adresseSecondaire
	 */
	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}


	/**
	 * @param adresseSecondaire the adresseSecondaire to set
	 */
	public void setAdresseSecondaire(Adresse adresseSecondaire) {
		this.adresseSecondaire = adresseSecondaire;
	}


	/**
	 * @return the conjoint
	 */
	public Individu getConjoint() {
		return conjoint;
	}


	/**
	 * @param conjoint the conjoint to set
	 */
	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}


	/**
	 * @return the date
	 */
	public RegDate getDate() {
		return date;
	}


	/**
	 * @param date the date to set
	 */
	public void setDate(RegDate date) {
		this.date = date;
	}


	/**
	 * @return the individu
	 */
	public Individu getIndividu() {
		return individu;
	}


	/**
	 * @param individu the individu to set
	 */
	public void setIndividu(Individu individu) {
		this.individu = individu;
	}


	/**
	 * @return the numeroEvenement
	 */
	public Long getNumeroEvenement() {
		return numeroEvenement;
	}


	/**
	 * @param numeroEvenement the numeroEvenement to set
	 */
	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}


	/**
	 * @return the numeroOfsCommuneAnnonce
	 */
	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}


	/**
	 * @param numeroOfsCommuneAnnonce the numeroOfsCommuneAnnonce to set
	 */
	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}


	/**
	 * @return the type
	 */
	public TypeEvenementCivil getType() {
		return type;
	}


	/**
	 * @param type the type to set
	 */
	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}


	public boolean isContribuablePresentBefore() {
		return true;
	}

}
