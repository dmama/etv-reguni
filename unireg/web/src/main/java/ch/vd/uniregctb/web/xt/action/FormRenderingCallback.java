package ch.vd.uniregctb.web.xt.action;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.validation.ObjectError;
import org.springmodules.xt.ajax.AjaxAction;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.TaggedText;
import org.springmodules.xt.ajax.validation.ErrorRenderingCallback;
import org.springmodules.xt.ajax.validation.SuccessRenderingCallback;

/**
 * Callback for rendering errors on forms.
 *
 * @author Sergio Bossa
 */
public class FormRenderingCallback implements ErrorRenderingCallback, SuccessRenderingCallback {



	@Override
	public AjaxAction[] getErrorActions(AjaxSubmitEvent event, ObjectError error) {
		return null;
	}

	@Override
	public Component getErrorComponent(AjaxSubmitEvent event, ObjectError error, MessageSource messageSource, Locale locale) {
		TaggedText text = new TaggedText(messageSource.getMessage(error.getCode(), error.getArguments(), error.getDefaultMessage(), locale), TaggedText.Tag.SPAN);
        text.addAttribute("style","color : red;");
        return text;
	}

	@Override
	public AjaxAction getRenderingAction(ObjectError error) {
		AjaxAction[] actions = this.getErrorActions(null, error);
        return actions[0];
	}

	@Override
	public Component getRenderingComponent(ObjectError error, MessageSource messageSource, Locale locale) {
		 return this.getErrorComponent(null, error, messageSource, locale);
	}

	@Override
	public AjaxAction[] getSuccessActions(AjaxSubmitEvent event) {
		return null;
	}
}
