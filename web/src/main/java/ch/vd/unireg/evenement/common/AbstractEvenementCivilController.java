package ch.vd.unireg.evenement.common;

import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.pagination.ParamPagination;

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
