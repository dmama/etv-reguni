package ch.vd.uniregctb.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.MontantMonetaire;
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
		// Hateful stub
		//numeroEntreprise = 100983251L;
		//numeroEntreprise = 100980874L; // FIXME: Faire le ménage

		if (numeroEntreprise != null) {
			/*
				L'entreprise a un identifiant cantonal et donc existe dans le registre civil cantonal.
			 */
			entrepriseView.setSource(EntrepriseView.SourceCivile.RCENT);

			Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);
			//Organisation organisation = HorribleMockOrganisationService.getOrg(); // FIXME: Faire le ménage

			entrepriseView.setRaisonSociale(CollectionsUtils.getLastElement(organisation.getNom()).getPayload());
			entrepriseView.setAutresRaisonsSociales(organisation.getNomsAdditionnels(RegDate.get()));

			entrepriseView.setSieges(getSiegesFromOrganisation(organisation.getSiegesPrincipaux()));
			entrepriseView.setFormesJuridiques(getFormesJuridiques(organisation.getFormeLegale()));
			entrepriseView.setCapitaux(extractCapitaux(organisation));
			//entrepriseView.setEtats(getEtatsPM(pm.getEtats()));
			List<DateRanged<String>> noIdeList = organisation.getNumeroIDE();
			if (noIdeList != null && noIdeList.size() > 0) {
				DateRanged<String> noIdeRange = noIdeList.get(0);
				if (noIdeRange != null) {
					entrepriseView.setNumerosIDE(Collections.singletonList(noIdeRange.getPayload()));
				}
			}
		}
		else {
			/*
				L'entreprise n'est pas connue du régistre civil cantonal et on doit faire avec les informations dont on dispose.
			 */
			entrepriseView.setSource(EntrepriseView.SourceCivile.UNIREG);
			final List<DonneesRegistreCommerce> donneesRC = new ArrayList<>(entreprise.getDonneesRC());
			Collections.sort(donneesRC, new DateRangeComparator<>());

			entrepriseView.setRaisonSociale(CollectionsUtils.getLastElement(donneesRC).getRaisonSociale());
			entrepriseView.setSieges(extractSieges(tiersService.getEtablissementsPrincipauxEntreprise(entreprise)));
			entrepriseView.setFormesJuridiques(extractFormesJuridiques(donneesRC));
			entrepriseView.setCapitaux(extractCapitaux(donneesRC));
		}

		// les états
		final List<EtatEntreprise> etats = new ArrayList<>(entreprise.getEtats());
		Collections.sort(etats, new DateRangeComparator<EtatEntreprise>() {
			@Override
			public int compare(EtatEntreprise o1, EtatEntreprise o2) {
				int comparison = Boolean.compare(o1.isAnnule(), o2.isAnnule());     // false < true
				if (comparison == 0) {
					comparison = - super.compare(o1, o2);       // les plus récents d'abord
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
			final EtatEntrepriseView view = new EtatEntrepriseView(etat.getDateDebut(),
			                                                       etat.getDateFin(),
			                                                       etat.getType(),
			                                                       etat.isAnnule());
			views.add(view);
		}
		return views;
	}

	private List<SiegeView> extractSieges(List<DateRanged<Etablissement>> principaux) {
		if (principaux != null) {
			final List<DomicileEtablissement> siegeIds = new ArrayList<>(principaux.size());
			for (DateRanged<Etablissement> principal : principaux) {
				// FIXME: Extraire et trier spéciallement les domiciles annulés !!
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

	private static List<CapitalView> extractCapitaux(List<DonneesRegistreCommerce> donneesRC) {
		final List<CapitalView> views = extractAndCollateFromDonneesRegistreCommerce(donneesRC,
		                                                                             new ExtractorDonneesRegistreCommerce<CapitalView>() {
			                                                                             @Override
			                                                                             public CapitalView extract(DonneesRegistreCommerce source) {
				                                                                             final MontantMonetaire capitalLibere = source.getCapital();
				                                                                             if (capitalLibere != null) {
					                                                                             return new CapitalView(source.getDateDebut(), source.getDateFin(), capitalLibere);
				                                                                             }
				                                                                             else {
					                                                                             return null;
				                                                                             }
			                                                                             }
		                                                                             });
		Collections.reverse(views);
		return views;
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

	private static List<CapitalView> extractCapitaux(Organisation organisation) {
		final List<Capital> capitaux = organisation.getCapitaux();
		if (capitaux == null) {
			return null;
		}
		final List<CapitalView> list = new ArrayList<>(capitaux.size());
		for (Capital capital : capitaux) {
			final CapitalView view = new CapitalView(capital);
			list.add(view);
		}
		Collections.sort(list, new DateRangeComparator<CapitalView>());
		Collections.reverse(list);
		return list;
	}
}
