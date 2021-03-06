package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.LocalisationFiscale;
import ch.vd.unireg.tiers.LocalizedDateRange;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

@Controller
@RequestMapping("/processuscomplexe/demenagement")
public class DemenagementSiegeController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "DemenagementSiegeCriteria";

	private static final String TYPES_AUTORITE_FISCALE = "typesAutoriteFiscale";

	private static final String CONNUE_CIVILEMENT = "entrepriseConnueAuRegistreCivil";

	private static final String DEBUT_SIEGE_ACTUEL = "dateDebutSiegeActuel";
	private static final String TYPE_AUTORITE_SIEGE_ACTUEL = "typeAutoriteFiscaleSiegeActuel";
	private static final String OFS_SIEGE_ACTUEL = "noOfsSiegeActuel";

	private static final String DEBUT_FOR_PRINCIPAL_ACTUEL = "dateDebutForPrincipalActuel";
	private static final String TYPE_AUTORITE_FOR_PRINCIPAL_ACTUEL = "typeAutoriteFiscaleForPrincipalActuel";
	private static final String OFS_FOR_PRINCIPAL_ACTUEL = "noOfsForPrincipalActuel";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de déménagement de siège d'entreprise.",
		                Role.DEMENAGEMENT_SIEGE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/demenagement/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new DemenagementSiegeView(idEntreprise));
	}

	private String showStart(final Model model, final DemenagementSiegeView view) {
		model.addAttribute(ACTION_COMMAND, view);
		model.addAttribute(TYPES_AUTORITE_FISCALE, tiersMapHelper.getMapTypeAutoriteFiscale());
		return doInReadOnlyTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			model.addAttribute(CONNUE_CIVILEMENT, entreprise.isConnueAuCivil());

			// le dernier siège
			final List<DomicileHisto> sieges = tiersService.getSieges(entreprise, false);
			final Pair<RegDate, LocalisationFiscale> dernierSiege = getDerniereLocalisation(sieges);
			if (dernierSiege != null) {
				model.addAttribute(DEBUT_SIEGE_ACTUEL, dernierSiege.getLeft());
				model.addAttribute(TYPE_AUTORITE_SIEGE_ACTUEL, dernierSiege.getRight().getTypeAutoriteFiscale());
				model.addAttribute(OFS_SIEGE_ACTUEL, dernierSiege.getRight().getNumeroOfsAutoriteFiscale());
			}

			// le dernier for principal
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			final Pair<RegDate, LocalisationFiscale> dernierForPrincipal = getDerniereLocalisation(ffps);
			if (dernierForPrincipal != null) {
				model.addAttribute(DEBUT_FOR_PRINCIPAL_ACTUEL, dernierForPrincipal.getLeft());
				model.addAttribute(TYPE_AUTORITE_FOR_PRINCIPAL_ACTUEL, dernierForPrincipal.getRight().getTypeAutoriteFiscale());
				model.addAttribute(OFS_FOR_PRINCIPAL_ACTUEL, dernierForPrincipal.getRight().getNumeroOfsAutoriteFiscale());
			}
			return "entreprise/demenagement/start";
		});
	}

	@Nullable
	private static Pair<RegDate, LocalisationFiscale> getDerniereLocalisation(List<? extends LocalizedDateRange> sorted) {
		if (sorted == null || sorted.isEmpty()) {
			return null;
		}
		final LocalizedDateRange dernierElement = CollectionsUtils.getLastElement(sorted);
		if (sorted.size() == 1) {
			// raccourci en présence d'une seule valeur
			return Pair.of(dernierElement.getDateDebut(), dernierElement);
		}

		// passage à une liste accessible facilement par index
		final List<? extends LocalizedDateRange> indexed;
		if (sorted instanceof RandomAccess) {
			indexed = sorted;
		}
		else {
			indexed = new ArrayList<>(sorted);
		}

		// ne nous intéressent que les éléments contigus à la même position
		LocalizedDateRange firstAtLocation = dernierElement;
		for (int index = indexed.size() - 2; index >= 0 ; -- index) {
			final LocalizedDateRange current = indexed.get(index);
			final LocalizedDateRange next = indexed.get(index + 1);
			if (!DateRangeHelper.isCollatable(current, next)
					|| current.getTypeAutoriteFiscale() != next.getTypeAutoriteFiscale()
					|| !Objects.equals(current.getNumeroOfsAutoriteFiscale(), next.getNumeroOfsAutoriteFiscale())) {
				break;
			}
			firstAtLocation = current;
		}
		return Pair.of(firstAtLocation.getDateDebut(), dernierElement);
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doDemenagement(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final DemenagementSiegeView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			metierService.demenageSiege(entreprise, view.getDateDebutNouveauSiege(), view.getTypeAutoriteFiscale(), view.getNoAutoriteFiscale());
		});

		// tout s'est bien passé... mais si le contribuable est sous le coup d'une décision ACI
		// (et que l'utilisateur courant a le droit de modification, ce qui est le cas si nous sommes
		// arrivés jusqu'ici), il faut lui laisser un petit mot...
		doInReadOnlyTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			if (tiersService.isSousInfluenceDecisions(entreprise)) {
				Flash.warning("Cette entreprise est actuellement sous l'influence d'une décision ACI. Veuillez en vérifier la pertinence après ce déménagement de siège.");
			}
			return null;
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
