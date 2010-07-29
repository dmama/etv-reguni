package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.mouvement.view.BordereauEnvoiReceptionView;

/**
 * Controlleur pour l'overlay qui permet de réceptionner les mouvements d'envoi
 */
public class MouvementMasseDetailReceptionBordereauController extends AbstractMouvementMasseController {

	private final static String ID = "id";

	private final static String RECEPTIONNER = "receptionner";

	@Override
	protected BordereauEnvoiReceptionView formBackingObject(HttpServletRequest request) throws Exception {
		final String idStr = request.getParameter(ID);
		return getMouvementManager().getBordereauPourReception(Long.valueOf(idStr));
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final String receptionner = request.getParameter(RECEPTIONNER);
		if (receptionner != null) {
			checkAccess();
			final BordereauEnvoiReceptionView view = (BordereauEnvoiReceptionView) command;
			getMouvementManager().receptionnerMouvementsEnvoi(view.getSelection());
			getMouvementManager().refreshView(view);
			view.setApresReception(true);
		}
		return showForm(request, response, errors);
	}
}
