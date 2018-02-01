package ch.vd.unireg.common;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * Message source qui ne fait rien de spécial et retourne les messages tels quels
 */
public class MockMessageSource implements MessageSource {
	@Override
	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return code;
	}

	@Override
	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
		return code;
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return resolvable.getDefaultMessage();
	}
}
