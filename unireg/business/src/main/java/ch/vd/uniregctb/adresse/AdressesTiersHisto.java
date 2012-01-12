package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Contient toutes les adresses surchargées (= spécifiées différemment du registre civil)
 */
public class AdressesTiersHisto {
	public final List<AdresseTiers> courrier = new ArrayList<AdresseTiers>();
	public final List<AdresseTiers> representation = new ArrayList<AdresseTiers>();
	public final List<AdresseTiers> poursuite = new ArrayList<AdresseTiers>();
	public final List<AdresseTiers> domicile = new ArrayList<AdresseTiers>();

	public List<AdresseTiers> ofType(TypeAdresseTiers type) {
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

	public void add(AdresseTiers adresse) {
		switch (adresse.getUsage()) {
		case COURRIER:
			courrier.add(adresse);
			break;
		case REPRESENTATION:
			representation.add(adresse);
			break;
		case POURSUITE:
			poursuite.add(adresse);
			break;
		case DOMICILE:
			domicile.add(adresse);
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + adresse.getUsage().name());
		}
	}
}