package ch.vd.uniregctb.tiers;

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

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		AdresseView adresseView = (AdresseView) command;
		checkAccesDossierEnEcriture(adresseView.getNumCTB());

		if (("reprise".equals(adresseView.getMode())) || ("repriseCivil".equals(adresseView.getMode()))) {
			adresseManager.saveReprise(adresseView);
		}
		else {
			adresseManager.save(adresseView);
		}

		return new ModelAndView("redirect:../adresses/edit.do?id=" + adresseView.getNumCTB());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}
}
