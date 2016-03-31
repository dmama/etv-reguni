package ch.vd.uniregctb.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.tiers.CapitalHisto;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.DomicileEtablissementView;
import ch.vd.uniregctb.tiers.view.EtatEntrepriseView;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
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

	@Nullable
	private static <T> T getLastElementPayload(List<DateRanged<T>> elements) {
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		final DateRanged<T> lastElement = CollectionsUtils.getLastElement(elements);
		return lastElement.getPayload();
	}

	@Override
	public EntrepriseView getEntreprise(Entreprise entreprise) {

		EntrepriseView entrepriseView = new EntrepriseView();

		Long numeroEntreprise = entreprise.getNumeroEntreprise();

		entrepriseView.setId(entreprise.getNumero());

		if (numeroEntreprise != null) {

			Organisation organisation = serviceOrganisationService.getOrganisationHistory(numeroEntreprise);

			List<DateRanged<String>> nomsAdditionnels = organisation.getNomAdditionnel();
			Collections.sort(nomsAdditionnels, new DateRangeComparator<>());
			entrepriseView.setNomsAdditionnels(nomsAdditionnels);

			final DonneesRC donneesRC = organisation.getSitePrincipal(null).getPayload().getDonneesRC();
			entrepriseView.setDateInscriptionRC(getLastElementPayload(donneesRC.getDateInscription()));
			entrepriseView.setDateInscriptionRCVD(getLastElementPayload(donneesRC.getDateInscriptionVd()));
			entrepriseView.setStatusRC(donneesRC.getStatusInscription(null));
			entrepriseView.setDateRadiationRCVD(getLastElementPayload(donneesRC.getDateRadiationVd()));
			entrepriseView.setDateRadiationRC(getLastElementPayload(donneesRC.getDateRadiation()));

			final DonneesRegistreIDE donneesRegistreIDE = organisation.getSitePrincipal(null).getPayload().getDonneesRegistreIDE();
			//entrepriseView.setDateInscritpionIde(CollectionsUtils.getLastElement(donneesRegistreIDE.getDateInscription()).getPayload()); // TODO: apporter la date d'inscription Ide en 16L1
			entrepriseView.setStatusIde(getLastElementPayload(donneesRegistreIDE.getStatus()));
		}

		entrepriseView.setNumerosIDE(tiersService.getNumeroIDE(entreprise));

		entrepriseView.setConnueAuCivil(entreprise.isConnueAuCivil());
		entrepriseView.setSieges(getSieges(tiersService.getSieges(entreprise, true)));

		final List<RaisonSocialeHisto> raisonsSociales = tiersService.getRaisonsSociales(entreprise, true);
		entrepriseView.setRaisonsSociales(getRaisonSociale(raisonsSociales));

		final List<FormeLegaleHisto> formesJuridiques = tiersService.getFormesLegales(entreprise, true);
		entrepriseView.setFormesJuridiques(getFormesJuridiques(formesJuridiques));

		entrepriseView.setCapitaux(extractCapitaux(tiersService.getCapitaux(entreprise, true)));

		// les états
		final List<EtatEntreprise> etats = new ArrayList<>(entreprise.getEtats());
		Collections.sort(etats, new AnnulableHelper.AnnulesApresWrappingComparator<>(new Comparator<EtatEntreprise>() {
			@Override
			public int compare(EtatEntreprise o1, EtatEntreprise o2) {
				int comparison = - o1.getDateObtention().compareTo(o2.getDateObtention());       // les plus récents d'abord
				if (comparison == 0) {
					// à dates d'obtention équivalentes, il faut trier par identifiant technique décroissant (pour avoir le plus récent d'abord)
					comparison = - Long.compare(o1.getId(), o2.getId());
				}
				return comparison;
			}
		}));
		entrepriseView.setEtats(getEtats(etats));

		return entrepriseView;
	}

	@Override
	public EtablissementView getEtablissement(Etablissement etablissement) {

		EtablissementView etablissementView = new EtablissementView();
		etablissementView.setId(etablissement.getNumero());

		if (etablissement.isConnuAuCivil()) {
			SiteOrganisation site = tiersService.getSiteOrganisationPourEtablissement(etablissement);

			etablissementView.setRaisonSociale(site.getNom(null));

			List<DateRanged<String>> noIdeList = site.getNumeroIDE();
			if (noIdeList != null && noIdeList.size() > 0) {
				DateRanged<String> noIdeRange = noIdeList.get(0);
				if (noIdeRange != null) {
					etablissementView.setNumerosIDE(Collections.singletonList(noIdeRange.getPayload()));
				}
			}

		} else {
			final Set<IdentificationEntreprise> identificationsEntreprise = etablissement.getIdentificationsEntreprise();
			final List<String> numerosIDE = getNumerosIDE(identificationsEntreprise);
			etablissementView.setNumerosIDE(numerosIDE);

			etablissementView.setRaisonSociale(etablissement.getRaisonSociale());
		}

		etablissementView.setEnseigne(etablissement.getEnseigne());

		etablissementView.setDomiciles(getDomiciles(tiersService.getDomiciles(etablissement, true)));

		etablissementView.setConnueAuCivil(etablissement.isConnuAuCivil());
		return etablissementView;
	}

	@NotNull
	protected List<String> getNumerosIDE(Set<IdentificationEntreprise> identificationsEntreprise) {
		final List<String> numerosIDE = new ArrayList<>();
		for (IdentificationEntreprise ident : identificationsEntreprise) {
			numerosIDE.add(ident.getNumeroIde());
		}
		return numerosIDE;
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
			                                                       etat.getGeneration(),
			                                                       etat.isAnnule());
			views.add(view);
		}
		if (AnnulableHelper.sansElementsAnnules(views).size() > 1) {
			views.get(0).setDernierElement(true);
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
		Collections.sort(views, new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<CapitalView>(DateRangeComparator.CompareOrder.DESCENDING)));
		if (AnnulableHelper.sansElementsAnnules(views).size() > 1) {
			views.get(0).setDernierElement(true);
		}
		return views;
	}

	private static List<FormeJuridiqueView> getFormesJuridiques(List<FormeLegaleHisto> formesLegale) {
		if (formesLegale == null) {
			return null;
		}
		final List<FormeJuridiqueView> list = new ArrayList<>(formesLegale.size());
		for (FormeLegaleHisto formeLegale : formesLegale) {
			list.add(new FormeJuridiqueView(formeLegale));
		}
		Collections.sort(list, new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<FormeJuridiqueView>(DateRangeComparator.CompareOrder.DESCENDING)));
		if (AnnulableHelper.sansElementsAnnules(list).size() > 1) {
			list.get(0).setDernierElement(true);
		}
		return list;
	}

	private static List<RaisonSocialeView> getRaisonSociale(List<RaisonSocialeHisto> raisonsSociales) {
		if (raisonsSociales == null) {
			return null;
		}
		final List<RaisonSocialeView> list = new ArrayList<>(raisonsSociales.size());
		for (RaisonSocialeHisto raisonSociale : raisonsSociales) {
			list.add(new RaisonSocialeView(raisonSociale, false));
		}
		Collections.sort(list, new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<RaisonSocialeView>(DateRangeComparator.CompareOrder.DESCENDING)));
		if (AnnulableHelper.sansElementsAnnules(list).size() > 1) {
			list.get(0).setDernierElement(true);
		}
		return list;
	}

	private static List<SiegeView> getSieges(List<DomicileHisto> sieges) {
		if (sieges == null) {
			return null;
		}
		final List<SiegeView> list = new ArrayList<>(sieges.size());
		for (DomicileHisto siege : sieges) {
			list.add(new SiegeView(siege));
		}
		Collections.sort(list, new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<SiegeView>(DateRangeComparator.CompareOrder.DESCENDING)));
		if (AnnulableHelper.sansElementsAnnules(list).size() > 1) {
			list.get(0).setDernierElement(true);
		}
		return list;
	}

	private static List<DomicileEtablissementView> getDomiciles(List<DomicileHisto> domiciles) {
		if (domiciles == null) {
			return null;
		}
		final List<DomicileEtablissementView> list = new ArrayList<>(domiciles.size());
		for (DomicileHisto siege : domiciles) {
			list.add(new DomicileEtablissementView(siege));
		}
		Collections.sort(list, new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<DomicileEtablissementView>(DateRangeComparator.CompareOrder.DESCENDING)));
		if (AnnulableHelper.sansElementsAnnules(list).size() > 1) {
			list.get(0).setDernierElement(true);
		}
		return list;
	}
}
