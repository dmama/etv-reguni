package ch.vd.uniregctb.web.xt.handler.remarque;

import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Composant qui permet d'afficher le texte de la tabulation "remarques" avec le nombre entre parenthèses.
 */
public class RemarqueTabText extends SimpleText {
	public RemarqueTabText(int count) {
		super(count == 0 ? "Remarques" : "Remarques (" + count + ")");
	}
}
