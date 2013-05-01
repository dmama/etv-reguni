package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.tiers.manager.AdresseManager;
import ch.vd.uniregctb.tiers.manager.TiersEditManager;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;

public class CloseAdressesController extends AbstractTiersController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(CloseAdressesController.class);

	public final static String TARGET_FERMER_ADRESSE = "fermerAdresse";

	private static final String ID_ADRESSE_PARAMETER_NAME = "idAdresse";

	private TiersEditManager tiersEditManager;

	private AdresseManager adresseManager;

	public TiersEditManager getTiersEditManager() {
		return tiersEditManager;
	}

	public void setTiersEditManager(TiersEditManager tiersEditManager) {
		this.tiersEditManager = tiersEditManager;
	}

	public AdresseManager getAdresseManager() {
		return adresseManager;
	}

	public void setAdresseManager(AdresseManager adresseManager) {
		this.adresseManager = adresseManager;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		AdresseView adresseAFermerView = new AdresseView();
		String idParam = request.getParameter(ID_ADRESSE_PARAMETER_NAME);
		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (!"".equals(idParam)) {
				adresseAFermerView = adresseManager.getAdresseView(id);

				if (adresseAFermerView != null) {
					//gestion des droits d'édition d'un tier par tiersEditManager
					checkAccesDossierEnLecture(adresseAFermerView.getNumCTB());

				}

			}
		}
		return adresseAFermerView;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		AdresseView bean = (AdresseView) command;
		checkAccesDossierEnEcriture(bean.getNumCTB());
		adresseManager.fermerAdresse(bean);
		//tiersEditManager.refresh(bean, bean.getTiers().getNumero());		
		return new ModelAndView("redirect:edit.do?id=" + bean.getNumCTB());
	}


}