package ch.vd.uniregctb.web.xt.component;

import org.springmodules.xt.ajax.component.Component;

import ch.vd.uniregctb.web.HtmlTextWriter;

public class WrapperControlComponent extends Control {

	private static final long serialVersionUID = 1616272189754984179L;
	private final Component component;

	public WrapperControlComponent(Component component) {
		this.component = component;
	}

	@Override
	public void render(HtmlTextWriter writer) {
		if (component != null) {
			writer.write(component.render());
		}

	}

}
