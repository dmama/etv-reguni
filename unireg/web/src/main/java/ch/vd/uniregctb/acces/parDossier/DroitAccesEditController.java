package ch.vd.uniregctb.acces.parDossier;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.acces.parDossier.manager.DossierEditRestrictionManager;
import ch.vd.uniregctb.acces.parDossier.view.DroitAccesView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesEditController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(DroitAccesEditController.class);

	public final static String TYPE_DROIT_ACCES_NOM_MAP_NAME = "typesDroitAcces";

	private DossierEditRestrictionManager dossierEditRestrictionManager;

	private TiersMapHelper tiersMapHelper;

	public DossierEditRestrictionManager getDossierEditRestrictionManager() {
		return dossierEditRestrictionManager;
	}

	public void setDossierEditRestrictionManager(DossierEditRestrictionManager dossierEditRestrictionManager) {
		this.dossierEditRestrictionManager = dossierEditRestrictionManager;
	}

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}



	private final String NUMERO_PARAMETER_NAME = "numero";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour modifier la sécurité des droits");
		}
		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);

		DroitAccesView droitAccesView = new DroitAccesView();
		droitAccesView.setNumero(numero);
		droitAccesView.setType(TypeDroitAcces.AUTORISATION);

		return droitAccesView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(TYPE_DROIT_ACCES_NOM_MAP_NAME, tiersMapHelper.getDroitAcces());

		return data;
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
		DroitAccesView droitAccesView = (DroitAccesView) command;

		try {
			dossierEditRestrictionManager.save(droitAccesView);
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage());
		}

		return mav;

	}


}
