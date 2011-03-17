package ch.vd.uniregctb.rt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.rt.manager.RapportPrestationEditManager;
import ch.vd.uniregctb.rt.view.DebiteurListView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.utils.WebContextUtils;

public class DebiteurListController  extends  AbstractTiersListController implements MessageSourceAware {

	protected final Logger LOGGER = Logger.getLogger(DebiteurListController.class);

	private final String NUMERO_SOURCIER_PARAMETER_NAME = "numeroSrc";

	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String ACTION_PARAMETER_EFFACER = "effacer";
	public static final String ACTION_PARAMETER_RECHERCHER = "rechercher";

	public static final String DEBITEUR_CRITERIA_NAME = "debiteurCriteria";
	public static final String DEBITEUR_LIST_ATTRIBUTE_NAME = "list";

	private MessageSource messageSource;

	private RapportPrestationEditManager rapportPrestationEditManager;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		// vérification droit création rapport de travail
		if (!SecurityProvider.isGranted(Role.RT)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit de créer un rapport de travail");
		}

		final HttpSession session = request.getSession();
		final String action = request.getParameter(ACTION_PARAMETER_NAME);

		final String numeroSrcParam = request.getParameter(NUMERO_SOURCIER_PARAMETER_NAME);
		final Long numeroSrc = Long.parseLong(numeroSrcParam);

		if (!rapportPrestationEditManager.isExistingTiers(numeroSrc)) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.sourcier.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		final Niveau acces = rapportPrestationEditManager.getAccessLevel(numeroSrc);
		if (acces == null || acces == Niveau.LECTURE) {
			throw new AccessDeniedException("Vous ne possédez pas le droit de créer un rapport de travail sur ce contribuable");
		}

		DebiteurListView bean = (DebiteurListView) session.getAttribute(DEBITEUR_CRITERIA_NAME);
		if (bean == null || (action != null && action.equals(EFFACER_PARAMETER_VALUE))) {
			bean = rapportPrestationEditManager.getDebiteurList(numeroSrc);
		}

		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {

		final HttpSession session = request.getSession();
		final DebiteurListView bean = (DebiteurListView) session.getAttribute(DEBITEUR_CRITERIA_NAME);
		final ModelAndView mav = showFormForList(request, response, errors, model, DEBITEUR_CRITERIA_NAME, DEBITEUR_LIST_ATTRIBUTE_NAME, bean, true);
		session.removeAttribute(DEBITEUR_CRITERIA_NAME);
		return mav;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		final ModelAndView mav = super.onSubmit(request, response, command, errors);

		final DebiteurListView bean = (DebiteurListView) command;
		final HttpSession session = request.getSession();
		session.setAttribute(DEBITEUR_CRITERIA_NAME, bean);

		if (request.getParameter(BOUTON_RECHERCHER) != null) {
			mav.setView(new RedirectView("list-debiteur.do?numeroSrc=" + bean.getNumeroSourcier()));
		} else if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("list-debiteur.do?numeroSrc=" + bean.getNumeroSourcier() + "&action=effacer"));
		}
		return mav;
	}

	public void setRapportPrestationEditManager(RapportPrestationEditManager rapportPrestationEditManager) {
		this.rapportPrestationEditManager = rapportPrestationEditManager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
