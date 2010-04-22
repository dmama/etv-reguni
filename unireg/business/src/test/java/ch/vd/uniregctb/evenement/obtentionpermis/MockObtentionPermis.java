package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Bouchon pour un événement de typeNaissance
 *
 * @author Ludovic Bertin
 *
 */
public class MockObtentionPermis implements ObtentionPermis {

	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private Individu conjoint;
	private RegDate date;
	private Individu individu;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private Integer numeroOfsEtenduCommunePrincipale;
	private TypeEvenementCivil type;
	private EnumTypePermis typePermis;

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
	 * @param typePermis the typePermis to set
	 */
	public void setTypePermis(EnumTypePermis typePermis) {
		this.typePermis = typePermis;
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

	public EnumTypePermis getTypePermis() {
		return this.typePermis;
	}

	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}

	public void setNumeroOfsEtenduCommunePrincipale(
			Integer numeroOfsEtenduCommunePrincipale) {
		this.numeroOfsEtenduCommunePrincipale = numeroOfsEtenduCommunePrincipale;
	}
}
