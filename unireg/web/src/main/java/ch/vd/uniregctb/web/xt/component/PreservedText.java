package ch.vd.uniregctb.web.xt.component;

import org.springmodules.xt.ajax.component.SimpleHTMLComponent;

/**
 * Composant qui permet d'afficher un champ texte en préservant les espaces et les retours de ligne.
 */
public class PreservedText extends SimpleHTMLComponent {

	public PreservedText(String text) {
		internalAddContent(new SimpleText(text));
	}

	@Override
	protected String getTagName() {
		return "pre";
	}
}
