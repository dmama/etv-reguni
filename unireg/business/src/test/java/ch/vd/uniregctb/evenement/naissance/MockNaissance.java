package ch.vd.uniregctb.evenement.naissance;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 *
 */
public class MockNaissance implements Naissance {

	List<Individu> parents = null;
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
	 * @return the parents
	 */
	public List<Individu> getParents() {
		return parents;
	}

	/**
	 * @return the adresseCourrier
	 */
	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	/**
	 * @return the adressePrincipale
	 */
	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	/**
	 * @return the adresseSecondaire
	 */
	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	/**
	 * @return the conjoint
	 */
	public Individu getConjoint() {
		return conjoint;
	}

	/**
	 * @return the date
	 */
	public RegDate getDate() {
		return date;
	}

	/**
	 * @return the individu
	 */
	public Individu getIndividu() {
		return individu;
	}

	/**
	 * @return the numeroEvenement
	 */
	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	/**
	 * @return the numeroOfsCommuneAnnonce
	 */
	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	/**
	 * @return the type
	 */
	public TypeEvenementCivil getType() {
		return type;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

	/**
	 * @param parents the parents to set
	 */
	public void setParents(List<Individu> parents) {
		this.parents = parents;
	}

	/**
	 * @param adresseCourrier the adresseCourrier to set
	 */
	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}

	/**
	 * @param adressePrincipale the adressePrincipale to set
	 */
	public void setAdressePrincipale(Adresse adressePrincipale) {
		this.adressePrincipale = adressePrincipale;
	}

	/**
	 * @param adresseSecondaire the adresseSecondaire to set
	 */
	public void setAdresseSecondaire(Adresse adresseSecondaire) {
		this.adresseSecondaire = adresseSecondaire;
	}

	/**
	 * @param conjoint the conjoint to set
	 */
	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(RegDate date) {
		this.date = date;
	}

	/**
	 * @param individu the individu to set
	 */
	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	/**
	 * @param numeroEvenement the numeroEvenement to set
	 */
	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	/**
	 * @param numeroOfsCommuneAnnonce the numeroOfsCommuneAnnonce to set
	 */
	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

}
