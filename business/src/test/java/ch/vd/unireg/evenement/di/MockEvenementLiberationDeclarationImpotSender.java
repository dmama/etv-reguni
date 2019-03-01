package ch.vd.unireg.evenement.di;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;

public class MockEvenementLiberationDeclarationImpotSender implements EvenementLiberationDeclarationImpotSender {

	@Override
	public String demandeLiberationDeclarationImpot(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull TypeDeclarationLiberee type) throws EvenementDeclarationException {
		// on ne fait rien, c'est un mock...
		return null;
	}
}
