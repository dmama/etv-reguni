package ch.vd.uniregctb.entreprise;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.interfaces.model.EtatPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.view.EtatPMView;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 */
public class EntrepriseServiceImpl implements ch.vd.uniregctb.entreprise.EntrepriseService {

	private ServiceOrganisationService serviceOrganisationService;
	private ServiceInfrastructureService serviceInfra;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisationService(ServiceOrganisationService serviceOrganisationService) {
		this.serviceOrganisationService = serviceOrganisationService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	@Override
	public EntrepriseView get(Entreprise entreprise, List<DateRanged<Etablissement>> etablissements) {

		EntrepriseView entrepriseView = new EntrepriseView();

		Long numeroEntreprise = entreprise.getNumeroEntreprise();
		// Hateful stub
		//numeroEntreprise = 100983251L;
		//numeroEntreprise = 100980874L; // FIXME: Faire le ménage

		if (numeroEntreprise != null) {
			/*
				L'entreprise a un identifiant cantonal et donc existe dans le registre civil cantonal.
			 */

			Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);
			//Organisation organisation = HorribleMockOrganisationService.getOrg(); // FIXME: Faire le ménage

			entrepriseView.setRaisonSociale(CollectionsUtils.getLastElement(organisation.getNom()).getPayload());
			entrepriseView.setAutresRaisonsSociales(getNomsAdditionnels(organisation));

			entrepriseView.setSieges(getSiegesFromOrganisation(organisation.getSiegesPrincipal()));
			entrepriseView.setFormesJuridiques(getFormesJuridiques(organisation.getFormeLegale()));
			entrepriseView.setCapitaux(extractCapitaux(organisation));
			//entrepriseView.setEtats(getEtatsPM(pm.getEtats()));
			List<DateRanged<String>> noIdeList = organisation.getNoIDE();
			if (noIdeList != null && noIdeList.size() > 0) {
				DateRanged<String> noIdeRange = noIdeList.get(0);
				if (noIdeRange != null) {
					entrepriseView.setNumerosIDE(Collections.singletonList(noIdeRange.getPayload()));
				}
			}
		} else {
			/*
				L'entreprise n'est pas connue du régistre civil cantonal et on doit faire avec les informations dont on dispose.
			 */
			List<DonneesRegistreCommerce> donneesRC = new ArrayList<>(entreprise.getDonneesRC());
			Collections.sort(donneesRC, new DateRangeComparator<DonneesRegistreCommerce>());
			Collections.reverse(donneesRC);

			entrepriseView.setRaisonSociale(CollectionsUtils.getLastElement(donneesRC).getRaisonSociale());
			entrepriseView.setSieges(extractSieges(etablissements));
			entrepriseView.setFormesJuridiques(extractFormesJuridiques(donneesRC));
			entrepriseView.setCapitaux(extractCapitaux(donneesRC));
		}

		return entrepriseView;
	}

	private List<DateRanged<Etablissement>> extractPrincipals(List<DateRanged<Etablissement>> etablissements) {
		if (etablissements != null) {
			List<DateRanged<Etablissement>> principals = new ArrayList<>();
			for (DateRanged<Etablissement> etablissement : etablissements) {
				if (etablissement.getPayload().isPrincipal()) {
					principals.add(etablissement);
				}
			}
			return principals;
		}
		return null;
	}

	private List<SiegeView> extractSieges(List<DateRanged<Etablissement>> etablissements) {
		if (etablissements != null) {
			List<DateRanged<Etablissement>> principals = extractPrincipals(etablissements);
			List<DomicileEtablissement> siegeIds = new ArrayList<>();
			for (DateRanged<Etablissement> principal : principals) {
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

	private List<CapitalView> extractCapitaux(List<DonneesRegistreCommerce> donneesRC) {
		if (donneesRC != null) {
			List<CapitalView> capitaux = new ArrayList<>(donneesRC.size());
			for (DonneesRegistreCommerce donnee : donneesRC) {
				CapitalView capitalView = new CapitalView();
				capitalView.setDateDebut(donnee.getDateDebut());
				capitalView.setDateFin(donnee.getDateFin());
				capitalView.setCapitalLibere(donnee.getCapital().getMontant());
				capitaux.add(capitalView);
			}
			return capitaux;
		}
		return null;
	}

	private List<FormeJuridiqueView> extractFormesJuridiques(List<DonneesRegistreCommerce> donneesRC) {
		if (donneesRC != null) {
			List<FormeJuridiqueView> formes = new ArrayList<>(donneesRC.size());
			for (DonneesRegistreCommerce donnee : donneesRC) {
				FormeJuridiqueView formeJuridiqueView = new FormeJuridiqueView();
				formeJuridiqueView.setDateDebut(donnee.getDateDebut());
				formeJuridiqueView.setDateFin(donnee.getDateFin());
				formeJuridiqueView.setCode(HorribleFormeJuridiqueMapper.map(donnee.getFormeJuridique()).name()); // FIXME: Faire le ménage
				formes.add(formeJuridiqueView);
			}
			return formes;
		}
		return null;
	}


	private List<String> getNomsAdditionnels(Organisation organisation) {
		List<String> l = new ArrayList<>();
		List<DateRanged<String>> nomsAdditionels = organisation.getNomsAdditionels();
		if (nomsAdditionels != null) {
			for (DateRanged dr : nomsAdditionels) {
				if (dr.isValidAt(RegDate.get())) {
					l.add((String) dr.getPayload());
				}
			}
		}
		return l;
	}

	private List<SiegeView> getSiegesFromOrganisation(List<DateRanged<Integer>> sieges) {
		if (sieges == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<>(sieges.size());
		for (DateRanged<Integer> siege : sieges) {

			final TypeNoOfs type; // FIXME: Si par malheur un pays se trouve avoir un numéro identique à une commune ... boom. Il faut qu'on obtienne l'information proprement.
			Commune commune = serviceInfra.getCommuneByNumeroOfs(siege.getPayload(), siege.getDateFin());
			if (commune != null) {
				type = TypeNoOfs.COMMUNE_CH;
			} else {
				type = TypeNoOfs.PAYS_HS;
			}

			list.add(new SiegeView(siege, type));
		}
		Collections.sort(list, new DateRangeComparator<SiegeView>());
		Collections.reverse(list);
		return list;
	}

	private List<SiegeView> getSieges(List<DomicileEtablissement> domiciles) {
		if (domiciles == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<>(domiciles.size());

		for (DomicileEtablissement domicile : domiciles) {
			final TypeNoOfs type;

			if (domicile.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				type = TypeNoOfs.PAYS_HS;
			} else {
				type = TypeNoOfs.COMMUNE_CH;
			}
			list.add(new SiegeView(new DateRanged<>(domicile.getDateDebut(), domicile.getDateFin(), domicile.getNumeroOfsAutoriteFiscale()), type));
		}
		Collections.sort(list, new DateRangeComparator<SiegeView>());
		Collections.reverse(list);
		return list;
	}

	private List<FormeJuridiqueView> getFormesJuridiques(List<DateRanged<FormeLegale>> formesLegale) {
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

	private List<CapitalView> extractCapitaux(Organisation organisation) {
		List<DateRanged<Capital>> capitaux = organisation.getCapital();
		if (capitaux == null) {
			return null;
		}
		final List<CapitalView> list = new ArrayList<>(capitaux.size());
		for (DateRanged<Capital> capital : capitaux) {
			CapitalView view = new CapitalView();
			view.setDateDebut(capital.getDateDebut());
			view.setDateFin(capital.getDateFin());
			BigDecimal capitalAmount = capital.getPayload().getCapitalAmount();
			if (capitalAmount != null) {
				view.setCapitalAction(capitalAmount.longValue());
			}
			BigDecimal cashedInAmount = capital.getPayload().getCashedInAmount();
			if (cashedInAmount != null) {
				view.setCapitalLibere(cashedInAmount.longValue());
			}
			list.add(view);
		}
		Collections.sort(list, new DateRangeComparator<CapitalView>());
		Collections.reverse(list);
		return list;
	}

	private List<EtatPMView> getEtatsPM(List<EtatPM> etats) {
		if (etats == null) {
			return null;
		}
		final List<EtatPMView> list = new ArrayList<>(etats.size());
		for (EtatPM r : etats) {
			final EtatPMView v = new EtatPMView();
			v.setDateDebut(r.getDateDebut());
			v.setDateFin(r.getDateFin());
			v.setCode(r.getCode());
			final TypeEtatPM type = serviceInfra.getTypeEtatPM(r.getCode());
			if (type != null) {
				v.setLibelle(type.getLibelle());
			}
			list.add(v);
		}
		Collections.sort(list, new DateRangeComparator<EtatPMView>());
		Collections.reverse(list);
		return list;
	}


}
