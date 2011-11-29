package ch.vd.uniregctb.adresse;

import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Contient les adresses surchargées (= spécifiées différemment du registre civil) à un instant donné.
 */
public class AdressesTiers {

	public AdresseTiers courrier;
	public AdresseTiers representation;
	public AdresseTiers poursuite;
	public AdresseTiers domicile;

	public AdresseTiers ofType(TypeAdresseTiers type) {
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

	public void set(AdresseTiers adresse) throws AdressesResolutionException {
		switch (adresse.getUsage()) {
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
			throw new IllegalArgumentException("Type d'adresse inconnu = " + adresse.getUsage().name());
		}
	}

	private static void assertIsEmpty(AdresseTiers existante, AdresseTiers nouvelle, TypeAdresseTiers type) throws AdressesResolutionException {
		if (existante != null) {
			AdressesResolutionException exception = new AdressesResolutionException("Deux adresses fiscales de type " + type.name() + " se recoupent. Adresse existante = [" + existante.getDateDebut()
					+ ", " + existante.getDateFin() + "] et nouvelle adresse = [" + nouvelle.getDateDebut() + ", " + existante.getDateFin()
					+ ']');
			exception.addAdresse(existante);
			exception.addAdresse(nouvelle);
			throw exception;
		}
	}
}
