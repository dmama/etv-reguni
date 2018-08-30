package ch.vd.unireg.evenement.cybercontexte;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * Service d'envoi des événements pour publier des informations dans le contexte de Cyberprestations.
 */
public interface EvenementCyberContexteSender {

	/**
	 * Met-à-jour le contexte Cyber pour signifier qu'une déclaration d'impôt été émise et est disponible.
	 *
	 * @param numeroContribuable le numéro du contribuable qui possède la déclaration
	 * @param periodeFiscale     la période fiscale de la déclaration
	 * @param numeroSequence     le numéro de séquence de la déclaration
	 * @param codeControle       le code de contrôle de la déclaration
	 * @param dateEvenement      la date d'événement
	 * @throws EvenementCyberContexteException si l'envoi n'a pas été possible
	 */
	void sendEmissionDeclarationEvent(Long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) throws EvenementCyberContexteException;

	/**
	 * Met-à-jour le contexte Cyber pour signifier qu'une déclaration d'impôt été annulée.
	 *
	 * @param numeroContribuable le numéro du contribuable qui possède la déclaration
	 * @param periodeFiscale     la période fiscale de la déclaration
	 * @param numeroSequence     le numéro de séquence de la déclaration
	 * @param codeControle       le code de contrôle de la déclaration
	 * @param dateEvenement      la date d'événement
	 * @throws EvenementCyberContexteException si l'envoi n'a pas été possible
	 */
	void sendAnnulationDeclarationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) throws EvenementCyberContexteException;
}
