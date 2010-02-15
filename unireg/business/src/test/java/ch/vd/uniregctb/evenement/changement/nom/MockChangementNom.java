package ch.vd.uniregctb.evenement.changement.nom;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockChangementNom implements ChangementNom {

	Individu individu = null;
	Individu conjoint = null;
	Adresse adresseCourrier = null;
	Adresse adressePrincipale = null;
	Adresse adresseSecondaire = null;
	RegDate date = null;
	Long numeroEvenement = 0L;
	Integer numeroOfsCommuneAnnonce = null;
	TypeEvenementCivil type = null;

	public String getNouveauNom() {
		return null;
	}

	public String getNouveauPrenom() {
		return null;
	}

	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}

	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	public RegDate getDate() {
		return date;
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
		return true;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	public void setNumeroOfsCommuneAnnonce(Integer numeroOfsCommuneAnnonce) {
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	public void setNumeroEvenement(Long numeroEvenement) {
		this.numeroEvenement = numeroEvenement;
	}

	public void setType(TypeEvenementCivil type) {
		this.type = type;
	}

}
