package ch.vd.unireg.evenement.di;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;

/**
 * Interface du service d'envoi des demandes de libération des déclarations d'impôt
 */
public interface EvenementLiberationDeclarationImpotSender {

	enum TypeDeclarationLiberee {
		DI_PP,
		DI_PM
	}

	/**
	 * Envoi d'un message JMS de demande de libération d'une déclaration d'impôt
	 * @param numeroContribuable numéro du contribuable lié à la déclaration
	 * @param periodeFiscale période fiscale de la déclaration
	 * @param numeroSequence numéro de séquence de la déclaration dans la période fiscale
	 * @param type type de déclaration d'impôt
	 * @throws EvenementDeclarationException en cas de problème
	 */
	void demandeLiberationDeclarationImpot(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull TypeDeclarationLiberee type) throws EvenementDeclarationException;
}
