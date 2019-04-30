package ch.vd.unireg.efacture;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;

public class MockEFactureMessageSender implements EFactureMessageSender {
	@Override
	public String envoieRefusDemandeInscription(String idDemande, String description, boolean retourAttendu) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieMiseEnAttenteDemandeInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu, String description) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieSuspensionContribuable(long noCtb, boolean retourAttendu, String description) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieActivationContribuable(long noCtb, boolean retourAttendu, String description) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String envoieDemandeChangementEmail(long noCtb, @Nullable String newMail, boolean retourAttendu, String description) throws EFactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public void demandeDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EFactureException {
	}
}
