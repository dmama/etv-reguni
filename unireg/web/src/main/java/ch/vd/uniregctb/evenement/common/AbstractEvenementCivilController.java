package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;

abstract public class AbstractEvenementCivilController {

	protected ControllerUtils controllerUtils;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	protected String buildNavListRedirect(ParamPagination pagination, final String tableName, final String navListPath) {
		String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(tableName, pagination);
		if (displayTagParameter != null) {
			displayTagParameter = "?" + displayTagParameter;
		}
		return ("redirect:" + navListPath + displayTagParameter);
	}

}
