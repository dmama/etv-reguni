package ch.vd.uniregctb.lr;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.lr.manager.ListeRecapEditManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class ListeRecapEditDelaiController  extends AbstractListeRecapController {

	private ListeRecapEditManager lrEditManager;

	public final static String LR_ID = "idLR";

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapEditDelaiController.class);
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if(!SecurityProvider.isGranted(Role.LR)){
			throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
		}
		String idLrParam = request.getParameter(LR_ID);
		Long idLr = Long.parseLong(idLrParam);
		HttpSession session = request.getSession();
		session.setAttribute(LR_ID, idLr);

		lrEditManager.controleLR(idLr);

		DelaiDeclaration  echeance = new DelaiDeclaration();
		return echeance;
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
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		HttpSession session = request.getSession();
		Long idLr = (Long) session.getAttribute(LR_ID);

		DelaiDeclaration delai = (DelaiDeclaration) command;
		lrEditManager.saveDelai(idLr, delai);
		return mav;
	}

	public ListeRecapEditManager getLrEditManager() {
		return lrEditManager;
	}

	public void setLrEditManager(ListeRecapEditManager lrEditManager) {
		this.lrEditManager = lrEditManager;
	}

}
