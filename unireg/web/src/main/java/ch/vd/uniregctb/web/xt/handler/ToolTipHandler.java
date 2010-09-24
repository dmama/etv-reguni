package ch.vd.uniregctb.web.xt.handler;

import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;

import ch.vd.uniregctb.web.xt.action.TooltipAction;

public class ToolTipHandler  extends AbstractAjaxHandler {


	public AjaxResponse includeTooltip(AjaxActionEvent event) {
        TooltipAction action = new TooltipAction(event, "UTF-8");
        return action.getAjaxResponse();
    }
}
