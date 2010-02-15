package ch.vd.uniregctb.evenement.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockArrivee implements Arrivee, Cloneable {

	private Commune nouvelleCommunePrincipale;
	private Adresse ancienneAdressePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private Commune nouvelleCommuneSecondaire;
	private Commune ancienneCommunePrincipale;
	private Commune ancienneCommuneSecondaire;
	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private Individu individu;
	private Individu conjoint;
	private RegDate date;
	private String etat;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private TypeEvenementCivil type;


	public void setNouvelleAdressePrincipale(Adresse nouvelleAdressePrincipale) {
		this.adressePrincipale = nouvelleAdressePrincipale;
	}

	public void setNouvelleCommunePrincipale(Commune nouvelleCommune) {
		this.nouvelleCommunePrincipale = nouvelleCommune;
	}

	public void setAncienneCommunePrincipale(Commune ancienneCommunePrincipale) {
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
	}

	public void setAncienneCommuneSecondaire(Commune ancienneCommuneSecondaire) {
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
	}

	public void setAdresseCourrier(Adresse adresseCourrier) {
		this.adresseCourrier = adresseCourrier;
	}

	public void setAdressePrincipale(Adresse adressePrincipale) {
		this.adressePrincipale = adressePrincipale;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

	public void setNouvelleAdresseSecondaire(Adresse nouvelleAdresseSecondaire) {
		this.adresseSecondaire = nouvelleAdresseSecondaire;
	}

	public void setNouvelleCommuneSecondaire(Commune nouvelleCommuneSecondaire) {
		this.nouvelleCommuneSecondaire = nouvelleCommuneSecondaire;
	}
	
	public void setAncienneAdresseSecondaire(Adresse ancienneAdresseSecondaire) {
		this.ancienneAdresseSecondaire = ancienneAdresseSecondaire;
	}

	public void setAdresseSecondaire(Adresse adresseSecondaire) {
		this.adresseSecondaire = adresseSecondaire;
	}

	public void setAncienneAdressePrincipale(Adresse ancienneAdressePrincipale) {
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
	}
	
	public Adresse getNouvelleAdressePrincipale() {
		return adressePrincipale;
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public Adresse getNouvelleAdresseSecondaire() {
		return adresseSecondaire;
	}

	public Commune getNouvelleCommuneSecondaire() {
		return nouvelleCommuneSecondaire;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public Commune getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public RegDate getDate() {
		return date;
	}

	public String getEtat() {
		return etat;
	}

	public Individu getIndividu() {
		return individu;
	}

	public Long getNumeroEvenement() {
		return numeroEvenement;
	}

	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public TypeEvenementCivil getType() {
		return type;
	}

	public boolean isContribuablePresentBefore() {
		return false;	// par définition
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
