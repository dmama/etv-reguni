package ch.vd.unireg.interfaces.organisation.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

/**
 * Quelques méthodes pratiques d'interprétation des données dans le cadre des organisations
 *
 * Règle générale: Toutes les dates sont optionnelles et en leur absence, la date du jour est utilisée.
 *
 * Utiliser la méthode defaultDate(RegDate date) pour gérer la date automatiquement.
 */
public abstract class OrganisationHelper {

	/**
	 * Détermine les valeurs en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param map
	 * @param date
	 * @param <K> Type de la clé identifiant la valeur
	 * @param <V> Type de la valeur
	 * @return La liste des valeurs pour la date fournie ou la date du jour si non fournie. null si map était null
	 */
	public static <K, V> List<V> valuesForDate(@Nullable Map<K, List<DateRanged<V>>> map, @Nullable RegDate date) {
		if (map == null) {
			return null;
		}
		List<V> na = new ArrayList<>();
		for (Map.Entry<K, List<DateRanged<V>>> entry : map.entrySet()) {
			final DateRanged<V> vDateRanged = DateRangeHelper.rangeAt(entry.getValue(), defaultDate(date));
			if (vDateRanged != null) {
				na.add(vDateRanged.getPayload());
			}
		}
		return na;
	}

	/**
	 * Détermine la valeur en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param list
	 * @param date Date de valeur
	 * @param <V> Type de la valeur
	 * @return La valeur pour la date fournie, ou null si pas de valeur à cette date.
	 */
	public static <V> V valueForDate(@Nullable List<DateRanged<V>> list, @Nullable RegDate date) {
		if (list == null) {
			return null;
		}
		DateRanged<V> item = DateRangeHelper.rangeAt(list, defaultDate(date));
		return item != null ? item.getPayload() : null;
	}

	/**
	 * Détermine la valeur en cours à une date donnée, ou la date du jour si pas de date fournie.
	 * @param list
	 * @param date Date de valeur
	 * @param <V> Type de la valeur
	 * @return La valeur pour la date fournie, ou null si pas de valeur à cette date.
	 */
	public static <V extends DateRange> V dateRangeForDate(@Nullable List<V> list, @Nullable RegDate date) {
		if (list == null) {
			return null;
		}
		return  DateRangeHelper.rangeAt(list, defaultDate(date));
	}

	/**
	 * @param identifiants map d'identifiants datés triés par une clé qui indique leur type (numéro IDE, identifiant cantonal...)
	 * @param cle la clé en question (CT.VD.PARTY pour l'identifiant cantonal, CH.IDE pour le numéro IDE)
	 * @return la liste historisée des valeurs de ce type
	 */
	@Nullable
	public static List<DateRanged<String>> extractIdentifiant(Map<String, List<DateRanged<String>>> identifiants, String cle) {
		final List<DateRanged<String>> extracted = identifiants.get(cle);
		return extracted == null || extracted.isEmpty() ? null : extracted;
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	public static FormeLegale getFormeLegale(Organisation organisation, RegDate date) {
		DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(organisation.getFormeLegale(), defaultDate(date));
		return formeLegaleRange != null ? formeLegaleRange.getPayload() : null;
	}

	/**
	 * Determine le siège principal en vigueur le jour précédant la date.
	 * @param organisation L'organisation concernée
	 * @param date La date de référence
	 * @return Le siège le jour précédant, ou null si aucun.
	 */
	public static Siege siegePrincipalPrecedant(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getSiegesPrincipaux(), defaultDate(date).getOneDayBefore());
	}

	public static DateRanged<SiteOrganisation> getSitePrincipal(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getSitePrincipaux(), defaultDate(date));
	}

	/**
	 * Liste des sites principaux
	 * @return La liste des sites principaux
	 */
	public static List<DateRanged<SiteOrganisation>> getSitePrincipaux(Organisation organisation) {
		List<DateRanged<SiteOrganisation>> sitePrincipaux = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
					sitePrincipaux.add(new DateRanged<>(siteRange.getDateDebut(), siteRange.getDateFin(), site));
				}
			}
		}
		return sitePrincipaux;
	}

	// TODO: A générer dans l'adapter?
	/**
	 * Liste des sites secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des sites secondaires
	 * @return La liste des sites secondaire
	 */
	public static List<SiteOrganisation> getSitesSecondaires(Organisation organisation, @Nullable RegDate date) {
		List<SiteOrganisation> siteSecondaires = new ArrayList<>();
		for (SiteOrganisation site : organisation.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_SECONDAIRE && siteRange.isValidAt(defaultDate(date))) {
					siteSecondaires.add(site);
				}
			}
		}
		return siteSecondaires;
	}

	public static List<Adresse> getAdresses(Map<Long, SiteOrganisation> donneesSites) {
		// la règle dit :
		// - en l'absence de données IDE, on prend l'adresse légale des données RC
		// - sinon, c'est l'adresse effective des données IDE qui fait foi

		final DateRangeLimitatorImpl<AdresseRCEnt> limitator = new DateRangeLimitatorImpl<>();
		final List<AdresseRCEnt> rcLegale = extractDataFromSitesPrincipaux(donneesSites, limitator, new SiteDataExtractor<List<AdresseRCEnt>>() {
			@Override
			public List<AdresseRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRC() == null ? null : site.getDonneesRC().getAdresseLegale();
			}
		});
		final List<AdresseRCEnt> ideEffective = extractDataFromSitesPrincipaux(donneesSites, limitator, new SiteDataExtractor<List<AdresseRCEnt>>() {
			@Override
			public List<AdresseRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRegistreIDE() == null ? null : site.getDonneesRegistreIDE().getAdresseEffective();
			}
		});
		final List<AdresseRCEnt> flat = DateRangeHelper.override(rcLegale, ideEffective, buildAdapterCallbackFromLimitator(limitator));
		return new ArrayList<Adresse>(flat);
	}

	public static List<Adresse> getAdressesPourSite(SiteOrganisation site) {
		// la règle dit :
		// - en l'absence de données IDE, on prend l'adresse légale des données RC
		// - sinon, c'est l'adresse effective des données IDE qui fait foi

		List<AdresseRCEnt> rcLegale = site.getDonneesRC() == null ? null : site.getDonneesRC().getAdresseLegale();
		List<AdresseRCEnt> ideEffective = site.getDonneesRegistreIDE() == null ? null : site.getDonneesRegistreIDE().getAdresseEffective();

		final DateRangeLimitatorImpl<AdresseRCEnt> limitator = new DateRangeLimitatorImpl<>();
		final List<AdresseRCEnt> flat = DateRangeHelper.override(rcLegale, ideEffective, buildAdapterCallbackFromLimitator(limitator));
		return new ArrayList<Adresse>(flat);
	}

	/**
	 * Retourne une liste représantant la succession des valeurs de capital de l'entreprise.
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	public static List<Capital> getCapitaux(Map<Long, SiteOrganisation> donneesSites) {
		return extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<Capital>(), new SiteDataExtractor<List<Capital>>() {
			@Override
			public List<Capital> extractData(SiteOrganisation site) {
				return site.getDonneesRC() != null ? site.getDonneesRC().getCapital() : null;
			}
		});
	}

	public static Capital getCapital(Organisation organisation, @Nullable RegDate date) {
		return DateRangeHelper.rangeAt(organisation.getCapitaux(), defaultDate(date));
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	public static List<Siege> getSiegesPrincipaux(Map<Long, SiteOrganisation> donneesSites) {
		return extractDataFromSitesPrincipaux(donneesSites, new DateRangeLimitatorImpl<Siege>(), new SiteDataExtractor<List<Siege>>() {
			@Override
			public List<Siege> extractData(SiteOrganisation site) {
				return site.getSieges();
			}
		});
	}

	public static boolean isInscritAuRC(Organisation organisation, RegDate date) {
		DateRanged<StatusRC> statusRCDateRanged = DateRangeHelper.rangeAt(organisation.getSitePrincipal(defaultDate(date)).getPayload().getDonneesRC().getStatus(), defaultDate(date));
		return statusRCDateRanged != null && statusRCDateRanged.getPayload() == StatusRC.INSCRIT;
	}


	private static RegDate defaultDate(RegDate date) {
		return date != null ? date : RegDate.get();
	}

	private interface SiteDataExtractor<T> {
		T extractData(SiteOrganisation site);
	}

	private interface DateRangeLimitator<T extends DateRange> {
		T limitTo(T source, RegDate dateDebut, RegDate dateFin);
	}

	private static class DateRangeLimitatorImpl<T extends DateRange & DateRangeLimitable<T>> implements DateRangeLimitator<T> {
		public T limitTo(T source, RegDate dateDebut, RegDate dateFin) {
			return source.limitTo(dateDebut, dateFin);
		}
	}

	@NotNull
	private static <T> List<DateRanged<T>> extractRangedDataFromSitesPrincipaux(Map<Long, SiteOrganisation> donneesSites, SiteDataExtractor<List<DateRanged<T>>> extractor) {
		final List<DateRanged<T>> extracted = new ArrayList<>();
		for (Map.Entry<Long, SiteOrganisation> entry : donneesSites.entrySet()) {
			final SiteOrganisation site = entry.getValue();
			final List<DateRanged<T>> toExtract = extractor.extractData(site);
			if (toExtract != null && !toExtract.isEmpty()) {
				for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
					if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
						final List<DateRanged<T>> extractedData = DateRangeHelper.extract(toExtract,
						                                                                  type.getDateDebut(),
						                                                                  type.getDateFin(),
						                                                                  new DateRangeHelper.AdapterCallback<DateRanged<T>>() {
							                                                                  @Override
							                                                                  public DateRanged<T> adapt(DateRanged<T> range, RegDate debut, RegDate fin) {
								                                                                  return new DateRanged<>(debut != null ? debut : range.getDateDebut(),
								                                                                                          fin != null ? fin : range.getDateFin(),
								                                                                                          range.getPayload());
							                                                                  }
						                                                                  });
						extracted.addAll(extractedData);
					}
				}
			}
		}
		Collections.sort(extracted, new DateRangeComparator<>());
		return extracted;
	}

	@NotNull
	private static <T extends DateRange> List<T> extractDataFromSitesPrincipaux(Map<Long, SiteOrganisation> donneesSites, final DateRangeLimitator<T> limitator, SiteDataExtractor<List<T>> extractor) {
		final List<T> extracted = new ArrayList<>();
		for (Map.Entry<Long, SiteOrganisation> entry : donneesSites.entrySet()) {
			final SiteOrganisation site = entry.getValue();
			final List<T> toExtract = extractor.extractData(site);
			if (toExtract != null && !toExtract.isEmpty()) {
				for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
					if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
						final List<T> extractedData = DateRangeHelper.extract(toExtract,
						                                                      type.getDateDebut(),
						                                                      type.getDateFin(),
						                                                      buildAdapterCallbackFromLimitator(limitator));
						extracted.addAll(extractedData);
					}
				}
			}
		}
		Collections.sort(extracted, new DateRangeComparator<>());
		return extracted;
	}

	private static <T extends DateRange> DateRangeHelper.AdapterCallback<T> buildAdapterCallbackFromLimitator(final DateRangeLimitator<T> limitator) {
		return new DateRangeHelper.AdapterCallback<T>() {
			@Override
			public T adapt(T range, RegDate debut, RegDate fin) {
				if (debut == null && fin == null) {
					return range;
				}
				return limitator.limitTo(range,
				                         debut == null ? range.getDateDebut() : debut,
				                         fin == null ? range.getDateFin() : fin);
			}
		};
	}
}
