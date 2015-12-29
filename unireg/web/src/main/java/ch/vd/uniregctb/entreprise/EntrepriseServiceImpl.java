package ch.vd.uniregctb.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.CapitalHisto;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.EtatEntrepriseView;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 */
public class EntrepriseServiceImpl implements EntrepriseService {

	private ServiceOrganisationService serviceOrganisationService;
	private TiersService tiersService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	@Override
	public EntrepriseView get(Entreprise entreprise) {

		EntrepriseView entrepriseView = new EntrepriseView();

		Long numeroEntreprise = entreprise.getNumeroEntreprise();

		if (numeroEntreprise != null) {
			/*
				L'entreprise a un identifiant cantonal et donc existe dans le registre civil cantonal.
			 */
			entrepriseView.setSource(EntrepriseView.SourceCivile.RCENT);

			Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);

			entrepriseView.setRaisonSociale(organisation.getNom());
			Collections.sort(entrepriseView.getRaisonSociale(), new DateRangeComparator<>());

			List<DateRanged<String>> nomsAdditionnels = new ArrayList<>();
			for (List<DateRanged<String>> noms : organisation.getNomsAdditionnels().values()) {
				nomsAdditionnels.addAll(noms);
			}
			Collections.sort(nomsAdditionnels, new DateRangeComparator<>());
			entrepriseView.setNomsAdditionnels(nomsAdditionnels);

			entrepriseView.setSieges(getSiegesFromOrganisation(organisation.getSiegesPrincipaux()));
			entrepriseView.setFormesJuridiques(getFormesJuridiques(organisation.getFormeLegale()));
			//entrepriseView.setEtats(getEtatsPM(pm.getEtats()));
			List<DateRanged<String>> noIdeList = organisation.getNumeroIDE();
			if (noIdeList != null && noIdeList.size() > 0) {
				DateRanged<String> noIdeRange = noIdeList.get(0);
				if (noIdeRange != null) {
					entrepriseView.setNumerosIDE(Collections.singletonList(noIdeRange.getPayload()));
				}
			}

			DonneesRC donneesRC = organisation.getSitePrincipal(null).getPayload().getDonneesRC();
			entrepriseView.setDateInscriptionRC(CollectionsUtils.getLastElement(donneesRC.getDateInscription()).getPayload());
			entrepriseView.setStatusRC(CollectionsUtils.getLastElement(donneesRC.getStatus()).getPayload());
			entrepriseView.setDateRadiationRC(CollectionsUtils.getLastElement(donneesRC.getDateRadiation()).getPayload());

			DonneesRegistreIDE donneesRegistreIDE = organisation.getSitePrincipal(null).getPayload().getDonneesRegistreIDE();
			//entrepriseView.setDateInscritpionIde(CollectionsUtils.getLastElement(donneesRegistreIDE.getDateInscription()).getPayload()); // TODO: apporter la date d'inscription Ide en 16L1
			entrepriseView.setStatusIde(CollectionsUtils.getLastElement(donneesRegistreIDE.getStatus()).getPayload());
		}
		else {
			/*
				L'entreprise n'est pas connue du régistre civil cantonal et on doit faire avec les informations dont on dispose.
			 */
			entrepriseView.setSource(EntrepriseView.SourceCivile.UNIREG);
			final List<DonneesRegistreCommerce> donneesRC = new ArrayList<>(entreprise.getDonneesRC());
			Collections.sort(donneesRC, new DateRangeComparator<>());

			if (!donneesRC.isEmpty()) {
				List<DateRanged<String>> raisonSociale = new ArrayList<>();
				for (DonneesRegistreCommerce rc : donneesRC) {
					raisonSociale.add(new DateRanged<>(rc.getDateDebut(), rc.getDateFin(), rc.getRaisonSociale()));
				}
				entrepriseView.setRaisonSociale(raisonSociale);
			}
			entrepriseView.setSieges(extractSieges(tiersService.getEtablissementsPrincipauxEntreprise(entreprise)));
			entrepriseView.setFormesJuridiques(extractFormesJuridiques(donneesRC));
		}

		entrepriseView.setCapitaux(extractCapitaux(tiersService.getCapitaux(entreprise)));

		// les états
		final List<EtatEntreprise> etats = new ArrayList<>(entreprise.getEtats());
		Collections.sort(etats, new Comparator<EtatEntreprise>() {
			@Override
			public int compare(EtatEntreprise o1, EtatEntreprise o2) {
				int comparison = Boolean.compare(o1.isAnnule(), o2.isAnnule());     // false < true
				if (comparison == 0) {
					comparison = - o1.getDateObtention().compareTo(o2.getDateObtention());       // les plus récents d'abord
				}
				return comparison;
			}
		});
		entrepriseView.setEtats(getEtats(etats));

		return entrepriseView;
	}

	private static List<EtatEntrepriseView> getEtats(List<EtatEntreprise> data) {
		if (data == null || data.isEmpty()) {
			return Collections.emptyList();
		}
		final List<EtatEntrepriseView> views = new ArrayList<>(data.size());
		for (EtatEntreprise etat : data) {
			final EtatEntrepriseView view = new EtatEntrepriseView(etat.getId(),
			                                                       etat.getDateObtention(),
			                                                       etat.getType(),
			                                                       etat.isAnnule());
			views.add(view);
		}
		return views;
	}

	private static List<CapitalView> extractCapitaux(List<CapitalHisto> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return Collections.emptyList();
		}

		final List<CapitalView> views = new ArrayList<>(capitaux.size());
		for (CapitalHisto capital : capitaux) {
			views.add(new CapitalView(capital));
		}
		return views;
	}

	private List<SiegeView> extractSieges(List<DateRanged<Etablissement>> principaux) {
		if (principaux != null) {
			final List<DomicileEtablissement> siegeIds = new ArrayList<>(principaux.size());
			for (DateRanged<Etablissement> principal : principaux) {
				// FIXME: Extraire et trier spéciallement les domiciles annulés !!fh

				List<DomicileEtablissement> extractedDomiciles = DateRangeHelper.extract(principal.getPayload().getSortedDomiciles(false),
				                                                                         principal.getDateDebut(),
				                                                                         principal.getDateFin(),
				                                                                         new DateRangeHelper.AdapterCallback<DomicileEtablissement>() {
					                                                                         @Override
					                                                                         public DomicileEtablissement adapt(DomicileEtablissement domicile, RegDate debut, RegDate fin) {
						                                                                         return new DomicileEtablissement(debut != null ? debut : domicile.getDateDebut(),
						                                                                                                          fin != null ? fin : domicile.getDateFin(),
						                                                                                                          domicile.getTypeAutoriteFiscale(),
						                                                                                                          domicile.getNumeroOfsAutoriteFiscale(),
						                                                                                                          domicile.getEtablissement()

						                                                                         );
					                                                                         }
				                                                                         });

				siegeIds.addAll(extractedDomiciles);
			}
			return getSieges(siegeIds);
		}
		return null;
	}

	private interface ExtractorDonneesRegistreCommerce<T> {
		T extract(DonneesRegistreCommerce source);
	}

	private static <T extends DateRange> List<T> extractFromDonneesRegistreCommerce(List<DonneesRegistreCommerce> source,
	                                                                                ExtractorDonneesRegistreCommerce<? extends T> extractor) {
		final List<T> extractedData = new ArrayList<>(source.size());
		for (DonneesRegistreCommerce data : source) {
			final T extractedValue = extractor.extract(data);
			if (extractedValue != null) {
				extractedData.add(extractedValue);
			}
		}
		return extractedData;
	}

	private static <T extends CollatableDateRange> List<T> extractAndCollateFromDonneesRegistreCommerce(List<DonneesRegistreCommerce> source,
	                                                                                                    ExtractorDonneesRegistreCommerce<? extends T> extractor) {
		final List<T> nonCollatedData = extractFromDonneesRegistreCommerce(source, extractor);
		return DateRangeHelper.collate(nonCollatedData);
	}

	private static List<FormeJuridiqueView> extractFormesJuridiques(List<DonneesRegistreCommerce> donneesRC) {
		final List<FormeJuridiqueView> views = extractAndCollateFromDonneesRegistreCommerce(donneesRC,
		                                                                                    new ExtractorDonneesRegistreCommerce<FormeJuridiqueView>() {
			                                                                                    @Override
			                                                                                    public FormeJuridiqueView extract(DonneesRegistreCommerce source) {
				                                                                                    final FormeLegale fl = FormeLegale.fromCode(source.getFormeJuridique().getCodeECH());
				                                                                                    if (fl != null) {
					                                                                                    return new FormeJuridiqueView(source.getDateDebut(), source.getDateFin(), fl);
				                                                                                    }
				                                                                                    else {
					                                                                                    // TODO ne faudrait-il pas plutôt lever une exception ??
					                                                                                    return null;
				                                                                                    }
			                                                                                    }
		                                                                                    });
		Collections.reverse(views);
		return views;
	}

	private List<SiegeView> getSiegesFromOrganisation(List<Siege> sieges) {
		if (sieges == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<>(sieges.size());
		for (Siege siege : sieges) {
			list.add(new SiegeView(siege));
		}
		Collections.sort(list, new DateRangeComparator<>());
		Collections.reverse(list);
		return list;
	}

	private List<SiegeView> getSieges(List<DomicileEtablissement> domiciles) {
		if (domiciles == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<>(domiciles.size());

		for (DomicileEtablissement domicile : domiciles) {
			list.add(new SiegeView(domicile));
		}
		Collections.sort(list, new DateRangeComparator<SiegeView>());
		Collections.reverse(list);
		return list;
	}

	private static List<FormeJuridiqueView> getFormesJuridiques(List<DateRanged<FormeLegale>> formesLegale) {
		if (formesLegale == null) {
			return null;
		}
		final List<FormeJuridiqueView> list = new ArrayList<>(formesLegale.size());
		for (DateRanged<FormeLegale> formeLegale : formesLegale) {
			list.add(new FormeJuridiqueView(formeLegale));
		}
		Collections.sort(list, new DateRangeComparator<FormeJuridiqueView>());
		Collections.reverse(list);
		return list;
	}
}
