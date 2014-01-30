package ch.vd.uniregctb.efacture;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public class DummyEFactureService implements EFactureService {

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException {
		return StringUtils.EMPTY;
	}

	@Override
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		return null;
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) {
		return null;
	}

	@Override
	public String modifierEmailContribuable(long noCtb, @Nullable String newEmail, boolean retourAttendu, String description) throws EvenementEfactureException {
		return StringUtils.EMPTY;
	}

	@Override
	public void demanderDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EvenementEfactureException {
	}
}
