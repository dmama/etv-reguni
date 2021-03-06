package ch.vd.unireg.documentfiscal;

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
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

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

	private Validator ajouterDelaiValidator;
	private Validator ajouterQuittanceValidator;

	private static final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> TYPES_DOC_ALLOWED = buildTypesDocAllowed();
	private MessageHelper messageHelper;

	private static Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> buildTypesDocAllowed() {
		final Map<Role, Set<TypeAutreDocumentFiscalEmettableManuellement>> map = new EnumMap<>(Role.class);
		map.put(Role.ENVOI_AUTORISATION_RADIATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.AUTORISATION_RADIATION));
		map.put(Role.ENVOI_DEMANDE_BILAN_FINAL, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.DEMANDE_BILAN_FINAL));
		map.put(Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.LETTRE_TYPE_INFORMATION_LIQUIDATION));
		map.put(Role.GEST_QUIT_LETTRE_BIENVENUE, EnumSet.of(TypeAutreDocumentFiscalEmettableManuellement.LETTRE_BIENVENUE));
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

	public void setAjouterDelaiValidator(Validator ajouterDelaiValidator) {
		this.ajouterDelaiValidator = ajouterDelaiValidator;
	}

	public void setAjouterQuittanceValidator(Validator ajouterQuittanceValidator) {
		this.ajouterQuittanceValidator = ajouterQuittanceValidator;
	}

	@InitBinder(value = "ajouterDelai")
	public void initAjouterDelaiBinder(WebDataBinder binder) {
		binder.setValidator(ajouterDelaiValidator);
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "ancienDelaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "ajouterQuittance")
	public void initAjouterQuittanceBinder(WebDataBinder binder) {
		binder.setValidator(ajouterQuittanceValidator);
		binder.registerCustomEditor(RegDate.class, "dateRetour", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	@InitBinder(value = "print")
	public void initPrintBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new ImprimerAutreDocumentFiscalValidator());
	}

	private void checkAnyRight() throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ENVOI_AUTORISATION_RADIATION, Role.ENVOI_DEMANDE_BILAN_FINAL, Role.ENVOI_LETTRE_TYPE_INFO_LIQUIDATION,
		                                 Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour gérer les autres documents fiscaux.");
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
		model.addAttribute("typesLettreBienvenue", tiersMapHelper.getTypesLettreBienvenue());
		model.addAttribute("print", view);
		model.addAttribute("isRadieeRCOuDissoute", autreDocumentFiscalManager.hasAnyEtat(idEntreprise, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
		model.addAttribute("docsAvecSuivi", new AutreDocumentFiscalListView(idEntreprise, autreDocumentFiscalManager.getAutresDocumentsFiscauxAvecSuivi(idEntreprise)));

		return "tiers/edition/pm/edit-autresdocs";
	}

	/**
	 * Impression (et ajout sur l'entreprise) d'un nouveau document
	 */
	@RequestMapping(value = "/print.do", method = RequestMethod.POST)
	public String imprimerNouveauDocument(@Valid @ModelAttribute("print") final ImprimerAutreDocumentFiscalView view, BindingResult bindingResult, Model model) throws IOException {

		if (bindingResult.hasErrors()) {
			return showEditList(model, view);
		}

		// gestion des droits
		final Set<TypeAutreDocumentFiscalEmettableManuellement> allowed = getTypesAutreDocumentFiscalEmettablesManuellement();
		if (!allowed.contains(view.getTypeDocument())) {
			throw new AccessDeniedException("Vous ne possédez pas les droits permettant d'émettre ce type de document.");
		}

		// impression
		final EditiqueResultat resultat;
		try {
			resultat = autreDocumentFiscalManager.createAndPrint(view);
		}
		catch (AutreDocumentFiscalException e) {
			throw new ActionException("Impossible d'imprimer le document voulu", e);
		}

		// gestion du résultat de l'impression
		final String redirect = String.format("redirect:/autresdocs/edit-list.do?pmId=%d", view.getNoEntreprise());
		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        view.getTypeDocument().name().toLowerCase(),
		                                                                        redirect,
		                                                                        false,
		                                                                        r -> redirect,
		                                                                        null,
		                                                                        r -> {
			                                                                        Flash.error(EditiqueErrorHelper.getMessageErreurEditique(r));
			                                                                        return redirect;
		                                                                        });
	}

	/**
	 * Affichage de la fenêtre de détail pour un autre document fiscal
	 */
	@RequestMapping(value = "/details.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@ResponseBody
	public AutreDocumentFiscalView detailsAutreDocFiscal(@RequestParam("id") long id) throws AccessDeniedException {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits de consultation pour l'application Unireg");
		}

		final AutreDocumentFiscal autreDocumentFiscal = getDocumentFiscal(id);

		// Vérification des droits en lecture
		final Long tiersId = autreDocumentFiscal.getTiers().getId();
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		return AutreDocumentFiscalViewFactory.buildView(autreDocumentFiscal, infraService, messageHelper);
	}

	/**
	 * Affiche un écran qui permet d'éditer d'un document fiscal avec suivi.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/editer.do", method = RequestMethod.GET)
	public String editer(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		/* Pour l'instant, la lettre de bienvenue est le seul document fiscal avec suivi qu'on est susceptible d'éditer. */
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'édition des documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final AutreDocumentFiscalView view;

		if (doc instanceof AutreDocumentFiscalAvecSuivi) {
			view = AutreDocumentFiscalViewFactory.buildView(doc, infraService, messageHelper);
		}
		else {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas un document fiscal avec suivi.");
		}

		// [SIFISC-29014] Une fois le rappel envoyé pour une lettre de bienvenue ou un formulaire de dégrèvement, il ne doit plus être possible d'ajouter un délai

		model.addAttribute("isAjoutDelaiAutorise", isAjoutDelaiAutorise(doc));

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
	public String ajouterDelai(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un délai à un autre document fiscal.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// [SIFISC-29014] Une fois le rappel envoyé pour une lettre de bienvenue ou un formulaire de dégrèvement, il ne doit plus être possible d'ajouter un délai.
		if(!isAjoutDelaiAutorise(doc)) {
			Flash.warning("Impossible d'ajouter un délai : document déjà retourné, ou rappel déjà été envoyé.");
			return "redirect:/autresdocs/editer.do?id=" + id;
		}

		final RegDate delaiAccordeAu = determineDateAccordDelaiParDefaut(doc.getDelaiAccordeAu());
		model.addAttribute("ajouterDelai", new AjouterDelaiDocumentFiscalView(doc, delaiAccordeAu));
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
	public String ajouterDelai(@Valid @ModelAttribute("ajouterDelai") final AjouterDelaiDocumentFiscalView view, BindingResult result) {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas les droits de gestion des delais d'un autre document fiscal");
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

		// [SIFISC-29014] Une fois le rappel envoyé pour une lettre de bienvenue ou un formulaire de dégrèvement, il ne doit plus être possible d'ajouter un délai.
		if(!isAjoutDelaiAutorise(doc)) {
			Flash.warning("Impossible d'ajouter un délai : document déjà retourné, ou rappel déjà été envoyé.");
			return "redirect:/autresdocs/editer.do?id=" + id;
		}

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
	public String annulerDelai(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous n'avez pas les droits de gestion des delais d'un autre document fiscal");
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
	public String ajouterQuittance(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de quittancement des autres documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);
		if (!(doc instanceof LettreBienvenue)) {
			throw new IllegalArgumentException("A ce jour, seule une lettre de bienvenue peut être quittancée via l'IHM des autres documents fiscaux.");
		}

		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		AjouterQuittanceDocumentFiscalView view = new AjouterQuittanceDocumentFiscalView(doc, infraService, messageHelper);
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
	public String ajouterQuittance(@Valid @ModelAttribute("ajouterQuittance") final AjouterQuittanceDocumentFiscalView view, BindingResult result) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de quittancement des autres documents fiscaux.");
		}

		if (result.hasErrors()) {
			final AutreDocumentFiscal doc = getDocumentFiscal(view.getId());
			view.resetDocumentInfo(doc, infraService, messageHelper);
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
				Flash.warning(String.format("La lettre de bienvenue est déjà retournée en date du %s, nouvelle quittance non prise en compte.",
				                            RegDateHelper.dateToDisplayString(doc.getDateRetour())));
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
	public String annulerQuittancement(@RequestParam("id") final long id) {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de quittancement des autres documents fiscaux.");
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
	public String annuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de consultation pour l'application Unireg");
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
	 * Désannuler une demande de dégrèvement ICI.
	 *
	 * @param id l'id du document fiscal à désannuler
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/desannuler.do", method = RequestMethod.POST)
	public String desannuler(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("vous ne possédez pas les droits de désannulation des autres documents fiscaux.");
		}

		final AutreDocumentFiscal doc = getDocumentFiscal(id);

		if (!(doc instanceof LettreBienvenue)) {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas un document annulable.");
		}

		if (!doc.isAnnule()) {
			throw new IllegalArgumentException("Le document fiscal n°" + id + " n'est pas annulée.");
		}

		// vérification des droits en écriture
		final Entreprise ctb = (Entreprise) doc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// désannulation de l'autre document fiscal
		autreDocumentFiscalManager.desannulerAutreDocumentFiscal(doc);

		return "redirect:/autresdocs/edit-list.do?pmId=" + ctb.getId();
	}

	/**
	 * Imprime un duplicata de document fiscal
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/duplicata.do", method = RequestMethod.POST)
	public String duplicataLettreBienvenue(@RequestParam("id") long id,
	                                       HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_QUIT_LETTRE_BIENVENUE)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits pour imprimer des duplicata de lettre de bienvenue.");
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

	/**
	 * @return true si il est possible d'ajouter un délai sur le document
	 */
	public static boolean isAjoutDelaiAutorise(@NotNull DocumentFiscal doc) {
		if (!doc.isAvecSuivi()) {
			// pas de suivi, pas de délai
			return false;
		}
		final boolean documentRetourne = (doc.getDateRetour() != null);
		// [SIFISC-29014] Une fois le rappel envoyé pour une lettre de bienvenue ou un formulaire de dégrèvement, il ne doit plus être possible d'ajouter un délai
		final boolean rappelEnvoye = (doc.getDernierEtatOfType(TypeEtatDocumentFiscal.RAPPELE) != null);
		// [FISCPROJ-911] le bouton "Ajouter" du tableau délai d'un SNC "Echue" ne doit pas être affiché
		final boolean qsncEstEchu = (doc.getDernierEtatOfType(TypeEtatDocumentFiscal.ECHU) != null);
		return !documentRetourne && !rappelEnvoye && !qsncEstEchu;
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
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
