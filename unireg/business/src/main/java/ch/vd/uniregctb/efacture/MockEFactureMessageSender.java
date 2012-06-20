package ch.vd.uniregctb.efacture;

public class MockEFactureMessageSender implements EFactureMessageSender {
	@Override
	public void envoieRefusDemandeInscription(String idDemande, TypeRefusEFacture typeRefusEFacture) throws EvenementEfactureException {

	}

	@Override
	public void envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {

	}

	@Override
	public void envoieReceptionDemandeInscription(String idDemande) throws EvenementEfactureException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void envoieAcceptationDemandeInscription(String idDemande) throws EvenementEfactureException {

	}

	@Override
	public void envoieSuspensionContribuable(long noCtb) throws EvenementEfactureException {

	}

	@Override
	public void envoieActivationContribuable(long noCtb) throws EvenementEfactureException {

	}
}
