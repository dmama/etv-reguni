package ch.vd.uniregctb.di;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.ChoixDeclarationImpotView;
import ch.vd.uniregctb.di.view.DeclarationView;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TacheDAO;

/**
 * Controller Spring 3 pour la gestion des déclarations d'impôt ordinaires (à compléter lors des divers refactoring)
 */
@Controller
public class DeclarationImpotController {

	private HibernateTemplate hibernateTemplate;
	private DeclarationImpotService diService;
	private TacheDAO tacheDAO;
	private MessageSource messageSource;
	private DeclarationImpotEditManager manager;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setManager(DeclarationImpotEditManager manager) {
		this.manager = manager;
	}

	/**
	 * @param id l'id de la déclaration d'impôt ordinaire
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/decl/details.do", method = RequestMethod.GET)
	@ResponseBody
	public DeclarationView details(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL, Role.VISU_LIMITE)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final Declaration decl = hibernateTemplate.get(Declaration.class, id);
		if (decl == null) {
			return null;
		}

		// vérification des droits en lecture
		final Long tiersId = decl.getTiers().getId();
		ControllerUtils.checkAccesDossierEnLecture(tiersId);

		return new DeclarationView(decl, messageSource);
	}

	/**
	 * Annuler une déclaration d'impôt ordinaire.
	 *
	 * @param id          l'id de la déclaration d'impôt ordinaire à annuler
	 * @param depuisTache vrai si l'annulation de la déclaration d'impôt a été appelée depuis l'écran des tâches
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/decl/annuler.do", method = RequestMethod.POST)
	public String annuler(@RequestParam("id") long id, @RequestParam(value = "depuisTache", required = false) Boolean depuisTache) throws AccessDeniedException {

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL, Role.VISU_LIMITE)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final Declaration decl = hibernateTemplate.get(Declaration.class, id);
		if (decl == null) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'existe pas.");
		}

		if (!(decl instanceof DeclarationImpotOrdinaire)) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas une déclaration d'impôt ordinaire.");
		}

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decl;

		// vérification des droits en écriture
		final Long tiersId = di.getTiers().getId();
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);

		// annulation de la déclaration
		final Contribuable tiers = (Contribuable) di.getTiers();
		diService.annulationDI(tiers, di, RegDate.get());

		if (depuisTache == null) {
			return "redirect:/di/edit.do?action=listdis&numero=" + tiersId;
		}
		else {
			return "redirect:/tache/list.do";
		}
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire.
	 *
	 * @param id          l'id de la déclaration d'impôt ordinaire à désannuler
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/decl/desannuler.do", method = RequestMethod.POST)
	public String desannuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.DI_DESANNUL_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de désannulation des déclarations d'impôt.");
		}

		final Declaration decl = hibernateTemplate.get(Declaration.class, id);
		if (decl == null) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'existe pas.");
		}

		if (!(decl instanceof DeclarationImpotOrdinaire)) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas une déclaration d'impôt ordinaire.");
		}

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decl;
		if (!di.isAnnule()) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas annulée.");
		}

		// vérification des droits en écriture
		final Long tiersId = di.getTiers().getId();
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);

		// désannulation de la déclaration
		final Contribuable tiers = (Contribuable) di.getTiers();
		diService.desannulationDI(tiers, di, RegDate.get());

		return "redirect:/di/edit.do?action=listdis&numero=" + tiersId;
	}

	/**
	 * Affiche un écran qui permet de choisir une déclaration parmis une liste dans le but de l'ajouter sur le contribuable spécifié.
	 *
	 * @param tiersId le numéro de contribuable
	 * @param model   le modèle sous-jacent
	 * @return la vue à afficher
	 * @throws AccessDeniedException si l'utilisateur ne possède pas les droits d'accès sur le contribuable
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/decl/choisir.do", method = RequestMethod.GET)
	public String choisir(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("tiersId", tiersId);

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(tiersId);
		if (ranges == null || ranges.isEmpty()) {
			// [UNIREG-832] impossible d'imprimer une nouvelle DI: on reste dans le même écran et on affiche un message d'erreur
			Flash.warning(DeclarationImpotEditManager.CANNOT_ADD_NEW_DI);
			model.addAttribute("ranges", Collections.emptyList());
			return "/decl/choisir";
		}
		else if (ranges.size() == 1) {
			final DateRange range = ranges.get(0);
			// il reste exactement une DI à créer : on continue directement sur l'écran d'impression
			return "redirect:/di/edit.do?action=newdi&numero=" + tiersId + "&debut=" + range.getDateDebut().index() + "&fin=" + range.getDateFin().index();
		}
		else {
			// [UNIREG-889] il y reste plusieurs DIs à créer : on demande à l'utilisateur de choisir
			final ArrayList<ChoixDeclarationImpotView> views = new ArrayList<ChoixDeclarationImpotView>(ranges.size());
			for (PeriodeImposition r : ranges) {
				views.add(new ChoixDeclarationImpotView(r, r.isOptionnelle()));
			}
			model.addAttribute("ranges", views);
			return "/decl/choisir";
		}
	}
}
