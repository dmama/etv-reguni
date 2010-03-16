package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;

/**
 * Facade des événements externes. Ce service est dédié à la communication d'applications publiant
 * des événements.
 * <p><b>Note : </b>La delegation est obligatoire pour démarrer la réception des événements afin
 * de synchronizer les événement reçus et le traitement à effectuer avec ces mème événements.
 * Lors de l'obtention de ce service, vous devrez uliser la méthode {@link #setDelegate(DelegateEvenementExterne)}
 * en lui passant le processus de traitement.
 * </p>
 * @author xcicfh (last modified by $Author:$ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementExterneFacade {


	/**
	 * Définit le délégué responsable de la réception des événements.
	 * @param delegate Le délégué responsable de la réception des événements.
	 */
	void setDelegate(DelegateEvenementExterne delegate);


	/**
	 * Envoie un événement.
	 * @param evenementExterne l'événement à envoyer.
	 * @throws Exception exception.
	 */
	void sendEvent( IEvenementExterne evenementExterne) throws Exception;


	/**
	 * Crée une nouvelle instance d'un événement externe impôt source.
	 * @return Retourne une nouvelle instance d'un événement externe impôt source.
	 */
	 EvenementImpotSourceQuittanceType creerEvenementImpotSource();
}
