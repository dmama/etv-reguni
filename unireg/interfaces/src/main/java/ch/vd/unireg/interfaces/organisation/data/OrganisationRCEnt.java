package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;

public class OrganisationRCEnt implements Organisation, Serializable {

	private static final long serialVersionUID = -1369195090559377725L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long numeroOrganisation;

	private final List<DateRanged<String>> numeroIDE;

	@NotNull
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> nomsAdditionels;
	private final List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private final Map<Long, SiteOrganisation> donneesSites;

	private final List<DateRanged<Long>> transfereA;
	private final List<DateRanged<Long>> transferDe;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;

	public OrganisationRCEnt(long numeroOrganisation,
	                         @NotNull Map<String, List<DateRanged<String>>> identifiants,
	                         @NotNull List<DateRanged<String>> nom,
	                         List<DateRanged<String>> nomsAdditionels,
	                         List<DateRanged<FormeLegale>> formeLegale,
	                         @NotNull Map<Long, SiteOrganisation> donneesSites,
	                         List<DateRanged<Long>> transfereA, List<DateRanged<Long>> transferDe,
	                         List<DateRanged<Long>> remplacePar, List<DateRanged<Long>> enRemplacementDe) {
		this.numeroOrganisation = numeroOrganisation;
		this.numeroIDE = OrganisationHelper.extractIdentifiant(identifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.nomsAdditionels = nomsAdditionels;
		this.formeLegale = formeLegale;
		this.donneesSites = donneesSites;
		this.transfereA = transfereA;
		this.transferDe = transferDe;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	@Override
	public long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	@Override
	public List<DateRanged<String>> getNumeroIDE() {
		return numeroIDE;
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
	@Override
	public List<Siege> getSiegesPrincipaux() {
		return extractDataFromSitesPrincipaux(new DateRangeLimitatorImpl<Siege>(), new SiteDataExtractor<List<Siege>>() {
			@Override
			public List<Siege> extractData(SiteOrganisation site) {
				return site.getSieges();
			}
		});
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return Le siège, ou null si aucun siège valide à la date donnée
	 */
	@Override
	public Siege getSiegePrincipal(RegDate date) {
		return DateRangeHelper.rangeAt(getSiegesPrincipaux(), date != null ? date : RegDate.get());
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	public FormeLegale getFormeLegale(RegDate date) {
		List<DateRanged<FormeLegale>> formeLegaleRanges = getFormeLegale();
		if (formeLegaleRanges != null) {
			DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(formeLegaleRanges, date != null ? date : RegDate.get());
			return formeLegaleRange != null ? formeLegaleRange.getPayload() : null;
		}
		return null;
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
	@Override
	public List<Capital> getCapitaux() {
		return extractDataFromSitesPrincipaux(new DateRangeLimitatorImpl<Capital>(), new SiteDataExtractor<List<Capital>>() {
			@Override
			public List<Capital> extractData(SiteOrganisation site) {
				return site.getDonneesRC() != null ? site.getDonneesRC().getCapital() : null;
			}
		});
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
	private <T> List<DateRanged<T>> extractRangedDataFromSitesPrincipaux(SiteDataExtractor<List<DateRanged<T>>> extractor) {
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
	private <T extends DateRange> List<T> extractDataFromSitesPrincipaux(final DateRangeLimitator<T> limitator, SiteDataExtractor<List<T>> extractor) {
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

	@Override
	@NotNull
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<>(donneesSites.values());
	}

	@Override
	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	@Override
	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@Override
	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	@Override
	public List<DateRanged<String>> getNomsAdditionels() {
		return nomsAdditionels;
	}

	@Override
	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	@Override
	public List<DateRanged<Long>> getTransferDe() {
		return transferDe;
	}

	@Override
	public List<DateRanged<Long>> getTransfereA() {
		return transfereA;
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

	@Override
	public List<Adresse> getAdresses() {
		// la règle dit :
		// - en l'absence de données IDE, on prend l'adresse légale des données RC
		// - sinon, c'est l'adresse effective des données IDE qui fait foi

		final DateRangeLimitatorImpl<AdresseRCEnt> limitator = new DateRangeLimitatorImpl<>();
		final List<AdresseRCEnt> rcLegale = extractDataFromSitesPrincipaux(limitator, new SiteDataExtractor<List<AdresseRCEnt>>() {
			@Override
			public List<AdresseRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRC() == null ? null : site.getDonneesRC().getAdresseLegale();
			}
		});
		final List<AdresseRCEnt> ideEffective = extractDataFromSitesPrincipaux(limitator, new SiteDataExtractor<List<AdresseRCEnt>>() {
			@Override
			public List<AdresseRCEnt> extractData(SiteOrganisation site) {
				return site.getDonneesRegistreIDE() == null ? null : site.getDonneesRegistreIDE().getAdresseEffective();
			}
		});
		final List<AdresseRCEnt> flat = DateRangeHelper.override(rcLegale, ideEffective, buildAdapterCallbackFromLimitator(limitator));
		return new ArrayList<Adresse>(flat);
	}

	// TODO: A générer dans l'adapter?
	/**
	 * Liste des sites principaux
	 * @return La liste des sites principaux
	 */
	@Override
	public List<DateRanged<SiteOrganisation>> getSitePrincipaux() {
		List<DateRanged<SiteOrganisation>> sitePrincipaux = new ArrayList<>();
		for (SiteOrganisation site : this.getDonneesSites()) {
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
	 * Le site principal à une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date
	 * @return Le site princpial
	 */
	@Override
	public DateRanged<SiteOrganisation> getSitePrincipal(RegDate date) {
		RegDate theDate= date != null ? date : RegDate.get();
		return DateRangeHelper.rangeAt(getSitePrincipaux(), theDate);
	}

	// TODO: A générer dans l'adapter?
	/**
	 * Liste des sites secondaire pour une date donnée. Si la date est nulle, la date du jour est utilisée.
	 * @param date La date pour laquelle on désire la liste des sites secondaires
	 * @return La liste des sites secondaire
	 */
	@Override
	public List<SiteOrganisation> getSitesSecondaires(RegDate date) {
		RegDate theDate= date != null ? date : RegDate.get();
		List<SiteOrganisation> siteSecondaires = new ArrayList<>();
		for (SiteOrganisation site : this.getDonneesSites()) {
			for (DateRanged<TypeDeSite> siteRange : site.getTypeDeSite()) {
				if (siteRange != null && siteRange.getPayload() == TypeDeSite.ETABLISSEMENT_SECONDAIRE && siteRange.isValidAt(theDate)) {
					siteSecondaires.add(site);
				}
			}
		}
		return siteSecondaires;
	}
}
