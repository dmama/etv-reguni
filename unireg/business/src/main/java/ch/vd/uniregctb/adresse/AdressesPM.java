package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.type.TypeAdressePM;

/**
 * Contient les adresses PM à un instant donné d'une entreprise regroupées par type.
 */
public class AdressesPM {

	public AdresseEntreprise siege;
	public AdresseEntreprise courrier;
	public AdresseEntreprise facturation;

	public void set(AdresseEntreprise adresse) {
		if (TypeAdressePM.SIEGE.equals(adresse.getType())) {
			Assert.isNull(siege, "Plus d'une adresse 'siège' détectée.");
			siege = adresse;
		}
		else if (TypeAdressePM.COURRIER.equals(adresse.getType())) {
			Assert.isNull(courrier, "Plus d'une adresse 'courrier' détectée.");
			courrier = adresse;
		}
		else if (TypeAdressePM.FACTURATION.equals(adresse.getType())) {
			Assert.isNull(facturation, "Plus d'une adresse 'secondaire' détectée.");
			facturation = adresse;
		}
		else {
			Assert.fail("Type d'adresse inconnue");
		}
	}

	public AdresseEntreprise ofType(TypeAdressePM type) {
		if (TypeAdressePM.SIEGE.equals(type)) {
			return siege;
		}
		else if (TypeAdressePM.COURRIER.equals(type)) {
			return courrier;
		}
		else {
			Assert.isTrue(TypeAdressePM.FACTURATION.equals(type));
			return facturation;
		}
	}
}
