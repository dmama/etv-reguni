package ch.vd.uniregctb.efacture;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

public class MockEFactureService implements EFactureService {
	@Override
	public String notifieMiseEnattenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws
			EvenementEfactureException {
		return "";
	}

	@Override
	public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException {
		return "";
	}

	@Override
	public DemandeAvecHisto getDemandeInscriptionEnCoursDeTraitement(long ctbId) {
		return null;
	}

	@Override
	public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String noAvs) throws AdresseException {
		return null;
	}

	@Override
	public void updateEmailContribuable(long ctbId, String email) {
	}

	@Override
	public boolean valideEtatContribuablePourInscription(long ctbId) {
		return false;
	}

	@Override
	public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
		return null;
	}

	@Override
	public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return "";
	}

	@Override
	public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
		return "";
	}
}
