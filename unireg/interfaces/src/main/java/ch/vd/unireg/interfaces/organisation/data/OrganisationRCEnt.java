package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class OrganisationRCEnt implements Organisation, Serializable {

	private static final long serialVersionUID = 4122626842958310332L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long idOrganisation;

	private final List<DateRanged<String>> numeroIDE;

	@NotNull
	private final List<DateRanged<String>> nom;
	private final List<DateRanged<String>> nomsAdditionels;
	private final List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private final List<DateRanged<Long>> sites;
	@NotNull
	private final Map<Long, SiteOrganisation> donneesSites;

	private final List<DateRanged<Long>> transfereA;
	private final List<DateRanged<Long>> transferDe;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;

	public OrganisationRCEnt(@NotNull Map<String, List<DateRanged<String>>> identifiants, @NotNull List<DateRanged<String>> nom,
	                         List<DateRanged<String>> nomsAdditionels, List<DateRanged<FormeLegale>> formeLegale, @NotNull List<DateRanged<Long>> sites,
	                         @NotNull Map<Long, SiteOrganisation> donneesSites, List<DateRanged<Long>> transfereA, List<DateRanged<Long>> transferDe,
	                         List<DateRanged<Long>> remplacePar, List<DateRanged<Long>> enRemplacementDe) {
		this.idOrganisation = OrganisationHelper.extractIdCantonal(identifiants);
		this.numeroIDE = OrganisationHelper.extractIdentifiant(identifiants, OrganisationConstants.CLE_IDE);
		this.nom = nom;
		this.nomsAdditionels = nomsAdditionels;
		this.formeLegale = formeLegale;
		this.sites = sites;
		this.donneesSites = donneesSites;
		this.transfereA = transfereA;
		this.transferDe = transferDe;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	@Override
	public long getNumeroOrganisation() {
		return idOrganisation;
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
	 * TODO: Ecrire le test
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	public List<DateRanged<Integer>> getSiegePrincipal() {
		return extractDataFromSitesPrincipaux(new SiteDataExtractor<List<DateRanged<Integer>>>() {
			@Override
			public List<DateRanged<Integer>> extractData(SiteOrganisation site) {
				return site.getSiege();
			}
		});
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return L'identifiant OFS, ou null si absent
	 */
	public Integer getSiegePrincipal(RegDate date) {
		DateRanged<Integer> siegeRanged = DateRangeHelper.rangeAt(getSiegePrincipal(), date != null ? date : RegDate.get());
		return siegeRanged != null ? siegeRanged.getPayload() : null;
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
	 * TODO: Ecrire le test
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	public List<DateRanged<Capital>> getCapital() {
		return extractDataFromSitesPrincipaux(new SiteDataExtractor<List<DateRanged<Capital>>>() {
			@Override
			public List<DateRanged<Capital>> extractData(SiteOrganisation site) {
				return site.getDonneesRC() != null ? site.getDonneesRC().getCapital() : null;
			}
		});
	}

	private interface SiteDataExtractor<T> {
		T extractData(SiteOrganisation site);
	}

	private <T> List<DateRanged<T>> extractDataFromSitesPrincipaux(SiteDataExtractor<List<DateRanged<T>>> extractor) {
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
	public List<SiteOrganisation> getDonneesSites() {
		return new ArrayList<>(donneesSites.values());
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionels() {
		return nomsAdditionels;
	}

	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	@NotNull
	public List<DateRanged<Long>> getSites() {
		return sites;
	}

	public List<DateRanged<Long>> getTransferDe() {
		return transferDe;
	}

	public List<DateRanged<Long>> getTransfereA() {
		return transfereA;
	}
}
