package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

/**
 * Classe de base des controlleurs qui ont des interactions avec éditique
 */
public abstract class AbstractSimpleFormEditiqueAwareController extends AbstractSimpleFormController {

	public static final String MESSAGE_REROUTAGE_INBOX = "L'impression n'est pas encore revenue. Pour vous permettre de continuer à travailler, celle-ci sera mise à disposition dans la boîte de réception dès que possible.";

	private EditiqueDownloadService editiqueDownloadService;

	public void setEditiqueDownloadService(EditiqueDownloadService editiqueDownloadService) {
		this.editiqueDownloadService = editiqueDownloadService;
	}

	/**
	 * Permet de spécifier des comportements
	 */
	protected static interface TraitementRetourEditique {
		ModelAndView doJob(EditiqueResultat resultat);
	}

	/**
	 * Ajoute juste une erreur globale de communication éditique dans les erreurs de binding
	 */
	protected static final class ErreurGlobaleCommunicationEditique implements TraitementRetourEditique {

		private final BindException errors;

		public ErreurGlobaleCommunicationEditique(BindException errors) {
			this.errors = errors;
		}

		@Override
		public ModelAndView doJob(EditiqueResultat resultat) {
			errors.reject("global.error.communication.editique");
			return null;
		}
	}

	protected ModelAndView traiteRetourEditique(EditiqueResultat resultat, HttpServletResponse response, String filenameRadical,
												TraitementRetourEditique onReroutageInbox, TraitementRetourEditique onTimeout,
												TraitementRetourEditique onError) throws IOException {
		if (resultat instanceof EditiqueResultatDocument) {
			editiqueDownloadService.download((EditiqueResultatDocument) resultat, filenameRadical, response);
		}
		else if (resultat instanceof EditiqueResultatReroutageInbox) {
			flash(MESSAGE_REROUTAGE_INBOX);
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
		return null;
	}
}
