package ch.vd.unireg.evenement.externe;

/**
 * Interface de callback pour traiter les événements externes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface EvenementExterneHandler {

	/**
	 * Traite l'événement externe spécifié.
	 *
	 * @param event un événement externe (non-persisté).
	 * @throws EvenementExterneException en cas de traitement impossible.
	 */
	void onEvent(EvenementExterne event) throws EvenementExterneException;
}
