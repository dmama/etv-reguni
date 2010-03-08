package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Contient les adresses fiscales (= spécifiées dans le registre civil, ou surchargées dans le registre fiscal) à un instant donné.
 */
public class AdressesFiscales {

	public AdresseGenerique courrier;
	public AdresseGenerique representation;
	public AdresseGenerique poursuite;
	public AdresseGenerique domicile;

	public AdresseGenerique ofType(TypeAdresseTiers type) {
		switch (type) {
		case COURRIER:
			return courrier;
		case REPRESENTATION:
			return representation;
		case POURSUITE:
			return poursuite;
		case DOMICILE:
			return domicile;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	public AdresseGenerique ofType(TypeAdresseFiscale type) {
		switch (type) {
		case COURRIER:
			return courrier;
		case REPRESENTATION:
			return representation;
		case POURSUITE:
			return poursuite;
		case DOMICILE:
			return domicile;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	public void set(TypeAdresseFiscale type, AdresseGenerique adresse) {
		switch (type) {
		case COURRIER:
			assertIsEmpty(courrier, adresse, TypeAdresseTiers.COURRIER);
			courrier = adresse;
			break;
		case REPRESENTATION:
			assertIsEmpty(representation, adresse, TypeAdresseTiers.REPRESENTATION);
			representation = adresse;
			break;
		case POURSUITE:
			assertIsEmpty(poursuite, adresse, TypeAdresseTiers.POURSUITE);
			poursuite = adresse;
			break;
		case DOMICILE:
			assertIsEmpty(domicile, adresse, TypeAdresseTiers.DOMICILE);
			domicile = adresse;
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	private static void assertIsEmpty(AdresseGenerique existante, AdresseGenerique nouvelle, TypeAdresseTiers type) {
		if (existante != null) {
			Assert.fail("Deux adresses fiscales de type " + type.name() + " se recoupent. Adresse existante = [" + existante.getDateDebut()
					+ ", " + existante.getDateFin() + "] et nouvelle adresse = [" + nouvelle.getDateDebut() + ", " + existante.getDateFin()
					+ "]");
		}
	}
}
