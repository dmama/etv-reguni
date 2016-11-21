package ch.vd.uniregctb.registrefoncier;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.scheduler.JobAlreadyStartedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityCheck;
import ch.vd.uniregctb.tiers.TiersMapHelper;

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
	private static final int PAGE_SIZE = 10;

	private RegistreFoncierService serviceRF;
	private TiersMapHelper tiersMapHelper;
	private EvenementRFImportDAO evenementRFImportDAO;

	public void setServiceRF(RegistreFoncierService serviceRF) {
		this.serviceRF = serviceRF;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setEvenementRFImportDAO(EvenementRFImportDAO evenementRFImportDAO) {
		this.evenementRFImportDAO = evenementRFImportDAO;
	}

	/**
	 * Affiche l'écran de suivi des annonces. L'écran de suivi contient un formulaire de recherche et des résultats paginés.
	 */
	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/import/list.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String list(HttpServletRequest request, @ModelAttribute(value = "view") ImportRFCriteriaView view, BindingResult bindingResult, Model model) {

		model.addAttribute("etatsEvenements", tiersMapHelper.getEtatEvenementRF());

		if (bindingResult.hasErrors()) {
			model.addAttribute("list", Collections.<EvenementRFImportView>emptyList());
			model.addAttribute("count", (int) 0);
			return "registrefoncier/import/list";
		}

		// on exécute la requête
		final List<EtatEvenementRF> etats = view.buildEtats();
		final WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE);
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

	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/import/restart.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String restart(@RequestParam(value = "importId") long importId) {

		try {
			serviceRF.startImport(importId);
			Flash.message("Le job est démarré.");
			return "redirect:/registrefoncier/list.do";
		}
		catch (JobAlreadyStartedException | SchedulerException e) {
			Flash.error("Le job n'a pas pu être démarré pour la raison suivante :" + e.getMessage());
			return "redirect:/registrefoncier/list.do";
		}
	}

	@SecurityCheck(rolesToCheck = {Role.EVEN}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "/import/force.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String force(@RequestParam(value = "importId") long importId) {

		// on force le job
		serviceRF.forceImport(importId);

		Flash.message("Le job n°" + importId + " a été forcé.");
		return "redirect:/registrefoncier/list.do";
	}
}
