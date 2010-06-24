package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.manager.ListeRecapVisuManager;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet metier donne.
 *
 * @author XCIFDE
 */
public class ListeRecapVisuController extends AbstractTiersController {

	//protected final Logger LOGGER = Logger.getLogger(ListeRecapVisuController.class);

	private static final String ID_LR_PARAM = "idLr";

	private ListeRecapVisuManager lrVisuManager;

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		ListeRecapDetailView lrView = null;

		String idLrParam = request.getParameter(ID_LR_PARAM);
		if (idLrParam != null&& !"".equals(idLrParam)) {
			if(SecurityProvider.isGranted(Role.VISU_LIMITE)){
				throw new AccessDeniedException("vous n'avez pas le droit de consulter un " +
					"débiteur de prestation imposable pour l'application Unireg");
			}
			Long id = Long.parseLong(idLrParam);
			lrView = lrVisuManager.get(id);
			checkAccesDossierEnLecture(lrView.getDpi().getNumero());
		}

		return lrView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
		throws Exception {
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

	public ListeRecapVisuManager getLrVisuManager() {
		return lrVisuManager;
	}

	public void setLrVisuManager(ListeRecapVisuManager lrVisuManager) {
		this.lrVisuManager = lrVisuManager;
	}

}
