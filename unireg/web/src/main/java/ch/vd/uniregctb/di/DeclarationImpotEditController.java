package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.DeclarationImpotSelectView;
import ch.vd.uniregctb.di.view.DeclarationImpotView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.type.TypeDocument;

public class DeclarationImpotEditController extends AbstractDeclarationImpotController {

	private static final String ERREUR_COMMUNICATION_EDITIQUE = "erreurCommunicationEditique";

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditController.class);

	public final static String ACTION_LIST_DIS = "listdis";
	public final static String ACTION_NEW_DI = "newdi";
	public final static String ACTION_EDIT_DI = "editdi";
	public final static String ACTION_SELECT_DI = "selectdi";


	public final static String BUTTON_SAVE_DI = "__confirmed_save";
	public final static String BUTTON_SOMMER_DI = "sommer";
	public final static String BUTTON_AJOUTER_DI = "ajouterDI";
	public final static String BUTTON_ANNULER_DI = "annulerDI";
	public final static String TARGET_IMPRIMER_DI = "imprimerDI";
	public final static String BUTTON_DUPLICATA_DI = "duplicataDI";
	public final static String TARGET_ANNULER_DELAI = "annulerDelai";
	public final static String TARGET_CREER_DI = "creerDI";
	public final static String BUTTON_IMPRIMER_TO = "imprimerTO";
	public final static String BUTTON_MAINTENIR_DI = "maintenir";
	public final static String TACHE_ID_PARAMETER_NAME = "idTache";

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public final static String DI_ID_PARAMETER_NAME = "id";
	public final static String CONTRIBUABLE_ID_PARAMETER_NAME = "numero";
	public final static String TYPE_DECLARATION_PARAMETER_NAME = "typeDeclaration";
	public final static String DELAI_RETOUR_PARAMETER_NAME = "delaiRetour";
	public final static String DATE_DEBUT_PARAMETER_NAME = "debut";
	public final static String DATE_FIN_PARAMETER_NAME = "fin";
	public final static String DEPUIS_TACHE_PARAMETER_NAME = "depuisTache";
	public final static String SELECTION_DI_PARAMETER_NAME = "selection";

	private DeclarationImpotEditManager diEditManager;

	public DeclarationImpotEditController() {
		super();
		/*
		 * [UNIREG-486] Workaround pour un bug de IE6 qui empêche d'ouvrir correctement les fichier attachés PDFs.
		 *
		 * Voir aussi:
		 *  - http://drupal.org/node/93787
		 *  - http://support.microsoft.com/default.aspx?scid=kb;en-us;316431
		 *  - http://bugs.php.net/bug.php?id=16173
		 *  - http://pkp.sfu.ca/support/forum/viewtopic.php?p=359&sid=4516e6d325c613c7875f67e1b9194c57
		 *  - http://forum.springframework.org/showthread.php?t=24466
		 */
		setCacheSeconds(-1);
	}

	@Override
	protected boolean suppressValidation(HttpServletRequest request, Object command, BindException errors) {
		if ((getTarget() != null && !TARGET_IMPRIMER_DI.equals(getTarget())) ||
				request.getParameter(BUTTON_SOMMER_DI) != null ||
				request.getParameter(BUTTON_ANNULER_DI) != null ||
				request.getParameter(BUTTON_MAINTENIR_DI) != null) {
			return true;
		}
		return super.suppressValidation(request, command, errors);
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		Object object = null;

		final String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action == null || ACTION_LIST_DIS.equals(action)) {
			object = new DeclarationImpotListView();
		}
		else if (ACTION_SELECT_DI.equals(action)) {
			object = new DeclarationImpotSelectView();
		}
		else if (ACTION_NEW_DI.equals(action)) {
			object = new DeclarationImpotDetailView();
		}
		else if (ACTION_EDIT_DI.equals(action)) {
			object = new DeclarationImpotDetailView();
		}

		return object;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map controlModel) throws Exception {

		ModelAndView mav = super.showForm(request, response, errors, controlModel);
		final Map model = mav.getModel();
		HttpSession session = request.getSession(true);

		try {
			final String action = request.getParameter(ACTION_PARAMETER_NAME);
			if (action == null || ACTION_LIST_DIS.equals(action)) {
				mav = listDIsForm(request, response, errors, model);
				if (session.getAttribute(ERREUR_COMMUNICATION_EDITIQUE) != null) {
					mav.addObject(ERREUR_COMMUNICATION_EDITIQUE, session.getAttribute(ERREUR_COMMUNICATION_EDITIQUE));
					session.removeAttribute(ERREUR_COMMUNICATION_EDITIQUE);
				}
			}
			else if (ACTION_SELECT_DI.equals(action)) {
				mav = selectDiForm(request, response, errors, model);
			}
			else if (ACTION_NEW_DI.equals(action)) {
				mav = newDiForm(request, response, errors, model);
			}
			else if (ACTION_EDIT_DI.equals(action)) {
				mav = editDiForm(request, response, errors, model);
				if (session.getAttribute(ERREUR_COMMUNICATION_EDITIQUE) != null) {
					mav.addObject(ERREUR_COMMUNICATION_EDITIQUE, session.getAttribute(ERREUR_COMMUNICATION_EDITIQUE));
					session.removeAttribute(ERREUR_COMMUNICATION_EDITIQUE);
				}
			}
		}
		catch (HighProbabilityThatBackWasUsedException e) {
			LOGGER.warn("Il semblerait que la fonctionalité 'Back' ait été utilisée sur le navigateur, on récupère comme on peut...");
		}

		return mav;
	}

	/**
	 * Exception lancée en interne par la méthode checkBean() pour signaler que le bean ne correspond pas du tout à ce qui est attendu : ou bien il n'est carrément pas de la bonne classe, ou bien le
	 * contribuable n'y est pas renseigné...
	 */
	private static final class HighProbabilityThatBackWasUsedException extends Exception {
	}

	@SuppressWarnings({"unchecked"})
	private static <T extends DeclarationImpotView> T checkBean(Class<T> expectedClass, Object bean, boolean checkPresenceContribuable) throws HighProbabilityThatBackWasUsedException {
		if (bean == null || !expectedClass.isAssignableFrom(bean.getClass())) {
			throw new HighProbabilityThatBackWasUsedException();
		}
		final T typedBean = (T) bean;
		if (checkPresenceContribuable && typedBean.getContribuable() == null) {
			throw new HighProbabilityThatBackWasUsedException();
		}
		return typedBean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		ModelAndView mav = null;

		// on blinde un peu : si la commande passée est nulle, on ne fait rien (on réaffiche la page)
		if (command != null) {

			try {

				// FIXME (CGD) implémenter la sécurité IFOSec sur les actions ci-dessous !
				final String target = getTarget();
				if (target != null) {
					if (TARGET_ANNULER_DELAI.equals(target)) {
						mav = annulerDelai(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (TARGET_IMPRIMER_DI.equals(target)) {
						mav = imprimerDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (TARGET_CREER_DI.equals(target)) {
						mav = creerDI(request, response, checkBean(DeclarationImpotSelectView.class, command, true), errors);
					}
				}
				else {
					if (request.getParameter(BUTTON_AJOUTER_DI) != null) {
						mav = ajouterDI(request, response, checkBean(DeclarationImpotListView.class, command, true), errors);
					}
					else if (request.getParameter(BUTTON_SOMMER_DI) != null) {
						mav = sommerDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (request.getParameter(BUTTON_ANNULER_DI) != null) {
						mav = annulerDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (request.getParameter(BUTTON_IMPRIMER_TO) != null) {
						mav = imprimerTO(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (request.getParameter(BUTTON_MAINTENIR_DI) != null) {
						mav = maintenirDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
					else if (request.getParameter(BUTTON_SAVE_DI) != null) {
						mav = sauverDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
					}
				}
			}
			catch (HighProbabilityThatBackWasUsedException e) {
				// on dirait bien que quelqu'un a joué avec le "back" du navigateur et que les choses sont devenues incohérentes !!
				// -> ré-affichage de la page
				mav = null;
			}
		}

		if (mav == null) {
			return showForm(request, response, errors);
		}
		else {
			return mav;
		}
	}

	@SuppressWarnings("unchecked")
	private ModelAndView selectDiForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws HighProbabilityThatBackWasUsedException, AccessDeniedException, AdressesResolutionException {

		final Long id = extractLongParam(request, CONTRIBUABLE_ID_PARAMETER_NAME);
		if (id == null) {
			return null;
		}
		checkAccesDossierEnEcriture(id);

		final DeclarationImpotSelectView view = checkBean(DeclarationImpotSelectView.class, model.get(getCommandName()), false);
		Assert.notNull(view);

		view.setContribuable(diEditManager.creerCtbDI(id));
		view.setRangesFromDateRanges(diEditManager.calculateRangesProchainesDIs(id));

		return new ModelAndView("di/edit/select", model);
	}

	@SuppressWarnings("unchecked")
	private ModelAndView listDIsForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws HighProbabilityThatBackWasUsedException, AccessDeniedException {

		final Long id = extractLongParam(request, CONTRIBUABLE_ID_PARAMETER_NAME);
		if (id == null) {
			return null;
		}

		// vérification des droits d'accès au dossier du contribuable
		checkAccesDossierEnLecture(id);

		final DeclarationImpotListView diListView = checkBean(DeclarationImpotListView.class, model.get(getCommandName()), false);
		Assert.notNull(diListView);

		diEditManager.findByNumero(id, diListView);
		NatureTiers natureTiers = diListView.getContribuable().getNatureTiers();
		if (natureTiers == NatureTiers.Habitant || natureTiers == NatureTiers.MenageCommun
				|| natureTiers == NatureTiers.NonHabitant) {
			if (SecurityProvider.isGranted(Role.DI_EMIS_PP)) {
				diListView.setAllowedEmission(true);
			}
			else {
				if (!SecurityProvider.isAnyGranted(Role.DI_DELAI_PP, Role.DI_DUPLIC_PP, Role.DI_QUIT_PP, Role.DI_SOM_PP)) {
					throw new AccessDeniedException("Vous ne possédez pas les droits d'édition des DIs d'une personne physique");
				}
				diListView.setAllowedEmission(false);
			}
		}
		/*
		 * les entreprise ne sont pqs éditable pour le moment else if(natureTiers.equals(Tiers.NATURE_ENTREPRISE)){
		 * if(SecurityProvider.isGranted(Role.ROLE_DI_EMIS_PM)){ diListView.setAllowedEmission(true); } else {
		 * if(!(SecurityProvider.isGranted(Role.ROLE_DI_DELAI_PM) || SecurityProvider.isGranted(Role.ROLE_DI_DUPLIC_PM) ||
		 * SecurityProvider.isGranted(Role.ROLE_DI_QUIT_PM) || SecurityProvider.isGranted(Role.ROLE_DI_SOM_PM))){ throw new
		 * AccessDeniedException("Vous ne possédez pas les droits d'édition des DI d'une entreprise"); }
		 * diListView.setAllowedEmission(false); } }
		 */
		else { // seules les PP et les entreprises ont des DI
			throw new IllegalArgumentException("Tentative d'accès en édition aux DI d'un tiers de type " + natureTiers);
		}

		return new ModelAndView("di/edit/edit-contribuable", model);
	}

	@SuppressWarnings("unchecked")
	private ModelAndView newDiForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws HighProbabilityThatBackWasUsedException, AccessDeniedException {

		final String typeDeclarationImpot = request.getParameter(TYPE_DECLARATION_PARAMETER_NAME);
		final String delaiRetourEnJours = request.getParameter(DELAI_RETOUR_PARAMETER_NAME);
		final String dateDebutString = request.getParameter(DATE_DEBUT_PARAMETER_NAME);
		final String dateFinString = request.getParameter(DATE_FIN_PARAMETER_NAME);

		final Long idCtb = extractLongParam(request, CONTRIBUABLE_ID_PARAMETER_NAME);
		final RegDate dateDebut = RegDateHelper.indexStringToDate(dateDebutString);
		final RegDate dateFin = RegDateHelper.indexStringToDate(dateFinString);

		final DeclarationImpotDetailView diDetailView = checkBean(DeclarationImpotDetailView.class, model.get(getCommandName()), false);

		if (idCtb != null && dateDebut != null && dateFin != null) {

			final DateRange range = new Range(dateDebut, dateFin);

			// vérification des droits d'accès au dossier du contribuable
			checkAccesDossierEnEcriture(idCtb);

			diEditManager.creerDI(idCtb, range, diDetailView);

			if (typeDeclarationImpot != null) {
				diDetailView.setTypeDeclarationImpot(TypeDocument.valueOf(typeDeclarationImpot));
			}
			if (delaiRetourEnJours != null) {
				Integer iDelaiRetourEnJours = new Integer(delaiRetourEnJours);
				RegDate dateJour = RegDate.get();
				RegDate delaiAccorde = dateJour.addDays(iDelaiRetourEnJours);
				diDetailView.setDelaiAccorde(delaiAccorde);
			}

			final NatureTiers natureTiers = diDetailView.getContribuable().getNatureTiers();
			if (natureTiers == NatureTiers.Habitant || natureTiers == NatureTiers.MenageCommun || natureTiers == NatureTiers.NonHabitant) {
				if (!SecurityProvider.isGranted(Role.DI_EMIS_PP)) {
					throw new AccessDeniedException("vous n'avez pas le droit d'émettre une DI");
				}
			}
			else {
				throw new IllegalArgumentException("Tentative de création d'une DI pour un tiers de type " + natureTiers);
			}
		}

		// [UNIREG-832] Si la DI n'a pas pu être créée, on utilise le message d'erreur maintenant
		final String errorMessage = diDetailView.getErrorMessage();
		if (errorMessage != null) {
			errors.reject("global.error.msg", errorMessage);
		}

		return new ModelAndView("di/edit/edit", model);
	}

	@SuppressWarnings("unchecked")
	private ModelAndView editDiForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws HighProbabilityThatBackWasUsedException, AccessDeniedException {

		final String idDiParam = request.getParameter(DI_ID_PARAMETER_NAME);

		final DeclarationImpotDetailView diDetailView = checkBean(DeclarationImpotDetailView.class, model.get(getCommandName()), false);

		if (idDiParam != null) {
			final Long id = extractLongParam(request, DI_ID_PARAMETER_NAME);
			if (id != null) {
				//gestion des droits pour les DIs par diEditManager
				diEditManager.get(id, diDetailView);

				// vérification des droits d'accès au dossier du contribuable
				checkAccesDossierEnEcriture(diDetailView.getContribuable().getNumero());
			}
		}

		return new ModelAndView("di/edit/edit", model);
	}

	private ModelAndView imprimerDI(final HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		final Long noCtb = bean.getContribuable().getNumero();
		checkAccesDossierEnEcriture(noCtb);

		if (!bean.isImprimable()) {
			throw new AccessDeniedException("Vous ne possédez pas les droits nécessaires pour imprimer cette déclaration.");
		}
		// [UNIREG-832] Si la DI n'a pas pu être créée, il ne doit pas être possible de l'imprimer
		final String errorMessage = bean.getErrorMessage();
		if (errorMessage != null) {
			throw new ValidationException(null, errorMessage);
		}

		final TraitementRetourEditique inbox = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				return new ModelAndView("redirect:edit.do?action=listdis&numero=" + noCtb);
			}
		};

		final TraitementRetourEditique erreur = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				final HttpSession session = request.getSession();
				session.setAttribute(ERREUR_COMMUNICATION_EDITIQUE,
						String.format("%s Veuillez imprimer un duplicata de la déclaration d'impôt.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return new ModelAndView("redirect:edit.do?action=listdis&numero=" + noCtb);
			}
		};

		final EditiqueResultat resultat = diEditManager.envoieImpressionLocalDI(bean);
		final ModelAndView mav = traiteRetourEditique(resultat, response, "di", inbox, erreur, erreur);
		if (mav == null && bean.getId() != null) {
			diEditManager.refresh(bean);
		}
		return mav;
	}

	private ModelAndView creerDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotSelectView bean, BindException errors) throws Exception {

		final Long numero = bean.getContribuable().getNumero();
		checkAccesDossierEnEcriture(numero);

		final String rangeAsString = request.getParameter(SELECTION_DI_PARAMETER_NAME);
		if (rangeAsString == null) {
			// [UNIREG-1031]
			errors.reject("global.error.msg", "Veuillez sélectionner une période");
			return null;
		}

		RegDate dateDebut = null;
		RegDate dateFin = null;
		try {
			String[] indexes = rangeAsString.split("-");
			int dateDebutIndex = Integer.parseInt(indexes[0]);
			int dateFinIndex = Integer.parseInt(indexes[1]);
			dateDebut = RegDate.fromIndex(dateDebutIndex, false);
			dateFin = RegDate.fromIndex(dateFinIndex, false);
		}
		catch (NumberFormatException e) {
			// ignored
		}

		if (dateDebut != null && dateFin != null) {
			// le range a été correctement décodé, on continue sur la page de détails
			return new ModelAndView("redirect:edit.do?action=newdi&numero=" + numero + "&debut=" + dateDebut.index() + "&fin="
					+ dateFin.index());
		}
		else {
			// il y a un problème, on reste où on est
			return null;
		}
	}

	private ModelAndView imprimerTO(final HttpServletRequest request, HttpServletResponse response, final DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		final TraitementRetourEditique inbox = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				return new ModelAndView("redirect:edit.do?action=editdi&id=" + bean.getId());
			}
		};

		final TraitementRetourEditique erreur = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				final HttpSession session = request.getSession();
				session.setAttribute(ERREUR_COMMUNICATION_EDITIQUE, String.format("%s Veuillez recommencer plus tard.", EditiqueErrorHelper.getMessageErreurEditique(resultat)));
				return new ModelAndView("redirect:edit.do?action=editdi&id=" + bean.getId());
			}
		};

		final EditiqueResultat resultat = diEditManager.envoieImpressionLocalTaxationOffice(bean);
		final ModelAndView mav = traiteRetourEditique(resultat, response, "to", inbox, erreur, erreur);
		if (mav == null) {

			if (bean.getId() != null) {
				diEditManager.refresh(bean);
			}

			return new ModelAndView("redirect:edit.do?action=listdis&numero=" + bean.getContribuable().getNumero());
		}

		return mav;
	}

	private ModelAndView annulerDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		String depuisTache = request.getParameter(DEPUIS_TACHE_PARAMETER_NAME);
		diEditManager.annulerDI(bean);
		if (depuisTache == null) {
			return new ModelAndView("redirect:edit.do?action=listdis&numero=" + bean.getContribuable().getNumero());
		}
		else {
			return new ModelAndView("redirect:/tache/list.do");
		}
	}

	private ModelAndView maintenirDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());
		String idTacheAsString = request.getParameter(TACHE_ID_PARAMETER_NAME);
		diEditManager.maintenirDI(Long.valueOf(idTacheAsString));
		return new ModelAndView("redirect:/tache/list.do");
	}

	private ModelAndView sommerDI(HttpServletRequest request, HttpServletResponse response, final DeclarationImpotDetailView bean, final BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		final TraitementRetourEditique inbox = new TraitementRetourEditique() {
			@Override
			public ModelAndView doJob(EditiqueResultat resultat) {
				return new ModelAndView("redirect:edit.do?action=editdi&id=" + bean.getId());
			}
		};

		final TraitementRetourEditique erreurTimeout = new ErreurGlobaleCommunicationEditique(errors);
		final EditiqueResultat resultat = diEditManager.envoieImpressionLocalSommationDI(bean);
		final ModelAndView mav = traiteRetourEditique(resultat, response, "sommationDi", inbox, erreurTimeout, erreurTimeout);
		if (mav == null) {
			if (bean.getId() != null) {
				diEditManager.refresh(bean);
			}
		}
		return mav;
	}


	private ModelAndView ajouterDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotListView bean, BindException errors) throws Exception {

		final Long numero = bean.getContribuable().getNumero();
		checkAccesDossierEnEcriture(numero);

		final List<PeriodeImposition> ranges = diEditManager.calculateRangesProchainesDIs(numero);
		if (ranges == null || ranges.isEmpty()) {
			// [UNIREG-832] impossible d'imprimer une nouvelle DI: on reste dans le même écran et on affiche un message d'erreur
			errors.rejectValue(null, null, DeclarationImpotEditManager.CANNOT_ADD_NEW_DI);
			return null;
		}
		else if (ranges.size() == 1) {
			final DateRange range = ranges.get(0);
			// il reste exactement une DI à créer : on continue directement sur l'écran d'impression
			return new ModelAndView("redirect:edit.do?action=newdi&numero=" + numero + "&debut=" + range.getDateDebut().index() + "&fin="
					+ range.getDateFin().index());
		}
		else {
			// [UNIREG-889] il y reste plusieurs DIs à créer : on demande à l'utilisateur de choisir
			return new ModelAndView("redirect:edit.do?action=selectdi&numero=" + numero);
		}
	}

	private ModelAndView sauverDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		diEditManager.save(bean);

		setModified(false);
		return new ModelAndView("redirect:edit.do?action=listdis&numero=" + bean.getContribuable().getNumero());
	}

	private ModelAndView annulerDelai(HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		final String delai = getEventArgument();
		final Long idDelai = Long.parseLong(delai);
		diEditManager.annulerDelai(bean, idDelai);
		if (bean.getId() != null) {
			diEditManager.refresh(bean);
		}
		return null;
	}

	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}
}
