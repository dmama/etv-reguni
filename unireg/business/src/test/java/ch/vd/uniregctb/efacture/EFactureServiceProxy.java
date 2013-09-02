package ch.vd.uniregctb.efacture;

import java.math.BigInteger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public class EFactureServiceProxy implements EFactureService {

	private EFactureService target;

	public void setUp(MockEFactureService service) {
		this.target = service;
		service.init();
	}

	private void checkTarget() {
		if (target == null) {
			throw new IllegalStateException("Target should have been assigned already.");
		}
	}

	@Override
	public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage,
	                                              boolean retourAttendu) throws EvenementEfactureException {
		checkTarget();
		return target.notifieMiseEnAttenteInscription(idDemande, typeAttenteEFacture, description, idArchivage, retourAttendu);
	}

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException {
		checkTarget();
		return target.imprimerDocumentEfacture(ctbId, typeDocument, dateDemande, noAdherent, dateDemandePrecedente, noAdherentPrecedent);
	}

	@Override
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		checkTarget();
		return target.getDestinataireAvecSonHistorique(ctbId);
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		checkTarget();
		return target.suspendreContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		checkTarget();
		return target.activerContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		checkTarget();
		return target.accepterDemande(idDemande, retourAttendu, description);
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		checkTarget();
		return target.refuserDemande(idDemande, retourAttendu, description);
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		checkTarget();
		return target.quittancer(noCtb);
	}
}
