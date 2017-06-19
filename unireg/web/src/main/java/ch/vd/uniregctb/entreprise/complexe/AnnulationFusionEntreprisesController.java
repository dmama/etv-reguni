package ch.vd.uniregctb.entreprise.complexe;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.transaction.TransactionHelper;

@Controller
@RequestMapping("/processuscomplexe/annulation/fusion")
public class AnnulationFusionEntreprisesController extends AbstractProcessusComplexeRechercheController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnulationFusionEntreprisesController.class);

	public static final String CRITERIA_NAME = "AnnulationFusionEntreprisesCriteria";

	private static final String DATES_BILAN = "datesBilan";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe d'annulation de fusion d'entreprises.",
		                Role.FUSION_ENTREPRISES);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	/**
	 * @param idEntreprise identifiant d'une entreprise considérée dans son rôle d'entreprise absorbante
	 * @return la liste (triée par ordre chronologique inverse) des dates de bilan de fusion passées
	 */
	@RequestMapping(value = "/dates-bilan.do", method = RequestMethod.GET)
	@ResponseBody
	public List<RegDate> getDatesBilanFusionExistantes(@RequestParam("idEntreprise") final long idEntreprise) {
		return doInReadOnlyTransaction(new TransactionCallback<List<RegDate>>() {
			@Override
			public List<RegDate> doInTransaction(TransactionStatus status) {
				final Entreprise absorbante = getTiers(Entreprise.class, idEntreprise);
				final List<Pair<RegDate, RegDate>> dates = getDatesFusionExistantes(absorbante);
				final List<RegDate> datesBilans = new ArrayList<>(dates.size());
				for (Pair<RegDate, RegDate> pair : dates) {
					datesBilans.add(pair.getLeft());
				}
				datesBilans.sort(Collections.<RegDate>reverseOrder());
				return datesBilans;
			}
		});
	}

	@RequestMapping(value = "/dates-contrat.do", method = RequestMethod.GET)
	@ResponseBody
	public List<RegDate> getDatesContratsPourDateBilan(@RequestParam("idEntreprise") final long idEntreprise,
	                                                   @RequestParam("dateBilan") String strDateBilan) {
		final RegDate dateBilan;
		try {
			dateBilan = RegDateHelper.displayStringToRegDate(strDateBilan, false);
		}
		catch (ParseException e) {
			LOGGER.error("Mauvais format de date", e);
			return Collections.emptyList();
		}

		return doInReadOnlyTransaction(new TransactionCallback<List<RegDate>>() {
			@Override
			public List<RegDate> doInTransaction(TransactionStatus status) {
				final Entreprise absorbante = getTiers(Entreprise.class, idEntreprise);
				final Set<RegDate> contratsPourBilan = getDatesContratsParDateBilan(absorbante).get(dateBilan);
				final List<RegDate> contrats;
				if (contratsPourBilan != null) {
					contrats = new ArrayList<>(contratsPourBilan);
					contrats.sort(Collections.<RegDate>reverseOrder());
				}
				else {
					contrats = Collections.emptyList();
				}
				return contrats;
			}
		});
	}

	/**
	 * @param absorbante une entreprise considérée dans son rôle d'absorbante
	 * @return la liste des couples (date de bilan, date de contrat) retrouvés dans l'histoire de l'entreprise (ordre de tri complètement arbitraire !)
	 */
	@NotNull
	private List<Pair<RegDate, RegDate>> getDatesFusionExistantes(Entreprise absorbante) {

		// récupération des information dans une map
		final Map<RegDate, Set<RegDate>> datesContratsParDateBilan = getDatesContratsParDateBilan(absorbante);

		// reconstitution des couples
		final List<Pair<RegDate, RegDate>> couples = new LinkedList<>();
		for (Map.Entry<RegDate, Set<RegDate>> entry : datesContratsParDateBilan.entrySet()) {
			final RegDate dateBilan = entry.getKey();
			for (RegDate dateContrat : entry.getValue()) {
				couples.add(Pair.of(dateBilan, dateContrat));
			}
		}
		return couples;
	}

	/**
	 * @param absorbante une entreprise considérée dans son rôle d'absorbante
	 * @return une map (clé = date bilan, valeur = dates de contrat associées) des informations de fusion retrouvées dans le passé de l'entreprise
	 */
	@NotNull
	private Map<RegDate, Set<RegDate>> getDatesContratsParDateBilan(Entreprise absorbante) {
		final Set<FusionEntreprisesHelper.DatesFusion> datesFusions = FusionEntreprisesHelper.getAbsorptions(absorbante, tiersService).keySet();
		final Map<RegDate, Set<RegDate>> map = new HashMap<>(datesFusions.size());
		for (FusionEntreprisesHelper.DatesFusion datesFusion : datesFusions) {
			addCoupleDatesFusion(map, datesFusion.dateBilan, datesFusion.dateContrat);
		}
		return map;
	}

	private static void addCoupleDatesFusion(Map<RegDate, Set<RegDate>> datesContratsParDateBilan, RegDate dateBilan, RegDate dateContrat) {
		Set<RegDate> contrats = datesContratsParDateBilan.get(dateBilan);
		if (contrats == null) {
			contrats = new HashSet<>();
			datesContratsParDateBilan.put(dateBilan, contrats);
		}
		contrats.add(dateContrat);
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setCorporationMergeResult(Boolean.TRUE);
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/annulation-fusion/list";
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("absorbante") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new FusionEntreprisesView(idEntreprise));
	}

	private String showStart(final Model model, final FusionEntreprisesView view) {
		model.addAttribute(ACTION_COMMAND, view);

		final List<RegDate> datesBilan = getDatesBilanFusionExistantes(view.getIdEntrepriseAbsorbante());
		if (datesBilan.isEmpty()) {
			Flash.error(String.format("Impossible de reconstituer des absorptions passées d'entreprises pour l'entreprise %s.", FormatNumeroHelper.numeroCTBToDisplay(view.getIdEntrepriseAbsorbante())));
			return "redirect:list.do";
		}
		model.addAttribute(DATES_BILAN, datesBilan);
		if (view.getDateBilanFusion() == null) {
			view.setDateBilanFusion(datesBilan.get(0));
		}
		return "entreprise/annulation-fusion/choix-dates";
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.POST)
	public String doAnnulationFusion(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FusionEntreprisesView view, BindingResult bindingResult) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<MetierServiceException>() {
			@Override
			public void execute(TransactionStatus status) throws MetierServiceException {
				// récupération des données et vérification des droits d'accès
				final Entreprise absorbante = getTiers(Entreprise.class, view.getIdEntrepriseAbsorbante());
				controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseAbsorbante());
				controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntrepriseAbsorbante());

				final Map<FusionEntreprisesHelper.DatesFusion, List<Entreprise>> absorptions = FusionEntreprisesHelper.getAbsorptions(absorbante, tiersService);
				final FusionEntreprisesHelper.DatesFusion key = new FusionEntreprisesHelper.DatesFusion(view.getDateBilanFusion(), view.getDateContratFusion());
				final List<Entreprise> absorbees = absorptions.get(key);
				if (absorbees == null || absorbees.isEmpty()) {
					throw new MetierServiceException("Aucune entreprise absorbée trouvée!");
				}

				// petite boucle pour vérifier les droits d'accès sur les absorbées aussi
				for (Entreprise absorbee : absorbees) {
					controllerUtils.checkAccesDossierEnEcriture(absorbee.getNumero());
					controllerUtils.checkTraitementContribuableAvecDecisionAci(absorbee.getNumero());
				}

				// envoi de la sauce pour dé-tricoter tout ça
				metierService.annuleFusionEntreprises(absorbante, absorbees, view.getDateContratFusion(), view.getDateBilanFusion());
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntrepriseAbsorbante();
	}
}