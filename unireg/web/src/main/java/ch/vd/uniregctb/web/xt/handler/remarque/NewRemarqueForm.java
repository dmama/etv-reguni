package ch.vd.uniregctb.web.xt.handler.remarque;

import org.springmodules.xt.ajax.component.Container;
import org.springmodules.xt.ajax.component.InputField;
import org.springmodules.xt.ajax.component.TextArea;

import ch.vd.uniregctb.web.xt.component.SimpleText;
import ch.vd.uniregctb.web.xt.handler.BreakLineComponent;

/**
 * Composant qui affiche un formulaire permettant de saisir une nouvelle remarque.
 */
public class NewRemarqueForm extends Container {

	public NewRemarqueForm(long tiersId) {
		super(Type.DIV);
		addAttribute("class", "new_remarque");

		final TextArea text = new TextArea(3, 80);
		text.addAttribute("id", "new_remarque_text");
		addComponent(text);

		addComponent(new BreakLineComponent());

		final InputField button = new InputField("new_remarque_submit", "Ajouter", InputField.InputType.BUTTON);
		button.addAttribute("id", "new_remarque_submit");
		button.addAttribute("onclick",
				"XT.doAjaxSubmit('saveRemarque', E$('new_remarque_text'), {'tiersId' : " + tiersId + ", 'texte' : E$('new_remarque_text').value}, { 'clearQueryString' : false});");
		addComponent(button);

		addComponent(new SimpleText("&nbsp;ou&nbsp;"));
		addComponent(new RefreshRemarqueLink("annuler", tiersId));
	}
}
