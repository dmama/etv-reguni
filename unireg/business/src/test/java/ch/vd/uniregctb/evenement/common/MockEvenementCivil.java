package ch.vd.uniregctb.evenement.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockEvenementCivil implements EvenementCivil {

	private Adresse adresseCourrier;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private Individu individu;
	private Individu conjoint;
	private Long numeroEvenement = 0L;
	private Integer numeroOfsCommuneAnnonce;
	private TypeEvenementCivil type;
	private RegDate date;

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

	public Individu getIndividu() {
		return individu;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
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

	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public boolean isContribuablePresentBefore() {
		return false;
	}

}
