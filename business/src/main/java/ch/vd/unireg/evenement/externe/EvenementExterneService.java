package ch.vd.unireg.evenement.externe;

public interface EvenementExterneService extends EvenementExterneHandler {

	/**
	 * Permet de re-traiter les evenements externes depuis un batch de relance
	 *
 	 * @param event événement à re-traiter
	 * @return <code>true</code> si l'événement a été traité, <code>false</code> s'il est parti/resté en erreur
	 */
	boolean retraiterEvenementExterne(EvenementExterne event);
}
