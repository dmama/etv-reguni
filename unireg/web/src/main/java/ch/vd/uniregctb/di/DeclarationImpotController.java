package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.AjouterDelaiDeclarationView;
import ch.vd.uniregctb.di.view.AjouterEtatDeclarationView;
import ch.vd.uniregctb.di.view.ChoixDeclarationImpotView;
import ch.vd.uniregctb.di.view.DeclarationListView;
import ch.vd.uniregctb.di.view.DeclarationView;
import ch.vd.uniregctb.di.view.EditerDeclarationImpotView;
import ch.vd.uniregctb.di.view.ImprimerDuplicataDeclarationImpotView;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Controller Spring 3 pour la gestion des déclarations d'impôt ordinaires (à compléter lors des divers refactoring)
 */
@Controller
public class DeclarationImpotController {

	private HibernateTemplate hibernateTemplate;
	private DeclarationImpotService diService;
	private DeclarationImpotOrdinaireDAO diDAO;
	private MessageSource messageSource;
	private DeclarationImpotEditManager manager;
	private DelaisService delaisService;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private PlatformTransactionManager transactionManager;
	private Validator validator;
	private ModeleDocumentDAO modeleDocumentDAO;
	private PeriodeImpositionService periodeImpositionService;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;
	private TicketService ticketService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setManager(DeclarationImpotEditManager manager) {
		this.manager = manager;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.INDEX));
		// champs du formulaire de création d'une nouvelle déclaration
		binder.registerCustomEditor(RegDate.class, "delaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateDebutPeriodeImposition", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateFinPeriodeImposition", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateRetour", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		// champs du formulaire d'ajout de délai à une déclaration
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	private void checkAccessRights(DeclarationImpotOrdinaire di, boolean emission, boolean quittancement, boolean delais, boolean sommation, boolean duplicata) {
		if (di instanceof DeclarationImpotOrdinairePP) {
			if (emission && !SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'émission des déclarations d'impôt des personnes physiques.");
			}
			if (quittancement && !SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt des personnes physiques.");
			}
			if (delais && !SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de gestion des délais des déclarations d'impôt des personnes physiques.");
			}
			if (sommation && !SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de sommation des déclarations d'impôt des personnes physiques.");
			}
			if (duplicata && !SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'émission de duplicata des déclarations d'impôt des personnes physiques.");
			}
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			if (emission && !SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PM)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'émission des déclarations d'impôt des personnes morales.");
			}
			if (quittancement && !SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt des personnes morales.");
			}
			if (delais && !SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de gestion des délais des déclarations d'impôt des personnes morales.");
			}
			if (sommation && !SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PM)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de sommation des déclarations d'impôt des personnes morales.");
			}
			if (duplicata && !SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PM)) {
				throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'émission de duplicata des déclarations d'impôt des personnes morales.");
			}
		}
		else {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
	}

	/**
	 * Liste les déclarations d'impôt d'un contribuable
	 * @param tiersId le numéro d'un contribuable
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/list.do", method = RequestMethod.GET)
	public String list(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}
		if (!(tiers instanceof Contribuable)) {
			throw new IllegalArgumentException("Le tiers spécifié n'est pas un contribuable.");
		}
		final Contribuable ctb = (Contribuable) tiers;

		// vérification des droits en écriture
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		model.addAttribute("command", new DeclarationListView(ctb, messageSource));
		return "/di/lister";
	}

	/**
	 * @param id l'id de la déclaration d'impôt ordinaire
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/details.do", method = RequestMethod.GET)
	@ResponseBody
	public DeclarationView details(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final Declaration decl = hibernateTemplate.get(Declaration.class, id);
		if (decl == null) {
			return null;
		}

		// vérification des droits en lecture
		final Long tiersId = decl.getTiers().getId();
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		return new DeclarationView(decl, messageSource);
	}

	/**
	 * Annuler une déclaration d'impôt ordinaire.
	 *
	 * @param id      l'id de la déclaration d'impôt ordinaire à annuler
	 * @param tacheId si l'annulation de la déclaration d'impôt a été appelée depuis l'écran des tâches, l'ID de la tâche...
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/annuler.do", method = RequestMethod.POST)
	public String annuler(@RequestParam("id") long id, @RequestParam(value = "tacheId", required = false) Long tacheId) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
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
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		// annulation de la déclaration
		final Contribuable tiers = di.getTiers();
		diService.annulationDI(tiers, di, tacheId, RegDate.get());

		if (tacheId != null) {
			return "redirect:/tache/list.do";
		}
		else {
			return "redirect:/di/list.do?tiersId=" + tiersId;
		}
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire PP.
	 *
	 * @param id l'id de la déclaration d'impôt ordinaire à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/desannuler-pp.do", method = RequestMethod.POST)
	public String desannulerDeclarationImpotPP(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DESANNUL_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de désannulation des déclarations d'impôt PP.");
		}

		return desannuler(id, DeclarationImpotOrdinairePP.class);
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire PM.
	 *
	 * @param id l'id de la déclaration d'impôt ordinaire à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/desannuler-pm.do", method = RequestMethod.POST)
	public String desannulerDeclarationImpotPM(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DESANNUL_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de désannulation des déclarations d'impôt PM.");
		}

		return desannuler(id, DeclarationImpotOrdinairePM.class);
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire.
	 * @param diId l'id de la déclaration d'impôt ordinaire à désannuler
	 * @param clazz la classe de la déclaration d'impôt
	 */
	private String desannuler(long diId, Class<? extends DeclarationImpotOrdinaire> clazz) throws AccessDeniedException {

		final DeclarationImpotOrdinaire di = hibernateTemplate.get(clazz, diId);
		if (di == null) {
			throw new IllegalArgumentException("La déclaration n°" + diId + " n'existe pas.");
		}
		if (!di.isAnnule()) {
			throw new IllegalArgumentException("La déclaration n°" + diId + " n'est pas annulée.");
		}

		// vérification des droits en écriture
		final Long tiersId = di.getTiers().getId();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		// désannulation de la déclaration
		final Contribuable tiers = di.getTiers();
		diService.desannulationDI(tiers, di, RegDate.get());

		return "redirect:/di/list.do?tiersId=" + tiersId;
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
	@RequestMapping(value = "/di/choisir-pp.do", method = RequestMethod.GET)
	public String choisirDeclarationAEmettrePP(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}

		return choisirDeclarationAEmettre(tiersId, model, "imprimer-pp");
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
	@RequestMapping(value = "/di/choisir-pm.do", method = RequestMethod.GET)
	public String choisirDeclarationAEmettrePM(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes morales.");
		}

		return choisirDeclarationAEmettre(tiersId, model, "imprimer-pm");
	}

	/**
	 * Affiche un écran qui permet de choisir une déclaration parmis une liste dans le but de l'ajouter sur le contribuable spécifié.
	 *
	 * @param tiersId le numéro de contribuable
	 * @param model   le modèle sous-jacent
	 * @return la vue à afficher
	 * @throws AccessDeniedException si l'utilisateur ne possède pas les droits d'accès sur le contribuable
	 */
	public String choisirDeclarationAEmettre(long tiersId, Model model, String actionImpression) throws AccessDeniedException {

		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(tiersId);
		if (ranges == null || ranges.isEmpty()) {
			// [UNIREG-832] impossible d'imprimer une nouvelle DI: on reste dans le même écran et on affiche un message d'erreur
			Flash.warning(DeclarationImpotEditManager.CANNOT_ADD_NEW_DI);
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", Collections.emptyList());
			model.addAttribute("actionImpression", actionImpression);
			return "/di/choisir";
		}
		else if (ranges.size() == 1) {
			final DateRange range = ranges.get(0);
			// il reste exactement une DI à créer : on continue directement sur l'écran d'impression
			return String.format("redirect:/di/%s.do?tiersId=%d&debut=%d&fin=%d", actionImpression, tiersId, range.getDateDebut().index(), range.getDateFin().index());
		}
		else {
			// [UNIREG-889] il y reste plusieurs DIs à créer : on demande à l'utilisateur de choisir
			final ArrayList<ChoixDeclarationImpotView> views = new ArrayList<>(ranges.size());
			for (PeriodeImposition r : ranges) {
				views.add(new ChoixDeclarationImpotView(r, r.isDeclarationOptionnelle()));
			}
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", views);
			model.addAttribute("actionImpression", actionImpression);
			return "/di/choisir";
		}
	}

	/**
	 * Affiche un écran qui permet de prévisualiser une déclaration avant son impression (et donc son ajout sur le contribuable).
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/imprimer-pp.do", method = RequestMethod.GET)
	public String imprimerDeclarationPP(@RequestParam("tiersId") long tiersId,
	                                    @RequestParam("debut") RegDate dateDebut,
	                                    @RequestParam("fin") RegDate dateFin,
	                                    @RequestParam(value = "typeDocument", required = false) TypeDocument typeDocument,
	                                    @RequestParam(value = "delaiRetour", required = false) Integer delaiRetour,
	                                    @RequestParam(value = "depuisTache", required = false, defaultValue = "false") boolean depuisTache,
	                                    Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}
		if (!(tiers instanceof ContribuableImpositionPersonnesPhysiques)) {
			throw new IllegalArgumentException("Le tiers spécifié n'est pas un contribuable soumis au régime des personnes physiques.");
		}
		final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) tiers;

		final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView(tiersId, depuisTache, SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP));
		view.setTypeContribuable(ImprimerNouvelleDeclarationImpotView.TypeContribuable.PP);
		model.addAttribute("command", view);
		model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpotPP());
		model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());

		// Vérifie que les paramètres reçus sont valides
		final PeriodeImpositionPersonnesPhysiques periode;
		try {
			periode = manager.checkRangeDi(ctb, new DateRangeHelper.Range(dateDebut, dateFin));
		}
		catch (ValidationException e) {
			view.setImprimable(false);
			Flash.error(e.getMessage());
			return "di/imprimer";
		}

		// Détermine quelques valeurs par défaut si nécessaires
		final RegDate delaiAccorde;
		if (delaiRetour != null) {
			delaiAccorde = RegDate.get().addDays(delaiRetour);
		}
		else {
			delaiAccorde = delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate.get());
		}

		if (typeDocument == null) {
			typeDocument = determineTypeDocumentPPParDefaut(tiersId);
		}

		view.setPeriode(periode);
		view.setDelaiAccorde(delaiAccorde);
		view.setTypeDocument(typeDocument);

		// [UNIREG-2705] s'il existe une DI retournée annulée pour le même contribuable et la même
		// période, alors on propose de marquer cette nouvelle DI comme déjà retournée
		if (SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP)) {
			final DeclarationImpotOrdinaire diAnnulee = findDeclarationRetourneeEtAnnulee(tiersId, periode);
			if (diAnnulee != null) {
				view.setDateRetour(RegDate.get());
				view.setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(true);
			}
		}

		return "di/imprimer";
	}

	/**
	 * Affiche un écran qui permet de prévisualiser une déclaration avant son impression (et donc son ajout sur le contribuable).
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/imprimer-pm.do", method = RequestMethod.GET)
	public String imprimerDeclarationPM(@RequestParam("tiersId") long tiersId,
	                                    @RequestParam("debut") RegDate dateDebut,
	                                    @RequestParam("fin") RegDate dateFin,
	                                    @RequestParam(value = "typeDocument", required = false) TypeDocument typeDocument,
	                                    @RequestParam(value = "delaiRetour", required = false) Integer delaiRetour,
	                                    @RequestParam(value = "depuisTache", required = false, defaultValue = "false") boolean depuisTache,
	                                    Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes morales.");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}
		if (!(tiers instanceof ContribuableImpositionPersonnesMorales)) {
			throw new IllegalArgumentException("Le tiers spécifié n'est pas un contribuable soumis au régime des personnes morales.");
		}
		final ContribuableImpositionPersonnesMorales ctb = (ContribuableImpositionPersonnesMorales) tiers;

		final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView(tiersId, depuisTache, SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM));
		view.setTypeContribuable(ImprimerNouvelleDeclarationImpotView.TypeContribuable.PM);
		model.addAttribute("command", view);
		model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());

		// Vérifie que les paramètres reçus sont valides
		final PeriodeImpositionPersonnesMorales periode;
		try {
			periode = manager.checkRangeDi(ctb, new DateRangeHelper.Range(dateDebut, dateFin));
		}
		catch (ValidationException e) {
			view.setImprimable(false);
			Flash.error(e.getMessage());
			return "di/imprimer";
		}

		// Détermine quelques valeurs par défaut si nécessaires
		final RegDate delaiAccorde;
		if (delaiRetour != null) {
			delaiAccorde = RegDate.get().addDays(delaiRetour);
		}
		else {
			delaiAccorde = delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate.get());
		}

		if (typeDocument == null) {
			typeDocument = periode.getTypeDocumentDeclaration();
		}

		view.setPeriode(periode);
		view.setDelaiAccorde(delaiAccorde);
		view.setTypeDocument(typeDocument);

		// [UNIREG-2705] s'il existe une DI retournée annulée pour le même contribuable et la même
		// période, alors on propose de marquer cette nouvelle DI comme déjà retournée
		if (SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM)) {
			final DeclarationImpotOrdinaire diAnnulee = findDeclarationRetourneeEtAnnulee(tiersId, periode);
			if (diAnnulee != null) {
				view.setDateRetour(RegDate.get());
				view.setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(true);
			}
		}

		return "di/imprimer";
	}

	/**
	 * Crée, sauve en base et imprime la déclaration d'impôt.
	 */
	@RequestMapping(value = "/di/imprimer.do", method = RequestMethod.POST)
	public String imprimer(@Valid @ModelAttribute("command") final ImprimerNouvelleDeclarationImpotView view, BindingResult result, HttpServletResponse response, Model model) throws Exception {

		if ((view.getTypeContribuable() == ImprimerNouvelleDeclarationImpotView.TypeContribuable.PM && !SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PM))
				|| (view.getTypeContribuable() == ImprimerNouvelleDeclarationImpotView.TypeContribuable.PP && !SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP))) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt.");
		}
		if (view.getTypeContribuable() == null) {
			throw new IllegalArgumentException("Un type de contribuable doit être fourni...");
		}

		final Long tiersId = view.getTiersId();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (result.hasErrors()) {
			if (view.getTypeContribuable() == ImprimerNouvelleDeclarationImpotView.TypeContribuable.PP) {
				model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpotPP());
			}
			model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());
			return "di/imprimer";
		}

		// On imprime la nouvelle déclaration d'impôt
		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(tiersId);
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, 500);
			try {

				// ... sauf que si on n'a pas de type de document, on n'est pas capable d'imprimer quoi que ce soit...
				// (effectivement, pour les PM par exemple, il y a une période pendant laquelle on doit être capable de
				// générer les DI pour les suivre (sommation...) mais sans possibilité d'impression de la fourre de DI,
				// qui doit être remplie et envoyée à la main...)

				if (view.getTypeDocument() != null) {
					final EditiqueResultat resultat = manager.envoieImpressionLocaleDI(tiersId, view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition(), view.getTypeDocument(),
					                                                                   view.getTypeAdresseRetour(), view.getDelaiAccorde(), view.getDateRetour());

					final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
							new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
								@Override
								public String doJob(EditiqueResultatReroutageInbox resultat) {
									return "redirect:/di/list.do?tiersId=" + tiersId;
								}
							};

					final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
						@Override
						public String doJob(EditiqueResultatErreur resultat) {
							Flash.error(String.format("%s Veuillez imprimer un duplicata de la déclaration d'impôt.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
							return "redirect:/di/list.do?tiersId=" + tiersId;
						}
					};

					return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, null, erreur);
				}
				else {

					// ici, nous sommes donc dans le cas intermédiaire où nous avons une période d'imposition mais pas de modèle de document
					// -> nous devons générer une DI en base mais nous ne pouvons pas l'imprimer...

					manager.genererDISansImpression(tiersId, view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition(), view.getDelaiAccorde(), view.getDateRetour());
					Flash.warning("La déclaration a maintenant été générée dans le système, mais aucune impression n'est prévue pour cette période fiscale. Veuillez procéder si nécessaire à l'envoi de la fourre de déclaration manuellement.");
					return "redirect:/di/list.do?tiersId=" + tiersId;
				}
			}
			finally {
				ticketService.releaseTicket(ticket);
			}
		}
		catch (TicketTimeoutException e) {
			throw new ActionException("Une DI est actuellement en cours d'impression pour ce contribuable. Veuillez ré-essayer ultérieurement.", e);
		}
	}

	private TypeDocument determineTypeDocumentPPParDefaut(long tiersId) {
		//Par défaut le type de DI est celui de la dernière DI émise
		final TypeDocument typeDocument;
		EtatDeclaration etatDiPrecedente = diDAO.findDerniereDiEnvoyee(tiersId);
		if (etatDiPrecedente != null) {
			DeclarationImpotOrdinaire diPrecedente = (DeclarationImpotOrdinaire) etatDiPrecedente.getDeclaration();
			if (diPrecedente.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH) {
				typeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL;
			}
			else {
				typeDocument = diPrecedente.getTypeDeclaration();
			}
		}
		else {
			typeDocument = TypeDocument.DECLARATION_IMPOT_VAUDTAX;
		}
		return typeDocument;
	}

	private DeclarationImpotOrdinaire findDeclarationRetourneeEtAnnulee(long tiersId, PeriodeImposition periode) {

		DeclarationImpotOrdinaire diAnnulee = null;

		final DeclarationImpotCriteria criteres = new DeclarationImpotCriteria();
		criteres.setAnnee(periode.getDateDebut().year());
		criteres.setContribuable(tiersId);

		final List<DeclarationImpotOrdinaire> dis = diDAO.find(criteres);
		if (dis != null && !dis.isEmpty()) {
			for (DeclarationImpotOrdinaire di : dis) {
				if (di.isAnnule() && DateRangeHelper.equals(di, periode)) {
					final EtatDeclaration etat = di.getDernierEtat();
					if (etat != null && etat.getEtat() == TypeEtatDeclaration.RETOURNEE) {
						diAnnulee = di;
						break;
					}
				}
			}
		}

		return diAnnulee;
	}

	/**
	 * Affiche un écran qui permet de quittancer une déclaration.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/etat/ajouter.do", method = RequestMethod.GET)
	public String ajouterEtat(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, false, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final AjouterEtatDeclarationView view;
		if (di instanceof DeclarationImpotOrdinairePP) {
			view = new AjouterEtatDeclarationView((DeclarationImpotOrdinairePP) di, messageSource);
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			view = new AjouterEtatDeclarationView((DeclarationImpotOrdinairePM) di, messageSource);
		}
		else {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		model.addAttribute("command", view);
		model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());

		return "di/etat/ajouter";
	}

	/**
	 * Quittance une déclaration d'impôt manuellement
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/ajouter.do", method = RequestMethod.POST)
	public String ajouterEtat(@Valid @ModelAttribute("command") final AjouterEtatDeclarationView view, BindingResult result, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(view.getId());
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, false, false, false);

		if (result.hasErrors()) {
			view.initReadOnlyValues(di, view.isTypeDocumentEditable(), messageSource);
			model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());
			return "di/etat/ajouter";
		}

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On quittance la DI
		final TypeDocument typeDocument;
		if (di.getTypeDeclaration() == TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH && view.getTypeDocument() == TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL) {
			typeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH; // inutile de changer le type de batch à local
		}
		else {
			typeDocument = view.getTypeDocument();
		}
		manager.quittancerDI(view.getId(), typeDocument, view.getDateRetour());

		return "redirect:/di/editer.do?id=" + di.getId();
	}

	/**
	 * Annuler le quittancement spécifié.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/annuler.do", method = RequestMethod.POST)
	public String annulerEtat(@RequestParam("id") final long id) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt.");
		}

		// Vérifie les paramètres
		final EtatDeclaration etat = hibernateTemplate.get(EtatDeclaration.class, id);
		if (etat == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.etat.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(etat instanceof EtatDeclarationRetournee)) {
			throw new IllegalArgumentException("Seuls les quittancements peuvent être annulés.");
		}

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) etat.getDeclaration();
		checkAccessRights(di, false, true, false, false, false);
		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On annule le quittancement
		final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
		retour.setAnnule(true);

		Flash.message("Le quittancement du " + RegDateHelper.dateToDisplayString(retour.getDateObtention()) + " a été annulé.");
		return "redirect:/di/editer.do?id=" + di.getId();
	}

	/**
	 * Affiche un écran qui permet d'éditer une déclaration.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/editer.do", method = RequestMethod.GET)
	public String editer(@RequestParam("id") long id,
	                     @RequestParam(value = "tacheId", required = false) Long tacheId,
	                     Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM, Role.DI_DELAI_PP, Role.DI_DELAI_PM, Role.DI_SOM_PP, Role.DI_SOM_PM, Role.DI_DUPLIC_PP, Role.DI_DUPLIC_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, true, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final EditerDeclarationImpotView view;
		if (di instanceof DeclarationImpotOrdinairePP) {
			view = new EditerDeclarationImpotView(di, tacheId, messageSource,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP));
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			view = new EditerDeclarationImpotView(di, tacheId, messageSource,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PM),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PM) && di.getTypeDeclaration() != null);
		}
		else {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas une déclaration d'impôt ordinaire PP ou PM.");
		}

		model.addAttribute("command", view);
		return "di/editer";
	}

	/**
	 * Sommer la déclaration d'impôt spécifiée.
	 */
	@RequestMapping(value = "/di/sommer.do", method = RequestMethod.POST)
	public String sommerDeclarationImpotPersonnesPhysiques(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_SOM_PP, Role.DI_SOM_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de sommation des déclarations d'impôt.");
		}

		// Vérifie les paramètres
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}
				checkAccessRights(di, false, false, false, true, false);

				if (!EditerDeclarationImpotView.isSommable(di)) {
					throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas dans un état sommable.");
				}

				final Contribuable ctb = di.getTiers();
				controllerUtils.checkAccesDossierEnEcriture(ctb.getId());
				return null;
			}
		});

		// On imprime la sommation
		final EditiqueResultat resultat = manager.envoieImpressionLocalSommationDI(id);
		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "sommationDi", inbox, null, erreur);
	}

	/**
	 * Imprime un duplicata de DI PM (= sans possibilité de choisir les annexes ou le type de document, repris de l'original)
	 */
	@RequestMapping(value = "/di/duplicata-pm.do", method = RequestMethod.POST)
	public String duplicataDeclarationPersonnesMorales(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes morales.");
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final TypeDocument typeDocument = template.execute(new TxCallback<TypeDocument>() {
			@Override
			public TypeDocument execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null || !(di instanceof DeclarationImpotOrdinairePM)) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}

				final ContribuableImpositionPersonnesMorales ctb = (ContribuableImpositionPersonnesMorales) di.getTiers();
				controllerUtils.checkAccesDossierEnEcriture(ctb.getId());
				return di.getTypeDeclaration();
			}
		});

		final EditiqueResultat resultat = manager.envoieImpressionLocalDuplicataDI(id, typeDocument, null, false);
		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, null, erreur);
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'impression d'un duplicata de DI
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/duplicata-pp.do", method = RequestMethod.GET)
	public String choixDuplicata(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, false, false, true);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		model.addAttribute("command", new ImprimerDuplicataDeclarationImpotView(di, modeleDocumentDAO));
		model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpotPP());       // seules les DI PP en ont besoin, les autres sont en duplicata "direct"
		return "di/duplicata";
	}

	/**
	 * Imprime un duplicata de DI (avec annexes et potentiellement nouveau type de document).
	 */
	@RequestMapping(value = "/di/duplicata-pp.do", method = RequestMethod.POST)
	public String duplicataDeclarationPersonnesPhysiques(@Valid @ModelAttribute("command") final ImprimerDuplicataDeclarationImpotView view,
	                                                     BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes physiques.");
		}

		if (result.hasErrors()) {
			return "di/duplicata";
		}

		// Vérifie les paramètres
		final Long id = view.getIdDI();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final String redirect = template.execute(new TxCallback<String>() {
			@Override
			public String execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null || !(di instanceof DeclarationImpotOrdinairePP)) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}

				final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) di.getTiers();
				controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

				// Vérification de la période d'imposition du contribuable
				if (periodeImpositionService.determine(ctb, di) == null) {
					Flash.error("Echec de l'impression du duplicata, le contribuable n'a pas de données valides à la fin de la période de la déclaration d'impôt.");
					return "redirect:/di/editer.do?id=" + di.getId();
				}

				return null;
			}
		});
		if (redirect != null) {
			return redirect;
		}

		//Si la valeur de toSave est nulle c'est que nous sommes sur le même type de document, on a pas besoin de le sauvegarder
		final boolean saveTypeDoc = (view.getToSave() == null ? false : view.getToSave());
		// On imprime le duplicata

		final EditiqueResultat resultat = manager.envoieImpressionLocalDuplicataDI(view.getIdDI(), view.getSelectedTypeDocument(), view.getSelectedAnnexes(), saveTypeDoc);

		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, null, erreur);
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'un délai sur une DI
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/delai/ajouter.do", method = RequestMethod.GET)
	public String ajouterDelai(@RequestParam("id") long id,
	                        Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DELAI_PP, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, true, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate.get());
		model.addAttribute("command", new AjouterDelaiDeclarationView(di, delaiAccordeAu));
		return "di/delai/ajouter";
	}

	/**
	 * Ajoute un délai sur une DI
	 */
	@RequestMapping(value = "/di/delai/ajouter.do", method = RequestMethod.POST)
	public String ajouterDelai(@Valid @ModelAttribute("command") final AjouterDelaiDeclarationView view,
	                        BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DELAI_PP, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final Long id = view.getIdDeclaration();

		if (result.hasErrors()) {
			fixModel(id, view);
			return "di/delai/ajouter";
		}

		// Vérifie les paramètres
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}
				checkAccessRights(di, false, false, true, false, false);

				final Contribuable ctb = di.getTiers();
				controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

				return null;
			}
		});

		// On ajoute le délai
		final Long idDelai = manager.saveDelai(id, view.getDateDemande(), view.getDelaiAccordeAu(), view.isConfirmationEcrite());

		if (view.isConfirmationEcrite()) {

			// On imprime le duplicata
			final EditiqueResultat resultat = manager.envoieImpressionLocalConfirmationDelai(id, idDelai);

			final RedirectEditDI inbox = new RedirectEditDI(id);
			final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
			return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "delai", inbox, null, erreur);
		}
		else {
			// Pas de duplicata -> on retourne à l'édition de la DI
			return "redirect:/di/editer.do?id=" + id;
		}
	}

	private void fixModel(final long id, final AjouterDelaiDeclarationView view) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		template.setReadOnly(true);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}
				view.setDiInfo(di);
			}
		});
	}

	private static class RedirectEditDI implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> {
		private final long id;

		public RedirectEditDI(long id) {
			this.id = id;
		}

		@Override
		public String doJob(EditiqueResultatReroutageInbox resultat) {
			return "redirect:/di/editer.do?id=" + id;
		}
	}

	private static class RedirectEditDIApresErreur implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> {
		private final long id;
		private final MessageSource messageSource;

		public RedirectEditDIApresErreur(long id, MessageSource messageSource) {
			this.id = id;
			this.messageSource = messageSource;
		}

		@Override
		public String doJob(EditiqueResultatErreur resultat) {
			final String message = messageSource.getMessage("global.error.communication.editique", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "redirect:/di/editer.do?id=" + id;
		}
	}
}
