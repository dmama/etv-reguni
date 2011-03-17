package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.manager.DeclarationImpotVisuManager;

public class DeclarationImpotVisuController extends AbstractTiersController {

	//protected final Logger LOGGER = Logger.getLogger(DeclarationImpotVisuController.class);

	private static final String ID_DI_PARAM = "idDi";

	private DeclarationImpotVisuManager diVisuManager;


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		DeclarationImpotDetailView diView = null;

		String idDiParam = request.getParameter(ID_DI_PARAM);
		if (idDiParam != null && !"".equals(idDiParam)) {
			if(!SecurityProvider.isGranted(Role.VISU_ALL) &&
					!SecurityProvider.isGranted(Role.VISU_LIMITE)){
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
			}
			Long id = Long.parseLong(idDiParam);
			diView = diVisuManager.get(id);
		}

		// vérification des droits d'accès au dossier du contribuable
		checkAccesDossierEnLecture(diView.getContribuable().getNumero());

		return diView;
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

	public DeclarationImpotVisuManager getDiVisuManager() {
		return diVisuManager;
	}

	public void setDiVisuManager(DeclarationImpotVisuManager diVisuManager) {
		this.diVisuManager = diVisuManager;
	}

}
