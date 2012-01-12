package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.AdresseManager;
import ch.vd.uniregctb.tiers.view.AdresseView;

public class TiersAdresseController extends AbstractTiersController {

	protected final Logger LOGGER = Logger.getLogger(TiersAdresseController.class);

	private final static String NUMERO_CTB_PARAMETER_NAME = "numero";
	private final static String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	private AdresseManager adresseManager;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		final Long idAdr = extractLongParam(request, ID_ADRESSE_PARAMETER_NAME);
		final Long numeroCtb = extractLongParam(request, NUMERO_CTB_PARAMETER_NAME);
		checkAccesDossierEnLecture(numeroCtb);

		//les droits sont vérifiés lors de la sauvegarde (TiersAdresseValidator)
		final AdresseView adresseView;
		if (idAdr != null) {
			adresseView = adresseManager.getAdresseView(idAdr);
		}
		else if (numeroCtb != null) {
			adresseView = adresseManager.create(numeroCtb);
		}
		else {
			adresseView = new AdresseView();
		}

		return adresseView;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final AdresseView adresseView = (AdresseView) command;
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
