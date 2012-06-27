package ch.vd.uniregctb.efacture;

public class MockEFactureMessageSender implements EFactureMessageSender {
	@Override
	public void envoieRefusDemandeInscription(String idDemande, TypeRefusEFacture typeRefusEFacture) throws EvenementEfactureException {

	}

	@Override
	public void envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {

	}

	@Override
	public void envoieAcceptationDemandeInscription(String idDemande) throws EvenementEfactureException {

	}

	@Override
	public String envoieSuspensionContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException {

	}

	@Override
	public String envoieActivationContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException {

	}
}
