package ch.vd.uniregctb.adresse;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeAdresseTiers;


/**
 * Contient l'historique des adresses fiscales (= spécifiées dans le registre civil, ou surchargées dans le registre fiscal).
 */
public class AdressesFiscalesHisto {
	public List<AdresseGenerique> courrier;
	public List<AdresseGenerique> representation;
	public List<AdresseGenerique> domicile;
	public List<AdresseGenerique> poursuite;

	/**
	 * Adresses du/des tiers <i>représentant</i> le tiers principal dans le cas d'une poursuite (voir spécification "BesoinsContentieux.doc").
	 */
	public List<AdresseGenerique> poursuiteAutreTiers;

	public List<AdresseGenerique> ofType(TypeAdresseTiers type) {
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

	public List<AdresseGenerique> ofType(TypeAdresseFiscale type) {
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

	public void add(TypeAdresseFiscale type, AdresseGenerique adresse) {
		switch (type) {
		case COURRIER:
			courrier.add(adresse);
			break;
		case REPRESENTATION:
			representation.add(adresse);
			break;
		case DOMICILE:
			domicile.add(adresse);
			break;
		case POURSUITE:
			poursuite.add(adresse);
			break;
		case POURSUITE_AUTRE_TIERS:
			poursuiteAutreTiers.add(adresse);
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

	/**
	 * @param date la date de validité demandée
	 * @return les adresses fiscales valides à une date donnée.
	 */
	public AdressesFiscales at(RegDate date) {
		final AdressesFiscales adresses = new AdressesFiscales();
		for (TypeAdresseFiscale type : TypeAdresseFiscale.values()) {
			final List<AdresseGenerique> list = ofType(type);
			if (list != null) {
				for (AdresseGenerique a : list) {
					if (a.isValidAt(date)) {
						adresses.set(type, a);
						break;
					}
				}
			}
		}
		return adresses;
	}
}