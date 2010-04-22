package ch.vd.uniregctb.mouvement;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.mouvement.manager.MouvementEditManager;
import ch.vd.uniregctb.mouvement.view.MouvementListView;

public class MouvementEditContribuableController extends AbstractMouvementController{

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public final static String NUMERO_CONTRIBUABLE_PARAMETER_NAME = "numero";

	public final static String TARGET_ANNULER_MOUVEMENT = "annulerMvt";

	private MouvementEditManager mouvementEditManager;

	public MouvementEditManager getMouvementEditManager() {
		return mouvementEditManager;
	}

	public void setMouvementEditManager(MouvementEditManager mouvementEditManager) {
		this.mouvementEditManager = mouvementEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		MouvementListView  mvtListView = null;
		String idParam = request.getParameter(NUMERO_CONTRIBUABLE_PARAMETER_NAME);

		if (idParam != null) {
			Long id = Long.parseLong(idParam);
			if (idParam != null && !"".equals(idParam)) {
				checkAccesDossierEnLecture(id);
				mvtListView = mouvementEditManager.findByNumeroDossier(id, true);
			}
		}

		return mvtListView;
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
		//bouton ajouter un mvt
		super.getFormSessionAttributeName();
		MouvementListView mvtListView = (MouvementListView) command;
		checkAccesDossierEnEcriture(mvtListView.getContribuable().getNumero());

		if (getTarget() != null) {
			if (TARGET_ANNULER_MOUVEMENT.equals(getTarget())) {
				final String mvt = getEventArgument();
				final long idMvt = Long.parseLong(mvt);
				mouvementEditManager.annulerMvt(idMvt);
				return new ModelAndView( new RedirectView("/mouvement/edit-contribuable.do?numero=" + mvtListView.getContribuable().getNumero(), true));
			}
		}

		return new ModelAndView( new RedirectView("/mouvement/edit.do?numero=" + mvtListView.getContribuable().getNumero(), true));
	}


}
