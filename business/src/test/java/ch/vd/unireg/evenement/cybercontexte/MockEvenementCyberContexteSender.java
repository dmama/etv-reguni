package ch.vd.unireg.evenement.cybercontexte;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

public class MockEvenementCyberContexteSender implements EvenementCyberContexteSender {
	@Override
	public void sendEmissionDeclarationEvent(Long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) {

	}

	@Override
	public void sendAnnulationDeclarationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, @NotNull String codeControle, @NotNull RegDate dateEvenement) {

	}
}
