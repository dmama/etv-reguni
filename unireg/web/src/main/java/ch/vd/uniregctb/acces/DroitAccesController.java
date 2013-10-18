package ch.vd.uniregctb.acces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.acces.copie.manager.CopieDroitAccesManager;
import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.acces.copie.view.ConfirmedDataView;
import ch.vd.uniregctb.acces.copie.view.SelectUtilisateursView;
import ch.vd.uniregctb.acces.parDossier.manager.DossierEditRestrictionManager;
import ch.vd.uniregctb.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;
import ch.vd.uniregctb.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.SelectUtilisateurView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurListPersonneView;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.general.manager.UtilisateurManager;
import ch.vd.uniregctb.general.view.UtilisateurView;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.TooManyResultsIndexerException;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.security.DroitAccesConflitAvecDonneesContribuable;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersIndexedDataView;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeOperation;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/acces")
public class DroitAccesController {

	private static final Logger LOGGER = Logger.getLogger(DroitAccesController.class);

	public static final String DOSSIER_CRITERIA_NAME = "DossierCriteria";
	public static final String UTILISATEUR_CRITERIA_NAME = "PersonneCriteria";
	public static final String CONFLICTS_NAME = "DroitAccesConflits";

	private static final String TYPE_DROIT_ACCES_NOM_MAP_NAME = "typesDroitAcces";
	private static final String TYPES_RECHERCHE_NOM = "typesRechercheNom";
	private static final String TYPE_OPERATION_MAP_NAME = "typesOperation";

	private static final String LIST = "list";
	private static final String COMMAND = "command";
	private static final String NUMERO = "numero";
	private static final String NO_INDIVIDU_OPERATEUR = "noIndividuOperateur";
	private static final String NO_OPERATEUR_REFERENCE = "noOperateurReference";
	private static final String NO_OPERATEUR_DESTINATION = "noOperateurDestination";
	private static final String TYPE_OPERATION = "typeOperation";
	private static final String WITH_CONFLICTS = "withConflicts";
	private static final String CONFLICTS = "conflicts";

	private static final String READ_REQUIRED = "Vous ne possédez aucun droit Ifo-Sec pour accéder à cet écran.";
	private static final String WRITE_REQUIRED = "Vous ne possédez aucun droit Ifo-Sec pour accéder à cet écran en modification.";

	private TiersMapHelper tiersMapHelper;
	private TiersService tiersService;
	private Validator validator;
	private DossierEditRestrictionManager dossierEditManager;
	private UtilisateurEditRestrictionManager utilisateurEditManager;
	private UtilisateurManager utilisateurManager;
	private CopieDroitAccesManager copieManager;

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setDossierEditManager(DossierEditRestrictionManager dossierEditManager) {
		this.dossierEditManager = dossierEditManager;
	}

	public void setUtilisateurEditManager(UtilisateurEditRestrictionManager utilisateurEditManager) {
		this.utilisateurEditManager = utilisateurEditManager;
	}

	public void setUtilisateurManager(UtilisateurManager utilisateurManager) {
		this.utilisateurManager = utilisateurManager;
	}

	public void setCopieManager(CopieDroitAccesManager copieManager) {
		this.copieManager = copieManager;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	private List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteriaView) {
		final List<TiersIndexedData> results = tiersService.search(criteriaView.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}
		return list;
	}

	@RequestMapping(value = "/par-dossier.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String accesParDossier(Model model, HttpSession session) {
		final TiersCriteriaView bean = (TiersCriteriaView) session.getAttribute(DOSSIER_CRITERIA_NAME);
		return accesParDossier(model, bean, false);
	}

	private String accesParDossier(Model model, TiersCriteriaView criteria, boolean error) {
		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
			criteria.setTypeTiers(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro AVS
			if (StringUtils.isNotBlank(criteria.getNumeroAVS())){
				criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(criteria.getNumeroAVS()));
			}
			try {
				final List<TiersIndexedDataView> results = searchTiers(criteria);
				model.addAttribute(LIST, results);
			}
			catch (TooManyResultsIndexerException ee) {
				LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
				model.addAttribute("errorMessage", "error.preciser.recherche");
			}
			catch (IndexerException e) {
				LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
				model.addAttribute("errorMessage", "error.recherche");
			}
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM, tiersMapHelper.getMapTypeRechercheNom());
		return "acces/par-dossier/list-pp";
	}

	@RequestMapping(value = "/par-dossier.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String accesParDossierPostFormulaire(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model,
	                                            @RequestParam(value = "effacer", required = false) String actionEffacer) {
		if (StringUtils.isNotBlank(actionEffacer)) {
			session.removeAttribute(DOSSIER_CRITERIA_NAME);
		}
		else if (bindingResult.hasErrors()) {
			return accesParDossier(model, view, true);
		}
		else {
			session.setAttribute(DOSSIER_CRITERIA_NAME, view);
		}
		return "redirect:/acces/par-dossier.do";
	}

	@RequestMapping(value = "/par-dossier/restrictions.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String getRestrictionsPourDossier(Model model, @RequestParam(value = NUMERO) long ctbId) {
		final DossierEditRestrictionView view = dossierEditManager.get(ctbId);
		model.addAttribute(COMMAND, view);
		return "acces/par-dossier/restrictions-pp";
	}

	@RequestMapping(value = "/par-dossier/annuler-restriction.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String doEditRestrictionsPourDossier(@RequestParam(value = "id") long droitId, @RequestParam(value = "ctbId") long ctbId) {
		try {
			dossierEditManager.annulerRestriction(droitId);
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}
		return String.format("redirect:/acces/par-dossier/restrictions.do?%s=%d", NUMERO, ctbId);
	}

	@RequestMapping(value = "/par-dossier/ajouter-restriction.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String getAjouterAccesDossier(Model model,
	                                     @RequestParam(value = NUMERO) long ctbId,
	                                     @RequestParam(value = "ajoutEffectue", required = false, defaultValue = "false") boolean ajoutEffectue) {
		final DroitAccesView view = new DroitAccesView();
		view.setNumero(ctbId);
		view.setType(TypeDroitAcces.AUTORISATION);
		view.setAjoutEffectue(ajoutEffectue);
		return displayAjouterAccesDossier(model, view);
	}

	private String displayAjouterAccesDossier(Model model, DroitAccesView view) {
		model.addAttribute(COMMAND, view);
		model.addAttribute(TYPE_DROIT_ACCES_NOM_MAP_NAME, tiersMapHelper.getDroitAcces());
		return "acces/par-dossier/edit-restriction";
	}

	@RequestMapping(value = "/par-dossier/ajouter-restriction.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String doAjouterRestrictionDossier(@Valid @ModelAttribute(value = COMMAND) DroitAccesView view, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return displayAjouterAccesDossier(model, view);
		}
		try {
			dossierEditManager.save(view);
			Flash.message(String.format("Le nouveau droit d'accès de l'opérateur %s sur le dossier %s a été sauvegardé.",
			                            view.getUtilisateur(), FormatNumeroHelper.numeroCTBToDisplay(view.getNumero())));
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}

		return String.format("redirect:/acces/par-dossier/ajouter-restriction.do?%s=%d&ajoutEffectue=true", NUMERO, view.getNumero());
	}

	@RequestMapping(value = "/par-utilisateur.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String accesParUtilisateur(Model model) {
		return accesParUtilisateur(model, new SelectUtilisateurView());
	}

	private String accesParUtilisateur(Model model, SelectUtilisateurView view) {
		model.addAttribute(COMMAND, view);
		return "acces/par-utilisateur/select-utilisateur";
	}

	@RequestMapping(value = "/par-utilisateur.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String chooseUtilisateur(@Valid @ModelAttribute(value = COMMAND) SelectUtilisateurView view, BindingResult bindingResult, Model model) {
		if (bindingResult.hasErrors()) {
			return accesParUtilisateur(model, view);
		}
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d", NO_INDIVIDU_OPERATEUR, view.getNumeroUtilisateur());
	}

	@RequestMapping(value = "/par-utilisateur/restrictions.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR, Role.SEC_DOS_LEC}, accessDeniedMessage = READ_REQUIRED)
	public String getRestrictionsUtilisateur(HttpServletRequest request,
	                                         HttpSession session,
	                                         Model model,
	                                         @RequestParam(value = NO_INDIVIDU_OPERATEUR) long noIndividuOperateur,
											 @RequestParam(value = WITH_CONFLICTS, required = false, defaultValue = "false") boolean withConflicts) throws Exception {
		final WebParamPagination pagination = new WebParamPagination(request, "restriction", 25, "id", true);
		final UtilisateurEditRestrictionView view = utilisateurEditManager.get(noIndividuOperateur, pagination);
		model.addAttribute(COMMAND, view);
		if (withConflicts) {
			model.addAttribute(CONFLICTS, session.getAttribute(CONFLICTS_NAME));
		}
		else {
			// cleanup...
			session.removeAttribute(CONFLICTS_NAME);
		}
		return "acces/par-utilisateur/restrictions-utilisateur";
	}

	@RequestMapping(value = "/par-utilisateur/annuler-restriction.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String onPostAnnulerRestriction(@RequestParam(value = NO_INDIVIDU_OPERATEUR) Long noIndividuOperateur,
	                                       @RequestParam(value = "aAnnuler", required = false) List<Long> restrictionsAAnnuler,
	                                       @RequestParam(value = "annuleTout", required = false, defaultValue = "false") boolean annuleTout) {
		try {
			if (annuleTout) {
				utilisateurEditManager.annulerToutesLesRestrictions(noIndividuOperateur);
			}
			else {
				utilisateurEditManager.annulerRestrictions(restrictionsAAnnuler);
			}
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage(), e);
		}
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d", NO_INDIVIDU_OPERATEUR, noIndividuOperateur);
	}

	@RequestMapping(value = "/par-utilisateur/exporter-restrictions.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String onPostExporter(@RequestParam(value = NO_INDIVIDU_OPERATEUR) long noIndividuOperateur) {
		final ExtractionJob job = utilisateurEditManager.exportListeDroitsAcces(noIndividuOperateur);
		Flash.message(String.format("Demande d'export enregistrée (%s)", job.getDescription()));
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d", NO_INDIVIDU_OPERATEUR, noIndividuOperateur);
	}

	@RequestMapping(value = "/par-utilisateur/exporter-conflits.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String exporterConflits(@RequestParam(value = NO_INDIVIDU_OPERATEUR) long noIndividuOperateur) {
		Flash.message("Demande d'export enregistrée.");
		// TODO faire l'export du contenu de la variable de session...
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d&%s=true", NO_INDIVIDU_OPERATEUR, noIndividuOperateur, WITH_CONFLICTS);
	}

	@RequestMapping(value = "/par-utilisateur/ajouter-restriction.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String getAjouterAccesUtilisateur(Model model, HttpSession session,
	                                         @RequestParam(value = NO_INDIVIDU_OPERATEUR) long noIndividuOperateur) {
		final UtilisateurListPersonneView bean = (UtilisateurListPersonneView) session.getAttribute(UTILISATEUR_CRITERIA_NAME);
		return accesParUtilisateur(model, noIndividuOperateur, bean, false);
	}

	private String accesParUtilisateur(Model model, long noIndividuOperateur, UtilisateurListPersonneView bean, boolean error) {
		if (bean == null) {
			bean = new UtilisateurListPersonneView();
			bean.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
			bean.setTypeTiers(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		}
		else if (!error) {
			// recherche selon les critères donnés

			if (StringUtils.isNotBlank(bean.getNumeroAVS())){
				bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
			}
			try {
				final List<TiersIndexedDataView> results = searchTiers(bean);
				model.addAttribute(LIST, results);
			}
			catch (TooManyResultsIndexerException ee) {
				LOGGER.error("Exception dans l'indexer: " + ee.getMessage(), ee);
				model.addAttribute("errorMessage", "error.preciser.recherche");
			}
			catch (IndexerException e) {
				LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
				model.addAttribute("errorMessage", "error.recherche");
			}
		}

		bean.setNoIndividuOperateur(noIndividuOperateur);
		final UtilisateurView utilisateurView = utilisateurManager.get(noIndividuOperateur);
		bean.setUtilisateurView(utilisateurView);

		model.addAttribute(COMMAND, bean);
		model.addAttribute(TYPES_RECHERCHE_NOM, tiersMapHelper.getMapTypeRechercheNom());
		return "acces/par-utilisateur/list-pp";
	}

	@RequestMapping(value = "/par-utilisateur/ajouter-restriction.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String rechercheDossierPourAccesUtilisateur(@Valid @ModelAttribute(value = COMMAND) UtilisateurListPersonneView view,
	                                                   BindingResult bindingResult,
	                                                   Model model,
	                                                   HttpSession session,
	                                                   @RequestParam(value = "effacer", required = false) String actionEffacer) {

		if (StringUtils.isNotBlank(actionEffacer)) {
			session.removeAttribute(UTILISATEUR_CRITERIA_NAME);
		}
		else if (bindingResult.hasErrors()) {
			return accesParUtilisateur(model, view.getNoIndividuOperateur(), view, true);
		}
		else {
			session.setAttribute(UTILISATEUR_CRITERIA_NAME, view);
		}
		return String.format("redirect:/acces/par-utilisateur/ajouter-restriction.do?%s=%d", NO_INDIVIDU_OPERATEUR, view.getNoIndividuOperateur());
	}

	@RequestMapping(value = "/par-utilisateur/recap.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String recapitulationNouvelAccesUtilisateur(Model model,
	                                                   @RequestParam(value = NUMERO) long noCtb,
	                                                   @RequestParam(value = NO_INDIVIDU_OPERATEUR) long noIndividuOperateur) throws Exception {

		final RecapPersonneUtilisateurView view = utilisateurEditManager.get(noCtb, noIndividuOperateur);
		model.addAttribute(COMMAND, view);
		model.addAttribute(TYPE_DROIT_ACCES_NOM_MAP_NAME, tiersMapHelper.getDroitAcces());
		return "acces/par-utilisateur/recap";
	}

	@RequestMapping(value = "/par-utilisateur/sauver-restriction.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String sauveNouvelleRestrictionUtilisateur(@ModelAttribute(value = COMMAND) RecapPersonneUtilisateurView view) {

		try {
			utilisateurEditManager.save(view);
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}

		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d", NO_INDIVIDU_OPERATEUR, view.getNoIndividuOperateur());
	}

	@RequestMapping(value = "/copie-transfert.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String getCopieTransfert(Model model, HttpSession session) {
		final SelectUtilisateursView view = new SelectUtilisateursView();
		view.setTypeOperation(TypeOperation.COPIE);
		session.removeAttribute(CONFLICTS_NAME);
		return getCopieTransfert(model, view);
	}

	public String getCopieTransfert(Model model, SelectUtilisateursView view) {
		model.addAttribute(COMMAND, view);
		model.addAttribute(TYPE_OPERATION_MAP_NAME, tiersMapHelper.getTypeOperation());
		return "acces/copie-transfert/select-utilisateurs";
	}

	@RequestMapping(value = "/copie-transfert.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String postCopieTransfert(@Valid @ModelAttribute(value = COMMAND) SelectUtilisateursView view,
	                                 BindingResult bindingResult,
	                                 Model model) {
		if (bindingResult.hasErrors()) {
			return getCopieTransfert(model, view);
		}

		return String.format("redirect:/acces/copie-transfert/confirm.do?%s=%d&%s=%d&%s=%s",
		                     NO_OPERATEUR_REFERENCE, view.getNumeroUtilisateurReference(),
		                     NO_OPERATEUR_DESTINATION, view.getNumeroUtilisateurDestination(),
		                     TYPE_OPERATION, view.getTypeOperation());
	}

	@RequestMapping(value = "/copie-transfert/confirm.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String getConfirmCopieTransfert(Model model,
	                                       @RequestParam(value = NO_OPERATEUR_REFERENCE) long noOperateurReference,
	                                       @RequestParam(value = NO_OPERATEUR_DESTINATION) long noOperateurDestination,
	                                       @RequestParam(value = TYPE_OPERATION) TypeOperation typeOperation) throws Exception {

		final ConfirmCopieView view = copieManager.get(noOperateurReference, noOperateurDestination);
		model.addAttribute(COMMAND, view);
		model.addAttribute(NO_OPERATEUR_REFERENCE, noOperateurReference);
		model.addAttribute(NO_OPERATEUR_DESTINATION, noOperateurDestination);
		model.addAttribute(TYPE_OPERATION, typeOperation);
		return "acces/copie-transfert/recap";
	}

	@RequestMapping(value = "/copie-transfert/copie.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String copieDroitsAccess(@ModelAttribute ConfirmedDataView view, HttpSession session) throws Exception {
		return doCopieTransfert(view, TypeOperation.COPIE, session);
	}

	@RequestMapping(value = "/copie-transfert/transfert.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String transfereDroitsAccess(@ModelAttribute ConfirmedDataView view, HttpSession session) throws Exception {
		return doCopieTransfert(view, TypeOperation.TRANSFERT, session);
	}

	private String doCopieTransfert(ConfirmedDataView view, TypeOperation type, HttpSession session) throws AdresseException {
		final List<DroitAccesConflitAvecDonneesContribuable> conflits;
		switch (type) {
			case COPIE:
				conflits = copieManager.copie(view);
				break;
			case TRANSFERT:
				conflits = copieManager.transfert(view);
				break;
			default:
				throw new IllegalArgumentException("Unsupported type: " + type);
		}

		String conflictUrlPart = StringUtils.EMPTY;
		if (!conflits.isEmpty()) {
			session.setAttribute(CONFLICTS_NAME, conflits);
			conflictUrlPart = String.format("&%s=true", WITH_CONFLICTS);
			Flash.warning("Des conflits ont été détectés.");
		}
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d%s", NO_INDIVIDU_OPERATEUR, view.getNoOperateurDestination(), conflictUrlPart);
	}
}
