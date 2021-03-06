package ch.vd.unireg.evenement.di;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;

/**
 * Interface pour envoyer des événements de déclarations PP à destination d'ADDI.
 */
public interface EvenementDeclarationPPSender {

	/**
	 * Envoie un événement d'émission d'une déclaration d'impôt ordinaire PP.
	 *
	 * @param numeroContribuable le numéro du contribuable à qui l'on envoie une déclaration
	 * @param periodeFiscale     la période fiscale de la déclaration considérée
	 * @param numeroSequence     le numéro de séquence de la déclaraiton considérée
	 * @param date               la date d'émission de la déclaration d'impôt (date d'événement)
	 * @param codeControle       le code de contrôle qui permet de vérifier la validité d'un retour par voie électronique
	 * @param codeRoutage        un code de routage fourni par TAO
	 * @throws EvenementDeclarationException en cas de problème lors de l'envoi de l'événement
	 */
	void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, RegDate date, String codeControle, String codeRoutage) throws EvenementDeclarationException;

	/**
	 * Envoie un événement d'annulation d'une déclaration d'impôt ordinaire PP.
	 *
	 * @param numeroContribuable le numéro du contribuable
	 * @param periodeFiscale     la période fiscale de la déclaration annulée
	 * @param numeroSequence     le numéro de séquence de la déclaraiton considérée
	 * @param codeControle       le code de contrôle qui permet de vérifier la validité d'un retour par voie électronique
	 * @param date               la date d'annulation de la déclaration d'impôt (date d'événement)
	 * @throws EvenementDeclarationException en cas de problème lors de l'envoi de l'événement
	 */
	void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, RegDate date) throws EvenementDeclarationException;
}
