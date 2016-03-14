package ch.vd.uniregctb.evenement.di;

/**
 * Interface pour envoyer des événements de déclarations PM à destination du SI fiscal
 * (transmission du NIP, code routage... à l'application de saisie en ligne)
 */
public interface EvenementDeclarationPMSender {

	/**
	 * Envoie un événement d'émission d'une déclaration d'impôt PM avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro de séquence de la déclaration dans la période fiscale
	 * @param codeControle code de contrôle associé à la déclaration (<i>aka</i> NIP pour les intimes)
	 * @param codeRoutage code de routage associé à la déclaration
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;

	/**
	 * Envoie un événement d'annulation d'une déclaration d'impôt PM avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro de séquence de la déclaration dans la période fiscale
	 * @param codeControle code de contrôle associé à la déclaration (<i>aka</i> NIP pour les intimes)
	 * @param codeRoutage code de routage associé à la déclaration
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;

}
