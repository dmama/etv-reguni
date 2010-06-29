package ch.vd.uniregctb.web.xt.handler.remarque;

import org.springmodules.xt.ajax.component.Anchor;

import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Composant qui affiche un lien qui permet de recharger les remarques d'un tiers.
 */
public class RefreshRemarqueLink extends Anchor {
	public RefreshRemarqueLink(String libelle, Long tiersId) {
		super("#", new SimpleText(libelle));
		addAttribute("onclick", "XT.doAjaxAction('refreshRemarques', this, {'tiersId' : " + tiersId + "});");
	}
}