package ch.vd.uniregctb.adresse;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Contient les adresses fiscales (= spécifiées dans le registre civil, ou surchargées dans le registre fiscal) à un instant donné.
 */
public class AdressesFiscales {

	public AdresseGenerique courrier;
	public AdresseGenerique representation;
	public AdresseGenerique domicile;
	public AdresseGenerique poursuite;

	/**
	 * Adresse du tiers <i>représentant</i> le tiers principal dans le cas d'une poursuite (voir spécification "BesoinsContentieux.doc").
	 */
	public AdresseGenerique poursuiteAutreTiers; 

	public AdresseGenerique ofType(TypeAdresseTiers type) {
		switch (type) {
		case COURRIER:
			return courrier;
		case REPRESENTATION:
			return representation;
		case DOMICILE:
			return domicile;
		case POURSUITE:
			return poursuite;
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
		case POURSUITE_AUTRE_TIERS:
			return poursuiteAutreTiers;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	public void set(TypeAdresseFiscale type, AdresseGenerique adresse) {
		switch (type) {
		case COURRIER:
			assertIsEmpty(courrier, adresse, TypeAdresseFiscale.COURRIER);
			courrier = adresse;
			break;
		case REPRESENTATION:
			assertIsEmpty(representation, adresse, TypeAdresseFiscale.REPRESENTATION);
			representation = adresse;
			break;
		case DOMICILE:
			assertIsEmpty(domicile, adresse, TypeAdresseFiscale.DOMICILE);
			domicile = adresse;
			break;
		case POURSUITE:
			assertIsEmpty(poursuite, adresse, TypeAdresseFiscale.POURSUITE);
			poursuite = adresse;
			break;
		case POURSUITE_AUTRE_TIERS:
			assertIsEmpty(poursuiteAutreTiers, adresse, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS);
			poursuiteAutreTiers = adresse;
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	private static void assertIsEmpty(AdresseGenerique existante, AdresseGenerique nouvelle, TypeAdresseFiscale type) {
		if (existante != null) {
			Assert.fail("Deux adresses fiscales de type " + type.name() + " se recoupent. Adresse existante = [" + existante.getDateDebut()
					+ ", " + existante.getDateFin() + "] et nouvelle adresse = [" + nouvelle.getDateDebut() + ", " + existante.getDateFin()
					+ "]");
		}
	}
}
