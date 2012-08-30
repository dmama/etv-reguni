package ch.vd.uniregctb.di;

import org.springframework.context.MessageSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.di.view.DeclarationView;
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
}
