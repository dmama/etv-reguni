package ch.vd.uniregctb.evenement.arrivee;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;

public class MockArrivee extends MockEvenementCivil implements Arrivee, Cloneable {

	private Commune nouvelleCommunePrincipale;
	private Adresse ancienneAdressePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private Commune nouvelleCommuneSecondaire;
	private Commune ancienneCommunePrincipale;
	private Commune ancienneCommuneSecondaire;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;
	private String etat;


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

	public void setEtat(String etat) {
		this.etat = etat;
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

	public String getEtat() {
		return etat;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
