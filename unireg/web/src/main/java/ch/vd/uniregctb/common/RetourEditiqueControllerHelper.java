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

public class RetourEditiqueControllerHelper implements MessageSourceAware {

	public static final String MESSAGE_REROUTAGE_INBOX = "label.inbox.impression.reroutee";

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

	/**
	 * Permet de spécifier des comportements
	 */
	public static interface TraitementRetourEditique {
		/**
		 * Méthode appelée pour implémentation du comportement spécifique
		 * @param resultat résultat renvoyé par éditique
		 * @return en général une action de redirection (dans les cas d'erreur / timeout)
		 */
		String doJob(EditiqueResultat resultat);
	}
	/**
	 * Méthode à appeler depuis les classes dérivée afin de gérer un objet {@link EditiqueResultat} arrivé depuis Editique
	 * @param resultat le résultat à gérer
	 * @param response la réponse HTTP dans laquelle, le cas échéant, le contenu doit être bourré pour téléchargement
	 * @param filenameRadical radical (sans extension, qui sera déduite du type MIME du contenu) du nom de fichier sous lequel le contenu doit apparaître dans la réponse HTTP, le cas échéant
	 * @param onReroutageInbox action à effectuer après l'appel à la méthode {@link Flash#warning} dans le cas où le retour d'impression se fait un peu attendre et a été re-routé ver l'inbox
	 * @param onTimeout action à effectuer sur réception d'un timeout définitif
	 * @param onError action à effectuer à la réception d'une erreur depuis éditique
	 * @return une action de redirection dans les cas d'erreur / timeout, null en principe dans les cas de téléchargement
	 * @throws java.io.IOException en cas de problème IO
	 */
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
