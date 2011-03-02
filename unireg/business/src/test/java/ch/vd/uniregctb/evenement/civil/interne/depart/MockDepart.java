package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.Collection;
import java.util.Iterator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

public class MockDepart extends MockEvenementCivil implements Depart, Cloneable {

	private Adresse nouvelleAdressePrincipale;

	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;
	private Adresse nouvelleAdresseCourrier;
	private Commune ancienneCommunePrincipale;
	private Adresse ancienneAdresseCourrier;
	private Adresse ancienneAdresseSecondaire;
	private Commune ancienneCommuneSecondaire;

	public MockDepart(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Commune ancienneCommunePrincipale, Commune nouvelleCommunePrincipale,
	                  Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale, Adresse ancienneAdresseCourrier, Adresse nouvelleAdresseCourrier, Commune ancienneCommuneSecondaire,
	                  Adresse ancienneAdresseSecondaire, boolean departPrincipal) {
		super(individu, conjoint, (departPrincipal ? TypeEvenementCivil.DEPART_COMMUNE : TypeEvenementCivil.DEPART_SECONDAIRE), date, numeroOfsCommuneAnnonce);
		this.nouvelleAdressePrincipale = nouvelleAdressePrincipale;
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
		this.nouvelleAdresseCourrier = nouvelleAdresseCourrier;
		this.ancienneCommunePrincipale = ancienneCommunePrincipale;
		this.ancienneAdresseCourrier = ancienneAdresseCourrier;
		this.ancienneAdresseSecondaire = ancienneAdresseSecondaire;
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return nouvelleAdressePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Adresse getNouvelleAdresseCourrier() {
		return nouvelleAdresseCourrier;
	}

	public Adresse getAncienneAdresseCourrier() {
		return ancienneAdresseCourrier;
	}

	public boolean isContribuablePresentBefore() {
		return false; // par définition
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public void setNouvelleCommunePrincipale(Commune nouvelleCommunePrincipale) {
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public static boolean findEvenementFermetureFor(Collection<EvenementFiscal> lesEvenements,Depart depart) {

		boolean isPresent = false;
		Iterator<EvenementFiscal> iteEvFiscal = lesEvenements.iterator();
		EvenementFiscal evenement = null;
		while (iteEvFiscal.hasNext()) {
			evenement = iteEvFiscal.next();
			if (evenement.getType() == TypeEvenementFiscal.FERMETURE_FOR && evenement.getDateEvenement()==depart.getDate()) {
				isPresent = true;
				break;
			}
		}
		return isPresent;

	}

	public void setAncienneAdresseSecondaire(Adresse adresseSecondaire) {
		this.ancienneAdresseSecondaire = adresseSecondaire;
	}

	public Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public void setAncienneCommuneSecondaire(Commune ancienneCommuneSecondaire) {
		this.ancienneCommuneSecondaire = ancienneCommuneSecondaire;
	}

	public Pays getPaysInconnu() {
		return MockPays.PaysInconnu;
	}

	public boolean isAncienTypeDepart() {
		return false;
	}
}
