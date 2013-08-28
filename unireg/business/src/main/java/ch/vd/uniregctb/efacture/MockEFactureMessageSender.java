package ch.vd.uniregctb.efacture;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;

public class MockEFactureMessageSender implements EFactureMessageSender {
	@Override
	public String envoieRefusDemandeInscription(String idDemande, String description, boolean retourAttendu) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieSuspensionContribuable(long noCtb, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieActivationContribuable(long noCtb, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}
}
