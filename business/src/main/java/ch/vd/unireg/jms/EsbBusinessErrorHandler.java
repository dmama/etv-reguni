package ch.vd.unireg.jms;

import org.jetbrains.annotations.Nullable;

import ch.vd.technical.esb.EsbMessage;

/**
 * Interface implémentée par l'entité qui est chargée de traiter (= renvoyer en queue d'erreur, ou dans une queue spécifique)
 * les erreurs dites "métier" levées lors du traitement d'un message ESB entrant
 */
public interface EsbBusinessErrorHandler {

	/**
	 * Traite l'erreur levée lors du traitement du message ESB entrant
	 * @param messageEntrant le message ESB entrant
	 * @param errorDescription une description textuelle du problème
	 * @param throwable l'exception levée, si applicable
	 * @param errorCode le code d'erreur
	 */
	void onBusinessError(EsbMessage messageEntrant, String errorDescription, @Nullable Throwable throwable, EsbBusinessCode errorCode) throws Exception;
}
