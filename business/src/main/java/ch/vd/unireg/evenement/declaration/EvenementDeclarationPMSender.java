package ch.vd.unireg.evenement.declaration;

import ch.vd.registre.base.date.RegDate;

/**
 * Interface pour envoyer des événements de déclarations PM (DI + demande dégrèvement) à destination du SI fiscal
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
	void sendEmissionDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;

	/**
	 * Envoie un événement d'annulation d'une déclaration d'impôt PM avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro de séquence de la déclaration dans la période fiscale
	 * @param codeControle code de contrôle associé à la déclaration (<i>aka</i> NIP pour les intimes)
	 * @param codeRoutage code de routage associé à la déclaration
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendAnnulationDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;

	/**
	 * Envoie un événement d'émission d'un questionnaire SNC avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro du questionnaire dans la période fiscale
	 * @param codeControle code de contrôle associé au questionnaire (<i>aka</i> NIP pour les intimes)
	 * @param codeRoutage code de routage associé au questionnaire
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendEmissionQSNCEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;

	/**
	 * Envoie un événement d'annulation d'un questionnaire SNC avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro de séquence du questionnaire dans la période fiscale
	 * @param codeControle code de contrôle associé au questionnaire (<i>aka</i> NIP pour les intimes)
	 * @param codeRoutage code de routage associé au questionnaire
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendAnnulationQSNCEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException;


	/**
	 * Envoie un événement d'émission d'un formulaire de demande de dégrèvement ICI avec NIP
	 * @param numeroContribuable numéro de contribuable de la PM concernée
	 * @param periodeFiscale période fiscale de la demande
	 * @param numeroSequence numéro de séquence de la demande dans la période fiscale
	 * @param codeControle code de contrôle associé à la demande (<i>aka</i> NIP pour les intimes)
	 * @param commune nom de la commune de localisation de l'immeuble concerné
	 * @param numeroParcelle numéro de la parcelle de l'immeuble dans la commune
	 * @param delaiRetour délai de retour du formulaire de demande de dégrèvement
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void sendEmissionDemandeDegrevementICIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle,
	                                            String commune, String numeroParcelle, RegDate delaiRetour) throws EvenementDeclarationException;

}
