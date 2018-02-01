package ch.vd.unireg.acces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ch.vd.unireg.acces.copie.manager.CopieDroitAccesManager;
import ch.vd.unireg.acces.copie.view.ConfirmCopieView;
import ch.vd.unireg.acces.copie.view.ConfirmedDataView;
import ch.vd.unireg.acces.copie.view.SelectUtilisateursView;
import ch.vd.unireg.acces.parDossier.manager.DossierEditRestrictionManager;
import ch.vd.unireg.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.unireg.acces.parDossier.view.DroitAccesView;
import ch.vd.unireg.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.unireg.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.SelectUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurListPersonneView;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.HttpHelper;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.extraction.ExtractionJob;
import ch.vd.unireg.general.manager.UtilisateurManager;
import ch.vd.unireg.general.view.UtilisateurView;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.security.DroitAccesConflitAvecDonneesContribuable;
import ch.vd.unireg.security.DroitAccesException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.servlet.ServletService;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeOperation;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping(value = "/acces")
public class DroitAccesController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DroitAccesController.class);

	public static final String DOSSIER_CRITERIA_NAME = "DossierCriteria";
	public static final String UTILISATEUR_CRITERIA_NAME = "PersonneCriteria";
	public static final String CONFLICTS_NAME = "DroitAccesConflits";

	private static final String TYPE_DROIT_ACCES_NOM_MAP_NAME = "typesDroitAcces";
	private static final String TYPES_RECHERCHE_NOM = "typesRechercheNom";
	private static final String TYPE_OPERATION_MAP_NAME = "typesOperation";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

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
	private ServletService servletService;

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

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, true, false, RegDateHelper.StringFormat.DISPLAY));
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
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro AVS
			if (StringUtils.isNotBlank(criteria.getNumeroAVS())){
				criteria.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(criteria.getNumeroAVS()));
			}

			criteria.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, TiersCriteria.TypeTiers.ENTREPRISE));
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
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return "acces/par-dossier/list-pp";
	}

	@RequestMapping(value = "/par-dossier.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String accesParDossierPostFormulaire(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		if (bindingResult.hasErrors()) {
			return accesParDossier(model, view, true);
		}
		else {
			session.setAttribute(DOSSIER_CRITERIA_NAME, view);
		}
		return "redirect:/acces/par-dossier.do";
	}

	@RequestMapping(value = "/par-dossier/reset-search.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String resetCriteriaParDossier(HttpSession session) {
		session.removeAttribute(DOSSIER_CRITERIA_NAME);
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

	@RequestMapping(value = "/par-utilisateur/exporter-conflits.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String exporterConflits(HttpSession session, HttpServletRequest request, HttpServletResponse response) throws IOException {
		final List<DroitAccesConflitAvecDonneesContribuable> conflits = (List<DroitAccesConflitAvecDonneesContribuable>) session.getAttribute(CONFLICTS_NAME);
		if (conflits == null || conflits.isEmpty()) {
			Flash.warning("Aucun conflit d'accès à exporter.");
			return HttpHelper.getRedirectPagePrecedente(request);
		}

		try (TemporaryFile content = getConflitsAsCsvFile(conflits);
		     InputStream in = content.openInputStream()) {
			servletService.downloadAsFile("conflits.csv", CsvHelper.MIME_TYPE, in, null, response);
		}
		return null;
	}

	private static TemporaryFile getConflitsAsCsvFile(List<DroitAccesConflitAvecDonneesContribuable> conflits) {
		return CsvHelper.asCsvTemporaryFile(conflits, "conflits.csv", null, new CsvHelper.FileFiller<DroitAccesConflitAvecDonneesContribuable>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(CsvHelper.COMMA);
				b.append("NOM_RAISON_SOCIALE").append(CsvHelper.COMMA);
				b.append("LOCALITE").append(CsvHelper.COMMA);
				b.append("DATE_NAISSANCE_INSC_RC").append(CsvHelper.COMMA);
				b.append("PRE_EXISTANT_TYPE_ACCES").append(CsvHelper.COMMA);
				b.append("PRE_EXISTANT_LECTURE_ECRITURE").append(CsvHelper.COMMA);
				b.append("COPIE_TYPE_ACCES").append(CsvHelper.COMMA);
				b.append("COPIE_LECTURE_ECRITURE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DroitAccesConflitAvecDonneesContribuable elt) {
				b.append(elt.getNoContribuable()).append(CsvHelper.COMMA);
				b.append(CsvHelper.escapeChars(elt.getPrenomNom())).append(CsvHelper.COMMA);
				b.append(CsvHelper.escapeChars(elt.getNpaLocalite())).append(CsvHelper.COMMA);
				b.append(elt.getDateNaissance()).append(CsvHelper.COMMA);
				b.append(elt.getAccesPreexistant().getType()).append(CsvHelper.COMMA);
				b.append(elt.getAccesPreexistant().getNiveau()).append(CsvHelper.COMMA);
				b.append(elt.getAccesCopie().getType()).append(CsvHelper.COMMA);
				b.append(elt.getAccesCopie().getNiveau());
				return true;
			}
		});
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
		}
		else if (!error) {
			// recherche selon les critères donnés

			if (StringUtils.isNotBlank(bean.getNumeroAVS())){
				bean.setNumeroAVS(FormatNumeroHelper.removeSpaceAndDash(bean.getNumeroAVS()));
			}

			bean.setTypesTiersImperatifs(EnumSet.of(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, TiersCriteria.TypeTiers.ENTREPRISE));
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
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
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

	@RequestMapping(value = "/par-utilisateur/restriction/reset-search.do", method = RequestMethod.GET)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_LEC, Role.SEC_DOS_ECR}, accessDeniedMessage = READ_REQUIRED)
	public String resetCriteriaParUtilisateur(HttpSession session, @RequestParam(NO_INDIVIDU_OPERATEUR) long noIndividuOperateur) {
		session.removeAttribute(UTILISATEUR_CRITERIA_NAME);
		return String.format("redirect:/acces/par-utilisateur/ajouter-restriction.do?%s=%d", NO_INDIVIDU_OPERATEUR, noIndividuOperateur);
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
	                                       HttpServletRequest request,
	                                       @RequestParam(value = NO_OPERATEUR_REFERENCE) long noOperateurReference,
	                                       @RequestParam(value = NO_OPERATEUR_DESTINATION) long noOperateurDestination,
	                                       @RequestParam(value = TYPE_OPERATION) TypeOperation typeOperation) throws Exception {

		final WebParamPagination pagination = new WebParamPagination(request, "restriction", 25, "id", true);
		final ConfirmCopieView view = copieManager.get(noOperateurReference, noOperateurDestination, pagination);
		model.addAttribute(COMMAND, view);
		model.addAttribute(NO_OPERATEUR_REFERENCE, noOperateurReference);
		model.addAttribute(NO_OPERATEUR_DESTINATION, noOperateurDestination);
		model.addAttribute(TYPE_OPERATION, typeOperation);
		return "acces/copie-transfert/recap";
	}

	@RequestMapping(value = "/copie-transfert/copie.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String copieDroitsAccess(@ModelAttribute ConfirmedDataView view, HttpSession session) throws Exception {
		return doCopieTransfert(view, session, copieManager::copie);
	}

	@RequestMapping(value = "/copie-transfert/transfert.do", method = RequestMethod.POST)
	@SecurityCheck(rolesToCheck = {Role.SEC_DOS_ECR}, accessDeniedMessage = WRITE_REQUIRED)
	public String transfereDroitsAccess(@ModelAttribute ConfirmedDataView view, HttpSession session) throws Exception {
		return doCopieTransfert(view, session, copieManager::transfert);
	}

	private interface CopieTransfertAction {
		List<DroitAccesConflitAvecDonneesContribuable> execute(ConfirmedDataView view) throws AdresseException;
	}

	private String doCopieTransfert(ConfirmedDataView view, HttpSession session, CopieTransfertAction action) throws AdresseException {
		final List<DroitAccesConflitAvecDonneesContribuable> conflits = action.execute(view);
		final String conflictUrlPart;
		final int nbConflicts = conflits.size();
		if (nbConflicts > 0) {
			session.setAttribute(CONFLICTS_NAME, conflits);
			conflictUrlPart = String.format("&%s=true", WITH_CONFLICTS);

			final String warning;
			if (nbConflicts > 1) {
				warning = String.format("%d conflits ont été détectés.", nbConflicts);
			}
			else {
				warning = "Un conflit a été détecté.";
			}
			Flash.warning(warning);
		}
		else {
			conflictUrlPart = StringUtils.EMPTY;
		}
		return String.format("redirect:/acces/par-utilisateur/restrictions.do?%s=%d%s", NO_INDIVIDU_OPERATEUR, view.getNoOperateurDestination(), conflictUrlPart);
	}
}
