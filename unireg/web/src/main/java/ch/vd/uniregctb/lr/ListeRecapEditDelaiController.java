package ch.vd.uniregctb.lr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.di.view.DelaiDeclarationView;
import ch.vd.uniregctb.lr.manager.ListeRecapEditManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

public class ListeRecapEditDelaiController extends AbstractListeRecapController {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapEditDelaiController.class);

	private ListeRecapEditManager lrEditManager;

	public final static String LR_ID = "idLR";

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		if (!SecurityProvider.isGranted(Role.LR)) {
			throw new AccessDeniedException("vous n'avez pas le droit d'accéder aux listes récapitulatives pour l'application Unireg");
		}
		String idLrParam = request.getParameter(LR_ID);
		Long idLr = Long.parseLong(idLrParam);
		HttpSession session = request.getSession();
		session.setAttribute(LR_ID, idLr);

		return lrEditManager.creerDelai(idLr);
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		DelaiDeclarationView delai = (DelaiDeclarationView) command;
		lrEditManager.saveDelai(delai);
		return new ModelAndView("redirect:edit.do?id=" + delai.getIdDeclaration());
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLrEditManager(ListeRecapEditManager lrEditManager) {
		this.lrEditManager = lrEditManager;
	}

}
