package ch.vd.uniregctb.acces.parDossier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.acces.parDossier.manager.DossierEditRestrictionManager;
import ch.vd.uniregctb.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

/**
 * @author xcifde
 *
 */
public class DossierEditRestrictionController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(DossierEditRestrictionController.class);

	private static final String NUMERO_PARAMETER_NAME = "numero";

	public final static String TARGET_ANNULER_RESTRICTION = "annulerRestriction";

	private DossierEditRestrictionManager dossierEditRestrictionManager;

	public DossierEditRestrictionManager getDossierEditRestrictionManager() {
		return dossierEditRestrictionManager;
	}

	public void setDossierEditRestrictionManager(DossierEditRestrictionManager dossierEditRestrictionManager) {
		this.dossierEditRestrictionManager = dossierEditRestrictionManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isAnyGranted(Role.SEC_DOS_ECR, Role.SEC_DOS_LEC)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour accéder à la sécurité des droits");
		}
		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);

		DossierEditRestrictionView dossierEditRestrictionView = dossierEditRestrictionManager.get(numero);

		return dossierEditRestrictionView;
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
		super.getFormSessionAttributeName();
		DossierEditRestrictionView dossierEditRestrictionView = (DossierEditRestrictionView) command;

		if (getTarget() != null) {
			if (TARGET_ANNULER_RESTRICTION.equals(getTarget()) && SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
				String restriction = getEventArgument();
				Long idRestriction = Long.parseLong(restriction);
				try {
					dossierEditRestrictionManager.annulerRestriction(idRestriction);
				}
				catch (DroitAccesException e) {
					throw new ActionException(e.getMessage());
				}
			}
		}
		return new ModelAndView( new RedirectView("/acces/restrictions-pp.do?numero=" + dossierEditRestrictionView.getDossier().getNumero(), true));

	}
}
