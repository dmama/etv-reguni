package ch.vd.uniregctb.param;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.param.manager.ParamApplicationManager;
import ch.vd.uniregctb.param.view.ParamApplicationView;
import ch.vd.uniregctb.parametrage.ParametreEnum;

public class ParamApplicationController extends AbstractSimpleFormController {

	protected static final Logger LOGGER = Logger.getLogger(ParamApplicationController.class);

	private ParamApplicationManager manager;
	
	public ParamApplicationManager getParamApplicationManager() {
		return manager;
	}

	public void setParamApplicationManager(
			ParamApplicationManager paramApplicationManager) {
		this.manager = paramApplicationManager;
	}
	
	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		Map<String, Object> referenceData = new HashMap<String, Object> ();
		for (ParametreEnum p : ParametreEnum.values()) {
			referenceData.put(p.toString() + "ParDefaut", manager.getDefaut(p));
		}
		return referenceData;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return manager.getForm();
	}

	/**
	 * 
	 * Initialisation des valeurs pas défaut des paramètres dans le scope application
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
		
		// Initialise les valeurs par défaut des parametres d'UNIREG dans le scope application si cela n'a pas deja été fait
		if (request.getSession().getServletContext().getAttribute("initDefaultParamOk") == null) {
			synchronized (ParamApplicationController.class) {
				if (request.getSession().getServletContext().getAttribute("initDefaultParamOk") == null) {
					for (ParametreEnum p : ParametreEnum.values()) {
						request.getSession().getServletContext().setAttribute(p.toString() + "ParDefaut", manager.getDefaut(p));
					}
					request.getSession().getServletContext().setAttribute("initDefaultParamOk", Boolean.TRUE);
				}
			}
		}
		
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		ParamApplicationView form = (ParamApplicationView)command;
		switch (form.getAction()) {
			case save:
				manager.save(form);
				break;
			case reset:
				manager.reset();
				// Mise à jour du formBackingObject avec les valeurs par défaut
				BeanUtils.copyProperties(manager.getForm(), form);
				break;
			default:
				LOGGER.warn("action invalide : " + form.getAction());
		}
		return mav;
	}
	
}
