package ch.vd.uniregctb.evenement.deces;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDeces implements Deces {

	Individu individu = null;
	Individu conjointSurvivant = null;
	Adresse adresseCourrier = null;
	Adresse adressePrincipale = null;
	Adresse adresseSecondaire = null;
	Individu conjoint = null;
	RegDate date = null;
	Long numeroEvenement = 0L;
	Integer numeroOfsCommuneAnnonce = null;
	TypeEvenementCivil type = null;
	boolean contribuablePresentBefore = true;

	/**
	 * @return the individu
	 */
	public Individu getIndividu() {
		return individu;
	}

	/**
	 * @param individu
	 *            the individu to set
	 */
	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	/**
	 * @return the conjointSurvivant
	 */
	public Individu getConjointSurvivant() {
		return conjointSurvivant;
	}

	/**
	 * @param conjointSurvivant
	 *            the conjointSurvivant to set
	 */
	public void setConjointSurvivant(Individu conjointSurvivant) {
		this.conjointSurvivant = conjointSurvivant;
	}

	/**
	 * @return the adresseCourrier
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	/**
	 * @param adresseCourrier
	 *            the adresseCourrier to set
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
	 * @param adressePrincipale
	 *            the adressePrincipale to set
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
	 * @param adresseSecondaire
	 *            the adresseSecondaire to set
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
	 * @param conjoint
	 *            the conjoint to set
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
	 * @param date
	 *            the date to set
	 */
	public void setDate(RegDate date) {
		this.date = date;
	}

	/**
	 * @return the numeroEvenement
	 */
	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	/**
	 * @param numeroEvenement
	 *            the numeroEvenement to set
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
	 * @param numeroOfsCommuneAnnonce
	 *            the numeroOfsCommuneAnnonce to set
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
	 * @param type
	 *            the type to set
	 */
	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	/**
	 * @return the contribuablePresentBefore
	 */
	public boolean isContribuablePresentBefore() {
		return contribuablePresentBefore;
	}

	/**
	 * @param contribuablePresentBefore
	 *            the contribuablePresentBefore to set
	 */
	public void setContribuablePresentBefore(boolean contribuablePresentBefore) {
		this.contribuablePresentBefore = contribuablePresentBefore;
	}

}
