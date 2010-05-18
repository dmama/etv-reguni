package ch.vd.uniregctb.evenement.demenagement;

import ch.vd.uniregctb.evenement.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;

public class MockDemenagement extends MockEvenementCivil implements Demenagement, Cloneable {

	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;

	public boolean isContribuablePresentBefore() {
		return true;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public void setNouvelleAdressePrincipale(Adresse nouvelleAdressePrincipale) {
		setAdressePrincipale(nouvelleAdressePrincipale);
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public void setNouvelleCommunePrincipale(Commune nouvelleCommunePrincipale) {
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public void setAncienneAdressePrincipale(Adresse ancienneAdressePrincipale) {
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
	}
}
