package ch.vd.unireg.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractCommandController;

import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.manager.TiersVisuManager;
import ch.vd.unireg.tiers.view.RapportsPrestationView;

public class RapportsPrestationListController extends AbstractCommandController {

	private TiersVisuManager manager;
	private ControllerUtils controllerUtils;
	private SecurityProviderInterface securityProvider;

	public RapportsPrestationListController() {
		setCommandClass(RapportsPrestationView.class);
	}

	@Override
	protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		if(!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)){
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de consultation des débiteurs de prestations imposables");
		}

		final RapportsPrestationView bean = (RapportsPrestationView) command;
		final Long id = Long.valueOf(request.getParameter("idDpi"));
		controllerUtils.checkAccesDossierEnLecture(id);

		manager.fillRapportsPrestationView(id, bean);

		return new ModelAndView("tiers/visualisation/rt/list", errors.getModel());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(TiersVisuManager manager) {
		this.manager = manager;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
