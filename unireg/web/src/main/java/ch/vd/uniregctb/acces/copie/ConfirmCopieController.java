package ch.vd.uniregctb.acces.copie;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.acces.copie.manager.CopieDroitAccesManager;
import ch.vd.uniregctb.acces.copie.view.ConfirmCopieView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class ConfirmCopieController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(ConfirmCopieController.class);

	private final String NUMERO_OPERATEUR_REFERENCE_PARAMETER_NAME = "noOperateurReference";
	private final String NUMERO_OPERATEUR_DESTINATION_PARAMETER_NAME = "noOperateurDestination";

	public final static String BOUTON_COPIER = "copier";
	public final static String BOUTON_TRANSFERER = "transferer";

	private CopieDroitAccesManager copieDroitAccesManager;

	public CopieDroitAccesManager getCopieDroitAccesManager() {
		return copieDroitAccesManager;
	}

	public void setCopieDroitAccesManager(CopieDroitAccesManager copieDroitAccesManager) {
		this.copieDroitAccesManager = copieDroitAccesManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour modifier la sécurité des droits");
		}
		String noOperateurReferenceParam = request.getParameter(NUMERO_OPERATEUR_REFERENCE_PARAMETER_NAME);
		Long noOperateurReference = Long.parseLong(noOperateurReferenceParam);
		String noOperateurDestinationParam = request.getParameter(NUMERO_OPERATEUR_DESTINATION_PARAMETER_NAME);
		Long noOperateurDestination = Long.parseLong(noOperateurDestinationParam);

		ConfirmCopieView confirmCopieView = copieDroitAccesManager.get(noOperateurReference, noOperateurDestination);

		return confirmCopieView;
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
		ConfirmCopieView confirmCopieView = (ConfirmCopieView) command;

		try {
			if (request.getParameter(BOUTON_COPIER) != null) {
				copieDroitAccesManager.copie(confirmCopieView);
			}
			else if (request.getParameter(BOUTON_TRANSFERER) != null) {
				copieDroitAccesManager.transfert(confirmCopieView);
			}
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}

		return new ModelAndView( new RedirectView("/acces/restrictions-utilisateur.do?noIndividuOperateur=" + confirmCopieView.getUtilisateurDestinationView().getNumeroIndividu(), true));

	}
}