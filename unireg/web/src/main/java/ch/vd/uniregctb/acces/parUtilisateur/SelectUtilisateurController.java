package ch.vd.uniregctb.acces.parUtilisateur;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.acces.parUtilisateur.view.SelectUtilisateurView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class SelectUtilisateurController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(SelectUtilisateurController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {


		SelectUtilisateurView selectUtilisateurView = new SelectUtilisateurView();

		return selectUtilisateurView;
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

		if (!SecurityProvider.isAnyGranted(Role.SEC_DOS_ECR, Role.SEC_DOS_LEC)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour accéder à la sécurité des droits");
		}
		SelectUtilisateurView selectUtilisateurView = (SelectUtilisateurView) command;

		return  new ModelAndView("redirect:restrictions-utilisateur.do?noIndividuOperateur=" + selectUtilisateurView.getNumeroUtilisateur());

	}



}
