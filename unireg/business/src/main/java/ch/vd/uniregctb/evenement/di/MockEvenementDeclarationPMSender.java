package ch.vd.uniregctb.evenement.di;

public class MockEvenementDeclarationPMSender implements EvenementDeclarationPMSender {

	@Override
	public void sendEmissionEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
	}

	@Override
	public void sendAnnulationEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle) throws EvenementDeclarationException {
	}
}
