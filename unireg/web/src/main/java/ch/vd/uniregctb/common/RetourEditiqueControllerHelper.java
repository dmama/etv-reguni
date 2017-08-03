package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatReroutageInbox;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;

public interface RetourEditiqueControllerHelper {

	String MESSAGE_REROUTAGE_INBOX = "label.inbox.impression.reroutee";

	/**
	 * Méthode à appeler afin de gérer un objet {@link ch.vd.uniregctb.editique.EditiqueResultat} arrivé depuis Editique
	 * @param resultat le résultat à gérer
	 * @param response la réponse HTTP dans laquelle, le cas échéant, le contenu doit être bourré pour téléchargement
	 * @param filenameRadical radical (sans extension, qui sera déduite du type MIME du contenu) du nom de fichier sous lequel le contenu doit apparaître dans la réponse HTTP, le cas échéant
	 * @param onReroutageInbox action à effectuer après l'appel à la méthode {@link ch.vd.uniregctb.common.Flash#warning} dans le cas où le retour d'impression se fait un peu attendre et a été re-routé ver l'inbox
	 * @param onTimeout action à effectuer sur réception d'un timeout définitif
	 * @param onError action à effectuer à la réception d'une erreur depuis éditique
	 * @return une action de redirection dans les cas d'erreur / timeout, null en principe dans les cas de téléchargement
	 * @throws java.io.IOException en cas de problème IO
	 */
	String traiteRetourEditique(@Nullable EditiqueResultat resultat,
	                            HttpServletResponse response,
	                            String filenameRadical,
	                            @Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
	                            @Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
	                            @Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException;

	/**
	 * Méthode à appeler afin de gérer un objet {@link ch.vd.uniregctb.editique.EditiqueResultat} arrivé depuis Editique, sachant que
	 * le téléchargement du document doit se faire de manière décalée une fois le "redirect" effectué
	 * @param resultat le résultat à gérer
	 * @param filenameRadical radical (sans extension, qui sera déduite du type MIME du contenu) du nom de fichier sous lequel le contenu doit apparaître dans la réponse HTTP, le cas échéant
	 * @param redirectInstruction une chaîne de caractères de la forme "redirect:URI" qui sera retournée en cas de document avec contenu
	 * @param cleanupOnRollback <code>true</code> si les données qui conduiraient au chargement d'un document après le redirect doivent être effacées en cas de rollback de la transaction en cours (et qu'il y en a une)
	 * @param onReroutageInbox action à effectuer après l'appel à la méthode {@link ch.vd.uniregctb.common.Flash#warning} dans le cas où le retour d'impression se fait un peu attendre et a été re-routé ver l'inbox
	 * @param onTimeout action à effectuer sur réception d'un timeout définitif
	 * @param onError action à effectuer à la réception d'une erreur depuis éditique
	 * @return une action de redirection dans les cas d'erreur / timeout, null en principe dans les cas de téléchargement
	 * @throws java.io.IOException en cas de problème IO
	 */
	String traiteRetourEditiqueAfterRedirect(@Nullable EditiqueResultat resultat,
	                                         String filenameRadical,
	                                         String redirectInstruction,
	                                         boolean cleanupOnRollback,
	                                         @Nullable TraitementRetourEditique<? super EditiqueResultatReroutageInbox> onReroutageInbox,
	                                         @Nullable TraitementRetourEditique<? super EditiqueResultatTimeout> onTimeout,
	                                         @Nullable TraitementRetourEditique<? super EditiqueResultatErreur> onError) throws IOException;

	/**
	 * Permet de spécifier des comportements
	 */
	interface TraitementRetourEditique<T extends EditiqueResultat> {
		/**
		 * Méthode appelée pour implémentation du comportement spécifique
		 * @param resultat résultat renvoyé par éditique
		 * @return en général une action de redirection (dans les cas d'erreur / timeout)
		 */
		String doJob(T resultat) throws IOException;
	}
}
