package ch.vd.uniregctb.registrefoncier;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;

import static java.util.stream.Collectors.toList;

/**
 * Contrôleur qui permet le suivi des imports du registre foncier.
 */
@Controller
@RequestMapping(value = "/registrefoncier")
public class ImportRFController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportRFController.class);

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec de suivi des imports du Registre Foncier";

	private static final String TABLE_NAME = "tableImportsRF";
	private static final int IMPORT_PAGE_SIZE = 10;
	private static final int MUTATION_PAGE_SIZE = 50;

	private RegistreFoncierService serviceRF;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	public void setServiceRF(RegistreFoncierService serviceRF) {
		this.serviceRF = serviceRF;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	public void setEvenementRFMutationDAO(EvenementRFMutationDAO evenementRFMutationDAO) {
		this.evenementRFMutationDAO = evenementRFMutationDAO;
	}

	/**
	 * Affiche l'écran de suivi des annonces. L'écran de suivi contient un formulaire de recherche et des résultats paginés.
	 */
	@RequestMapping(value = "/import/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(HttpServletRequest request, @ModelAttribute(value = "view") ImportRFCriteriaView view, BindingResult bindingResult, Model model) {

		if (bindingResult.hasErrors()) {
			model.addAttribute("list", Collections.<EvenementRFImportView>emptyList());
			model.addAttribute("count", 0);
			return "registrefoncier/import/list";
		}

		// on exécute la requête
		final List<EtatEvenementRF> etats = view.buildEtats();
		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, IMPORT_PAGE_SIZE);
		final List<EvenementRFImport> list = evenementRFImportDAO.find(etats, pagination);
		final int count = evenementRFImportDAO.count(etats);

		// on interpète la requête
		final List<EvenementRFImportView> viewList = list.stream()
				.map(EvenementRFImportView::new)
				.collect(toList());
		model.addAttribute("list", viewList);
		model.addAttribute("count", count);

		return "registrefoncier/import/list";
	}

	/**
	 * Affiche l'écran détaillé d'un import.
	 */
	@RequestMapping(value = "/import/show.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(HttpServletRequest request, @RequestParam(value = "importId") long importId, @ModelAttribute(value = "view") ImportRFCriteriaView view, Model model) {

		final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
		if (importEvent == null) {
			throw new ObjectNotFoundException("L'import RF avec l'id = [" + importId + "] n'existe pas.");
		}

		// on va chercher les mutations de l'import
		final List<EtatEvenementRF> etats = view.buildEtats();
		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, MUTATION_PAGE_SIZE);
		final List<EvenementRFMutation> list = evenementRFMutationDAO.find(importId, etats, pagination);
		final int count = evenementRFMutationDAO.count(importId, etats);

		// on interpète la requête
		final List<EvenementRFMutationView> viewList = list.stream()
				.map(EvenementRFMutationView::new)
				.collect(toList());
		model.addAttribute("importEvent", new EvenementRFImportView(importEvent));
		model.addAttribute("mutations", viewList);
		model.addAttribute("count", count);

		return "registrefoncier/import/show";
	}

	@RequestMapping(value = "/import/get.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public ResponseEntity<EvenementRFImportView> getImport(@RequestParam(value = "importId") long importId) {
		final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
		if (importEvent == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(new EvenementRFImportView(importEvent), HttpStatus.OK);
	}

	@SecurityCheck(rolesToCheck = {Role.SUIVI_IMPORT_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/import/restart.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String restartImport(@RequestParam(value = "importId") long importId) {

		LOGGER.info("Relance du job de traitement de l'import du registre foncier sur l'import id=[" + importId + "]");

		try {
			serviceRF.startImport(importId);
			Flash.message("Le job de traitement de l'import n°" + importId + " est démarré.");
			return "redirect:/registrefoncier/import/list.do";
		}
		catch (JobAlreadyStartedException | SchedulerException e) {
			Flash.error("Le job n'a pas pu être démarré pour la raison suivante :" + e.getMessage());
			return "redirect:/registrefoncier/import/list.do";
		}
	}

	@SecurityCheck(rolesToCheck = {Role.SUIVI_IMPORT_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/import/force.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String forceImport(@RequestParam(value = "importId") long importId) {

		LOGGER.info("Forçage de l'import du registre foncier avec l'id=[" + importId + "]");

		// on force le job
		serviceRF.forceImport(importId);

		Flash.message("Le job n°" + importId + " a été forcé.");
		return "redirect:/registrefoncier/import/list.do";
	}

	@RequestMapping(value = "/mutation/get.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public ResponseEntity<EvenementRFMutationView> getMutation(@RequestParam(value = "mutId") long mutId) {
		final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutId);
		if (mutation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(new EvenementRFMutationView(mutation), HttpStatus.OK);
	}

	@SecurityCheck(rolesToCheck = {Role.SUIVI_IMPORT_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/mutation/restart.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String restartMutations(@RequestParam(value = "importId") long importId, HttpServletRequest request) {

		LOGGER.info("Relance du job de traitement des mutations du registre foncier sur l'import id=[" + importId + "]");

		try {
			// on relance le job
			serviceRF.startMutations(importId);
			Flash.message("Le job de traitement des mutations de l'import n°" + importId + " est démarré.");
		}
		catch (JobAlreadyStartedException | SchedulerException e) {
			Flash.error("Le job n'a pas pu être démarré pour la raison suivante :" + e.getMessage());
		}

		final String referrer = request.getHeader("referer");
		if (StringUtils.isNotBlank(referrer)) { // on redirige vers la page courante, pour garder les filtres et la pagination
			return "redirect:" + referrer;
		}
		else {
			return "redirect:/registrefoncier/import/list.do";
		}
	}

	@SecurityCheck(rolesToCheck = {Role.SUIVI_IMPORT_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/mutation/force.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String forceOneMutation(@RequestParam(value = "mutId") long mutId, HttpServletRequest request) {

		LOGGER.info("Forçage de la mutation du registre foncier avec l'id=[" + mutId + "]");

		final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutId);
		if (mutation == null) {
			throw new ObjectNotFoundException("La mutation avec l'id=[" + mutId + "] n'existe pas.");
		}

		// on force la mutation
		serviceRF.forceMutation(mutId);

		Flash.message("La mutation n°" + mutId + " a été forcée.");

		final String referrer = request.getHeader("referer");
		if (StringUtils.isNotBlank(referrer)) { // on redirige vers la page courante, pour garder les filtres et la pagination
			return "redirect:" + referrer;
		}
		else {
			return "redirect:/registrefoncier/import/show.do?importId=" + mutation.getParentImport().getId();
		}
	}

	@SecurityCheck(rolesToCheck = {Role.SUIVI_IMPORT_RF}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/mutation/forceAll.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String forceAllMutations(@RequestParam(value = "importId") long importId, HttpServletRequest request) {

		LOGGER.info("Forçage des mutations du registre foncier correspondant à l'import id=[" + importId + "]");

		// on force toutes les mutations non-traitée
		serviceRF.forceAllMutations(importId);

		Flash.message("Les mutations du job n°" + importId + " on été forcées.");

		final String referrer = request.getHeader("referer");
		if (StringUtils.isNotBlank(referrer)) { // on redirige vers la page courante, pour garder les filtres et la pagination
			return "redirect:" + referrer;
		}
		else {
			return "redirect:/registrefoncier/import/list.do";
		}
	}

	@RequestMapping(value = "/import/stats.do", method = RequestMethod.GET)
	@ResponseBody
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public ImportRFStatsView stats(@RequestParam(value = "importId") long importId) {

		final Map<EtatEvenementRF, Integer> countByState = evenementRFMutationDAO.countByState(importId);
		return new ImportRFStatsView(countByState);
	}
}