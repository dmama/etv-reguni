package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.TypeAdresseTiers;


/**
 * Contient l'historique des adresses d'envoi.
 */
public class AdressesEnvoiHisto {
	public List<AdresseEnvoiDetaillee> courrier;
	public List<AdresseEnvoiDetaillee> representation;
	public List<AdresseEnvoiDetaillee> domicile;
	public List<AdresseEnvoiDetaillee> poursuite;

	/**
	 * Adresses du/des tiers <i>représentant</i> le tiers principal dans le cas d'une poursuite (voir spécification "BesoinsContentieux.doc").
	 */
	public List<AdresseEnvoiDetaillee> poursuiteAutreTiers;

	public List<AdresseEnvoiDetaillee> ofType(TypeAdresseTiers type) {
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

	public List<AdresseEnvoiDetaillee> ofType(TypeAdresseFiscale type) {
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

	public void add(TypeAdresseFiscale type, AdresseEnvoiDetaillee adresse) {
		switch (type) {
		case COURRIER:
			if (courrier == null) {
				courrier = new ArrayList<AdresseEnvoiDetaillee>();
			}
			courrier.add(adresse);
			break;
		case REPRESENTATION:
			if (representation == null) {
				representation = new ArrayList<AdresseEnvoiDetaillee>();
			}
			representation.add(adresse);
			break;
		case DOMICILE:
			if (domicile == null) {
				domicile = new ArrayList<AdresseEnvoiDetaillee>();
			}
			domicile.add(adresse);
			break;
		case POURSUITE:
			if (poursuite == null) {
				poursuite = new ArrayList<AdresseEnvoiDetaillee>();
			}
			poursuite.add(adresse);
			break;
		case POURSUITE_AUTRE_TIERS:
			if (poursuiteAutreTiers == null) {
				poursuiteAutreTiers = new ArrayList<AdresseEnvoiDetaillee>();
			}
			poursuiteAutreTiers.add(adresse);
			break;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = " + type.name());
		}
	}

//	/**
//	 * @param date la date de validité demandée
//	 * @return l'adresse fiscale valide (et non-annulée) à une date donnée.
//	 */
//	public AdressesFiscales at(RegDate date) {
//		final AdressesFiscales adresses = new AdressesFiscales();
//		for (TypeAdresseFiscale type : TypeAdresseFiscale.values()) {
//			final List<AdresseEnvoiDetaillee> list = ofType(type);
//			if (list != null) {
//				for (AdresseEnvoiDetaillee a : list) {
//					if (a.isValidAt(date)) {
//						adresses.set(type, a);
//						break;
//					}
//				}
//			}
//		}
//		return adresses;
//	}
}