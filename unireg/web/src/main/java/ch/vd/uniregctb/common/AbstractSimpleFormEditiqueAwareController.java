package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

/**
 * Classe de base des controlleurs qui ont des interactions avec éditique
 * @deprecated Préférer l'utilisation du {@link RetourEditiqueControllerHelper}
 */
public abstract class AbstractSimpleFormEditiqueAwareController extends AbstractSimpleFormController {

	public static final String MESSAGE_REROUTAGE_INBOX = "label.inbox.impression.reroutee";

	private EditiqueDownloadService editiqueDownloadService;

	public void setEditiqueDownloadService(EditiqueDownloadService editiqueDownloadService) {
		this.editiqueDownloadService = editiqueDownloadService;
	}

	/**
	 * Permet de spécifier des comportements
	 */
	protected static interface TraitementRetourEditique<T extends EditiqueResultat> {

		/**
		 * Méthode appelée pour implémentation du comportement spécifique
		 * @param resultat résultat renvoyé par éditique
		 * @return en général une action de redirection (dans les cas d'erreur / timeout)
		 */
		ModelAndView doJob(T resultat);
	}

	/**
	 * Méthode à appeler depuis les classes dérivée afin de gérer un objet {@link EditiqueResultat} arrivé depuis Editique
	 * @param resultat le résultat à gérer
	 * @param response la réponse HTTP dans laquelle, le cas échéant, le contenu doit être bourré pour téléchargement
	 * @param filenameRadical radical (sans extension, qui sera déduite du type MIME du contenu) du nom de fichier sous lequel le contenu doit apparaître dans la réponse HTTP, le cas échéant
	 * @param onReroutageInbox action à effectuer après l'appel à la méthode {@link #flash} dans le cas où le retour d'impression se fait un peu attendre et a été re-routé ver l'inbox
	 * @param onTimeout action à effectuer sur réception d'un timeout définitif
	 * @param onError action à effectuer à la réception d'une erreur depuis éditique
	 * @return une action de redirection dans les cas d'erreur / timeout, null en principe dans les cas de téléchargement
	 * @throws IOException en cas de problème IO
	 */
	protected ModelAndView traiteRetourEditique(@Nullable EditiqueResultat resultat,
	                                            HttpServletResponse response,
	                                            String filenameRadical,
												@Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
												@Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
												@Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException {
		if (resultat instanceof EditiqueResultatDocument) {
			editiqueDownloadService.download((EditiqueResultatDocument) resultat, filenameRadical, response);
		}
		else if (resultat instanceof EditiqueResultatReroutageInbox) {
			final String msg = getMessageSourceAccessor().getMessage(MESSAGE_REROUTAGE_INBOX);
			flashWarning(msg);
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
		return null;
	}
}
