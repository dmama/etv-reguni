package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;

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
		ParamEncoder encoder = new ParamEncoder(tableId);
		setNumeroPage(decodeTaillePage(request, encoder));
		setTaillePage(taillePage);
		setChamp(decodeChamp(request, encoder, defaultChamp));
		setSensAscending(decodeSensAscending(request, encoder, defaultSensAscending));
	}

	private static boolean decodeSensAscending(HttpServletRequest request, ParamEncoder encoder, boolean defaultValue) {
		String sOrder = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER));
		if (sOrder == null) {
			return defaultValue;
		}
		else {
			return "1".equals(sOrder);
		}
	}

	private static String decodeChamp(HttpServletRequest request, ParamEncoder encoder, String defaultValue) {
		String nomAttribut = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT));
		if (nomAttribut == null) {
			return defaultValue;
		}
		else {
			return nomAttribut;
		}
	}

	private static int decodeTaillePage(HttpServletRequest request, ParamEncoder encoder) {
		String sPage = request.getParameter(encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE));
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
