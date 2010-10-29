package ch.vd.uniregctb.evenement.depart;

import java.util.Collection;
import java.util.Iterator;

import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

public class MockDepart extends MockEvenementCivil implements Depart, Cloneable {

	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;
	private Commune ancienneCommunePrincipale;
	private Adresse ancienneAdresseCourrier;
	private Adresse ancienneAdresseSecondaire;
	private Commune ancienneCommuneSecondaire;

	private final boolean isAncienTypeDepart = false;

	public void setAncienneAdresseCourrier(Adresse adresseCourrier) {
		this.ancienneAdresseCourrier = adresseCourrier;
	}

	public void setAncienneAdressePrincipale(Adresse adressePrincipale) {
		this.ancienneAdressePrincipale = adressePrincipale;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
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

	public void setAncienneCommunePrincipale(Commune communePrincipale) {
		this.ancienneCommunePrincipale = communePrincipale;
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
		// TODO Auto-generated method stub
		return isAncienTypeDepart;
	}
}
