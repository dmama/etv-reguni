package ch.vd.uniregctb.acces.parUtilisateur;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.uniregctb.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class RecapPersonneUtilisateurController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(RecapPersonneUtilisateurController.class);

	public final static String TYPE_DROIT_ACCES_NOM_MAP_NAME = "typesDroitAcces";

	private static final String NUMERO_INDIVIDU_OPERATEUR_PARAMETER_NAME = "noIndividuOperateur";
	private static final String NUMERO_PARAMETER_NAME = "numero";

	public final static String TARGET_ANNULER_RESTRICTION = "annulerRestriction";

	private UtilisateurEditRestrictionManager utilisateurEditRestrictionManager;
	private TiersMapHelper tiersMapHelper;

	public UtilisateurEditRestrictionManager getUtilisateurEditRestrictionManager() {
		return utilisateurEditRestrictionManager;
	}

	public void setUtilisateurEditRestrictionManager(UtilisateurEditRestrictionManager utilisateurEditRestrictionManager) {
		this.utilisateurEditRestrictionManager = utilisateurEditRestrictionManager;
	}

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(TYPE_DROIT_ACCES_NOM_MAP_NAME, tiersMapHelper.getDroitAcces());

		return data;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour modifier la sécurité des droits");
		}
		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);
		String noIndividuOperateurParam = request.getParameter(NUMERO_INDIVIDU_OPERATEUR_PARAMETER_NAME);
		Long noIndividuOperateur = Long.parseLong(noIndividuOperateurParam);

		RecapPersonneUtilisateurView recapPersonneUtilisateurView = utilisateurEditRestrictionManager.get(numero, noIndividuOperateur);

		return recapPersonneUtilisateurView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		RecapPersonneUtilisateurView recapPersonneUtilisateurView = (RecapPersonneUtilisateurView) command;

		try {
			utilisateurEditRestrictionManager.save(recapPersonneUtilisateurView);
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}

		return new ModelAndView( new RedirectView("/acces/restrictions-utilisateur.do?noIndividuOperateur=" + recapPersonneUtilisateurView.getUtilisateur().getNumeroIndividu(), true));

	}
}
