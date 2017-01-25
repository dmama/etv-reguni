package ch.vd.uniregctb.evenement.declaration;

public class MockEvenementDeclarationPMSender implements EvenementDeclarationPMSender {

	@Override
	public void sendEmissionDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
	}

	@Override
	public void sendAnnulationDIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String codeRoutage) throws EvenementDeclarationException {
	}

	@Override
	public void sendEmissionDemandeDegrevementICIEvent(long numeroContribuable, int periodeFiscale, int numeroSequence, String codeControle, String commune, String numeroParcelle, Long estimationFiscale) throws EvenementDeclarationException {
	}
}
