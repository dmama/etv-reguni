package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockArrivee extends MockEvenementCivil implements Arrivee, Cloneable {

	private Commune nouvelleCommunePrincipale;
	private Adresse ancienneAdressePrincipale;
	private Adresse ancienneAdresseSecondaire;
	private Commune nouvelleCommuneSecondaire;
	private Commune ancienneCommunePrincipale;
	private Commune ancienneCommuneSecondaire;
	private Adresse adressePrincipale;
	private Adresse adresseSecondaire;

	public MockArrivee(Individu individu, Individu conjoint, TypeEvenementCivil type, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommunePrincipale,
	                   Commune nouvelleCommunePrincipale,
	                   Commune ancienneCommuneSecondaire, Commune nouvelleCommuneSecondaire) {
		super(individu, conjoint, type, date, numeroOfsCommuneAnnonce);
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
		this.nouvelleCommuneSecondaire = nouvelleCommuneSecondaire;
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
	}

	public MockArrivee(Individu individu, Individu conjoint, TypeEvenementCivil type, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommunePrincipale,
	                   Commune nouvelleCommunePrincipale, Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale) {
		super(individu, conjoint, type, date, numeroOfsCommuneAnnonce);
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
		this.adressePrincipale = nouvelleAdressePrincipale;
	}

	public MockArrivee(Individu individu, Individu conjoint, TypeEvenementCivil type, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommunePrincipale,
	                   Commune nouvelleCommunePrincipale, Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale, Commune ancienneCommuneSecondaire, Commune nouvelleCommuneSecondaire,
	                   Adresse ancienneAdresseSecondaire, Adresse nouvelleAdresseSecondaire) {
		super(individu, conjoint, type, date, numeroOfsCommuneAnnonce);
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.ancienneAdresseSecondaire = ancienneAdresseSecondaire;
		this.nouvelleCommuneSecondaire = nouvelleCommuneSecondaire;
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
		this.adressePrincipale = nouvelleAdressePrincipale;
		this.adresseSecondaire = nouvelleAdresseSecondaire;
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

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
