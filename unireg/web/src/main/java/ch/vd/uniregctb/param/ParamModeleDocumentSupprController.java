package ch.vd.uniregctb.param;

import static ch.vd.uniregctb.param.Commun.getModelAndViewToPeriode;
import static ch.vd.uniregctb.param.Commun.getModeleIdFromRequest;
import static ch.vd.uniregctb.param.Commun.getPeriodeIdFromRequest;
import static ch.vd.uniregctb.param.Commun.verifieLesDroits;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import ch.vd.uniregctb.param.manager.ParamPeriodeManager;


public class ParamModeleDocumentSupprController extends AbstractController {

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
		
		ModelAndView mav = getModelAndViewToPeriode(getPeriodeIdFromRequest(request), false);
		
		try {
			manager.deleteModeleDocument(getModeleIdFromRequest(request));
		} catch (DataIntegrityViolationException e) {
			Map<Long, String> m = new HashMap<Long, String>(1);
			m.put(getModeleIdFromRequest(request), messageSource.getMessage("error.suppr.impossible", null, "error.suppr.impossible", Locale.getDefault()));
			request.getSession().setAttribute("error_modele", m);
		}
		return mav;
	}
}
