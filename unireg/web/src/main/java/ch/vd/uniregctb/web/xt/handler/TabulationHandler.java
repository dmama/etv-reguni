package ch.vd.uniregctb.web.xt.handler;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ExecuteJavascriptFunctionAction;

public class TabulationHandler extends AbstractAjaxHandler {

	private final static String PREFIX = TabulationHandler.class.getName() + "_";

	public AjaxResponse storeCurrentTabulation(AjaxActionEvent event) {
		String tabulationId = event.getParameters().get("tabulationId");
		String currentTabulation = event.getParameters().get("currentTabulation");
		setCurrentTabulation(event.getHttpRequest(), tabulationId, currentTabulation);
		return new AjaxResponseImpl();
	}

	public AjaxResponse getCurrentTabulation(AjaxActionEvent event) {
		String tabulationId = event.getParameters().get("tabulationId");
		String currentTabulation = getCurrentTabulation(event.getHttpRequest(), tabulationId);
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put("name", currentTabulation);
		ExecuteJavascriptFunctionAction action = new ExecuteJavascriptFunctionAction("Tabulation.setCurrentTabulation", map);
		AjaxResponse response = new AjaxResponseImpl();
		response.addAction(action);
		return response;
	}

	public static void setCurrentTabulation(HttpServletRequest request, String tabulationId, String tabulationName) {
		HttpSession session = request.getSession(true);
		Map<String, String> map = getMap( session);
		if (tabulationName != null && !"".equals(tabulationName)) {
			map.put(tabulationId, tabulationName);
		}
		else {
			map.remove(tabulationId);
		}

	}

	private static String getCurrentTabulation(HttpServletRequest request, String tabulationId) {
		HttpSession session = request.getSession(true);
		Map<String, String> map = getMap( session);
		return map.get(tabulationId);
	}

	public static void resetCurrentTabulation(HttpServletRequest request, String tabulationId) {
		HttpSession session = request.getSession(true);
		Map<String, String> map = getMap( session);
		map.remove(tabulationId);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,String> getMap( HttpSession session) {
		Map<String,String> map  = (Map<String, String>) session.getAttribute(PREFIX + "currentTabulation");
		if ( map == null) {
			map = new HashMap<String, String>();
			session.setAttribute(PREFIX + "currentTabulation", map);
		}
		return map;
	}
}
