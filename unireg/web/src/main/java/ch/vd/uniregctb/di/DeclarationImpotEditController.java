package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.EditiqueErrorHelper;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.DeclarationImpotView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.NatureTiers;

public class DeclarationImpotEditController extends AbstractDeclarationImpotController {

	private static final String ERREUR_COMMUNICATION_EDITIQUE = "erreurCommunicationEditique";

	protected static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditController.class);

	public final static String ACTION_LIST_DIS = "listdis";
	public final static String ACTION_EDIT_DI = "editdi";


	public final static String BUTTON_SAVE_DI = "__confirmed_save";
	public final static String BUTTON_SOMMER_DI = "sommer";
	public final static String TARGET_ANNULER_DELAI = "annulerDelai";
	public final static String BUTTON_IMPRIMER_TO = "imprimerTO";
	public final static String BUTTON_MAINTENIR_DI = "maintenir";
	public final static String TACHE_ID_PARAMETER_NAME = "idTache";

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public final static String DI_ID_PARAMETER_NAME = "id";
	public final static String CONTRIBUABLE_ID_PARAMETER_NAME = "numero";

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
		if (request.getParameter(BUTTON_SOMMER_DI) != null || request.getParameter(BUTTON_MAINTENIR_DI) != null) {
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
				}
				else {
					if (request.getParameter(BUTTON_SOMMER_DI) != null) {
						mav = sommerDI(request, response, checkBean(DeclarationImpotDetailView.class, command, true), errors);
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

	private ModelAndView sauverDI(HttpServletRequest request, HttpServletResponse response, DeclarationImpotDetailView bean, BindException errors) throws Exception {

		checkAccesDossierEnEcriture(bean.getContribuable().getNumero());

		diEditManager.save(bean.getContribuable().getNumero(), bean.getId(), bean.getRegDateDebutPeriodeImposition(), bean.getRegDateFinPeriodeImposition(), bean.getTypeDeclarationImpot(), bean.getTypeAdresseRetour(),
				bean.getRegDelaiAccorde(), bean.getRegDateRetour());

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
