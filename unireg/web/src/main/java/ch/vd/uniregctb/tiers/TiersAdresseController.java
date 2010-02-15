package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.AdresseManager;
import ch.vd.uniregctb.tiers.view.AdresseView;

public class TiersAdresseController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(TiersAdresseController.class);

	private AdresseManager adresseManager;

	private final static String NUMERO_CTB_PARAMETER_NAME = "numero";

	private final static String ID_ADRESSE_PARAMETER_NAME = "idAdresse";


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		AdresseView adresseView = new AdresseView();
		String idAdr =  request.getParameter(ID_ADRESSE_PARAMETER_NAME);
		Long numeroCtb = extractLongParam(request, NUMERO_CTB_PARAMETER_NAME);
		checkAccesDossierEnLecture(numeroCtb);

		//les droits sont vérifiés lors de la sauvegarde (TiersAdresseValidator)
		if ( (idAdr != null && !"".equals(idAdr.trim())))  {
			Long id = Long.parseLong(idAdr);
			adresseView = adresseManager.getAdresseView(id);
		}
		else {
			if (numeroCtb != null) {
				adresseView = adresseManager.create(numeroCtb);
			}
		}

		return adresseView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
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
		AdresseView adresseView = (AdresseView) command;
		checkAccesDossierEnEcriture(adresseView.getNumCTB());

		if (("reprise".equals(adresseView.getMode())) || ("repriseCivil".equals(adresseView.getMode()))) {
			adresseManager.saveReprise(adresseView);
		}
		else {
			adresseManager.save(adresseView);
		}

		return mav;
	}


	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}



}
