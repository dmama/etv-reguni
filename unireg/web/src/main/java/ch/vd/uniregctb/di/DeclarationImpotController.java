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
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelper;
import ch.vd.uniregctb.common.RetourEditiqueControllerHelperImpl;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.ChoixDeclarationImpotView;
import ch.vd.uniregctb.di.view.DeclarationView;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
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
	private TacheDAO tacheDAO;
	private MessageSource messageSource;
	private DeclarationImpotEditManager manager;
	private DelaisService delaisService;
	private TiersMapHelper tiersMapHelper;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private PlatformTransactionManager transactionManager;
	private Validator validator;

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

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
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

		final List<PeriodeImposition> ranges = manager.calculateRangesProchainesDIs(tiersId);
		if (ranges == null || ranges.isEmpty()) {
			// [UNIREG-832] impossible d'imprimer une nouvelle DI: on reste dans le même écran et on affiche un message d'erreur
			Flash.warning(DeclarationImpotEditManager.CANNOT_ADD_NEW_DI);
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", Collections.emptyList());
			return "/decl/choisir";
		}
		else if (ranges.size() == 1) {
			final DateRange range = ranges.get(0);
			// il reste exactement une DI à créer : on continue directement sur l'écran d'impression
			return "redirect:/decl/imprimer.do?tiersId=" + tiersId + "&debut=" + range.getDateDebut().index() + "&fin=" + range.getDateFin().index();
		}
		else {
			// [UNIREG-889] il y reste plusieurs DIs à créer : on demande à l'utilisateur de choisir
			final ArrayList<ChoixDeclarationImpotView> views = new ArrayList<ChoixDeclarationImpotView>(ranges.size());
			for (PeriodeImposition r : ranges) {
				views.add(new ChoixDeclarationImpotView(r, r.isOptionnelle()));
			}
			model.addAttribute("tiersId", tiersId);
			model.addAttribute("ranges", views);
			return "/decl/choisir";
		}
	}

	/**
	 * Affiche un écran qui permet de prévisualiser une déclaration avant son impression (et donc son ajout sur le contribuable).
	 */
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	@RequestMapping(value = "/decl/imprimer.do", method = RequestMethod.GET)
	public String imprimer(@RequestParam("tiersId") long tiersId,
	                       @RequestParam("debut") RegDate dateDebut,
	                       @RequestParam("fin") RegDate dateFin,
	                       @RequestParam(value = "typeDocument", required = false) TypeDocument typeDocument,
	                       @RequestParam(value = "delaiRetour", required = false) Integer delaiRetour,
	                       @RequestParam(value = "depuisTache", required = false, defaultValue = "false") boolean depuisTache,
	                       Model model) throws AccessDeniedException {

		if (!SecurityProvider.isGranted(Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);

		final Tiers tiers = hibernateTemplate.get(Tiers.class, tiersId);
		if (tiers == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.tiers.inexistant", null, WebContextUtils.getDefaultLocale()));
		}
		if (!(tiers instanceof Contribuable)) {
			throw new IllegalArgumentException("Le tiers spécifié n'est pas une personne physique.");
		}
		final Contribuable ctb = (Contribuable) tiers;

		final ImprimerNouvelleDeclarationImpotView view = new ImprimerNouvelleDeclarationImpotView(tiersId, depuisTache);
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
			return "decl/imprimer";
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
		if (SecurityProvider.isGranted(Role.DI_QUIT_PP)) {
			final DeclarationImpotOrdinaire diAnnulee = findDeclarationRetourneeEtAnnulee(tiersId, periode);
			if (diAnnulee != null) {
				view.setDateRetour(RegDate.get());
				view.setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(true);
			}
		}

		return "decl/imprimer";
	}

	/**
	 * Crée, sauve en base et imprime la déclaration d'impôt.
	 */
	@RequestMapping(value = "/decl/imprimer.do", method = RequestMethod.POST)
	public String imprimer(@Valid @ModelAttribute("command") final ImprimerNouvelleDeclarationImpotView view, BindingResult result,
	                       HttpServletRequest request, HttpServletResponse response, Model model) throws Exception {

		if (!SecurityProvider.isGranted(Role.DI_EMIS_PP)) {
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec d'ajout des déclarations d'impôt sur les personnes physiques.");
		}
		final Long tiersId = view.getTiersId();
		ControllerUtils.checkAccesDossierEnEcriture(tiersId);

		if (result.hasErrors()) {
			model.addAttribute("typesDeclarationImpot", tiersMapHelper.getTypesDeclarationImpot());
			model.addAttribute("typesAdresseRetour", tiersMapHelper.getTypesAdresseRetour());
			return "decl/imprimer";
		}

		// On imprime la nouvelle déclaration d'impôt

		final EditiqueResultat resultat = manager.envoieImpressionLocalDI(tiersId, null, view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition(), view.getTypeDocument(),
				view.getTypeAdresseRetour(), view.getDelaiAccorde(), view.getDateRetour());

		final RetourEditiqueControllerHelperImpl.TraitementRetourEditique inbox = new RetourEditiqueControllerHelperImpl.TraitementRetourEditique() {
			@Override
			public String doJob(EditiqueResultat resultat) {
				return "redirect:/di/edit.do?action=listdis&numero=" + tiersId;
			}
		};

		final RetourEditiqueControllerHelperImpl.TraitementRetourEditique erreur = new RetourEditiqueControllerHelperImpl.TraitementRetourEditique() {
			@Override
			public String doJob(EditiqueResultat resultat) {
				Flash.error(String.format("%s Veuillez imprimer un duplicata de la déclaration d'impôt.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return "redirect:/di/edit.do?action=listdis&numero=" + tiersId;
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
	 * Permet de traiter une tâche d'annulation de DI en n'annulant pas la DI et en marquant la tâche comme traitée. Il s'agit d'une fonctionnalité historique qui mériterait d'être respécifiée car
	 * fatalement la tâche d'annulation va réapparaître au prochain recalcul automatique des tâches.
	 *
	 * @param tacheId le numéro de la tâche d'annulation de déclaration qu'il faut passer à traiter sans autre forme de procès
	 * @return la vue à afficher après traitement
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/decl/maintenir.do", method = RequestMethod.POST)
	public String maintenir(@RequestParam("tacheId") final long tacheId) {

		final Tache tache = tacheDAO.get(tacheId);
		if (tache == null) {
			throw new ObjectNotFoundException("La tâche n°" + tacheId + " n'existe pas.");
		}
		if (!(tache instanceof TacheAnnulationDeclarationImpot)) {
			throw new IllegalArgumentException("La tâche n°" + tacheId + " n'est pas une tâche d'annulation de déclaration d'impôt");
		}

		final TacheAnnulationDeclarationImpot tacheAnnulation = (TacheAnnulationDeclarationImpot) tache;
		ControllerUtils.checkAccesDossierEnEcriture(tacheAnnulation.getContribuable().getNumero());

		if (tacheAnnulation.getEtat() != TypeEtatTache.TRAITE) {
			tacheAnnulation.setEtat(TypeEtatTache.TRAITE);
		}

		return "redirect:/tache/list.do";
	}
}
