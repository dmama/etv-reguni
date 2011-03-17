package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractCommandController;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.manager.TiersVisuManager;
import ch.vd.uniregctb.tiers.view.RapportsPrestationView;

public class RapportsPrestationListController extends AbstractCommandController {

	private TiersVisuManager manager;

	public RapportsPrestationListController() {
		setCommandClass(RapportsPrestationView.class);
	}

	@Override
	protected ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		if(!SecurityProvider.isGranted(Role.VISU_ALL)){
			throw new AccessDeniedException("vous ne possédez pas le droit IfoSec de consultation des débiteurs de prestations imposables");
		}

		final RapportsPrestationView bean = (RapportsPrestationView) command;
		final Long id = Long.valueOf(request.getParameter("idDpi"));
		ControllerUtils.checkAccesDossierEnLecture(id);

		manager.fillRapportsPrestationView(id, bean);

		return new ModelAndView("tiers/visualisation/rt/list", errors.getModel());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setManager(TiersVisuManager manager) {
		this.manager = manager;
	}
}
