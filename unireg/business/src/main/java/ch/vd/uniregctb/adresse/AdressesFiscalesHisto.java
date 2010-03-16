package ch.vd.uniregctb.adresse;

import java.util.List;

import ch.vd.uniregctb.type.TypeAdresseTiers;


/**
 * Contient l'historique des adresses fiscales (= spécifiées dans le registre civil, ou surchargées dans le registre fiscal).
 */
public class AdressesFiscalesHisto {
	public List<AdresseGenerique> courrier;
	public List<AdresseGenerique> representation;
	public List<AdresseGenerique> poursuite;
	public List<AdresseGenerique> domicile;

	public List<AdresseGenerique> ofType(TypeAdresseTiers type) {
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

	public void add(TypeAdresseTiers type, AdresseGenerique adresse) {
		switch (type) {
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
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}
}