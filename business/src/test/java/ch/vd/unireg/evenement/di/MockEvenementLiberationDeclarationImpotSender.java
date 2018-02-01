package ch.vd.uniregctb.evenement.di;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.declaration.EvenementDeclarationException;

public class MockEvenementLiberationDeclarationImpotSender implements EvenementLiberationDeclarationImpotSender {

	@Override
	public void demandeLiberationDeclarationImpot(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull TypeDeclarationLiberee type) throws EvenementDeclarationException {
		// on ne fait rien, c'est un mock...
	}
}
