package ch.vd.uniregctb.web.xt.handler;

import org.springmodules.xt.ajax.component.SimpleHTMLComponent;
import org.springmodules.xt.ajax.component.SimpleText;

public class BoldComponent extends SimpleHTMLComponent {
	/**
	 *
	 */
	private static final long serialVersionUID = -8163689520794000414L;


	public BoldComponent(String text){
		  this.internalAddContent(new SimpleText(text));
	}


	@Override
	protected String getTagName() {
		// TODO Auto-generated method stub
		return "b";
	}

}
