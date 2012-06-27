package ch.vd.uniregctb.efacture;

public class MockEFactureMessageSender implements EFactureMessageSender {
	@Override
	public String envoieRefusDemandeInscription(String idDemande, TypeRefusEFacture typeRefusEFacture, boolean retourAttendu) throws EvenementEfactureException {
	return "";
	}

	@Override
	public String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteEFacture typeAttenteEFacture, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String envoieSuspensionContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String envoieActivationContribuable(long noCtb, boolean retourAttendu) throws EvenementEfactureException {
		return "";
	}
}
