package ch.vd.unireg.entreprise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRC;
import ch.vd.unireg.interfaces.entreprise.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.tiers.CapitalHisto;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.DomicileEtablissementView;
import ch.vd.unireg.tiers.view.EtatEntrepriseView;

/**
 * Re-organisation des informations de l'entreprise pour l'affichage Web
 */
public class EntrepriseServiceImpl implements EntrepriseService {

	private ServiceEntreprise serviceEntreprise;
	private TiersService tiersService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceEntreprise(ServiceEntreprise serviceEntreprise) {
		this.serviceEntreprise = serviceEntreprise;
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

			EntrepriseCivile entrepriseCivile = serviceEntreprise.getEntrepriseHistory(numeroEntreprise);

			final List<DateRanged<String>> nomsAdditionnels = entrepriseCivile.getNomAdditionnel();
			nomsAdditionnels.sort(new DateRangeComparator<>());
			entrepriseView.setNomsAdditionnels(nomsAdditionnels);

			final DonneesRC donneesRC = entrepriseCivile.getEtablissementPrincipal(null).getPayload().getDonneesRC();
			final InscriptionRC inscriptionRC = getLastElementPayload(donneesRC.getInscription());
			if (inscriptionRC != null) {
				entrepriseView.setDateInscriptionRC(inscriptionRC.getDateInscriptionCH());
				entrepriseView.setDateInscriptionRCVD(inscriptionRC.getDateInscriptionVD());
				entrepriseView.setStatusRC(inscriptionRC.getStatus());
				entrepriseView.setDateRadiationRCVD(inscriptionRC.getDateRadiationVD());
				entrepriseView.setDateRadiationRC(inscriptionRC.getDateRadiationCH());
			}

			final DonneesRegistreIDE donneesRegistreIDE = entrepriseCivile.getEtablissementPrincipal(null).getPayload().getDonneesRegistreIDE();
			//entrepriseView.setDateInscritpionIde(CollectionsUtils.getLastElement(donneesRegistreIDE.getDateInscription()).getPayload()); // TODO: apporter la date d'inscription Ide en 16L1
			entrepriseView.setStatusIde(getLastElementPayload(donneesRegistreIDE.getStatus()));
		}

		entrepriseView.setNumerosIDE(tiersService.getNumeroIDE(entreprise));

		entrepriseView.setNoCantonal(entreprise.getNumeroEntreprise());
		entrepriseView.setSieges(getSieges(tiersService.getSieges(entreprise, true)));

		final List<RaisonSocialeHisto> raisonsSociales = tiersService.getRaisonsSociales(entreprise, true);
		entrepriseView.setRaisonsSociales(getRaisonSociale(raisonsSociales));

		final List<FormeLegaleHisto> formesJuridiques = tiersService.getFormesLegales(entreprise, true);
		entrepriseView.setFormesJuridiques(getFormesJuridiques(formesJuridiques));

		entrepriseView.setCapitaux(extractCapitaux(tiersService.getCapitaux(entreprise, true)));

		entrepriseView.setSecteurActivite(entreprise.getSecteurActivite());

		// les états
		final List<EtatEntreprise> etats = new ArrayList<>(entreprise.getEtats());
		etats.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new Comparator<EtatEntreprise>() {
			@Override
			public int compare(EtatEntreprise o1, EtatEntreprise o2) {
				int comparison = -o1.getDateObtention().compareTo(o2.getDateObtention());       // les plus récents d'abord
				if (comparison == 0) {
					// à dates d'obtention équivalentes, il faut trier par identifiant technique décroissant (pour avoir le plus récent d'abord)
					comparison = -Long.compare(o1.getId(), o2.getId());
				}
				return comparison;
			}
		}));
		entrepriseView.setEtats(getEtats(etats));

		entrepriseView.setDegreAssocCivil(tiersService.determineDegreAssociationCivil(entreprise, RegDate.get()));

		return entrepriseView;
	}

	@Override
	public EtablissementView getEtablissement(Etablissement etablissement) {

		final EtablissementView etablissementView = new EtablissementView();
		etablissementView.setId(etablissement.getNumero());

		final RegDate aujourdhui = RegDate.get();
		if (etablissement.isConnuAuCivil()) {
			final EtablissementCivil etablissementCivil = tiersService.getEtablissementCivil(etablissement);
			etablissementView.setRaisonSociale(etablissementCivil.getNom(null));

			final String noIde = etablissementCivil.getNumeroIDE(aujourdhui);
			if (StringUtils.isNotBlank(noIde)) {
				etablissementView.setNumerosIDE(Collections.singletonList(noIde));
			}
		}
		else {
			final Set<IdentificationEntreprise> identificationsEntreprise = etablissement.getIdentificationsEntreprise();
			final List<String> numerosIDE = getNumerosIDE(identificationsEntreprise);
			etablissementView.setNumerosIDE(numerosIDE);
			etablissementView.setRaisonSociale(etablissement.getRaisonSociale());
		}

		etablissementView.setEnseigne(etablissement.getEnseigne());
		etablissementView.setDomiciles(getDomiciles(tiersService.getDomiciles(etablissement, true)));
		etablissementView.setNoCantonal(etablissement.getNumeroEtablissement());

		etablissementView.setDegreAssocCivilEntreprise(tiersService.determineDegreAssociationCivil(etablissement, aujourdhui));

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
		final List<EtatEntrepriseView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(views);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		return views;
	}

	private static List<ShowCapitalView> extractCapitaux(List<CapitalHisto> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return Collections.emptyList();
		}

		final List<ShowCapitalView> views = new ArrayList<>(capitaux.size());
		for (CapitalHisto capital : capitaux) {
			views.add(new ShowCapitalView(capital));
		}
		views.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)));
		final List<ShowCapitalView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(views);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		return views;
	}

	private static List<ShowFormeJuridiqueView> getFormesJuridiques(List<FormeLegaleHisto> formesLegale) {
		if (formesLegale == null) {
			return null;
		}
		final List<ShowFormeJuridiqueView> list = new ArrayList<>(formesLegale.size());
		for (FormeLegaleHisto formeLegale : formesLegale) {
			list.add(new ShowFormeJuridiqueView(formeLegale));
		}
		list.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)));
		final List<ShowFormeJuridiqueView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(list);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		return list;
	}

	private static List<ShowRaisonSocialeView> getRaisonSociale(List<RaisonSocialeHisto> raisonsSociales) {
		if (raisonsSociales == null) {
			return null;
		}
		final List<ShowRaisonSocialeView> list = new ArrayList<>(raisonsSociales.size());
		for (RaisonSocialeHisto raisonSociale : raisonsSociales) {
			list.add(new ShowRaisonSocialeView(raisonSociale, false));
		}
		list.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)));
		final List<ShowRaisonSocialeView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(list);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		return list;
	}

	private static List<ShowSiegeView> getSieges(List<DomicileHisto> sieges) {
		if (sieges == null) {
			return null;
		}
		final List<ShowSiegeView> list = new ArrayList<>(sieges.size());
		for (DomicileHisto siege : sieges) {
			list.add(new ShowSiegeView(siege));
		}
		list.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)));
		final List<ShowSiegeView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(list);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		if (!listeSansAnnules.isEmpty()) {
			listeSansAnnules.get(0).setPeutEditerDateFin(true);
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
		list.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING)));
		final List<DomicileEtablissementView> listeSansAnnules = AnnulableHelper.sansElementsAnnules(list);
		if (listeSansAnnules.size() > 1) {
			listeSansAnnules.get(0).setDernierElement(true);
		}
		if (!listeSansAnnules.isEmpty()) {
			listeSansAnnules.get(0).setPeutEditerDateFin(true);
		}
		return list;
	}
}
