package ch.vd.uniregctb.acces.parUtilisateur;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class UtilisateurEditRestrictionController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(UtilisateurEditRestrictionController.class);

	private final String NUMERO_PARAMETER_NAME = "noIndividuOperateur";

	public final static String TARGET_ANNULER_RESTRICTION = "annulerRestriction";

	private final String EXPORTER = "exporter";


	private UtilisateurEditRestrictionManager utilisateurEditRestrictionManager;

	public UtilisateurEditRestrictionManager getUtilisateurEditRestrictionManager() {
		return utilisateurEditRestrictionManager;
	}

	public void setUtilisateurEditRestrictionManager(UtilisateurEditRestrictionManager utilisateurEditRestrictionManager) {
		this.utilisateurEditRestrictionManager = utilisateurEditRestrictionManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR) && !SecurityProvider.isGranted(Role.SEC_DOS_LEC)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour accéder à la sécurité des droits");
		}
		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);

		UtilisateurEditRestrictionView utilisateurEditRestrictionView = utilisateurEditRestrictionManager.get(numero);

		return utilisateurEditRestrictionView;
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
		UtilisateurEditRestrictionView utilisateurEditRestrictionView = (UtilisateurEditRestrictionView) command;

		if (getTarget() != null) {
			if (TARGET_ANNULER_RESTRICTION.equals(getTarget()) && SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
				String restriction = getEventArgument();
				Long idRestriction = Long.parseLong(restriction);
				try {
					utilisateurEditRestrictionManager.annulerRestriction(idRestriction);
				}
				catch (DroitAccesException e) {
					throw new ActionException(e.getMessage());
				}
			}
		}
		if (request.getParameter(EXPORTER) != null) {
			String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
			final Long numero = Long.parseLong(numeroParam);
			final ExtractionJob job = utilisateurEditRestrictionManager.exportListeDroitsAcces(numero);
			flash(String.format("Demande d'export enregistrée (%s)", job.getDescription()));
		}
		return new ModelAndView(new RedirectView("/acces/restrictions-utilisateur.do?noIndividuOperateur=" + utilisateurEditRestrictionView.getUtilisateur().getNumeroIndividu(), true));

	}
}
