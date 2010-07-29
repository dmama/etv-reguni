package ch.vd.uniregctb.web.xt.handler.remarque;

import org.springmodules.xt.ajax.component.Anchor;

import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Composant qui affiche un lien permettant d'ajouter une remarque.
 */
public class AddRemarqueLink extends Anchor {
	public AddRemarqueLink(Long tiersId) {
		super("#", new SimpleText("&nbsp;Ajouter une remarque"));
		addAttribute("class", "add");
		addAttribute("onclick", "XT.doAjaxAction('addRemarque', this, {'tiersId' : " + tiersId + "});");
	}
}
