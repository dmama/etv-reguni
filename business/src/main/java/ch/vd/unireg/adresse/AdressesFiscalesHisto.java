package ch.vd.unireg.adresse;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.type.TypeAdresseTiers;


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
	 * @return l'adresse fiscale valide (et non-annulée) à une date donnée.
	 */
	public AdressesFiscales at(RegDate date) {
		final AdressesFiscales adresses = new AdressesFiscales();
		for (TypeAdresseFiscale type : TypeAdresseFiscale.values()) {
			final List<AdresseGenerique> list = ofType(type);
			if (list != null) {
				for (AdresseGenerique a : list) {
					if (a.isValidAt(date)) {
						Assert.isFalse(a.isAnnule()); // [UNIREG-2895] on s'assure que les adresses annulées sont ignorées
						adresses.set(type, a);
						break;
					}
				}
			}
		}
		return adresses;
	}

	/**
	 * @param predicate prédicat de filtrage sur les adresses
	 * @return une nouvelle instance d'AdressesFiscalesHisto basée sur les adresses filtrées de l'instance courante
	 */
	public AdressesFiscalesHisto filter(Predicate<? super AdresseGenerique> predicate) {
		final AdressesFiscalesHisto filtered = new AdressesFiscalesHisto();
		filter(courrier, list -> filtered.courrier = list, predicate);
		filter(representation, list -> filtered.representation = list, predicate);
		filter(domicile, list -> filtered.domicile = list, predicate);
		filter(poursuite, list -> filtered.poursuite = list, predicate);
		filter(poursuiteAutreTiers, list -> filtered.poursuiteAutreTiers = list, predicate);
		return filtered;
	}

	private static void filter(List<AdresseGenerique> source, Consumer<List<AdresseGenerique>> setter, Predicate<? super AdresseGenerique> predicate) {
		if (source == null || source.isEmpty()) {
			setter.accept(source);
		}
		else {
			final List<AdresseGenerique> filtered = source.stream()
					.filter(predicate)
					.collect(Collectors.toList());
			setter.accept(filtered);
		}
	}
}