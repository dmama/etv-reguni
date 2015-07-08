package ch.vd.uniregctb.rt;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.rt.manager.RapportPrestationEditManager;
import ch.vd.uniregctb.rt.view.SourcierListView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.utils.WebContextUtils;

public class SourcierListController  extends  AbstractTiersListController implements MessageSourceAware {

	protected final Logger LOGGER = LoggerFactory.getLogger(SourcierListController.class);

	private static final String NUMERO_DEBITEUR_PARAMETER_NAME = "numeroDpi";

	public static final String ACTION_PARAMETER_NAME = "action";

	public static final String SOURCIER_CRITERIA_NAME = "sourcierCriteria";
	public static final String SOURCIER_LIST_ATTRIBUTE_NAME = "list";

	private MessageSource messageSource;

	private RapportPrestationEditManager rapportPrestationEditManager;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		// vérification droit création rapport de travail
		if (!SecurityHelper.isGranted(securityProvider, Role.RT)) {
			throw new AccessDeniedException("Vous ne possédez pas le droit de créer un rapport de travail");
		}

		final String numeroDpiParam = request.getParameter(NUMERO_DEBITEUR_PARAMETER_NAME);
		final Long numeroDpi = Long.parseLong(numeroDpiParam);

		if (!rapportPrestationEditManager.isExistingTiers(numeroDpi)) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.debiteur.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		final HttpSession session = request.getSession();

		SourcierListView bean = (SourcierListView) session.getAttribute(SOURCIER_CRITERIA_NAME);
		if (bean == null || bean.getNumeroDebiteur() == null || !bean.getNumeroDebiteur().equals(numeroDpi)) {
			session.removeAttribute(SOURCIER_CRITERIA_NAME);
			bean = rapportPrestationEditManager.getSourcierList(numeroDpi);
	 	}

		final String provenance = request.getParameter("provenance");
		if (StringUtils.isNotBlank(provenance)) {
			bean.setProvenance(provenance);
		}
		else {
			bean.setProvenance("debiteur");
		}

		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
		HttpSession session = request.getSession();
		SourcierListView bean = (SourcierListView) session.getAttribute(SOURCIER_CRITERIA_NAME);
		return showFormForList(request, response, errors, model, SOURCIER_CRITERIA_NAME, SOURCIER_LIST_ATTRIBUTE_NAME, bean, true);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		SourcierListView bean = (SourcierListView) command;
		HttpSession session = request.getSession();
		session.setAttribute(SOURCIER_CRITERIA_NAME, bean);

		String view = "list-sourcier.do?numeroDpi=" + bean.getNumeroDebiteur();
		if (StringUtils.isNotBlank(bean.getProvenance())) {
			view += "&provenance=" + bean.getProvenance();
		}

		mav.setView(new RedirectView(view));
		return mav;
	}

	public void setRapportPrestationEditManager(RapportPrestationEditManager rapportPrestationEditManager) {
		this.rapportPrestationEditManager = rapportPrestationEditManager;
	}

	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
