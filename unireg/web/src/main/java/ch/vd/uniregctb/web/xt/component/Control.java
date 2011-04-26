package ch.vd.uniregctb.web.xt.component;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.ReflectionUtils;
import org.springmodules.xt.ajax.component.Component;

import ch.vd.uniregctb.web.HtmlTextWriter;
import ch.vd.uniregctb.web.io.StringWriter;
import ch.vd.uniregctb.web.io.TextWriter;

public class Control implements Component {

	private static final long serialVersionUID = 8769225675446325069L;

	private ControlCollection controls;

	private HttpServletRequest request;
	private Control parent;

	/**
	 * Construct an HTML component.
	 */
	public Control() {
	}

	public Control(HttpServletRequest request) {
		this.request = request;
	}

	public String getId() {
		return null;
	}

	public String getName() {
		return null;
	}

	public HttpServletRequest getRequest() {
		Control ctrl = getRootParent();
		if (ctrl != null)
			return (ctrl).getInternalRequest();
		return null;
	}

	private HttpServletRequest getInternalRequest() {
		return request;
	}

	public Control getParent() {
		return parent;
	}

	private Control getRootParent() {
		Control ctrl = this;
		while (this.getParent() != null) {
			ctrl = ctrl.getParent();
		}
		return ctrl;
	}

	public boolean hasControls() {
		return (controls != null && controls.size() > 0);
	}

	final void addedControl(Control control) {
		if (control.parent != null)
			control.parent.getControls().remove(control);
		control.parent = this;
		Method method = ReflectionUtils.findMethod(Control.class, "onLoad");
		recursiveMethod(control, method);
		method = ReflectionUtils.findMethod(Control.class, "onInit");
		recursiveMethod(control, method);
	}

	final void removedControl(Control control) {
		control.unloadRecursive();
		control.parent = null;
	}

	public ControlCollection getControls() {
		if (controls == null) {
			controls = createControlCollection();
		}
		return controls;
	}

	protected ControlCollection createControlCollection() {
		return new ControlCollection(this);
	}

	protected void onInit() {
	}

	protected void onLoad() {
	}

	protected void onPreRender() {
	}

	/**
	 * Render the start and end tags of the HTML component, delegating the rendering of the body to the {@link #renderBody()} method..
	 */
	final public String render() {
		TextWriter output = new StringWriter();
		HtmlTextWriter writer = new HtmlTextWriter(output);
		preRenderRecursive();
		render(writer);
		unloadRecursive();
		return output.toString();
	}

	public void renderBeginTag(HtmlTextWriter writer) {
	}

	public void render(HtmlTextWriter writer) {
		renderBeginTag(writer);
		renderContent(writer);
		renderEndTag(writer);
	}

	public void renderContent(HtmlTextWriter writer) {
		renderChildren(writer);
	}

	public void renderChildren(HtmlTextWriter writer) {
		for (Control ctrl : getControls()) {
			ctrl.render(writer);
		}
	}

	public void renderEndTag(HtmlTextWriter writer) {
	}

	protected void onUnload() {

	}

	private void recursiveMethod(Control control, Method method) {
		method.setAccessible(true);
		ReflectionUtils.invokeMethod(method, control);
		for (Control ctrl : control.getControls()) {
			recursiveMethod(ctrl, method);
		}
	}

	void unloadRecursive() {
		if (hasControls()) {
			int len = controls.size();
			for (int i = 0; i < len; i++) {
				Control c = controls.get(i);
				c.unloadRecursive();
			}
		}
		onUnload();
	}

	void preRenderRecursive() {
		if (hasControls()) {
			int len = controls.size();
			for (int i = 0; i < len; i++) {
				Control c = controls.get(i);
				c.preRenderRecursive();
			}
		}
		onPreRender();
	}

}
