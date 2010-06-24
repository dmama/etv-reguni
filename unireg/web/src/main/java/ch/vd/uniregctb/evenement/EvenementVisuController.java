package ch.vd.uniregctb.evenement;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.evenement.view.EvenementView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class EvenementVisuController extends AbstractEvenementController {

	public final static String BOUTON_RECYCLER = "recycler";
	public final static String BOUTON_FORCER = "forcer";

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(EvenementVisuController.class);

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		if (!SecurityProvider.isGranted(Role.EVEN)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de gestion des évènements civils");
		}

		EvenementView evtView = null;
		String idParam = request.getParameter(EVENEMENT_ID_PARAMETER_NAME);
		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (idParam != null && !"".equals(idParam)) {
				evtView = getEvenementManager().get(id);
			}
		}
		return evtView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
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
		super.onSubmit(request, response, command, errors);
		String success = getSuccessView();
		EvenementView bean = (EvenementView) command;

		if (request.getParameter(BOUTON_RECYCLER) != null) {
			getEvenementManager().traiteEvenementCivil(bean.getEvenement().getId());
		}
		else if (request.getParameter(BOUTON_FORCER) != null) {
			getEvenementManager().forceEtatTraite(bean.getEvenement().getId());
		}
		super.getFormSessionAttributeName();


		success = "visu.do?id=" + bean.getEvenement().getId();
		return new ModelAndView(new RedirectView(success));
	}

}
