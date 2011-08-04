package ch.vd.uniregctb.param;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.param.manager.ParamPeriodeManager;

import static ch.vd.uniregctb.param.Commun.getFeuilleIdFromRequest;
import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.getModeleIdFromRequest;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;


public class ParamModeleFeuilleDocumentSupprController extends AbstractController {

	private ParamPeriodeManager manager;
	private MessageSource messageSource;
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public void setManager(ParamPeriodeManager manager) {
		this.manager = manager;
	}	

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		verifieLesDroits();

		ModelAndView mav = getModelAndViewToPeriode(
				getPeriodeIdFromRequest(request),
				getModeleIdFromRequest(request)
		);
		
		try {
			manager.deleteModeleFeuilleDocument(getFeuilleIdFromRequest(request));
		} catch (DataIntegrityViolationException e) {
			Map<Long, String> m = new HashMap<Long, String>(1);
			m.put(getFeuilleIdFromRequest(request), messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			request.getSession().setAttribute("error_feuille", m);
		}		
		
		return mav;
	}
	

	

	

}
