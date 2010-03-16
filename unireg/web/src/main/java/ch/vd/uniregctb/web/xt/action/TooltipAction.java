package ch.vd.uniregctb.web.xt.action;

import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.AppendContentAction;
import org.springmodules.xt.ajax.action.RemoveContentAction;
import org.springmodules.xt.ajax.component.Component;

import ch.vd.uniregctb.web.xt.component.InternalJspComponent;

public class TooltipAction extends AppendContentAction {

	/**
	 *
	 */
	private static final long serialVersionUID = 7544005695549972461L;

	private final String elementId;
	private String encoding = "UTF-8";

	public TooltipAction(AjaxActionEvent event, String encoding) {
		this(event.getParameters().get("elementId"), encoding, new InternalJspComponent(event.getHttpRequest(), event.getParameters().get("link")));
		// Create the component for including jsp content:
		event.getHttpRequest().setAttribute("link", event.getParameters().get("link"));
	}

	public TooltipAction(String elementId, String encoding, Component... components) {
		super(elementId, components);
		this.elementId = elementId;
		this.encoding = encoding;
		InternalJspComponent component = (InternalJspComponent) components[0];
		component.setCharacterEncoding(encoding);
		component.setContentType("text/html");

	}

	public AjaxResponse getAjaxResponse() {
		AjaxResponse response = new AjaxResponseImpl(encoding);
		// Add the action:
		response.addAction(new RemoveContentAction(this.elementId));
		response.addAction(this);
		//response.addAction(new ShowElement(this.elementId));
		return response;
	}




}
