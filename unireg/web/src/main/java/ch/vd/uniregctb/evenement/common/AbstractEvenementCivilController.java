package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;

public class AbstractEvenementCivilController {

	protected String buildNavListRedirect(ParamPagination pagination, final String tableName, final String navListPath) {
		String displayTagParameter = ControllerUtils.getDisplayTagRequestParametersForPagination(tableName, pagination);
		if (displayTagParameter != null) {
			displayTagParameter = "?" + displayTagParameter;
		}
		return ("redirect:" + navListPath + displayTagParameter);
	}

}
