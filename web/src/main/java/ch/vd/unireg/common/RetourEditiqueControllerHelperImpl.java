package ch.vd.unireg.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.editique.EditiqueResultatTimeout;
import ch.vd.unireg.utils.WebContextUtils;

public class RetourEditiqueControllerHelperImpl implements MessageSourceAware, RetourEditiqueControllerHelper {

	private EditiqueDownloadService downloadService;
	private DelayedDownloadService delayedDownloadService;

	private MessageSource messageSource;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDownloadService(EditiqueDownloadService downloadService) {
		this.downloadService = downloadService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDelayedDownloadService(DelayedDownloadService delayedDownloadService) {
		this.delayedDownloadService = delayedDownloadService;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public String traiteRetourEditique(@Nullable EditiqueResultat resultat,
	                                   final HttpServletResponse response,
	                                   final String filenameRadical,
	                                   @Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
	                                   @Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
	                                   @Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException {

		final TraitementRetourEditique<EditiqueResultatDocument> print = resultat1 -> {
			downloadService.download(resultat1, filenameRadical, response);
			return null;
		};

		return traiteRetourEditique(resultat, print, onReroutageInbox, onTimeout, onError);
	}

	@Override
	public String traiteRetourEditiqueAfterRedirect(@Nullable EditiqueResultat resultat,
	                                                final String filenameRadical,
	                                                final String redirectInstruction,
	                                                final boolean cleanupOnRollback,
	                                                @Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
	                                                @Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
	                                                @Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException {

		final TraitementRetourEditique<EditiqueResultatDocument> print = new TraitementRetourEditique<EditiqueResultatDocument>() {
			@Override
			public String doJob(EditiqueResultatDocument resultat) throws IOException {
				final UUID id = delayedDownloadService.putDocument(resultat, filenameRadical);
				setDelayedDownloadIdToSession(id);

				// [SIFISC-25996] on a mis quelque chose en session, mais ce quelque chose peut devoir disparaître si nous
				// sommes actuellement dans une transaction et que cette transaction doit être annulée
				if (cleanupOnRollback && TransactionSynchronizationManager.isActualTransactionActive()) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						@Override
						public void afterCompletion(int status) {
							// si rollback il y a, on doit oublier cet identifiant
							super.afterCompletion(status);
							if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {

								// effacement de la donnée en session
								setDelayedDownloadIdToSession(null);

								// effacement de la donnée enregistrée dans le service
								delayedDownloadService.eraseDocument(id);
							}
						}
					});
				}

				return redirectInstruction;
			}
		};

		return traiteRetourEditique(resultat, print, onReroutageInbox, onTimeout, onError);
	}

	private void setDelayedDownloadIdToSession(@Nullable UUID id) {
		// on met dans la session l'identifiant du document stocké
		final RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
		if (id != null) {
			attributes.setAttribute(DelayedDownloadService.SESSION_ATTRIBUTE_NAME, id, RequestAttributes.SCOPE_SESSION);
		}
		else {
			attributes.removeAttribute(DelayedDownloadService.SESSION_ATTRIBUTE_NAME, RequestAttributes.SCOPE_SESSION);
		}
	}

	private String traiteRetourEditique(@Nullable EditiqueResultat resultat,
	                                    TraitementRetourEditique<EditiqueResultatDocument> onDocument,
	                                    @Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
	                                    @Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
	                                    @Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException {

		if (resultat instanceof EditiqueResultatDocument) {
			return onDocument.doJob((EditiqueResultatDocument) resultat);
		}
		else if (resultat instanceof EditiqueResultatReroutageInbox) {
			final String msg = messageSource.getMessage(MESSAGE_REROUTAGE_INBOX, null, WebContextUtils.getDefaultLocale());
			Flash.warning(msg);
			if (onReroutageInbox != null) {
				return onReroutageInbox.doJob((EditiqueResultatReroutageInbox) resultat);
			}
		}
		else if (resultat instanceof EditiqueResultatTimeout && onTimeout != null) {
			return onTimeout.doJob((EditiqueResultatTimeout) resultat);
		}
		else if (resultat instanceof EditiqueResultatErreur) {
			if (onError != null) {
				return onError.doJob((EditiqueResultatErreur) resultat);
			}
		}

		throw new RuntimeException("Que faire avec résultat ? : " + resultat);
	}
}
