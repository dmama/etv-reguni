package ch.vd.unireg.evenement.di;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.declaration.EvenementDeclarationException;

public class MockEvenementDeclarationPPSender implements EvenementDeclarationPPSender {
	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, RegDate date, String codeControle, String codeRoutage) throws EvenementDeclarationException {
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, RegDate date) throws EvenementDeclarationException {
	}
}
