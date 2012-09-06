package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.DeclarationImpotListView;
import ch.vd.uniregctb.di.view.DeclarationImpotView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.NatureTiers;

public class DeclarationImpotEditController extends AbstractDeclarationImpotController {

	private static final Logger LOGGER = Logger.getLogger(DeclarationImpotEditController.class);

	public final static String ACTION_LIST_DIS = "listdis";

	/**
	 * Le nom du parametre utilise dans la request.
	 */
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
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		Object object = null;

		final String action = request.getParameter(ACTION_PARAMETER_NAME);
		if (action == null || ACTION_LIST_DIS.equals(action)) {
			object = new DeclarationImpotListView();
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

	public void setDiEditManager(DeclarationImpotEditManager diEditManager) {
		this.diEditManager = diEditManager;
	}
}
