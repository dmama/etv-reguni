package ch.vd.unireg.editique;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Service qui maintient la donnée des impressions directes éditiques qui reviennent
 * en attendant que le demandeur vienne les chercher
 */
public interface EditiqueRetourImpressionStorageService {

	/**
	 * Appelé à l'arrivée d'une nouvelle impression par le listener de messages éditique
	 * @param resultat représentation du document imprimé
	 */
	void onArriveeRetourImpression(EditiqueResultatRecu resultat);

	/**
	 * Récupère le document identifié par son nom (champ {@link ConstantesEditique#UNIREG_DOCUMENT_ID} dans l'entête)
	 * @param nomDocument identifiant du document à récupérer
	 * @param timeout temps maximal d'attente de l'arrivée de l'impression
	 * @return la représentation du document imprimé renvoyé par l'éditique, <code>null</code> si rien n'est revenu dans le temps imparti
	 */
	EditiqueResultat getDocument(String nomDocument, Duration timeout);

	/**
	 * Enregistre un trigger qui sera déclenché à la réception du retour d'impression identifié par son ID
	 * @param nomDocument identifiant du document déclencheur
	 * @param trigger action à lancer à la réception du document voulu
	 */
	void registerTrigger(String nomDocument, RetourImpressionTrigger trigger);

	/**
	 * @return le nombre de documents reçus depuis le démarrage du service
	 */
	int getDocumentsRecus();

	/**
	 * @return le nombre de documents reçus et encore en attente de dispatch
	 */
	int getDocumentsEnAttenteDeDispatch();

	/**
	 * Période du timer du cleanup (secondes) : à chaque tick, on va enlever de la map des impressions
	 * reçues les données qui étaient déjà là au tick précédent
	 * @return en secondes, la valeur de la période de purge
	 */
	int getCleanupPeriod();

	/**
	 * @return le nombre de documents purgés (= que personne n'a réclamé dans la période de cleanup) depuis le démarrage du service
	 */
	int getDocumentsPurges();

	/**
	 * @return la date de la dernière purge de document (ou <code>null</null> si aucune purge n'a jamais été effectuée)
	 */
	Date getDateDernierePurgeEffective();

	/**
	 * @return les triggers enregistrés en attente de la réception de documents, le Long de la paire étant le timestamp {@link System#nanoTime()} au moment de l'enregistrement de chaque trigger
	 */
	Collection<Pair<Long, RetourImpressionTrigger>> getTriggersEnregistres();

}
