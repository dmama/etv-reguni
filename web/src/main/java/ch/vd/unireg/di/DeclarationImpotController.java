package ch.vd.unireg.di;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.common.TicketTimeoutException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationGenerationOperation;
import ch.vd.unireg.declaration.DeclarationImpotCriteria;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.EtatDeclarationSuspendue;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.ParametrePeriodeFiscalePM;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.view.DeclarationView;
import ch.vd.unireg.di.manager.DeclarationImpotEditManager;
import ch.vd.unireg.di.view.AbstractEditionDelaiDeclarationView;
import ch.vd.unireg.di.view.AjouterDelaiDeclarationPMView;
import ch.vd.unireg.di.view.AjouterDelaiDeclarationPPView;
import ch.vd.unireg.di.view.AjouterEtatDeclarationView;
import ch.vd.unireg.di.view.ChoixDeclarationImpotView;
import ch.vd.unireg.di.view.DeclarationImpotListView;
import ch.vd.unireg.di.view.DeclarationImpotView;
import ch.vd.unireg.di.view.EditerDeclarationImpotView;
import ch.vd.unireg.di.view.ImprimerDuplicataDeclarationImpotView;
import ch.vd.unireg.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.unireg.di.view.ModifierEtatDelaiDeclarationPMView;
import ch.vd.unireg.di.view.TypeDeclaration;
import ch.vd.unireg.documentfiscal.TypeImpression;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;
import ch.vd.unireg.evenement.di.EvenementLiberationDeclarationImpotSender;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.GroupeTypesDocumentBatchLocal;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.utils.RegDateEditor;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Controller Spring 3 pour la gestion des déclarations d'impôt ordinaires (à compléter lors des divers refactoring)
 */
@Controller
public class DeclarationImpotController {

	private HibernateTemplate hibernateTemplate;
	private DeclarationImpotService diService;
	private DeclarationImpotOrdinaireDAO diDAO;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private ParametreAppService parametreAppService;
	private MessageSource messageSource;
	private MessageHelper messageHelper;
	private DeclarationImpotEditManager manager;
	private DelaisService delaisService;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private Validator validator;
	private ModeleDocumentDAO modeleDocumentDAO;
	private PeriodeImpositionService periodeImpositionService;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;
	private TicketService ticketService;
	private Set<String> sourcesQuittancementAvecLiberationPossible = Collections.emptySet();
	private EvenementLiberationDeclarationImpotSender liberationSender;
	private ServiceInfrastructureService infraService;

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

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
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

	public void setSourcesQuittancementAvecLiberationPossible(Set<String> sourcesQuittancementAvecLiberationPossible) {
		this.sourcesQuittancementAvecLiberationPossible = sourcesQuittancementAvecLiberationPossible;
	}

	public void setLiberationSender(EvenementLiberationDeclarationImpotSender liberationSender) {
		this.liberationSender = liberationSender;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
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
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	private void checkAccessRights(DeclarationImpotOrdinaire di, boolean emission, boolean quittancement, boolean delais, boolean sommation, boolean duplicata,
	                               boolean desannulation, boolean suspension, boolean desuspension, boolean liberation) {
		final boolean emissionOk;
		final boolean quittancementOk;
		final boolean delaisOk;
		final boolean sommationOk;
		final boolean duplicataOk;
		final boolean desannulationOk;
		final boolean suspensionOk;
		final boolean desuspensionOk;
		final boolean liberationOk;
		final String qualificatifPersonnes;
		if (di instanceof DeclarationImpotOrdinairePP) {
			emissionOk = emission && SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PP);
			quittancementOk = quittancement && SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP);
			delaisOk = delais && SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP);
			sommationOk = sommation && SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP);
			duplicataOk = duplicata && SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP);
			desannulationOk = desannulation && SecurityHelper.isGranted(securityProvider, Role.DI_DESANNUL_PP);
			suspensionOk = false;
			desuspensionOk = false;
			liberationOk = liberation && SecurityHelper.isGranted(securityProvider, Role.DI_LIBERER_PP);
			qualificatifPersonnes = "physiques";
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			emissionOk = emission && SecurityHelper.isGranted(securityProvider, Role.DI_EMIS_PM);
			quittancementOk = quittancement && SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM);
			delaisOk = delais && SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM);
			sommationOk = sommation && SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PM);
			duplicataOk = duplicata && SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PM);
			desannulationOk = desannulation && SecurityHelper.isGranted(securityProvider, Role.DI_DESANNUL_PM);
			suspensionOk = suspension && SecurityHelper.isGranted(securityProvider, Role.DI_SUSPENDRE_PM);
			desuspensionOk = desuspension && SecurityHelper.isGranted(securityProvider, Role.DI_DESUSPENDRE_PM);
			liberationOk = liberation && SecurityHelper.isGranted(securityProvider, Role.DI_LIBERER_PM);
			qualificatifPersonnes = "morales";
		}
		else {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		if (!emissionOk && !quittancementOk && !delaisOk && !sommationOk && !duplicataOk && !desannulationOk && !suspensionOk && !desuspensionOk && !liberationOk) {
			// on n'a pas le(s) droit(s) demandé(s)
			// pour plus de précision dans le message, si un seul droit était exigé, on va dire lequel manque (sinon, message générique)
			final int nbDroitsAutorises = (emission ? 1 : 0) + (quittancement ? 1 : 0) + (delais ? 1 : 0) + (sommation ? 1 : 0) + (duplicata ? 1 : 0) + (desannulation ? 1 : 0) + (suspension ? 1 : 0) + (desuspension ? 1 : 0) + (liberation ? 1 : 0);
			final String msg;
			if (nbDroitsAutorises == 1) {
				final String droitSpecifique;
				if (emission) {
					droitSpecifique = "d'émission";
				}
				else if (quittancement) {
					droitSpecifique = "de quittancement";
				}
				else if (delais) {
					droitSpecifique = "de gestion des délais";
				}
				else if (sommation) {
					droitSpecifique = "de sommation";
				}
				else if (duplicata) {
					droitSpecifique = "d'émission de duplicata";
				}
				else if (desannulation) {
					droitSpecifique = "de réactivation";
				}
				else if (suspension) {
					droitSpecifique = "de suspension";
				}
				else if (desuspension) {
					droitSpecifique = "de levée de suspension";
				}
				else if (liberation) {
					droitSpecifique = "de libération";
				}
				else {
					throw new IllegalArgumentException("Cas non supporté...");
				}
				msg = String.format("Vous ne possédez pas le droit IfoSec %s des déclarations d'impôt des personnes %s.", droitSpecifique, qualificatifPersonnes);
			}
			else {
				msg = String.format("Vous ne possédez pas les droits IfoSec nécessaires sur les déclarations d'impôt des personnes %s.", qualificatifPersonnes);
			}
			throw new AccessDeniedException(msg);
		}
	}

	/**
	 * Liste les déclarations d'impôt d'un contribuable
	 *
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

		model.addAttribute("command", new DeclarationImpotListView(ctb, infraService, messageHelper));
		return "/di/lister";
	}

	/**
	 * @param id l'id de la déclaration d'impôt ordinaire
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/details.do", method = RequestMethod.GET)
	@ResponseBody
	public DeclarationImpotView detailsDI(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final DeclarationImpotOrdinaire decl = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		if (decl == null) {
			return null;
		}

		// vérification des droits en lecture
		final Long tiersId = decl.getTiers().getId();
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		return new DeclarationImpotView(decl, infraService, messageHelper);
	}

	/**
	 * @param id l'id de la déclaration d'impôt ordinaire
	 * @return les détails d'une déclaration d'impôt au format JSON
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/lr/details.do", method = RequestMethod.GET)
	@ResponseBody
	public DeclarationView detailsLR(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.VISU_ALL)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
		}

		final DeclarationImpotSource decl = hibernateTemplate.get(DeclarationImpotSource.class, id);
		if (decl == null) {
			return null;
		}

		// vérification des droits en lecture
		final Long tiersId = decl.getTiers().getId();
		controllerUtils.checkAccesDossierEnLecture(tiersId);

		return new DeclarationView(decl, infraService, messageHelper);
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
	 *
	 * @param diId  l'id de la déclaration d'impôt ordinaire à désannuler
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
			delaiAccorde = delaisService.getDateFinDelaiRetourDeclarationImpotPPEmiseManuellement(RegDate.get());
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
			final PeriodeFiscale pf = periodeFiscaleDAO.getPeriodeFiscaleByYear(periode.getPeriodeFiscale());
			final ParametrePeriodeFiscalePM params = pf.getParametrePeriodeFiscalePM(periode.getTypeContribuable());

			final RegDate dateReferencePourDelai = params.getReferenceDelaiInitial() == ParametrePeriodeFiscalePM.ReferencePourDelai.EMISSION ? RegDate.get() : periode.getDateFin();
			final RegDate delaiTheorique = dateReferencePourDelai.addMonths(params.getDelaiImprimeMois());
			final RegDate delaiMinimal = RegDate.get().addMonths(parametreAppService.getDelaiMinimalRetourDeclarationImpotPM());
			delaiAccorde = RegDateHelper.maximum(delaiTheorique, delaiMinimal, NullDateBehavior.EARLIEST);
		}

		if (typeDocument == null) {
			final TypeDocument typeDocumentDeclaration = periode.getTypeDocumentDeclaration();
			if (typeDocumentDeclaration != null) {
				switch (typeDocumentDeclaration) {
				case DECLARATION_IMPOT_APM_BATCH:
				case DECLARATION_IMPOT_APM_LOCAL:
					typeDocument = TypeDocument.DECLARATION_IMPOT_APM_LOCAL;
					break;
				case DECLARATION_IMPOT_PM_BATCH:
				case DECLARATION_IMPOT_PM_LOCAL:
					typeDocument = TypeDocument.DECLARATION_IMPOT_PM_LOCAL;
					break;
				default:
					throw new ActionException("Type de document invalide : " + typeDocumentDeclaration);
				}
			}
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
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, Duration.ofMillis(500));
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
				ticket.release();
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
					final EtatDeclaration etat = di.getDernierEtatDeclaration();
					if (etat != null && etat.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
						diAnnulee = di;
						break;
					}
				}
			}
		}

		return diAnnulee;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/ajouter-suspension.do", method = RequestMethod.POST)
	public String suspendre(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_SUSPENDRE_PM)) {
			throw new AccessDeniedException("Lous ne possédez pas le droit IfoSec de suspension des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		if (!EditerDeclarationImpotView.isSuspendable(di)) {
			throw new ValidationException(di, "La déclaration d'impôt n'est pas dans un état 'suspendable'.");
		}

		di.addEtat(new EtatDeclarationSuspendue(RegDate.get()));

		return "redirect:/di/editer.do?id=" + di.getId();
	}

	/**
	 * Affiche un écran qui permet de quittancer une déclaration.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/etat/ajouter-quittance.do", method = RequestMethod.GET)
	public String ajouterEtat(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, false, false, false, false, false, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final AjouterEtatDeclarationView view;
		if (di instanceof DeclarationImpotOrdinairePP) {
			view = new AjouterEtatDeclarationView((DeclarationImpotOrdinairePP) di, infraService, messageHelper);
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			view = new AjouterEtatDeclarationView((DeclarationImpotOrdinairePM) di, infraService, messageHelper);
		}
		else {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		model.addAttribute("command", view);
		model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());

		return "di/etat/ajouter-quittance";
	}

	/**
	 * Quittance une déclaration d'impôt manuellement
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/ajouter-quittance.do", method = RequestMethod.POST)
	public String ajouterEtat(@Valid @ModelAttribute("command") final AjouterEtatDeclarationView view, BindingResult result, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de quittancement des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(view.getId());
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, false, false, false, false, false, false, false);

		if (result.hasErrors()) {
			view.initReadOnlyValues(di, view.isTypeDocumentEditable(), infraService, messageHelper);
			model.addAttribute("typesDeclarationImpotOrdinaire", tiersMapHelper.getTypesDeclarationsImpotOrdinaires());
			return "di/etat/ajouter-quittance";
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
	@RequestMapping(value = "/di/etat/annuler-quittance.do", method = RequestMethod.POST)
	public String annulerQuittancement(@RequestParam("id") final long id) throws Exception {

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
		checkAccessRights(di, false, true, false, false, false, false, false, false, false);
		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On annule le quittancement
		final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
		retour.setAnnule(true);

		Flash.message("Le quittancement du " + RegDateHelper.dateToDisplayString(retour.getDateObtention()) + " a été annulé.");
		return "redirect:/di/editer.do?id=" + di.getId();
	}

	/**
	 * Annulation d'une suspension
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/etat/annuler-suspension.do", method = RequestMethod.POST)
	public String annulerSuspension(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DESUSPENDRE_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'annulation de suspension des déclarations d'impôt.");
		}

		// Vérifie les paramètres
		final EtatDeclaration etat = hibernateTemplate.get(EtatDeclaration.class, id);
		if (etat == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.etat.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(etat instanceof EtatDeclarationSuspendue)) {
			throw new IllegalArgumentException("Seules les suspensions peuvent être annulées.");
		}

		etat.setAnnule(true);

		Flash.message("La suspension du " + RegDateHelper.dateToDisplayString(etat.getDateObtention()) + " a été annulée.");
		return "redirect:/di/editer.do?id=" + etat.getDeclaration().getId();
	}

	/**
	 * Affiche un écran qui permet d'éditer une déclaration.
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/editer.do", method = RequestMethod.GET)
	public String editer(@RequestParam("id") long id,
	                     @RequestParam(value = "tacheId", required = false) Long tacheId,
	                     Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_QUIT_PP, Role.DI_QUIT_PM, Role.DI_DELAI_PP, Role.DI_DELAI_PM, Role.DI_SOM_PP, Role.DI_SOM_PM, Role.DI_DUPLIC_PP, Role.DI_DUPLIC_PM, Role.DI_SUSPENDRE_PM, Role.DI_DESUSPENDRE_PM,
		                                 Role.DI_LIBERER_PM, Role.DI_LIBERER_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'édition des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, true, true, true, true, false, true, true, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final EditerDeclarationImpotView view;
		if (di instanceof DeclarationImpotOrdinairePP) {
			view = new EditerDeclarationImpotView(di, tacheId, infraService, messageHelper,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PP),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP) && isJusteEmise(di),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PP),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PP),
			                                      false,
			                                      false,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_LIBERER_PP) && isLiberable(di));
		}
		else if (di instanceof DeclarationImpotOrdinairePM) {
			view = new EditerDeclarationImpotView(di, tacheId, infraService, messageHelper,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_QUIT_PM),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM) && (isJusteEmise(di) || isSommee(di)),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_SOM_PM),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DUPLIC_PM) && di.getTypeDeclaration() != null,
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_SUSPENDRE_PM) && EditerDeclarationImpotView.isSuspendable(di),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_DESUSPENDRE_PM) && isSuspendue(di),
			                                      SecurityHelper.isGranted(securityProvider, Role.DI_LIBERER_PM) && isLiberable(di));
		}
		else {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas une déclaration d'impôt ordinaire PP ou PM.");
		}

		model.addAttribute("command", view);
		return "di/editer";
	}

	private static boolean isJusteEmise(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etat = di.getDernierEtatDeclaration();
		return etat == null || etat.getEtat() == TypeEtatDocumentFiscal.EMIS;
	}

	private static boolean isSommee(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etat = di.getDernierEtatDeclaration();
		return etat != null && etat.getEtat() == TypeEtatDocumentFiscal.SOMME;
	}

	private static boolean isSuspendue(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etat = di.getDernierEtatDeclaration();
		return etat != null && etat.getEtat() == TypeEtatDocumentFiscal.SUSPENDU;
	}

	private boolean isLiberable(DeclarationImpotOrdinaire di) {
		final List<EtatDeclaration> etatsRetournes = di.getEtatsDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE, false);
		final Set<String> sources = new HashSet<>(etatsRetournes.size());
		for (EtatDeclaration etat : etatsRetournes) {
			sources.add(((EtatDeclarationRetournee) etat).getSource());
		}
		sources.retainAll(sourcesQuittancementAvecLiberationPossible);
		return !sources.isEmpty();
	}

	/**
	 * Sommer la déclaration d'impôt spécifiée.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/sommer.do", method = RequestMethod.POST)
	public String sommerDeclarationImpotPersonnesPhysiques(@RequestParam("id") final long id, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_SOM_PP, Role.DI_SOM_PM)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de sommation des déclarations d'impôt.");
		}

		// Vérifie les paramètres
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, false, true, false, false, false, false, false);

		if (!EditerDeclarationImpotView.isSommable(di)) {
			throw new IllegalArgumentException("La déclaration n°" + id + " n'est pas dans un état sommable.");
		}

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On imprime la sommation
		final EditiqueResultat resultat = manager.envoieImpressionLocalSommationDI(id);
		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "sommationDi", inbox, null, erreur);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/duplicata-pm.do", method = RequestMethod.GET)
	public String choixDuplicataDeclarationPersonnesMorales(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes morales.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, false, false, true, false, false, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final GroupeTypesDocumentBatchLocal groupe = GroupeTypesDocumentBatchLocal.of(di.getTypeDeclaration());
		final TypeDeclaration typeDeclaration = TypeDeclaration.of(groupe);
		model.addAttribute("command", new ImprimerDuplicataDeclarationImpotView(di, typeDeclaration, modeleDocumentDAO));
		return "di/duplicata-pm";
	}

	/**
	 * Imprime un duplicata de DI PM
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/duplicata-pm.do", method = RequestMethod.POST)
	public String duplicataDeclarationPersonnesMorales(@Valid @ModelAttribute("command") final ImprimerDuplicataDeclarationImpotView view,
	                                                   BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes morales.");
		}

		if (result.hasErrors()) {
			return "di/duplicata-pm";
		}

		final Long id = view.getIdDI();
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		controllerUtils.checkAccesDossierEnEcriture(di.getTiers().getId());

		final TypeDocument typeDocument = di.getTypeDeclaration();
		final EditiqueResultat resultat = manager.envoieImpressionLocalDuplicataDI(id, typeDocument, view.getSelectedAnnexes(), false);
		final RedirectEditDI inbox = new RedirectEditDI(id);
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "di", inbox, null, erreur);
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'impression d'un duplicata de DI
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/duplicata-pp.do", method = RequestMethod.GET)
	public String choixDuplicataDeclarationPersonnesPhysiques(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes physiques.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, false, false, true, false, false, false, false);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		model.addAttribute("command", new ImprimerDuplicataDeclarationImpotView(di, TypeDeclaration.DI_PP, modeleDocumentDAO));
		model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpotPP());       // seules les DI PP en ont besoin, les autres sont en duplicata "direct"
		return "di/duplicata-pp";
	}

	/**
	 * Imprime un duplicata de DI (avec annexes et potentiellement nouveau type de document).
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/duplicata-pp.do", method = RequestMethod.POST)
	public String duplicataDeclarationPersonnesPhysiques(@Valid @ModelAttribute("command") final ImprimerDuplicataDeclarationImpotView view,
	                                                     BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DUPLIC_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec pour imprimer des duplicata de déclarations d'impôt des personnes physiques.");
		}

		if (result.hasErrors()) {
			return "di/duplicata-pp";
		}

		// Vérifie les paramètres
		final Long id = view.getIdDI();
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePP)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// Vérification de la période d'imposition du contribuable
		if (periodeImpositionService.determine(ctb, di) == null) {
			Flash.error("Echec de l'impression du duplicata, le contribuable n'a pas de données valides à la fin de la période de la déclaration d'impôt.");
			return "redirect:/di/editer.do?id=" + di.getId();
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
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'un délai sur une DI PP
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/delai/ajouter-pp.do", method = RequestMethod.GET)
	public String ajouterDelaiDiPP(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePP)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = delaisService.getDateFinDelaiRetourDeclarationImpotPPEmiseManuellement(RegDate.get());
		model.addAttribute("command", new AjouterDelaiDeclarationPPView(di, delaiAccordeAu));
		return "di/delai/ajouter-pp";
	}

	/**
	 * Ajoute un délai sur une DI PP
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/delai/ajouter-pp.do", method = RequestMethod.POST)
	public String ajouterDelaiDiPP(@Valid @ModelAttribute("command") final AjouterDelaiDeclarationPPView view,
	                               BindingResult result, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final Long id = view.getIdDeclaration();

		if (result.hasErrors()) {
			fillDiInfo(id, view);
			return "di/delai/ajouter-pp";
		}

		// Vérifie les paramètres
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePP)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		controllerUtils.checkAccesDossierEnEcriture(di.getTiers().getId());

		// On ajoute le délai
		final Long idDelai = manager.saveNouveauDelai(id, view.getDateDemande(), view.getDelaiAccordeAu(), EtatDelaiDocumentFiscal.ACCORDE, false);
		if (view.isConfirmationEcrite()) {

			// On imprime le document
			final EditiqueResultat resultat = manager.envoieImpressionLocalConfirmationDelaiPP(id, idDelai);

			final RedirectEditDI inbox = new RedirectEditDI(id);
			final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(id, messageSource);
			return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "delai", inbox, null, erreur);
		}
		else {
			// Pas de duplicata -> on retourne à l'édition de la DI
			return "redirect:/di/editer.do?id=" + id;
		}
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/delai/print-confirmation.do", method = RequestMethod.POST)
	public String imprimerConfirmation(@RequestParam("idDelai") long idDelai) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_DELAI_PP, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("Vous n'avez pas le droit d'apporter des modification sur les delais des DI");
		}

		// vérification des paramètres
		final Mutable<Long> idDeclaration = new MutableObject<>();
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
		if (delai == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (delai.isAnnule()) {
			throw new ActionException("Opération impossible sur un délai annulé.");
		}
		if (delai.getCleArchivageCourrier() != null || delai.getCleDocument() != null) {
			throw new ActionException("Le document a déjà été généré pour ce délai.");
		}

		// impression document...
		final EditiqueResultat resultat;
		final Declaration declaration = delai.getDeclaration();
		idDeclaration.setValue(declaration.getId());
		if (declaration instanceof DeclarationImpotOrdinairePP) {
			if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
				throw new AccessDeniedException("Vous n'avez pas le droit d'apporter des modification sur les delais des DI PP");
			}
			resultat = manager.envoieImpressionLocalConfirmationDelaiPP(declaration.getId(), idDelai);
		}
		else if (declaration instanceof DeclarationImpotOrdinairePM) {
			if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
				throw new AccessDeniedException("Vous n'avez pas le droit d'apporter des modification sur les delais des DI PM");
			}
			resultat = manager.envoieImpressionLocaleLettreDecisionDelaiPM(idDelai);
		}
		else {
			throw new ActionException("Type de déclaration non-supporté.");
		}

		// On imprime le document
		final RedirectEditDI inbox = new RedirectEditDI(idDeclaration.getValue());
		final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(idDeclaration.getValue(), messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        "delai",
		                                                                        String.format("redirect:/di/editer.do?id=%d", idDeclaration.getValue()),
		                                                                        false,
		                                                                        inbox,
		                                                                        null,
		                                                                        erreur);
	}

	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'une demande de délai sur une DI PM
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/delai/ajouter-pm.do", method = RequestMethod.GET)
	public String ajouterDelaiDiPM(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un delai à une DI");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final boolean sursis = di.getDernierEtatDeclaration() != null && di.getDernierEtatDeclaration().getEtat() == TypeEtatDocumentFiscal.SOMME;
		final RegDate delaiAccordeAu = determineDateAccordDelaiPMParDefaut(di.getDelaiAccordeAu());
		model.addAttribute("command", new AjouterDelaiDeclarationPMView(di, delaiAccordeAu, sursis));
		model.addAttribute("decisionsDelai", tiersMapHelper.getTypesEtatsDelaiDeclaration());
		return "di/delai/ajouter-pm";
	}

	/**
	 * [SIFISC-18869] la date par défaut du délai accordé (sursis ou pas) ne doit de toute façon pas être dans le passé
	 *
	 * @param delaiPrecedent la date actuelle du délai accordé
	 * @return la nouvelle date à proposer comme délai par défaut
	 */
	private RegDate determineDateAccordDelaiPMParDefaut(RegDate delaiPrecedent) {
		final RegDate delaiNormal = delaisService.getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(delaiPrecedent);
		return RegDateHelper.maximum(delaiNormal, RegDate.get(), NullDateBehavior.EARLIEST);
	}

	/**
	 * Ajoute un délai sur une DI PM
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/delai/ajouter-pm.do", method = RequestMethod.POST)
	public String ajouterDelaiDiPM(@Valid @ModelAttribute("command") final AjouterDelaiDeclarationPMView view,
	                               BindingResult result, Model model, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'une DI");
		}

		final Long id = view.getIdDeclaration();

		if (result.hasErrors()) {
			fillDiInfo(id, view);
			model.addAttribute("decisionsDelai", tiersMapHelper.getTypesEtatsDelaiDeclaration());
			return "di/delai/ajouter-pm";
		}

		// Vérifie les paramètres
		final DeclarationImpotOrdinaire di = diDAO.get(id);
		if (!(di instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		controllerUtils.checkAccesDossierEnEcriture(di.getTiers().getId());

		// On ajoute le délai
		final RegDate delaiAccordeAu = view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE ? view.getDelaiAccordeAu() : null;
		final Long idDelai = manager.saveNouveauDelai(id, view.getDateDemande(), delaiAccordeAu, view.getDecision(), view.isSursis());
		return gererImpressionCourrierDelaiDeclarationPM(idDelai, id, view.getTypeImpression(), response);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/di/delai/editer-pm.do", method = RequestMethod.GET)
	public String editerEtatDelaiPM(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des demandes de délai sur les DI");
		}

		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, id);
		if (delai == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (delai.getEtat() != EtatDelaiDocumentFiscal.DEMANDE) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.finalise", null, WebContextUtils.getDefaultLocale()));
		}

		final Declaration declaration = delai.getDeclaration();
		if (!(declaration instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = delaisService.getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(di.getDelaiAccordeAu());
		model.addAttribute("command", new ModifierEtatDelaiDeclarationPMView(delai, delaiAccordeAu));
		model.addAttribute("decisionsDelai", tiersMapHelper.getTypesEtatsDelaiDeclaration());
		return "di/delai/editer-pm";
	}

	/**
	 * Ajoute un délai sur une DI PM
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/delai/editer-pm.do", method = RequestMethod.POST)
	public String editerEtatDelaiPM(@Valid @ModelAttribute("command") final ModifierEtatDelaiDeclarationPMView view,
	                                   BindingResult result, Model model, HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
			throw new AccessDeniedException("vous n'avez pas le droit de gestion des delais d'une DI");
		}

		if (result.hasErrors()) {
			fillDiInfo(view.getIdDeclaration(), view);
			model.addAttribute("decisionsDelai", tiersMapHelper.getTypesEtatsDelaiDeclaration());
			return "di/delai/editer-pm";
		}

		// Vérifie les paramètres
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, view.getIdDelai());
		if (delai == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (delai.getEtat() != EtatDelaiDocumentFiscal.DEMANDE) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.finalise", null, WebContextUtils.getDefaultLocale()));
		}

		final Declaration declaration = delai.getDeclaration();
		if (!(declaration instanceof DeclarationImpotOrdinairePM)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}

		final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) declaration;
		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// On modifie le délai
		final RegDate delaiAccordeAu = view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE ? view.getDelaiAccordeAu() : null;
		manager.saveDelai(view.getIdDelai(), view.getDecision(), delaiAccordeAu);
		return gererImpressionCourrierDelaiDeclarationPM(view.getIdDelai(), di.getId(), view.getTypeImpression(), response);
	}

	private String gererImpressionCourrierDelaiDeclarationPM(long idDelai, long idDeclaration,
	                                                         TypeImpression typeImpression,
	                                                         HttpServletResponse response) throws EditiqueException, IOException {
		if (typeImpression != null) {
			if (typeImpression == TypeImpression.BATCH) {
				manager.envoieImpressionBatchLettreDecisionDelaiPM(idDelai);
				Flash.message("L'envoi automatique du document de décision a été programmé.");
			}
			else if (typeImpression == TypeImpression.LOCAL) {
				final EditiqueResultat resultat = manager.envoieImpressionLocaleLettreDecisionDelaiPM(idDelai);
				final RedirectEditDI inbox = new RedirectEditDI(idDeclaration);
				final RedirectEditDIApresErreur erreur = new RedirectEditDIApresErreur(idDeclaration, messageSource);
				return retourEditiqueControllerHelper.traiteRetourEditique(resultat, response, "delai", inbox, null, erreur);
			}
			else {
				throw new IllegalArgumentException("Valeur non-supportée pour le type d'impression : " + typeImpression);
			}
		}

		// Pas de document directement en retour -> on retourne à l'édition de la DI
		return "redirect:/di/editer.do?id=" + idDeclaration;
	}

	private void fillDiInfo(final long diId, final AbstractEditionDelaiDeclarationView view) {
		final DeclarationImpotOrdinaire di = diDAO.get(diId);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		view.setDiInfo(di);
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

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/di/liberer.do", method = RequestMethod.POST)
	public String libererDeclaration(@RequestParam("id") long idDeclaration) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_LIBERER_PP, Role.DI_LIBERER_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec de libération des déclarations d'impôt.");
		}

		final DeclarationImpotOrdinaire di = diDAO.get(idDeclaration);
		if (di == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.di.inexistante", null, WebContextUtils.getDefaultLocale()));
		}
		checkAccessRights(di, false, false, false, false, false, false, false, false, true);

		final Contribuable ctb = di.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// vérification de la libérabilité de la déclaration
		if (!isLiberable(di)) {
			final String message = messageSource.getMessage("error.di.non.liberable", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "redirect:/di/editer.do?id=" + idDeclaration;
		}

		// envoi de la demande de libération
		try {
			liberationSender.demandeLiberationDeclarationImpot(di.getTiers().getNumero(),
			                                                   di.getPeriode().getAnnee(),
			                                                   di.getNumero(),
			                                                   di instanceof DeclarationImpotOrdinairePP ? EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PP :
					                                                   EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PM);
			Flash.message("La demande de libération de la déclaration a été envoyée au service concerné.");
			return "redirect:/di/editer.do?id=" + idDeclaration;
		}
		catch (EvenementDeclarationException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}
}
