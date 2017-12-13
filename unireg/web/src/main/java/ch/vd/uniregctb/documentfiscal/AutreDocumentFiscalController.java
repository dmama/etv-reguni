package ch.vd.uniregctb.documentfiscal;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.utils.WebContextUtils;

@Controller
@RequestMapping("/autresdocs")
public class AutreDocumentFiscalController {

	private SecurityProviderInterface securityProvider;
	private AutreDocumentFiscalManager autreDocumentFiscalManager;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private SessionFactory sessionFactory;
	private ControllerUtils controllerUtils;
	private MessageSource messageSource;
	private ServiceInfrastructureService infraService;
	private DelaisService delaisService;

	private Validator editionValidator;

	private static final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> TYPES_DOC_ALLOWED = buildTypesDocAllowed();

	private static Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> buildTypesDocAllowed() {
		final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> map = new EnumMap<>(Role.class);
		map.put(Role.ENVOI_AUTORISATION_RADIATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.AUTORISATION_RADIATION));
		map.put(Role.ENVOI_DEMANDE_BILAN_FINAL, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.DEMANDE_BILAN_FINAL));
		map.put(Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.LETTRE_TYPE_INFORMATION_LIQUIDATION));
		return map;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutreDocumentFiscalManager(AutreDocumentFiscalManager autreDocumentFiscalManager) {
		this.autreDocumentFiscalManager = autreDocumentFiscalManager;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setEditionValidator(Validator editionValidator) {
		this.editionValidator = editionValidator;
	}

	@InitBinder(value = "ajouterView")
	public void initAjouterDelaiBinder(WebDataBinder binder) {
		binder.setValidator(editionValidator);
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "ancienDelaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}
	@InitBinder(value = "ajouterQuittance")
	public void initAjouterQuittanceBinder(WebDataBinder binder) {
		binder.setValidator(editionValidator);
		binder.registerCustomEditor(RegDate.class, "dateRetour", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "print")
	public void initPrintBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, "dateReference", new RegDateEditor(true, false, false));
		binder.setValidator(new ImprimerAutreDocumentFiscalValidator());
	}

	private void checkAnyRight() throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ENVOI_AUTORISATION_RADIATION, Role.ENVOI_DEMANDE_BILAN_FINAL, Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION,
		                                 Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les autres documents fiscaux.");
		}
	}

	/**
	 * Affiche le panneau d'édition des autres documents fiscaux
	 */
	@RequestMapping(value = "/edit-list.do", method = RequestMethod.GET)
	public String showEditList(Model model, @RequestParam(value = "pmId") long idEntreprise) {
		checkAnyRight();

		return showEditList(model, new ImprimerAutreDocumentFiscalView(idEntreprise, null));
	}

	private Set<TypeAutreDocumentFiscalEmettableManuellement> getTypesAutreDocumentFiscalEmettablesManuellement() {
		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = EnumSet.noneOf(TypeAutreDocumentFiscalEmettableManuellement.class);
		for (Map.Entry<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> entry : TYPES_DOC_ALLOWED.entrySet()) {
			if (SecurityHelper.isGranted(securityProvider, entry.getKey())) {
				allowed.addAll(entry.getValue());
			}
		}
		return allowed;
	}

	private Map<TypeAutreDocumentFiscalEmettableManuellement, String> getTypesAutreDocumentFiscalEmettableManuellement() {
		final Map<TypeAutreDocumentFiscalEmettableManuellement, String> all = tiersMapHelper.getTypesAutreDocumentFiscalEmettableManuellement();
		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = getTypesAutreDocumentFiscalEmettablesManuellement();
		final Map<TypeAutreDocumentFiscalEmettableManuellement, String> filtered = new LinkedHashMap<>(all);        // conservation de l'ordre !
		filtered.keySet().retainAll(allowed);
		return filtered;
	}

	private String showEditList(Model model, ImprimerAutreDocumentFiscalView view) {
		final long idEntreprise = view.getNoEntreprise();

		// vérification des droits en écriture
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		model.addAttribute("pmId", idEntreprise);
		model.addAttribute("documents", autreDocumentFiscalManager.getAutresDocumentsFiscauxSansSuivi(idEntreprise));
		model.addAttribute("typesDocument", getTypesAutreDocumentFiscalEmettableManuellement());
		model.addAttribute("print", view);
		model.addAttribute("isRadieeRCOuDissoute", autreDocumentFiscalManager.hasAnyEtat(idEntreprise, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
		model.addAttribute("docsAvecSuivi", new AutreDocumentFiscalListView(idEntreprise, autreDocumentFiscalManager.getAutresDocumentsFiscauxAvecSuivi(idEntreprise)));
		return "tiers/edition/pm/autresdocs";
	}

	/**
	 * Impression (et ajout sur l'entreprise) d'un nouveau document
	 */
	@RequestMapping(value = "/print.do", method = RequestMethod.POST)
	public String imprimerNouveauDocument(@Valid @ModelAttribute("print") final ImprimerAutreDocumentFiscalView view, BindingResult bindingResult, Model model) throws IOException {
		if (bindingResult.hasErrors()) {
			return showEditList(model, view);
		}

		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = getTypesAutreDocumentFiscalEmettablesManuellement();
		if (!allowed.contains(view.getTypeDocument())) {
			throw new AccessDeniedException("Vous ne possédez aucun des droits IfoSec permettant d'émettre ce type de document.");
		}

		final EditiqueResultat resultat;
		try {
			resultat = autreDocumentFiscalManager.createAndPrint(view);
		}
		catch (AutreDocumentFiscalException e) {
			throw new ActionException("Impossible d'imprimer le document voulu", e);
		}

		final String redirect = String.format("redirect:/autresdocs/edit-list.do?pmId=%d", view.getNoEntreprise());

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
					@Override
					public String doJob(EditiqueResultatReroutageInbox resultat) {
						return redirect;
					}
				};

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
			@Override
			public String doJob(EditiqueResultatErreur resultat) {
				Flash.error(EditiqueErrorHelper.getMessageErreurEditique(resultat));
				return redirect;
			}
		};

		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        view.getTypeDocument().name().toLowerCase(),
		                                                                        redirect,
		                                                                        false,
		                                                                        inbox,
		                                                                        null,
		                                                                        erreur);
	}

	/**
	 * Affichage de la fenêtre de détail pour un autre document fiscal
	 */
	@RequestMapping(value = "/details.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@ResponseBody
	public AutreDocumentFiscalView detailsAutreDocFiscal(@RequestParam("id") long id) throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final AutreDocumentFiscal autreDocumentFiscal = getDocumentFiscal(id);

		// Vérification des droits en lecture
		final Long tiersId = autreDocumentFiscal.getTiers().getId();
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		return AutreDocumentFiscalViewFactory.buildView(autreDocumentFiscal, infraService, messageSource);
	}

	/**
	 * Affiche un écran qui permet d'éditer d'un document fiscal avec suivi.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/editer.do", method = RequestMethod.GET)
	public String editer(@RequestParam("id") long id,
	                     Model model) throws AccessDeniedException {

		/* Pour l'instant, la lettre de bienvenue est le seul document fiscal avec suivi qu'on est susceptible d'éditer. */
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final AutreDocumentFiscalView view;

		if (doc instanceof AutreDocumentFiscalAvecSuivi) {
			view = AutreDocumentFiscalViewFactory.buildView(doc, infraService, messageSource);
		}
		else {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas un document fiscal avec suivi.");
		}

		model.addAttribute("command", view);
		return "documentfiscal/editer";
	}

	@NotNull
	private AutreDocumentFiscal getDocumentFiscal(@RequestParam("id") long id) {
		final AutreDocumentFiscal doc = (AutreDocumentFiscal) sessionFactory.getCurrentSession().get(AutreDocumentFiscal.class, id);
		if (doc == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.docfisc.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		return doc;
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'une demande de délai
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/delai/ajouter.do", method = RequestMethod.GET)
	public String ajouterDelaiDiPM(@RequestParam("id") long id,
	                               Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un délai à un autre document fiscal.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = determineDateAccordDelaiParDefaut(doc.getDelaiAccordeAu());
		model.addAttribute("ajouterView", new EditionDelaiAutreDocumentFiscalView(doc, delaiAccordeAu));
		return "documentfiscal/delai/ajouter";
	}

	/**
	 * [SIFISC-18869] la date par défaut du délai accordé (sursis ou pas) ne doit de toute façon pas être dans le passé
	 * @param delaiPrecedent la date actuelle du délai accordé
	 * @return la nouvelle date à proposer comme délai par défaut
	 */
	private RegDate determineDateAccordDelaiParDefaut(RegDate delaiPrecedent) {
		final RegDate delaiNormal = delaisService.getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(delaiPrecedent);
		return RegDateHelper.maximum(delaiNormal, RegDate.get(), NullDateBehavior.EARLIEST);
	}

	/**
	 * Ajoute un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/ajouter.do", method = RequestMethod.POST)
	public String ajouterDemandeDelaiPM(@Valid @ModelAttribute("ajouterView") final EditionDelaiAutreDocumentFiscalView view,
	                                    BindingResult result, Model model, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'un autre document fiscal");
		}

		final Long id = view.getIdDocumentFiscal();

		if (result.hasErrors()) {
			final AutreDocumentFiscal documentFiscal = getDocumentFiscal(id);
			view.resetDocumentInfo(documentFiscal);
			return "documentfiscal/delai/ajouter";
		}

		// Vérifie les paramètres
		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On ajoute le délai
		final RegDate delaiAccordeAu = view.getDelaiAccordeAu();
		autreDocumentFiscalManager.saveNouveauDelai(id, view.getDateDemande(), delaiAccordeAu, EtatDelaiDocumentFiscal.ACCORDE);
		return "redirect:/autresdocs/editer.do?id=" + id;
	}

	/**
	 * Annule un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/annuler.do", method = RequestMethod.POST)
	public String annuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'un autre document fiscal");
		}

		final DelaiDocumentFiscal delai = (DelaiDocumentFiscal) sessionFactory.getCurrentSession().get(DelaiDocumentFiscal.class, id);
		if (delai == null) {
			throw new IllegalArgumentException("Le délai n°" + id + " n'existe pas.");
		}

		final Entreprise ctb = (Entreprise) delai.getDocumentFiscal().getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		delai.setAnnule(true);

		return "redirect:/autresdocs/editer.do?id=" + delai.getDocumentFiscal().getId();
	}

	/**
	 * Affiche un écran qui permet de quittancer un autre document fiscal avec suivi.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/etat/ajouter-quittance.do", method = RequestMethod.GET)
	public String ajouterEtat(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des autres documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);
		if (!(doc instanceof LettreBienvenue)) {
			throw new IllegalArgumentException("A ce jour, seule une lettre de bienvenue peut être quittancée via l'IHM des autres documents fiscaux.");
		}

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		AjouterEtatAutreDocumentFiscalView view = new AjouterEtatAutreDocumentFiscalView(doc, infraService, messageSource);
		if (view.getDateRetour() == null) {
			view.setDateRetour(RegDate.get());
		}

		model.addAttribute("ajouterQuittance", view);

		return "documentfiscal/etat/ajouter-quittance";
	}

	/**
	 * Quittance d'un autre document fiscal avec suivi
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/etat/ajouter-quittance.do", method = RequestMethod.POST)
	public String ajouterEtat(@Valid @ModelAttribute("ajouterQuittance") final AjouterEtatAutreDocumentFiscalView view, BindingResult result, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des autres documents fiscaux.");
		}

		if (result.hasErrors()) {
			final AutreDocumentFiscal doc = getDocumentFiscal(view.getId());
			view.resetDocumentInfo(doc, infraService, messageSource);
			return "documentfiscal/etat/ajouter-quittance";
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(view.getId());

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On quittance
		if (doc instanceof LettreBienvenue) {
			final boolean success = autreDocumentFiscalManager.quittanceLettreBienvenue(doc.getId(), view.getDateRetour());
			if (success) {
				Flash.message(String.format("La lettre de bienvenue n°%s a été quittancée avec succès.", FormatNumeroHelper.numeroCTBToDisplay(doc.getId())));
			}
			else {
				Flash.warning(String.format("La lettre de bienvenue n°%s, étant déjà retournée en date du %s, n'a pas été quittancée à nouveau.",
				                            FormatNumeroHelper.numeroCTBToDisplay(doc.getId()), RegDateHelper.dateToDisplayString(doc.getDateRetour())));
			}
		}
		else {
			throw new IllegalArgumentException("A ce jour, seule une lettre de bienvenue peut être quittancée via l'IHM des autres documents fiscaux.");
		}

		return "redirect:/autresdocs/editer.do?id=" + doc.getId();
	}

	/**
	 * Annuler le quittancement spécifié.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/etat/annuler-quittance.do", method = RequestMethod.POST)
	public String annulerQuittancement(@RequestParam("id") final long id) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des autres documents fiscaux.");
		}

		// Vérifie les paramètres
		final EtatAutreDocumentFiscal etat = (EtatAutreDocumentFiscal) sessionFactory.getCurrentSession().get(EtatAutreDocumentFiscal.class, id);
		if (etat == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.etat.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(etat instanceof EtatAutreDocumentFiscalRetourne)) {
			throw new IllegalArgumentException("Seuls les quittancements peuvent être annulés.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(etat.getAutreDocumentFiscal().getId());
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On annule le quittancement
		final EtatAutreDocumentFiscalRetourne retour = (EtatAutreDocumentFiscalRetourne) etat;
		if (doc instanceof LettreBienvenue) {
			retour.setAnnule(true);
		}
		else {
			throw new IllegalArgumentException("A ce jour, seul le quittancement d'une lettre de bienvenue peut être annulé via l'IHM des autres documents fiscaux.");
		}

		Flash.message("Le quittancement du " + RegDateHelper.dateToDisplayString(retour.getDateObtention()) + " a été annulé.");
		return "redirect:/autresdocs/editer.do?id=" + doc.getId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/annuler.do", method = RequestMethod.POST)
	public String annuler(@RequestParam("id") long id, @RequestParam(value = "tacheId", required = false) Long tacheId) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		if (!(doc instanceof LettreBienvenue)) {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas un document annulable.");
		}

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// annulation de l'autre document fiscal
		autreDocumentFiscalManager.annulerAutreDocumentFiscal(doc);

		return "redirect:/autresdocs/edit-list.do?pmId=" + ctb.getId();
	}

	/**
	 * Désannuler une déclaration d'impôt ordinaire PM.
	 *
	 * @param id l'id de la déclaration d'impôt ordinaire à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/desannuler.do", method = RequestMethod.POST)
	public String desannulerDeclarationImpotPM(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de désannulation des autres documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		if (!(doc instanceof LettreBienvenue)) {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas un document annulable.");
		}

		if (!doc.isAnnule()) {
			throw new IllegalArgumentException("La document fiscal n°" + id + " n'est pas annulée.");
		}

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// désannulation de l'autre document fiscal
		autreDocumentFiscalManager.desannulerAutreDocumentFiscal(doc);

		return "redirect:/autresdocs/edit-list.do?pmId=" + ctb.getId();
	}

	/**
	 * Imprime un duplicata de DI PM
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/duplicata.do", method = RequestMethod.POST)
	public String duplicataDeclarationPersonnesMorales(@RequestParam("id") long id,
	                                                   HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec pour imprimer des duplicata de lettre de bienvenue.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final EditiqueResultat resultat = autreDocumentFiscalManager.envoieImpressionLocalDuplicataLettreBienvenue(id);
		final RedirectEditLettreBienvenue inbox = new RedirectEditLettreBienvenue(id);
		final RedirectEditLettreBienvenueApresErreur erreur = new RedirectEditLettreBienvenueApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "lb", inbox, null, erreur);
	}

	private static class RedirectEditLettreBienvenue implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> {
		private final long id;

		public RedirectEditLettreBienvenue(long id) {
			this.id = id;
		}

		@Override
		public String doJob(EditiqueResultatReroutageInbox resultat) {
			return "redirect:/autresdocs/editer.do?id=" + id;
		}
	}

	private static class RedirectEditLettreBienvenueApresErreur implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> {
		private final long id;
		private final MessageSource messageSource;

		public RedirectEditLettreBienvenueApresErreur(long id, MessageSource messageSource) {
			this.id = id;
			this.messageSource = messageSource;
		}

		@Override
		public String doJob(EditiqueResultatErreur resultat) {
			final String message = messageSource.getMessage("global.error.communication.editique", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "redirect:/autresdocs/editer.do?id=" + id;
		}
	}


}
