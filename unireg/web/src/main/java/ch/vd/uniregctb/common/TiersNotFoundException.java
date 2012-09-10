package ch.vd.uniregctb.common;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.utils.WebContextUtils;

public class TiersNotFoundException extends ObjectNotFoundException {

	private static MessageSource messageSource;

	private Long tiersId;

	public TiersNotFoundException() {
	}

	public TiersNotFoundException(long tiersId) {
		this.tiersId = tiersId;
	}

	@Override
	public String getMessage() {
		if (tiersId == null) {
			return messageSource.getMessage("error.tiers.inexistant", null, WebContextUtils.getDefaultLocale());
		}
		else {
			return messageSource.getMessage("error.tiers.no.inexistant", new Object[]{FormatNumeroHelper.numeroCTBToDisplay(tiersId)}, WebContextUtils.getDefaultLocale());
		}
	}

	public void setMessageSource(MessageSource messageSource) {
		TiersNotFoundException.messageSource = messageSource;
	}
}
