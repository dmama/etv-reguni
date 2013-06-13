package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;
import ch.vd.uniregctb.utils.WebContextUtils;

public class RetourEditiqueControllerHelperImpl implements MessageSourceAware, RetourEditiqueControllerHelper {

	private EditiqueDownloadService downloadService;

	private MessageSource messageSource;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDownloadService(EditiqueDownloadService downloadService) {
		this.downloadService = downloadService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public String traiteRetourEditique(@Nullable EditiqueResultat resultat,
	                                   HttpServletResponse response,
	                                   String filenameRadical,
	                                   @Nullable TraitementRetourEditique onReroutageInbox,
	                                   @Nullable TraitementRetourEditique onTimeout,
	                                   @Nullable TraitementRetourEditique onError) throws IOException {
		if (resultat instanceof EditiqueResultatDocument) {
			downloadService.download((EditiqueResultatDocument) resultat, filenameRadical, response);
			return null;
		}
		else if (resultat instanceof EditiqueResultatReroutageInbox) {
			final String msg = messageSource.getMessage(MESSAGE_REROUTAGE_INBOX, null, WebContextUtils.getDefaultLocale());
			Flash.warning(msg);
			if (onReroutageInbox != null) {
				return onReroutageInbox.doJob(resultat);
			}
		}
		else if (resultat instanceof EditiqueResultatTimeout) {
			if (onTimeout != null) {
				return onTimeout.doJob(resultat);
			}
		}
		else if (onError != null) {
			return onError.doJob(resultat);
		}

		throw new RuntimeException("Que faire avec résultat ? : " + resultat);
	}
}
