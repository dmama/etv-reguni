/**
 *
 */
package ch.vd.uniregctb.web.xt.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.action.AbstractExecuteJavascriptAction;

/**
 * @author xcicfh
 *
 */
public class AutoCompleteAction extends AbstractExecuteJavascriptAction {


	public static final String PARAM_SELECTED_VALUE ="selectedValue";
	/**
	 *
	 */
	private static final long serialVersionUID = -5300480086861764850L;


	private final String functionName;
    private final Map<String, Object> options = new HashMap<String, Object>();

    /**
     * Action constructor.
     * @param name The function name.
     * @param options A map of key/value pairs representing function options.
     */
    public AutoCompleteAction(AjaxEvent event, List<?>  list) {
        this.functionName = event.getParameters().get("NAME") + "_autoComplete.createControl";
        String selectedValue = event.getParameters().get(AutoCompleteAction.PARAM_SELECTED_VALUE);
        options.put("result", list);
        options.put("text", selectedValue);
        event.getHttpRequest().setAttribute(AutoCompleteAction.PARAM_SELECTED_VALUE, selectedValue);
    }



    @Override
	protected String getJavascript() {
        StringBuilder function = new StringBuilder();

        function.append(functionName).append("(");
        if (!this.options.isEmpty()) {
            JSONObject json = JSONObject.fromMap(this.options);
            function.append(json.toString());
        }
        function.append(");");

        return function.toString();
    }

}
