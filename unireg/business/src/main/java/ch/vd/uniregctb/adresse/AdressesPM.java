package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;

/**
 * Contient les adresses PM à un instant donné d'une entreprise regroupées par type.
 */
public class AdressesPM {

	public AdresseEntreprise siege;
	public AdresseEntreprise courrier;
	public AdresseEntreprise facturation;

	public void set(AdresseEntreprise adresse) {
		if (EnumTypeAdresseEntreprise.SIEGE.equals(adresse.getType())) {
			Assert.isNull(siege, "Plus d'une adresse 'siège' détectée.");
			siege = adresse;
		}
		else if (EnumTypeAdresseEntreprise.COURRIER.equals(adresse.getType())) {
			Assert.isNull(courrier, "Plus d'une adresse 'courrier' détectée.");
			courrier = adresse;
		}
		else if (EnumTypeAdresseEntreprise.FACTURATION.equals(adresse.getType())) {
			Assert.isNull(facturation, "Plus d'une adresse 'secondaire' détectée.");
			facturation = adresse;
		}
		else {
			Assert.fail("Type d'adresse inconnue");
		}
	}

	public AdresseEntreprise ofType(EnumTypeAdresseEntreprise type) {
		if (EnumTypeAdresseEntreprise.SIEGE.equals(type)) {
			return siege;
		}
		else if (EnumTypeAdresseEntreprise.COURRIER.equals(type)) {
			return courrier;
		}
		else {
			Assert.isTrue(EnumTypeAdresseEntreprise.FACTURATION.equals(type));
			return facturation;
		}
	}
}
