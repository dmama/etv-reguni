package ch.vd.uniregctb.evenement.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDemenagement implements Demenagement, Cloneable {

	private Adresse ancienneAdressePrincipale;

	private Commune nouvelleCommunePrincipale;

	private Adresse adresseCourrier;

	private Adresse adressePrincipale;

	private Adresse adresseSecondaire;

	private Individu conjoint;

	private RegDate date;

	private Individu individu;

	private Long numeroEvenement;

	private Integer numeroOfsCommuneAnnonce;

	private TypeEvenementCivil type;

	public boolean isContribuablePresentBefore() {
		return true;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public void setNouvelleAdressePrincipale(Adresse nouvelleAdressePrincipale) {
		this.adressePrincipale = nouvelleAdressePrincipale;
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public void setNouvelleCommunePrincipale(Commune nouvelleCommunePrincipale) {
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
	}

	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}

	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public void setAdressePrincipale(Adresse adressePrincipale) {
		this.adressePrincipale = adressePrincipale;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	public void setAdresseSecondaire(Adresse adresseSecondaire) {
		this.adresseSecondaire = adresseSecondaire;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}

	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public Individu getIndividu() {
		return individu;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public TypeEvenementCivil getType() {
		return type;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public void setAncienneAdressePrincipale(Adresse ancienneAdressePrincipale) {
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
	}
}
