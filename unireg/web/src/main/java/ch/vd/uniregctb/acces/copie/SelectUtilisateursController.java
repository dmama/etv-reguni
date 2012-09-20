package ch.vd.uniregctb.acces.copie;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.acces.copie.view.SelectUtilisateursView;
import ch.vd.uniregctb.acces.parUtilisateur.SelectUtilisateurController;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.TiersMapHelper;
import ch.vd.uniregctb.type.TypeOperation;

public class SelectUtilisateursController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(SelectUtilisateurController.class);

	private TiersMapHelper tiersMapHelper;

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public static final String TYPE_OPERATION_MAP_NAME = "typesOperation";

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();

		data.put(TYPE_OPERATION_MAP_NAME, getTiersMapHelper().getTypeOperation());

		return data;
	}


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec pour modifier la sécurité des droits");
		}
		
		SelectUtilisateursView selectUtilisateursView = new SelectUtilisateursView();
		selectUtilisateursView.setTypeOperation(TypeOperation.COPIE);
		return selectUtilisateursView;
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
		SelectUtilisateursView selectUtilisateursView = (SelectUtilisateursView) command;

		return  new ModelAndView("redirect:confirm-copie.do?noOperateurReference=" + selectUtilisateursView.getNumeroUtilisateurReference()
														+ "&noOperateurDestination=" + selectUtilisateursView.getNumeroUtilisateurDestination()
														+ "&typeOperation=" + selectUtilisateursView.getTypeOperation().toString());

	}

}
