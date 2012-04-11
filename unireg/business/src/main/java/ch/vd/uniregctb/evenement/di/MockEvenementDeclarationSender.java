package ch.vd.uniregctb.evenement.di;

import ch.vd.registre.base.date.RegDate;

public class MockEvenementDeclarationSender implements EvenementDeclarationSender {
	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, RegDate date, String codeControle, String codeRoutage) throws EvenementDeclarationException {
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, RegDate date) throws EvenementDeclarationException {
	}
}
