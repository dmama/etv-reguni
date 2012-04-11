package ch.vd.uniregctb.activation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.activation.manager.TiersAnnulationRecapManager;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;

public class TiersAnnulationRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(TiersAnnulationRecapController.class);

	private static final String NUMERO_PARAMETER_NAME = "numero";
	private static final String NUMERO_REMPLACANT_PARAMETER_NAME = "numeroRemplacant";
	private TiersAnnulationRecapManager tiersAnnulationRecapManager;

	public void setTiersAnnulationRecapManager(TiersAnnulationRecapManager tiersAnnulationRecapManager) {
		this.tiersAnnulationRecapManager = tiersAnnulationRecapManager;
	}

	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		super.initBinder(request, binder);
		final Locale locale = request.getLocale();
		final NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(false); // pour éviter d'afficher des virgules dans le numéro de contribuable
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);
		checkAccesDossierEnLecture(numero);
		String numeroRemplacantParam = request.getParameter(NUMERO_REMPLACANT_PARAMETER_NAME);

		TiersAnnulationRecapView tiersAnnulationRecapView = null;
		if (numeroRemplacantParam != null) {
			Long numeroRemplacant = Long.parseLong(numeroRemplacantParam);
			tiersAnnulationRecapView = tiersAnnulationRecapManager.get(numero, numeroRemplacant);
		}
		else {
			tiersAnnulationRecapView = tiersAnnulationRecapManager.get(numero);
		}
		return tiersAnnulationRecapView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		TiersAnnulationRecapView tiersAnnulationRecapView = (TiersAnnulationRecapView) command;
		checkAccesDossierEnEcriture(tiersAnnulationRecapView.getNumeroTiers());

		tiersAnnulationRecapManager.save(tiersAnnulationRecapView);
		if (tiersAnnulationRecapView.getNumeroTiersRemplacant() == null) {
			return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + tiersAnnulationRecapView.getNumeroTiers(), true));
		}
		else
		{
			return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + tiersAnnulationRecapView.getNumeroTiersRemplacant(), true));
		}

	}

}
