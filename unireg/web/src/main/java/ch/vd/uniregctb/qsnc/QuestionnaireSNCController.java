package ch.vd.uniregctb.qsnc;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.TicketService;
import ch.vd.uniregctb.common.TicketTimeoutException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationGenerationOperation;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.declaration.snc.QuestionnaireSNCService;
import ch.vd.uniregctb.declaration.view.QuestionnaireSNCView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;
import ch.vd.uniregctb.tiers.manager.Autorisations;
import ch.vd.uniregctb.transaction.TransactionHelper;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;
import ch.vd.uniregctb.utils.RegDateEditor;

@Controller
@RequestMapping("/qsnc")
public class QuestionnaireSNCController {

	private HibernateTemplate hibernateTemplate;
	private QuestionnaireSNCService qsncService;
	private MessageSource messageSource;
	private SecurityProviderInterface securityProvider;
	private AutorisationManager autorisationManager;
	private DelaisService delaisService;
	private TransactionHelper transactionHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private TacheDAO tacheDAO;
	private TicketService ticketService;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setQsncService(QuestionnaireSNCService qsncService) {
		this.qsncService = qsncService;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
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

	private void checkEditRight(boolean emission, boolean rappel, boolean duplicata, boolean quittancement) throws AccessDeniedException {
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
	 * @param callback action à lancer
	 * @param <T> type du résultat renvoyé par l'action
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
			views.add(new QuestionnaireSNCView(q, messageSource));
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
		return new QuestionnaireSNCView(questionnaire, messageSource);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String getList(Model model, @RequestParam("tiersId") long tiersId) {

		// un droit d'édition suffit pour arriver ici... mais il en faut un
		checkEditRight(true, true, true, true);

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
		checkEditRight(true, false, false, false);

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
		checkEditRight(true, false, false, false);

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
	public String printNewQuestionnaire(Model model, HttpServletResponse response, @Valid@ModelAttribute("added")final QuestionnaireSNCAddView view, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return showAdd(model, view);
		}

		// il faut le droit d'émission pour arriver ici
		checkEditRight(true, false, false, false);

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
		final EditiqueResultat retourEditique = doInTransaction(new TransactionHelper.ExceptionThrowingCallback<EditiqueResultat, DeclarationException>() {
			@Override
			public EditiqueResultat execute(TransactionStatus status) throws DeclarationException {

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
				delai.setEtat(EtatDelaiDeclaration.ACCORDE);
				questionnaire.addDelai(delai);
				questionnaire.setDelaiRetourImprime(view.getDelaiAccorde());

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
			}
		});

		// cas d'une demande d'impression avérée
		if (retourEditique != null) {
			final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
					new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
						@Override
						public String doJob(EditiqueResultatReroutageInbox resultat) {
							return "redirect:/qsnc/list.do?tiersId=" + view.getEntrepriseId();
						}
					};

			final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
				@Override
				public String doJob(EditiqueResultatErreur resultat) {
					Flash.error(String.format("%s Veuillez imprimer un duplicata du questionnaire SNC.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
					return "redirect:/qsnc/list.do?tiersId=" + view.getEntrepriseId();
				}
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
		checkEditRight(false, false, true, false);

		// appel à éditique
		final EditiqueResultat retourEditique = doInTransaction(new TransactionHelper.ExceptionThrowingCallback<EditiqueResultat, DeclarationException>() {
			@Override
			public EditiqueResultat execute(TransactionStatus status) throws DeclarationException {
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
			}
		});

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
					@Override
					public String doJob(EditiqueResultatReroutageInbox resultat) {
						return "redirect:/qsnc/editer.do?id=" + questionnaireId;
					}
				};

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
			@Override
			public String doJob(EditiqueResultatErreur resultat) {
				Flash.error(String.format("%s Veuillez ré-essayer ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return "redirect:/qsnc/editer.do?id=" + questionnaireId;
			}
		};

		return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "questionnaireSNC", inbox, null, erreur);
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/editer.do", method = RequestMethod.GET)
	public String showEditerQuestionnaire(Model model,
	                                      @RequestParam("id") long questionnaireId,
	                                      @RequestParam(value = "depuisTache", defaultValue = "false") boolean depuisTache) {

		// vérification des droits d'accès
		checkEditRight(false, true, true, true);

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
		                                                               messageSource,
		                                                               SecurityHelper.isAnyGranted(securityProvider, Role.QSNC_RAPPEL),
		                                                               SecurityHelper.isAnyGranted(securityProvider, Role.QSNC_DUPLICATA));
		model.addAttribute("questionnaire", view);
		model.addAttribute("depuisTache", depuisTache);
		return "qsnc/editer";
	}

	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/ajouter-quittance.do", method = RequestMethod.GET)
	public String showQuittancer(Model model, @RequestParam("id") long questionnaireId) {

		// vérification des droits d'accès
		checkEditRight(false, false, false, true);

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
		checkEditRight(false, false, false, true);

		// erreur de saisie ?
		if (bindingResult.hasErrors()) {
			return showQuittancer(model, view);
		}

		// on fait le boulot de quittancement
		doInTransaction(new TransactionHelper.ExceptionThrowingCallbackWithoutResult<DeclarationException>() {
			@Override
			public void execute(TransactionStatus status) throws DeclarationException {

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
			}
		});

		// et c'est fini (sauf bien-sûr si la quittance ne s'est pas bien passée...)
		return "redirect:editer.do?id=" + view.getQuestionnaireId();
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/annuler-quittance.do", method = RequestMethod.POST)
	public String annulerQuittance(@RequestParam("id") long idEtatRetour) {

		// vérification des droits d'accès
		checkEditRight(false, false, false, true);

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
	                                   @RequestParam(value = "depuisTache", defaultValue = "false") final boolean depuisTache) {

		// vérification des droits d'accès
		checkEditRight(true, false, false, false);

		return doInTransaction(new TransactionHelper.ExceptionThrowingCallback<String, DeclarationException>() {
			@Override
			public String execute(TransactionStatus status) throws DeclarationException {
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
				if (depuisTache) {
					return "redirect:/tache/list.do";
				}
				else {
					return "redirect:list.do?tiersId=" + tiers.getNumero();
				}
			}
		});
	}

	@RequestMapping(value = "/rappel.do", method = RequestMethod.POST)
	public String envoyerRappelQuestionnaire(HttpServletResponse response, @RequestParam("id") final long questionnaireId) throws IOException {

		// vérification des droits d'accès
		checkEditRight(false, true, false, false);

		// appel à éditique
		final EditiqueResultat retourEditique = doInTransaction(new TransactionHelper.ExceptionThrowingCallback<EditiqueResultat, DeclarationException>() {
			@Override
			public EditiqueResultat execute(TransactionStatus status) throws DeclarationException {
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
				final QuestionnaireSNCEditView view = new QuestionnaireSNCEditView(questionnaire, messageSource, true, false);
				if (!view.isRappelable()) {
					throw new ActionException("Le questionnaire SNC n'est pas dans un état 'rappelable'.");
				}

				return qsncService.envoiRappelQuestionnaireSNCOnline(questionnaire, RegDate.get());
			}
		});

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> inbox =
				new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox>() {
					@Override
					public String doJob(EditiqueResultatReroutageInbox resultat) {
						return "redirect:/qsnc/editer.do?id=" + questionnaireId;
					}
				};

		final RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> erreur = new RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur>() {
			@Override
			public String doJob(EditiqueResultatErreur resultat) {
				Flash.error(String.format("%s Veuillez ré-essayer ultérieurement.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return "redirect:/qsnc/editer.do?id=" + questionnaireId;
			}
		};

		return retourEditiqueControllerHelper.traiteRetourEditique(retourEditique, response, "questionnaireSNC", inbox, null, erreur);
	}
}
