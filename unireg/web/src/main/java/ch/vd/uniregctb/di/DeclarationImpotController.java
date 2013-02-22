package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
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
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
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
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Contribuable;
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

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false, RegDateHelper.StringFormat.INDEX));
		// champs du formulaire de création d'un nouvelle déclaration
		binder.registerCustomEditor(RegDate.class, "delaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateDebutPeriodeImposition", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateFinPeriodeImposition", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "dateRetour", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		// champs du formulaire d'ajout de délai à une déclaration
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	/**
	 * Liste les déclarations d'impôt d'un contribuable
	 * @param tiersId le numéro d'un contribuable
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/list.do", method = RequestMethod.GET)
	public String list(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DELAI_PP, Role.DI_DUPLIC_PP, Role.DI_QUIT_PP, Role.DI_SOM_PP)) {
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL, Role.VISU_LIMITE)) {
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL, Role.VISU_LIMITE)) {
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
		final Contribuable tiers = (Contribuable) di.getTiers();
		diService.annulationDI(tiers, di, tacheId, RegDate.get());

		if (tacheId != null) {
			return "redirect:/tache/list.do";
		}
		else {
			return "redirect:/di/list.do?tiersId=" + tiersId;
		}
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire.
	 *
	 * @param id l'id de la déclaration d'impôt ordinaire à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/desannuler.do", method = RequestMethod.POST)
	public String desannuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DESANNUL_PP)) {
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
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		// désannulation de la déclaration
		final Contribuable tiers = (Contribuable) di.getTiers();
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
	@RequestMapping(value = "/di/choisir.do", method = RequestMethod.GET)
	public String choisir(@RequestParam("tiersId") long tiersId, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(tiersId);
		if (ranges == null || ranges.isEmpty()) {
			// [UNIREG-832] impossible d'imprimer une nouvelle DI: on reste dans le même écran et on affiche un message d'erreur
			Flash.warning(DeclarationImpotEditManager.CANNOT_ADD_NEW_DI);
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", Collections.emptyList());
			return "/di/choisir";
		}
		else if (ranges.size() == 1) {
			final DateRange range = ranges.get(0);
			// il reste exactement une DI à créer : on continue directement sur l'écran d'impression
			return "redirect:/di/imprimer.do?tiersId=" + tiersId + "&debut=" + range.getDateDebut().index() + "&fin=" + range.getDateFin().index();
		}
		else {
			// [UNIREG-889] il y reste plusieurs DIs à créer : on demande à l'utilisateur de choisir
			final ArrayList<ChoixDeclarationImpotView> views = new ArrayList<ChoixDeclarationImpotView>(ranges.size());
			for (PeriodeImposition r : ranges) {
				views.add(new ChoixDeclarationImpotView(r, r.isOptionnelle()));
			}
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", views);
			return "/di/choisir";
		}
	}

	/**
	 * Affiche un écran qui permet de prévisualiser une déclaration avant son impression (et donc son ajout sur le contribuable).
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/imprimer.do", method = RequestMethod.GET)
	public String imprimer(@RequestParam("tiersId") long tiersId,
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
		if (!(tiers instanceof Contribuable)) {
			throw new IllegalArgumentException("Le tiers spécifié n'est pas un contribuable.");
		}
		final Contribuable ctb = (Contribuable) tiers;

		final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView(tiersId, depuisTache, SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP));
		model.addAttribute("command", view);
		model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpot());
		model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());

		// Vérifie que les paramètres reçus sont valides
		final PeriodeImposition periode;
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
			typeDocument = determineTypeDocumentParDefaut(tiersId);
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
	 * Crée, sauve en base et imprime la déclaration d'impôt.
	 */
	@RequestMapping(value = "/di/imprimer.do", method = RequestMethod.POST)
	public String imprimer(@Valid @ModelAttribute("command") final ImprimerNouvelleDeclarationImpotView view, BindingResult result,
	                       HttpServletRequest request, HttpServletResponse response, Model model) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		final Long tiersId = view.getTiersId();
		controllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (result.hasErrors()) {
			model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpot());
			model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());
			return "di/imprimer";
		}

		// On imprime la nouvelle déclaration d'impôt

		final EditiqueResultat resultat = manager.envoieImpressionLocalDI(tiersId, null, view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition(), view.getTypeDocument(),
				view.getTypeAdresseRetour(), view.getDelaiAccorde(), view.getDateRetour());

		final RetourEditiqueControllerHelper.TraitementRetourEditique inbox = new RetourEditiqueControllerHelper.TraitementRetourEditique() {
			@Override
			public String doJob(EditiqueResultat resultat) {
				return "redirect:/di/list.do?tiersId=" + tiersId;
			}
		};

		final RetourEditiqueControllerHelper.TraitementRetourEditique erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique() {
			@Override
			public String doJob(EditiqueResultat resultat) {
				Flash.error(String.format("%s Veuillez imprimer un duplicata de la déclaration d'impôt.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return "redirect:/di/list.do?tiersId=" + tiersId;
			}
		};

		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, erreur, erreur);
	}

	private TypeDocument determineTypeDocumentParDefaut(long tiersId) {
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt sur les personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final AjouterEtatDeclarationView view = new AjouterEtatDeclarationView(di, messageSource);
		model.addAttribute("command", view);
		model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());

		return "di/etat/ajouter";
	}

	/**
	 * Quittance une déclaration d'impôt manuellement
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/ajouter.do", method = RequestMethod.POST)
	public String ajouterEtat(@Valid @ModelAttribute("command") final AjouterEtatDeclarationView view, BindingResult result,
	                     Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt sur les personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(view.getId());
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		if (result.hasErrors()) {
			view.initReadOnlyValues(di, messageSource);
			model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());
			return "di/etat/ajouter";
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
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
	public String annulerEtat(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des délais sur les déclarations d'impôt des personnes physiques.");
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

		final Contribuable ctb = (Contribuable) di.getTiers();
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

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des déclarations d'impôt sur les personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final EditerDeclarationImpotView view = new EditerDeclarationImpotView(di, tacheId, messageSource, SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP),
				SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP), SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP), SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP));
		model.addAttribute("command", view);

		return "di/editer";
	}

	/**
	 * Sommer la déclaration spécifiée.
	 */
	@RequestMapping(value = "/di/sommer.do", method = RequestMethod.POST)
	public String sommer(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de sommation des déclarations d'impôt sur les personnes physiques.");
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

				if (!EditerDeclarationImpotView.isSommable(di)) {
					throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas dans un état sommable.");
				}

				final Contribuable ctb = (Contribuable) di.getTiers();
				controllerUtils.checkAccesDossierEnEcriture(ctb.getId());
				return null;
			}
		});

		// On imprime la sommation

		final EditiqueResultat resultat = manager.envoieImpressionLocalSommationDI(id);

		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "sommationDi", inbox, erreur, erreur);
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'impression d'un duplicata de DI
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/duplicata.do", method = RequestMethod.GET)
	public String duplicata(@RequestParam("id") long id,
	                       Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicats de déclarations d'impôt sur les personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		model.addAttribute("command", new ImprimerDuplicataDeclarationImpotView(di, modeleDocumentDAO));
		return "di/duplicata";
	}

	/**
	 * Imprime un duplicata de DI.
	 */
	@RequestMapping(value = "/di/duplicata.do", method = RequestMethod.POST)
	public String duplicata(@Valid @ModelAttribute("command") final ImprimerDuplicataDeclarationImpotView view,
	                        BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicats de déclarations d'impôt sur les personnes physiques.");
		}

		final Long id = view.getIdDI();

		if (result.hasErrors()) {
			return "di/duplicata";
		}

		// Vérifie les paramètres
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		final String redirect = template.execute(new TxCallback<String>() {
			@Override
			public String execute(TransactionStatus status) throws Exception {
				final DeclarationImpotOrdinaire di = diDAO.get(id);
				if (di == null) {
					throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
				}

				final Contribuable ctb = (Contribuable) di.getTiers();
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

		// On imprime le duplicata

		final EditiqueResultat resultat = manager.envoieImpressionLocalDuplicataDI(view.getIdDI(), view.getSelectedTypeDocument(), view.getSelectedAnnexes());

		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, erreur, erreur);
	}

	/**
	 * Annuler le délai spécifié.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/delai/annuler.do", method = RequestMethod.POST)
	public String annulerDelai(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des délais sur les déclarations d'impôt des personnes physiques.");
		}

		// Vérifie les paramètres
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, id);
		if (delai == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		final Declaration di = delai.getDeclaration();
		final RegDate premier = di.getPremierDelai();
		if (di.getDelaiAccordeAu() == premier) {
			throw new IllegalArgumentException("Le premier délai accordé ne peut pas être annulé.");
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On annule le délai

		manager.annulerDelai(di.getId(), delai.getId());

		Flash.message("Le délai a été annulé.");
		return "redirect:/di/editer.do?id=" + di.getId();
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'un délai sur une DI
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/delai/ajouter.do", method = RequestMethod.GET)
	public String ajouterDelai(@RequestParam("id") long id,
	                        Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = (Contribuable) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = delaisService.getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate.get());
		model.addAttribute("command", new AjouterDelaiDeclarationView(di, delaiAccordeAu));
		return "di/delai/ajouter";
	}

	/**
	 * Imprime un duplicata de DI.
	 */
	@RequestMapping(value = "/di/delai/ajouter.do", method = RequestMethod.POST)
	public String ajouterDelai(@Valid @ModelAttribute("command") final AjouterDelaiDeclarationView view,
	                        BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
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

				final Contribuable ctb = (Contribuable) di.getTiers();
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
			return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "delai", inbox, erreur, erreur);
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

	private static class RedirectEditDI implements RetourEditiqueControllerHelper.TraitementRetourEditique {
		private final long id;

		public RedirectEditDI(long id) {
			this.id = id;
		}

		@Override
		public String doJob(EditiqueResultat resultat) {
			return "redirect:/di/editer.do?id=" + id;
		}
	}

	private static class RedirectEditDIApresErreur implements RetourEditiqueControllerHelper.TraitementRetourEditique {
		private final long id;
		private final MessageSource messageSource;

		public RedirectEditDIApresErreur(long id, MessageSource messageSource) {
			this.id = id;
			this.messageSource = messageSource;
		}

		@Override
		public String doJob(EditiqueResultat resultat) {
			final String message = messageSource.getMessage("global.error.communication.editique", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "redirect:/di/editer.do?id=" + id;
		}
	}
}
