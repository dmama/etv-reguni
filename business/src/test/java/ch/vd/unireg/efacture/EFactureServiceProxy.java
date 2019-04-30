package ch.vd.unireg.efacture;

import java.math.BigInteger;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.type.TypeDocument;

public class EFactureServiceProxy implements EFactureService {

	private MockEFactureService target;

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
	                                              boolean retourAttendu) throws EFactureException {
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
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EFactureException {
		checkTarget();
		return target.suspendreContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EFactureException {
		checkTarget();
		return target.activerContribuable(ctbId, retourAttendu, description);
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EFactureException {
		checkTarget();
		return target.accepterDemande(idDemande, retourAttendu, description);
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EFactureException {
		checkTarget();
		return target.refuserDemande(idDemande, retourAttendu, description);
	}

	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EFactureException {
		checkTarget();
		return target.quittancer(noCtb);
	}

	@Override
	public String modifierEmailContribuable(long noCtb, @Nullable String newEmail, boolean retourAttendu, String description) throws EFactureException {
		checkTarget();
		return target.modifierEmailContribuable(noCtb, newEmail, retourAttendu, description);
	}

	@Override
	public void demanderDesinscriptionContribuable(long noCtb, String idNouvelleDemande, String description) throws EFactureException {
		checkTarget();
		target.demanderDesinscriptionContribuable(noCtb, idNouvelleDemande, description);
	}

	public void commit() {
		checkTarget();
		target.commit();
	}

	public void rollback() {
		checkTarget();
		target.rollback();
	}
}
