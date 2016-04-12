package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/processuscomplexe/fusion")
public class FusionEntreprisesController extends AbstractProcessusComplexeController {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionEntreprisesController.class);

	public static final String CRITERIA_NAME_ABSORBANTE = "FusionEntreprisesCriteriaAbsorbante";
	public static final String CRITERIA_NAME_ABSORBEE = "FusionEntreprisesCriteriaAbsorbee";
	public static final String FUSION_NAME = "FusionEntreprises";

	private static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	private static final String LIST = "list";
	private static final String COMMAND = "command";
	private static final String FUSION = "fusion";
	private static final String ERROR_MESSAGE = "errorMessage";

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe de fusion d'entreprises.",
		                Role.FUSION_ENTREPRISES);
	}

	@RequestMapping(value = "/absorbante/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheAbsorbante(Model model, HttpSession session) {
		checkDroitAcces();

		// quand on arrive sur l'écran, on efface les données de la fusion précédente, au cas où il y en aurait encore...
		session.removeAttribute(FUSION_NAME);

		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(CRITERIA_NAME_ABSORBANTE);
		return showRechercheAbsorbante(model, criteria, false);
	}

	@RequestMapping(value = "/absorbante/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheAbsorbante(HttpSession session) {
		checkDroitAcces();
		session.removeAttribute(CRITERIA_NAME_ABSORBANTE);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/absorbante/list.do", method = RequestMethod.POST)
	public String doRechercheAbsorbante(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showRechercheAbsorbante(model, view, true);
		}
		else {
			session.setAttribute(CRITERIA_NAME_ABSORBANTE, view);
		}
		return "redirect:list.do";
	}

	private String showRechercheAbsorbante(Model model, @Nullable TiersCriteriaView criteria, boolean error) {
		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro IDE
			if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
				criteria.setNumeroIDE(NumeroIDEHelper.normalize(criteria.getNumeroIDE()));
			}

			criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
			criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
			model.addAttribute(LIST, searchTiers(criteria, model, ERROR_MESSAGE));
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return "entreprise/fusion/list-absorbante";
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("absorbante") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new FusionEntreprisesView(idEntreprise));
	}

	@RequestMapping(value = "/retour-choix-dates.do", method = RequestMethod.GET)
	public String showRetourStart(Model model, HttpSession session) {
		checkDroitAcces();

		final FusionEntrepriseSessionData sessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:absorbante/list.do";
		}

		controllerUtils.checkAccesDossierEnEcriture(sessionData.getIdEntrepriseAbsorbante());
		return showStart(model, new FusionEntreprisesView(sessionData.getIdEntrepriseAbsorbante(),
		                                                  sessionData.getDateContratFusion(),
		                                                  sessionData.getDateBilanFusion()));
	}

	private String showStart(Model model, FusionEntreprisesView view) {
		model.addAttribute(COMMAND, view);
		return "entreprise/fusion/choix-dates";
	}

	@RequestMapping(value = "/choix-dates.do", method = RequestMethod.POST)
	public String doSetDates(Model model, @Valid @ModelAttribute(value = COMMAND) final FusionEntreprisesView view, BindingResult bindingResult, HttpSession session) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntrepriseAbsorbante());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}

		// plus de 6 mois entre les deux dates -> warning
		final RegDate dateContratFusion = view.getDateContratFusion();
		final RegDate dateBilanFusion = view.getDateBilanFusion();
		if ((dateBilanFusion.isBefore(dateContratFusion) && dateBilanFusion.addMonths(6).isBefore(dateContratFusion)) || dateContratFusion.isBefore(dateBilanFusion) && dateContratFusion.addMonths(6).isBefore(dateBilanFusion)) {
			Flash.warning("Les dates de contrat et de bilan de fusion sont séparées de plus de 6 mois.");
		}

		// on conserve les données de la fusion en session
		final FusionEntrepriseSessionData newSessionData = new FusionEntrepriseSessionData(view.getIdEntrepriseAbsorbante(), dateContratFusion, dateBilanFusion);;
		final FusionEntrepriseSessionData oldSessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
		if (oldSessionData != null) {
			for (FusionEntrepriseSessionData.EntrepriseAbsorbee oldAbsorbee : oldSessionData.getEntreprisesAbsorbees()) {
				newSessionData.addEntrepriseAbsorbee(oldAbsorbee);
			}
		}
		session.setAttribute(FUSION_NAME, newSessionData);

		return "redirect:/processuscomplexe/fusion/absorbees/list.do";
	}

	@RequestMapping(value = "/absorbees/list.do", method = RequestMethod.GET)
	public String showFormulaireRechercheAbsorbee(Model model, HttpSession session) {
		checkDroitAcces();
		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(CRITERIA_NAME_ABSORBEE);
		return showRechercheAbsorbee(model, session, criteria, false);
	}

	@RequestMapping(value = "/absorbees/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRechercheAbsorbee(HttpSession session) {
		checkDroitAcces();
		session.removeAttribute(CRITERIA_NAME_ABSORBEE);
		return "redirect:list.do?searched=true";
	}

	private void fillCriteresImperatifsPourEntrepriseAbsorbee(TiersCriteriaView criteria) {
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE));
	}

	private String showRechercheAbsorbee(Model model, HttpSession session, @Nullable TiersCriteriaView criteria, boolean error) {

		final FusionEntrepriseSessionData sessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:../absorbante/list.do";
		}

		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro IDE
			if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
				criteria.setNumeroIDE(NumeroIDEHelper.normalize(criteria.getNumeroIDE()));
			}

			fillCriteresImperatifsPourEntrepriseAbsorbee(criteria);

			final List<TiersIndexedDataView> searchResults = searchTiers(criteria, model, ERROR_MESSAGE);
			model.addAttribute(LIST, adapteSearchResults(searchResults, sessionData));
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(FUSION, sessionData);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return "entreprise/fusion/list-absorbees";
	}

	private List<FusionEntreprisesAbsorbeeView> adapteSearchResults(final List<TiersIndexedDataView> searchResults, @NotNull final FusionEntrepriseSessionData sessionData) {
		final List<FusionEntreprisesAbsorbeeView> results = new ArrayList<>(searchResults.size());
		final Set<Long> idsEntreprisesDejaAbsorbees = sessionData.getIdsEntreprisesAbsorbees();
		final RegDate dateSeuil = RegDateHelper.minimum(sessionData.getDateBilanFusion(), sessionData.getDateContratFusion(), NullDateBehavior.LATEST);
		if (!searchResults.isEmpty()) {
			doInReadOnlyTransaction(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					for (TiersIndexedDataView searchResult : searchResults) {
						final String explicationNonSelectionnable;

						// si l'entreprise est déjà sélectionnée comme entreprise absorbante, on ne peut pas la reprendre dans les absorbées
						if (sessionData.getIdEntrepriseAbsorbante() == searchResult.getNumero()) {
							explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.deja.utilisee.comme.absorbante", null, WebContextUtils.getDefaultLocale());
						}
						// si l'entreprise est déjà sélectionnée dans les entreprises absorbées, on ne peut pas la choisir à nouveau
						else if (idsEntreprisesDejaAbsorbees.contains(searchResult.getNumero())) {
							explicationNonSelectionnable = messageSource.getMessage("label.fusion.entreprise.deja.utilisee.comme.absorbee", null, WebContextUtils.getDefaultLocale());
						}
						else {
							final Entreprise entreprise = getTiers(Entreprise.class, searchResult.getNumero());
							final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
							String explicationTrouvee = null;
							for (EtatEntreprise etat : etats) {
								if (RegDateHelper.isBefore(etat.getDateObtention(), dateSeuil, NullDateBehavior.LATEST)) {
									if (etat.getType() == TypeEtatEntreprise.DISSOUTE) {
										explicationTrouvee = messageSource.getMessage("label.fusion.entreprise.dissoute.avant.dates.fusion", null, WebContextUtils.getDefaultLocale());
										break;
									}
									else if (etat.getType() == TypeEtatEntreprise.RADIEE_RC) {
										explicationTrouvee = messageSource.getMessage("label.fusion.entreprise.radiee.rc.avant.dates.fusion", null, WebContextUtils.getDefaultLocale());
										break;
									}
								}
							}

							explicationNonSelectionnable = explicationTrouvee;
						}

						final FusionEntreprisesAbsorbeeView view = new FusionEntreprisesAbsorbeeView(searchResult, explicationNonSelectionnable);
						results.add(view);
					}
				}
			});
		}
		return results;
	}

	@RequestMapping(value = "/absorbees/add.do", method = RequestMethod.POST)
	public String addAbsorbee(@RequestParam("id") long idAbsorbee, HttpSession session) {
		checkDroitAcces();

		// on vérifie qu'on a bien les droits sur l'entreprise rajoutée
		try {
			controllerUtils.checkAccesDossierEnEcriture(idAbsorbee);
			controllerUtils.checkTraitementContribuableAvecDecisionAci(idAbsorbee);
		}
		catch (AccessDeniedException | ObjectNotFoundException e) {
			Flash.error(e.getMessage());
			return "redirect:list.do";
		}

		// on va chercher à nouveau dans l'indexeur les données de cette entreprise
		final TiersCriteriaView criteria = new TiersCriteriaView();
		fillCriteresImperatifsPourEntrepriseAbsorbee(criteria);
		criteria.setNumero(idAbsorbee);

		try {
			final List<TiersIndexedDataView> resultats = _searchTiers(criteria);
			if (resultats.isEmpty()) {
				Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idAbsorbee));
				return "redirect:list.do";
			}

			// ajout de la donnée
			final FusionEntrepriseSessionData sessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
			if (sessionData == null) {
				Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
				return "redirect:../absorbante/list.do";
			}

			sessionData.addEntrepriseAbsorbee(new FusionEntrepriseSessionData.EntrepriseAbsorbee(resultats.get(0)));
			return "redirect:list.do";
		}
		catch (IndexerException e) {
			LOGGER.error(e.getMessage(), e);
			Flash.error("Impossible de récupérer les données du tiers " + FormatNumeroHelper.numeroCTBToDisplay(idAbsorbee));
			return "redirect:list.do";
		}
	}

	@RequestMapping(value = "/absorbees/remove.do", method = RequestMethod.POST)
	public String removeAbsorbee(@RequestParam("id") long idAbsorbee, HttpSession session) {
		checkDroitAcces();

		// retrait de la donnée
		final FusionEntrepriseSessionData sessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:../absorbante/list.do";
		}

		sessionData.removeEntrepriseAbsorbee(idAbsorbee);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/absorbees/list.do", method = RequestMethod.POST)
	public String doRechercheAbsorbee(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showRechercheAbsorbee(model, session, view, true);
		}
		else {
			session.setAttribute(CRITERIA_NAME_ABSORBEE, view);
		}
		return "redirect:list.do?searched=true";
	}

	@RequestMapping(value = "/fusionner.do", method = RequestMethod.POST)
	public String fusionner(HttpSession session) {
		checkDroitAcces();

		// récupération des données de fusion
		final FusionEntrepriseSessionData sessionData = (FusionEntrepriseSessionData) session.getAttribute(FUSION_NAME);
		if (sessionData == null) {
			Flash.warning("La session a été invalidée. Veuillez recommencer votre saisie.");
			return "redirect:absorbante/list.do";
		}
		else if (sessionData.getIdsEntreprisesAbsorbees().isEmpty()) {
			throw new ActionException("Veuillez sélectionner au moins une entreprise absorbée.");
		}

		// on récupère les données des entreprises et on envoie tout ça dans le moteur
		doInTransaction(new MetierServiceExceptionAwareWithoutResultCallback() {
			@Override
			protected void doExecute(TransactionStatus status) throws MetierServiceException {
				final Entreprise absorbante = getTiers(Entreprise.class, sessionData.getIdEntrepriseAbsorbante());
				final Set<Long> idsEntreprisesAbsorbees = sessionData.getIdsEntreprisesAbsorbees();
				final List<Entreprise> absorbees = new ArrayList<>(idsEntreprisesAbsorbees.size());
				for (Long id : idsEntreprisesAbsorbees) {
					absorbees.add(getTiers(Entreprise.class, id));
				}
				metierService.fusionne(absorbante, absorbees, sessionData.getDateContratFusion(), sessionData.getDateBilanFusion());
			}
		});

		// quand le boulot est terminé (correctement), on efface les données de la session
		session.removeAttribute(FUSION_NAME);

		// ... et on termine dans le dossier de l'entreprise absorbante
		return "redirect:/tiers/visu.do?id=" + sessionData.getIdEntrepriseAbsorbante();
	}
}
