package ch.vd.uniregctb.evenement.common;

import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.common.ParamPagination;

abstract public class AbstractEvenementCivilController {

	protected ControllerUtils controllerUtils;

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	protected String buildNavListRedirect(ParamPagination pagination, final String tableName, final String navListPath, Long id, Long nextId) {
		String displayTagParameter = controllerUtils.getDisplayTagRequestParametersForPagination(tableName, pagination);
			if (displayTagParameter != null) {
				displayTagParameter = "?" + displayTagParameter;
			}
			return String.format("redirect:%s%s%s%s%s",
			                                    navListPath,
			                                    displayTagParameter == null || id == null ? "" : "?",
			                                    displayTagParameter == null ? "" : displayTagParameter,
			                                    id == null ? "" : "&selectedEvtId=" + id,
			                                    nextId == null ? "" : "&nextEvtId=" + nextId
			                                    );
	}

}
