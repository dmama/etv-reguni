package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.MockEvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class MockDemenagement extends MockEvenementCivil implements Demenagement, Cloneable {

	private Adresse nouvelleAdressePrincipale;
	private Adresse ancienneAdressePrincipale;
	private Commune nouvelleCommunePrincipale;

	public MockDemenagement(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce,
	                        Commune communePrincipale, Adresse ancienneAdressePrincipale, Adresse nouvelleAdressePrincipale) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, date, numeroOfsCommuneAnnonce);
		this.nouvelleAdressePrincipale = nouvelleAdressePrincipale;
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
		this.nouvelleCommunePrincipale = communePrincipale;
	}

	public boolean isContribuablePresentBefore() {
		return true;
	}

	public Adresse getNouvelleAdressePrincipale() {
		return nouvelleAdressePrincipale;
	}

	public Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}
}
