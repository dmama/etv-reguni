package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;


public class WebParamPagination extends ParamPagination {

	public WebParamPagination(int numeroPage, int taillePage, String champ, boolean sensAscending) {
		super(numeroPage, taillePage, champ, sensAscending);
	}

	public WebParamPagination(HttpServletRequest request, String tableId, int taillePage) {
		this(request, tableId, taillePage, null, true);
	}

	public WebParamPagination(HttpServletRequest request, String tableId, int taillePage, String defaultChamp, boolean defaultSensAscending) {
		this(request, new ParamEncoder(tableId), taillePage, defaultChamp, defaultSensAscending);
	}

	private WebParamPagination(HttpServletRequest request, ParamEncoder encoder, int taillePage, String defaultChamp, boolean defaultSensAscending) {
		super(decodePage(request, encoder), taillePage, decodeChamp(request, encoder, defaultChamp), decodeSensAscending(request, encoder, defaultSensAscending));
	}

	private static boolean decodeSensAscending(HttpServletRequest request, ParamEncoder encoder, boolean defaultValue) {
		final String sOrder = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER));
		if (StringUtils.isBlank(sOrder)) {
			return defaultValue;
		}
		else {
			return "1".equals(sOrder);
		}
	}

	private static String decodeChamp(HttpServletRequest request, ParamEncoder encoder, String defaultValue) {
		final String nomAttribut = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT));
		if (StringUtils.isBlank(nomAttribut)) {
			return defaultValue;
		}
		else {
			return nomAttribut;
		}
	}

	private static int decodePage(HttpServletRequest request, ParamEncoder encoder) {
		final String sPage = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE));
		int page;
		try {
			page = Integer.valueOf(sPage);
		}
		catch (NumberFormatException e) {
			page = 1;
		}
		return page;
	}
	
}
