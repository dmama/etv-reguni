package ch.vd.unireg.qsnc;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.EditiqueErrorHelper;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.common.TicketTimeoutException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationGenerationOperation;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.QuestionnaireSNCDAO;
import ch.vd.unireg.declaration.snc.QuestionnaireSNCService;
import ch.vd.unireg.declaration.view.QuestionnaireSNCView;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalController;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
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
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.transaction.TransactionHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;
import ch.vd.unireg.utils.RegDateEditor;

@Controller
@RequestMapping("/qsnc")
public class QuestionnaireSNCController {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionnaireSNCController.class);
	/**
	 * Nom du model d'ajout de délai
	 */
	private static final String AJOUTER_DELAI = "ajouterDelai";
	/**
	 * Nom du model de modification de délai
	 */
	private static final String MODIFIER_DELAI = "modifDelai";
	private static final String DECISIONS_DELAI = "decisionsDelai";

	private HibernateTemplate hibernateTemplate;
	private QuestionnaireSNCService qsncService;
	private SecurityProviderInterface securityProvider;
	private AutorisationManager autorisationManager;
	private DelaisService delaisService;
	private TransactionHelper transactionHelper;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private TacheDAO tacheDAO;
	private TicketService ticketService;
	private ServiceInfrastructureService infraService;
	private QuestionnaireSNCDAO questionnaireSNCDAO;
	private ControllerUtils controllerUtils;
	private Set<String> sourcesQuittancementAvecLiberationPossible = Collections.emptySet();
	private EvenementLiberationDeclarationImpotSender liberationSender;
	private Validator ajouterDelaiValidator;
	private MessageHelper messageHelper;


	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setQsncService(QuestionnaireSNCService qsncService) {
		this.qsncService = qsncService;
	}


	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAutorisationManager(AutorisationManager autorisationManager) {
		this.autorisationManager = autorisationManager;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setTransactionHelper(TransactionHelper transactionHelper) {
		this.transactionHelper = transactionHelper;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setQuestionnaireSNCDAO(QuestionnaireSNCDAO questionnaireSNCDAO) {
		this.questionnaireSNCDAO = questionnaireSNCDAO;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSourcesQuittancementAvecLiberationPossible(Set<String> sourcesQuittancementAvecLiberationPossible) {
		this.sourcesQuittancementAvecLiberationPossible = sourcesQuittancementAvecLiberationPossible;
	}

	public void setLiberationSender(EvenementLiberationDeclarationImpotSender liberationSender) {
		this.liberationSender = liberationSender;
	}

	public void setAjouterDelaiValidator(Validator ajouterDelaiValidator) {
		this.ajouterDelaiValidator = ajouterDelaiValidator;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	private void checkEditRight(boolean emission, boolean rappel, boolean duplicata, boolean quittancement, boolean liberation) throws AccessDeniedException {
		final Set<Role> rolesRequis = EnumSet.noneOf(Role.class);
		if (emission) {
			rolesRequis.add(Role.QSNC_EMISSION);
		}
		if (rappel) {
			rolesRequis.add(Role.QSNC_RAPPEL);
		}
		if (duplicata) {
			rolesRequis.add(Role.QSNC_DUPLICATA);
		}
		if (quittancement) {
			rolesRequis.add(Role.QSNC_QUITTANCEMENT);
		}
		if (liberation) {
			rolesRequis.add(Role.QSNC_LIBERATION);
		}
		if (!rolesRequis.isEmpty() && !SecurityHelper.isAnyGranted(securityProvider, rolesRequis.toArray(new Role[rolesRequis.size()]))) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec requis pour effectuer cette opération.");
		}
	}

	private void checkEditRightOnEntreprise(Entreprise entreprise) throws AccessDeniedException {
		final Autorisations autorisations = autorisationManager.getAutorisations(entreprise, AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
		if (!autorisations.isQuestionnairesSNC()) {
			throw new AccessDeniedException("Vous ne possédez pas le droit de procéder à des modifications autours des questionnaires SNC sur cette entreprise.");
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture/écriture et transforme une éventuelle {@link DeclarationException} en {@link ActionException}
	 *
	 * @param callback action à lancer
	 * @param <T>      type du résultat renvoyé par l'action
	 * @return le résultat renvoyé par l'action
	 */
	protected final <T> T doInTransaction(TransactionHelper.ExceptionThrowingCallback<T, DeclarationException> callback) {
		try {
			return transactionHelper.doInTransactionWithException(false, callback);
		}
		catch (DeclarationException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}

	/**
	 * Lance le traitement du callback dans une transaction en lecture/écriture et transforme une éventuelle {@link DeclarationException} en {@link ActionException}
	 *
	 * @param callback action à lancer
	 */
	protected final void doInTransaction(TransactionHelper.ExceptionThrowingCallbackWithoutResult<DeclarationException> callback) {
		try {
			transactionHelper.doInTransactionWithException(false, callback);
		}
		catch (DeclarationException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}

	@InitBinder(value = "added")
	public void initAddBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new QuestionnaireSNCAddViewValidator());
	}

	@InitBinder(value = "quittance")
	public void initQuittanceBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, false));
		binder.setValidator(new QuestionnaireSNCQuittancementViewValidator());
	}

	@NotNull
	private Entreprise getEntreprise(long idEntreprise) throws TiersNotFoundException {
		final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, idEntreprise);
		if (entreprise == null) {
			throw new TiersNotFoundException(idEntreprise);
		}
		return entreprise;
	}

	@NotNull
	private List<QuestionnaireSNCView> asViews(Iterable<QuestionnaireSNC> core) {
		final List<QuestionnaireSNCView> views = new LinkedList<>();
		for (QuestionnaireSNC q : core) {
			views.add(new QuestionnaireSNCView(q, infraService, messageHelper));
		}
		return views;
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/details.do", method = RequestMethod.GET)
	@ResponseBody
	public QuestionnaireSNCView getDetails(@RequestParam("id") long idQuestionnaire) {
		final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, idQuestionnaire);
		if (questionnaire == null) {
			throw new ObjectNotFoundException("Questionnaire SNC inexistant.");
		}
		return new QuestionnaireSNCView(questionnaire, infraService, messageHelper);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String getList(Model model, @RequestParam("tiersId") long tiersId) {

		// un droit d'édition suffit pour arriver ici... mais il en faut un
		checkEditRight(true, true, true, true, false);

		final Entreprise entreprise = getEntreprise(tiersId);
		checkEditRightOnEntreprise(entreprise);

		final List<QuestionnaireSNC> questionnaires = entreprise.getDeclarationsTriees(QuestionnaireSNC.class, true);
		model.addAttribute("command", new QuestionnairesSNCEditView(tiersId, asViews(CollectionsUtils.revertedOrder(questionnaires))));
		return "qsnc/list";
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/choisir.do", method = RequestMethod.GET)
	public String showChoixPeriodePourNouveauQuestionnaire(Model model, @RequestParam("tiersId") long tiersId) {
		// il faut le droit d'émission pour arriver ici
		checkEditRight(true, false, false, false, false);

		final Entreprise entreprise = getEntreprise(tiersId);
		checkEditRightOnEntreprise(entreprise);

		final List<QuestionnaireSNC> questionnairesExistants = entreprise.getDeclarationsTriees(QuestionnaireSNC.class, false);
		final Set<Integer> pfCouvertes = new HashSet<>(questionnairesExistants.size());
		for (QuestionnaireSNC questionnaire : questionnairesExistants) {
			pfCouvertes.add(questionnaire.getPeriode().getAnnee());
		}
		final Set<Integer> pfManquantes = qsncService.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		pfManquantes.removeAll(pfCouvertes);

		if (pfManquantes.isEmpty()) {
			// si aucune pf n'est manquante, on ne peut en fait pas rajouter de questionnaire... retour à l'envoyeur
			Flash.error("Aucune période fiscale (passée ou présente) identifiée pour l'envoi d'un nouveau questionnaire SNC.");
			return "redirect:list.do?tiersId=" + tiersId;
		}
		else if (pfManquantes.size() == 1) {
			// s'il ne manque qu'une seule pf, pas la peine de proposer le choix, on peut directement y aller
			return "redirect:add.do?tiersId=" + tiersId + "&pf=" + pfManquantes.iterator().next();
		}
		else {
			// le choix est possible...
			model.addAttribute("periodes", pfManquantes);
			model.addAttribute("tiersId", tiersId);
			return "qsnc/choisir";
		}
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	public String showAdd(Model model,
	                      @RequestParam("tiersId") long tiersId,
	                      @RequestParam("pf") int pf,
	                      @RequestParam(value = "depuisTache", defaultValue = "false") boolean depuisTache) {
		// il faut le droit d'émission pour arriver ici
		checkEditRight(true, false, false, false, false);

		final RegDate delaiAccorde = delaisService.getDateFinDelaiRetourQuestionnaireSNCEmisManuellement(RegDate.get());
		final QuestionnaireSNCAddView view = new QuestionnaireSNCAddView(tiersId, pf);
		view.setDelaiAccorde(delaiAccorde);
		view.setDepuisTache(depuisTache);
		return showAdd(model, view);
	}

	private String showAdd(Model model, QuestionnaireSNCAddView view) {
		model.addAttribute("added", view);
		return "qsnc/ajouter";
	}

	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String printNewQuestionnaire(Model model, HttpServletResponse response, @Valid @ModelAttribute("added") final QuestionnaireSNCAddView view, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return showAdd(model, view);
		}

		// il faut le droit d'émission pour arriver ici
		checkEditRight(true, false, false, false, false);

		try {
			final DeclarationGenerationOperation operation = new DeclarationGenerationOperation(view.getEntrepriseId());
			final TicketService.Ticket ticket = ticketService.getTicket(operation, Duration.ofMillis(500));
			try {
				return printNewQuestionnaire(response, view);
			}
			finally {
				ticket.release();
			}
		}
		catch (TicketTimeoutException e) {
			throw new ActionException("Un questionnaire SNC est actuellement en cours d'impression pour ce contribuable. Veuillez ré-essayer ultérieurement.", e);
		}
	}

	private String printNewQuestionnaire(HttpServletResponse response, final QuestionnaireSNCAddView view) throws IOException {

		// création du nouveau questionnaire, impression locale...
		final EditiqueResultat retourEditique = doInTransaction(status -> {

			final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, view.getEntrepriseId());
			if (entreprise == null) {
				throw new TiersNotFoundException(view.getEntrepriseId());
			}
			checkEditRightOnEntreprise(entreprise);

			final PeriodeFiscale periodeFiscale = periodeFiscaleDAO.getPeriodeFiscaleByYear(view.getPeriodeFiscale());
			if (periodeFiscale == null) {
				throw new DeclarationException("Période fiscale " + view.getPeriodeFiscale() + " inexistante!");
			}

			final RegDate dateTraitement = RegDate.get();
			final ModeleDocument md = periodeFiscale.get(TypeDocument.QUESTIONNAIRE_SNC);

			final QuestionnaireSNC questionnaire = new QuestionnaireSNC();
			questionnaire.setDateDebut(RegDate.get(view.getPeriodeFiscale(), 1, 1));
			questionnaire.setDateFin(RegDate.get(view.getPeriodeFiscale(), 12, 31));
			questionnaire.setPeriode(periodeFiscale);
			questionnaire.setModeleDocument(md);
			questionnaire.addEtat(new EtatDeclarationEmise(dateTraitement));

			final DelaiDeclaration delai = new DelaiDeclaration();
			delai.setDateDemande(dateTraitement);
			delai.setDateTraitement(dateTraitement);
			delai.setDelaiAccordeAu(view.getDelaiAccorde());
			delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
			questionnaire.addDelai(delai);
			questionnaire.setDelaiRetourImprime(view.getDelaiAccorde());
			questionnaire.setCodeSegment(QuestionnaireSNCService.codeSegment);
			entreprise.addDeclaration(questionnaire);

			// s'il y avait une tâche d'envoi en instance, il faut la traiter
			final TacheCriteria tacheCriterion = new TacheCriteria();
			tacheCriterion.setAnnee(periodeFiscale.getAnnee());
			tacheCriterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			tacheCriterion.setInclureTachesAnnulees(false);
			tacheCriterion.setNumeroCTB(entreprise.getNumero());
			tacheCriterion.setTypeTache(TypeTache.TacheEnvoiQuestionnaireSNC);
			final List<Tache> tachesATraiter = tacheDAO.find(tacheCriterion);
			if (tachesATraiter != null && !tachesATraiter.isEmpty()) {
				// on marque la première comme traitée, et les (éventuelles) suivantes seront annulées par la synchronisation des tâches
				final TacheEnvoiQuestionnaireSNC tacheTraitee = (TacheEnvoiQuestionnaireSNC) tachesATraiter.get(0);
				tacheTraitee.setEtat(TypeEtatTache.TRAITE);
				tacheTraitee.setDateDebut(questionnaire.getDateDebut());        // éventuel ré-alignement
				tacheTraitee.setDateFin(questionnaire.getDateFin());
			}

			// [SIFISC-20041] si on a un modèle de document, on envoie tout ça à l'éditique... mais sinon, l'envoi se fera hors-application manuellement
			if (md != null) {
				return qsncService.envoiQuestionnaireSNCOnline(questionnaire, dateTraitement);
			}
			else {
				// c'est le signal que le questionnaire doit être imprimé et envoyé par un autre biais que l'application
				return null;
			}
		});

		// cas d'une demande d'impression avérée
		if (retourEditique != null) {
			final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
					resultat -> "redirect:/qsnc/list.do?tiersId=" + view.getEntrepriseId();

			final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
				Flash.error(String.format("%s Veuillez imprimer un duplicata du questionnaire SNC.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return "redirect:/qsnc/list.do?tiersId=" + view.getEntrepriseId();
			};

			return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "questionnaireSNC", inbox, null, erreur);
		}
		else {
			// ici nous sommes dans le cas où un nouveau QSNC a bien été généré mais pour une PF antérieure à 2016, en gros
			// -> un petit message comme quoi le document a bien été créé mais que l'envoi doit suivre un autre processus
			Flash.warning("Le questionnaire SNC a maintenant été généré dans le système, mais aucune impression n'est prévue pour cette période fiscale. Veuillez procéder si nécessaire à l'envoi du document manuellement.");
			return "redirect:/qsnc/list.do?tiersId=" + view.getEntrepriseId();
		}
	}

	@RequestMapping(value = "/duplicata.do", method = RequestMethod.POST)
	public String imprimerDuplicata(HttpServletResponse response,
	                                @RequestParam("id") final long questionnaireId) throws IOException {

		// vérification des droits d'accès
		checkEditRight(false, false, true, false, false);

		// appel à éditique
		final EditiqueResultat retourEditique = doInTransaction(status -> {
			// récupération du questionnaire
			final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, questionnaireId);
			if (questionnaire == null) {
				throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + questionnaireId);
			}

			// récupération de l'entreprise et vérification des droits de modification
			final Tiers tiers = questionnaire.getTiers();
			if (!(tiers instanceof Entreprise)) {
				throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
			}

			final Entreprise entreprise = (Entreprise) tiers;
			checkEditRightOnEntreprise(entreprise);

			return qsncService.envoiDuplicataQuestionnaireSNCOnline(questionnaire);
		});

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				resultat -> "redirect:/qsnc/editer.do?id=" + questionnaireId;

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
			Flash.error(String.format("%s Veuillez ré-essayer ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
			return "redirect:/qsnc/editer.do?id=" + questionnaireId;
		};

		return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "questionnaireSNC", inbox, null, erreur);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/editer.do", method = RequestMethod.GET)
	public String showEditerQuestionnaire(Model model,
	                                      @RequestParam("id") long questionnaireId,
	                                      @RequestParam(value = "tacheId", required = false) Long tacheId) {

		// vérification des droits d'accès
		checkEditRight(false, true, true, true, false);

		// récupération du questionnaire
		final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, questionnaireId);
		if (questionnaire == null) {
			throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + questionnaireId);
		}

		// récupération de l'entreprise et vérification des droits de modification
		final Tiers tiers = questionnaire.getTiers();
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
		}
		final Entreprise entreprise = (Entreprise) tiers;
		checkEditRightOnEntreprise(entreprise);

		// construction de la vue et affichage
		final QuestionnaireSNCView view = new QuestionnaireSNCEditView(questionnaire,
		                                                               infraService,
		                                                               SecurityHelper.isAnyGranted(securityProvider, Role.QSNC_RAPPEL),
		                                                               SecurityHelper.isAnyGranted(securityProvider, Role.QSNC_DUPLICATA), SecurityHelper.isAnyGranted(securityProvider, Role.QSNC_LIBERATION) && isLiberable(questionnaire),
		                                                               messageHelper);
		model.addAttribute("questionnaire", view);
		model.addAttribute("depuisTache", tacheId != null);
		model.addAttribute("tacheId", tacheId);
		model.addAttribute("isAjoutDelaiAutorise", AutreDocumentFiscalController.isAjoutDelaiAutorise(questionnaire));
		return "qsnc/editer";
	}

	@InitBinder(value = AJOUTER_DELAI)
	public void initAjouterDelaiBinder(WebDataBinder binder) {
		initDelaiBinder(binder);
	}


	/**
	 * Affiche un écran qui permet de choisir les paramètres pour l'ajout d'une demande de délai
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/delai/ajouter-snc.do", method = RequestMethod.GET)
	public String ajouterDelai(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.QSNC_DELAI)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'ajouter un délai sur un questionnaire SNC.");
		}

		final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, id);
		if (questionnaire == null) {
			throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + id);
		}

		final Entreprise ctb = (Entreprise) questionnaire.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		if (!AutreDocumentFiscalController.isAjoutDelaiAutorise(questionnaire)) {
			Flash.warning("Impossible d'ajouter un délai : document déjà retourné, ou rappel déjà été envoyé.");
			return "redirect:/qsnc/editer.do?id=" + id;
		}

		final RegDate delaiAccorde = delaisService.getDateFinDelaiRetourQuestionnaireSNCEmisManuellement(questionnaire.getDelaiAccordeAu());
		model.addAttribute(AJOUTER_DELAI, new QuestionnaireSNCAjouterDelaiView(questionnaire, delaiAccorde, EtatDelaiDocumentFiscal.ACCORDE));
		model.addAttribute(DECISIONS_DELAI, tiersMapHelper.getTypesEtatsDelaiDeclaration());
		return "qsnc/delai/ajouter-snc";


	}

	/**
	 * Ajoute un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/ajouter-snc.do", method = RequestMethod.POST)
	public String ajouterDelai(HttpServletResponse response, @Valid @ModelAttribute(AJOUTER_DELAI) final QuestionnaireSNCAjouterDelaiView view, BindingResult result, Model model) throws EditiqueException, IOException, AccessDeniedException {

		checkAccessGestionQSNC();

		if (result.hasErrors()) {
			final QuestionnaireSNC doc = hibernateTemplate.get(QuestionnaireSNC.class, view.getIdDocumentFiscal());
			view.setDeclarationRange(new DateRangeHelper.Range(doc));
			model.addAttribute(AJOUTER_DELAI, view);
			model.addAttribute(DECISIONS_DELAI, tiersMapHelper.getTypesEtatsDelaiDeclaration());
			return "qsnc/delai/ajouter-snc";
		}

		final Long id = view.getIdDocumentFiscal();
		final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, id);
		if (questionnaire == null) {
			final String message = messageHelper.getMessage("error.qsnc.ajout.delai.questionnaire.inconnu", id);
			throw new ObjectNotFoundException(message);
		}

		final Entreprise ctb = (Entreprise) questionnaire.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		if (!AutreDocumentFiscalController.isAjoutDelaiAutorise(questionnaire)) {
			final String message = messageHelper.getMessage("error.qsnc.ajout.delai.questionnaire.document.deja.retourne");
			Flash.warning(message);
			return "redirect:/qsnc/editer.do?id=" + id;
		}

		// On ajoute le délai
		final RegDate delaiAccordeAu = view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE ? view.getDelaiAccordeAu() : null;
		final Long idDelai = qsncService.ajouterDelai(id, view.getDateDemande(), delaiAccordeAu, view.getDecision());

		return gererImpressionCourrierDelai(idDelai, id, view.getTypeImpression(), response);
	}

	private String gererImpressionCourrierDelai(long idDelai, long idDeclaration,
	                                            TypeImpression typeImpression,
	                                            HttpServletResponse response) throws EditiqueException, IOException {
		if (typeImpression != null) {
			if (TypeImpression.BATCH == typeImpression) {
				qsncService.envoiDemandeDelaiQuestionnaireSNCBatch(idDelai, RegDate.get());
				final String message = messageHelper.getMessage("ajout.delai.qsnc.lettre.delai.editique.programmer", idDeclaration);
				Flash.message(message);
			}
			else if (TypeImpression.LOCAL == typeImpression) {
				final EditiqueResultat retourEditique = qsncService.envoiDemandeDelaiQuestionnaireSNCOnline(idDelai, RegDate.get());

				if (retourEditique != null) {
					final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
							resultat -> "redirect:/qsnc/editer.do?id=" + idDeclaration;

					final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
						Flash.error(messageHelper.getMessage("error.qsnc.ajout.delai.retour.editique", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
						return "redirect:/qsnc/editer.do?id=" + idDeclaration;
					};

					return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "delaiQuestionnaireSNC", inbox, null, erreur);
				}
				else {
					final String message = messageHelper.getMessage("ajout.delai.qsnc.lettre.delai.editique.document.generer.impression.non.programme");
					Flash.warning(message);
					return "redirect:/qsnc/list.do?tiersId=" + idDeclaration;
				}
			}
			else {
				throw new IllegalArgumentException("Valeur non-supportée pour le type d'impression : " + typeImpression);
			}
		}
		// Pas de document directement en retour -> on retourne à l'édition de la DI
		return "redirect:/qsnc/editer.do?id=" + idDeclaration;
	}

	@InitBinder(value = MODIFIER_DELAI)
	public void initModifierDelaiBinder(WebDataBinder binder) {
		initDelaiBinder(binder);
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/editer-snc.do", method = RequestMethod.GET)
	public String editerEtatDelai(@RequestParam("id") long id, Model model) throws AccessDeniedException {

		checkAccessGestionQSNC();

		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, id);
		if (delai == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.delai.inexistant"));
		}
		if (delai.getEtat() != EtatDelaiDocumentFiscal.DEMANDE) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.delai.finalise"));
		}

		final Declaration declaration = delai.getDeclaration();
		if (!(declaration instanceof QuestionnaireSNC)) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.qsnc.inexistante"));
		}

		final QuestionnaireSNC qsnc = (QuestionnaireSNC) declaration;
		final Contribuable ctb = qsnc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = delaisService.getDateFinDelaiRetourQuestionnaireSNCEmisManuellement(qsnc.getDelaiAccordeAu());
		model.addAttribute(MODIFIER_DELAI, new ModifierEtatDelaiQSNCView(delai, delaiAccordeAu));
		model.addAttribute(DECISIONS_DELAI, tiersMapHelper.getTypesEtatsDelaiDeclaration());
		return "qsnc/delai/editer-snc";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/editer-snc.do", method = RequestMethod.POST)
	public String editerEtatDelai(@Valid @ModelAttribute(MODIFIER_DELAI) final ModifierEtatDelaiQSNCView view,
	                              BindingResult result, Model model, HttpServletResponse response) throws AccessDeniedException, EditiqueException, IOException {
		checkAccessGestionQSNC();
		final Long id = view.getIdDeclaration();
		final QuestionnaireSNC qsnc = hibernateTemplate.get(QuestionnaireSNC.class, id);

		if (result.hasErrors()) {
			view.setDeclarationRange(new DateRangeHelper.Range(qsnc));
			model.addAttribute(MODIFIER_DELAI, view);
			model.addAttribute(DECISIONS_DELAI, tiersMapHelper.getTypesEtatsDelaiDeclaration());
			return "qsnc/delai/editer-snc";
		}

		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, view.getIdDelai());
		if (delai == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.delai.inexistant"));
		}
		if (delai.getEtat() != EtatDelaiDocumentFiscal.DEMANDE) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.delai.finalise"));
		}

		final Declaration declaration = delai.getDeclaration();
		if (!(declaration instanceof QuestionnaireSNC)) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.qsnc.inexistante"));
		}

		final Contribuable ctb = qsnc.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		final RegDate delaiAccordeAu = view.getDecision() == EtatDelaiDocumentFiscal.ACCORDE ? view.getDelaiAccordeAu() : null;
		final Long idDelai = qsncService.saveDelai(view.getIdDelai(), view.getDecision(), delaiAccordeAu);
		return gererImpressionCourrierDelai(idDelai, id, view.getTypeImpression(), response);
	}

	/**
	 * Annule un délai
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/delai/annuler.do", method = RequestMethod.POST)
	public String annulerDelai(@RequestParam("id") long id) throws AccessDeniedException {

		if (!SecurityHelper.isGranted(securityProvider, Role.QSNC_DELAI)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'annuler un délai sur un questionnaire SNC.");
		}

		final DelaiDocumentFiscal delai = hibernateTemplate.get(DelaiDocumentFiscal.class, id);
		if (delai == null) {
			throw new IllegalArgumentException("Le délai n°" + id + " n'existe pas.");
		}

		final Entreprise ctb = (Entreprise) delai.getDocumentFiscal().getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		delai.setAnnule(true);

		return "redirect:/qsnc/editer.do?id=" + delai.getDocumentFiscal().getId();
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/ajouter-quittance.do", method = RequestMethod.GET)
	public String showQuittancer(Model model, @RequestParam("id") long questionnaireId) {

		// vérification des droits d'accès
		checkEditRight(false, false, false, true, false);

		// récupération du questionnaire
		final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, questionnaireId);
		if (questionnaire == null) {
			throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + questionnaireId);
		}

		// récupération de l'entreprise et vérification des droits de modification
		final Tiers tiers = questionnaire.getTiers();
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
		}
		final Entreprise entreprise = (Entreprise) tiers;
		checkEditRightOnEntreprise(entreprise);

		// création de la vue et affichage
		final QuestionnaireSNCQuittancementView view = new QuestionnaireSNCQuittancementView(questionnaire);
		return showQuittancer(model, view);
	}

	private String showQuittancer(Model model, QuestionnaireSNCQuittancementView view) {
		model.addAttribute("quittance", view);
		return "qsnc/ajouter-quittance";
	}

	@RequestMapping(value = "/ajouter-quittance.do", method = RequestMethod.POST)
	public String quittanceQuestionnaire(Model model, @Valid @ModelAttribute("quittance") final QuestionnaireSNCQuittancementView view, BindingResult bindingResult) {

		// vérification des droits d'accès
		checkEditRight(false, false, false, true, false);

		// erreur de saisie ?
		if (bindingResult.hasErrors()) {
			return showQuittancer(model, view);
		}

		// on fait le boulot de quittancement
		doInTransaction(status -> {

			// récupération du questionnaire
			final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, view.getQuestionnaireId());
			if (questionnaire == null) {
				throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + view.getQuestionnaireId());
			}

			// récupération de l'entreprise et vérification des droits de modification
			final Tiers tiers = questionnaire.getTiers();
			if (!(tiers instanceof Entreprise)) {
				throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
			}
			final Entreprise entreprise = (Entreprise) tiers;
			checkEditRightOnEntreprise(entreprise);

			// quittancement
			qsncService.quittancerQuestionnaire(questionnaire, view.getDateRetour(), EtatDeclarationRetournee.SOURCE_WEB);
		});

		// et c'est fini (sauf bien-sûr si la quittance ne s'est pas bien passée...)
		return "redirect:editer.do?id=" + view.getQuestionnaireId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/annuler-quittance.do", method = RequestMethod.POST)
	public String annulerQuittance(@RequestParam("id") long idEtatRetour) {

		// vérification des droits d'accès
		checkEditRight(false, false, false, true, false);

		// récupération de l'état
		final EtatDeclarationRetournee etat = hibernateTemplate.get(EtatDeclarationRetournee.class, idEtatRetour);
		if (etat == null) {
			throw new ObjectNotFoundException("Quittance inconnue avec l'identifiant " + idEtatRetour);
		}

		// vérification des droits sur l'entreprise
		final Declaration decla = etat.getDeclaration();
		if (!(decla instanceof QuestionnaireSNC)) {
			throw new ObjectNotFoundException("L'état à annuler n'est pas associé à un questionnaire SNC");
		}

		// récupération de l'entreprise et vérification des droits de modification
		final Tiers tiers = decla.getTiers();
		if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
		}
		final Entreprise entreprise = (Entreprise) tiers;
		checkEditRightOnEntreprise(entreprise);

		// déjà annulé, rien à faire
		if (!etat.isAnnule()) {
			etat.setAnnule(true);
		}
		return "redirect:editer.do?id=" + decla.getId();
	}

	@RequestMapping(value = "/annuler.do", method = RequestMethod.POST)
	public String annulerQuestionnaire(@RequestParam("id") final long idQuestionnaire,
	                                   @RequestParam(value = "tacheId", required = false) final Long tacheId) {

		// vérification des droits d'accès
		checkEditRight(true, false, false, false, false);

		return doInTransaction(status -> {
			// récupération du questionnaire
			final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, idQuestionnaire);
			if (questionnaire == null) {
				throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + idQuestionnaire);
			}

			// récupération de l'entreprise et vérification des droits de modification
			final Tiers tiers = questionnaire.getTiers();
			if (!(tiers instanceof Entreprise)) {
				throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
			}
			final Entreprise entreprise = (Entreprise) tiers;
			checkEditRightOnEntreprise(entreprise);

			// annulation du questionnaire
			qsncService.annulerQuestionnaire(questionnaire);

			if (tacheId != null) {
				// traitement de la tâche
				final Tache tache = tacheDAO.get(tacheId);
				tache.setEtat(TypeEtatTache.TRAITE);

				return "redirect:/tache/list.do";
			}
			else {
				return "redirect:/qsnc/list.do?tiersId=" + tiers.getNumero();
			}
		});
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/liberer.do", method = RequestMethod.POST)
	public String libererQuestionnaire(@RequestParam("id") long idQuestionnaire) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.DI_LIBERER_PP, Role.DI_LIBERER_PM)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec de libération des questionnaires SNC.");
		}

		final QuestionnaireSNC questionnaireSNC = questionnaireSNCDAO.get(idQuestionnaire);
		if (questionnaireSNC == null) {
			throw new ObjectNotFoundException(messageHelper.getMessage("error.qsnc.inexistante"));
		}
		// vérification des droits d'accès
		checkEditRight(false, false, false, false, true);

		final Contribuable ctb = questionnaireSNC.getTiers();
		controllerUtils.checkAccesDossierEnEcriture(ctb.getId());

		// vérification de la libérabilité de la déclaration
		if (!isLiberable(questionnaireSNC)) {
			final String message = messageHelper.getMessage("error.qsnc.non.liberable");
			Flash.error(message);
			return "redirect:editer.do?id=" + idQuestionnaire;
		}

		// envoi de la demande de libération
		try {
			liberationSender.demandeLiberationDeclarationImpot(questionnaireSNC.getTiers().getNumero(),
			                                                   questionnaireSNC.getPeriode().getAnnee(),
			                                                   questionnaireSNC.getNumero(),
			                                                   EvenementLiberationDeclarationImpotSender.TypeDeclarationLiberee.DI_PM);
			Flash.message("La demande de libération du questionnaire SNC a été envoyée au service concerné.");
			return "redirect:editer.do?id=" + idQuestionnaire;
		}
		catch (EvenementDeclarationException e) {
			throw new ActionException(e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/rappel.do", method = RequestMethod.POST)
	public String envoyerRappelQuestionnaire(HttpServletResponse response, @RequestParam("id") final long questionnaireId) throws IOException {

		// vérification des droits d'accès
		checkEditRight(false, true, false, false, false);

		// appel à éditique
		final EditiqueResultat retourEditique = doInTransaction(status -> {
			// récupération du questionnaire
			final QuestionnaireSNC questionnaire = hibernateTemplate.get(QuestionnaireSNC.class, questionnaireId);
			if (questionnaire == null) {
				throw new ObjectNotFoundException("Questionnaire SNC inconnu avec l'identifiant " + questionnaireId);
			}

			// récupération de l'entreprise et vérification des droits de modification
			final Tiers tiers = questionnaire.getTiers();
			if (!(tiers instanceof Entreprise)) {
				throw new ObjectNotFoundException("Questionnaire SNC sans lien vers une entreprise...");
			}
			final Entreprise entreprise = (Entreprise) tiers;
			checkEditRightOnEntreprise(entreprise);

			// vérification du côté 'rappelable' du questionnaire, on ne sait jamais
			final QuestionnaireSNCEditView view = new QuestionnaireSNCEditView(questionnaire, infraService, true, false, false, messageHelper);
			if (!view.isRappelable()) {
				throw new ActionException("Le questionnaire SNC n'est pas dans un état 'rappelable'.");
			}

			return qsncService.envoiRappelQuestionnaireSNCOnline(questionnaire, RegDate.get());
		});

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				resultat -> "redirect:/qsnc/editer.do?id=" + questionnaireId;

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = resultat -> {
			Flash.error(String.format("%s Veuillez ré-essayer ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
			return "redirect:/qsnc/editer.do?id=" + questionnaireId;
		};

		return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "questionnaireSNC", inbox, null, erreur);
	}

	private boolean isLiberable(QuestionnaireSNC questionnaireSNC) {
		final List<EtatDeclaration> etatsRetournes = questionnaireSNC.getEtatsDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE, false);
		final Set<String> sources = new HashSet<>(etatsRetournes.size());
		for (EtatDeclaration etat : etatsRetournes) {
			sources.add(((EtatDeclarationRetournee) etat).getSource());
		}
		sources.retainAll(sourcesQuittancementAvecLiberationPossible);
		return !sources.isEmpty();
	}

	private void checkAccessGestionQSNC() {
		if (!SecurityHelper.isGranted(securityProvider, Role.QSNC_DELAI)) {
			final String message = messageHelper.getMessage("error.qsnc.ajout.delai.habilitation");
			throw new AccessDeniedException(message);
		}
	}

	private void initDelaiBinder(WebDataBinder binder) {
		binder.setValidator(ajouterDelaiValidator);
		//champs ajout formulaire ajout délai
		binder.registerCustomEditor(RegDate.class, "dateDemande", new RegDateEditor(false, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "delaiAccordeAu", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "ancienDelaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
		binder.registerCustomEditor(RegDate.class, "ancienDelaiAccorde", new RegDateEditor(true, false, false, RegDateHelper.StringFormat.DISPLAY));
	}

	public void setMessageHelper(MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}
}
