package ch.vd.uniregctb.contribuableAssocie;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.contribuableAssocie.manager.ContribuableAssocieEditManager;
import ch.vd.uniregctb.contribuableAssocie.view.ContribuableAssocieEditView;

public class ContribuableAssocieEditController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(ContribuableAssocieEditController.class);

	private final String NUMERO_DEBITEUR_PARAMETER_NAME = "numeroDpi";
	private final String NUMERO_CONTRIBUABLE_PARAMETER_NAME = "numeroContribuable";

	private ContribuableAssocieEditManager contribuableAssocieEditManager;

	public void setContribuableAssocieEditManager(ContribuableAssocieEditManager contribuableAssocieEditManager) {
		this.contribuableAssocieEditManager = contribuableAssocieEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroDebiteurParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);
		String numeroContribuableParam = request.getParameter(NUMERO_CONTRIBUABLE_PARAMETER_NAME);

		Long numeroDebiteur = Long.parseLong(numeroDebiteurParam);
		Long numeroContribuable = Long.parseLong(numeroContribuableParam);
		checkAccesDossierEnLecture(numeroDebiteur);
		checkAccesDossierEnLecture(numeroContribuable);

		//vérification des droits de création de rapport entre tiers non travail par rapportEditManager
		ContribuableAssocieEditView contribuableAssocieEditView = contribuableAssocieEditManager.get(numeroDebiteur, numeroContribuable);

		return contribuableAssocieEditView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		ContribuableAssocieEditView contribuableAssocieEditView = (ContribuableAssocieEditView) command;
		checkAccesDossierEnEcriture(contribuableAssocieEditView.getDebiteur().getNumero());
		checkAccesDossierEnEcriture(contribuableAssocieEditView.getContribuable().getNumero());

		try {
			contribuableAssocieEditManager.save(contribuableAssocieEditView);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			errors.reject(e.getMessage());
			return showForm(request, response, errors);
		}

		mav.setView(new RedirectView("../tiers/visu.do?id=" + contribuableAssocieEditView.getContribuable().getNumero()));

		return mav;
	}



}