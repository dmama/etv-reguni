package ch.vd.uniregctb.efacture.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.EtatDemandeWrapper;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataireWrapper;
import ch.vd.unireg.interfaces.efacture.data.HistoriqueDemandeWrapper;
import ch.vd.unireg.interfaces.efacture.data.HistoriqueDestinataireWrapper;
import ch.vd.unireg.interfaces.efacture.data.TypeStatusDemande;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.efacture.ArchiveKey;
import ch.vd.uniregctb.efacture.EFactureResponseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.efacture.EtatDemande;
import ch.vd.uniregctb.efacture.EtatDestinataire;
import ch.vd.uniregctb.efacture.EvenementEfactureException;
import ch.vd.uniregctb.efacture.HistoriqueDemande;
import ch.vd.uniregctb.efacture.HistoriqueDestinataire;
import ch.vd.uniregctb.efacture.TypeAttenteEFacture;
import ch.vd.uniregctb.efacture.TypeEtatDemande;
import ch.vd.uniregctb.type.TypeDocument;

public class EfactureManagerImpl implements EfactureManager {

	private EFactureService eFactureService;
	private EFactureResponseService eFactureResponseService;

	private long timeOutForReponse;

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande) throws EditiqueException, EvenementEfactureException {
		final String idArchivage = eFactureService.imprimerDocumentEfacture(ctbId, typeDocument, dateDemande);
		return eFactureService.notifieMiseEnattenteInscription(idDemande, typeDocument, idArchivage, true);
	}

	@Override
	public HistoriqueDestinataire getHistoriqueDestinataire(long ctbId) {
		HistoriqueDestinataireWrapper historiqueDestinataireWrapper = eFactureService.getHistoriqueDestinataire(ctbId);
		if(historiqueDestinataireWrapper == null){
			return new HistoriqueDestinataire();
		}
		final HistoriqueDestinataire historiqueDestinataire = getHistoriqueDestinataireFromWrapper(historiqueDestinataireWrapper);
		return historiqueDestinataire;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String suspendreContribuable(long ctbId) throws EvenementEfactureException {
		return eFactureService.suspendreContribuable(ctbId,true);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String activerContribuable(long ctbId) throws EvenementEfactureException {
		return eFactureService.activerContribuable(ctbId,true);
	}

	@Override
	public boolean isReponseReçuDeEfacture(String businessId) {
		return eFactureResponseService.waitForResponse(businessId,timeOutForReponse);
	}

	@Override
	public String accepterDemande(String idDemande) throws EvenementEfactureException {
		return eFactureService.accepterDemande(idDemande, true);
	}

	@Override
	public String refuserDemande(String idDemande) throws EvenementEfactureException {
		return eFactureService.refuserDemande(idDemande,true);
	}


	private HistoriqueDestinataire getHistoriqueDestinataireFromWrapper(HistoriqueDestinataireWrapper historiqueDestinataireWrapper) {
		final HistoriqueDestinataire historiqueDestinataire = new HistoriqueDestinataire();
		historiqueDestinataire.setCtbId(historiqueDestinataireWrapper.getCtbId());
		final List<HistoriqueDemande> demandes = new ArrayList<HistoriqueDemande>();
		//On charge l'historique des demandes.
		for (HistoriqueDemandeWrapper historiqueDemandeWrapper : historiqueDestinataireWrapper.getHistoriqueDemandeWrapper()) {
			HistoriqueDemande historiqueDemande  = new HistoriqueDemande();
			historiqueDemande.setIdDemande(historiqueDemandeWrapper.getId());
			historiqueDemande.setDateDemande(historiqueDemandeWrapper.getDateInscription());
			final List<EtatDemande>etatsDemande = new ArrayList<EtatDemande>();
			for(EtatDemandeWrapper etatDemandeWrapper: historiqueDemandeWrapper.getHistoriqueEtatDemandeWrapper()){
				EtatDemande etatDemande = getEtatDemande(etatDemandeWrapper);
				etatsDemande.add(etatDemande);
			}
			historiqueDemande.setEtats(etatsDemande);
			demandes.add(historiqueDemande);

		}
		historiqueDestinataire.setDemandes(demandes);

		//Chargement de l'historique des états du déstinataire
		List<EtatDestinataireWrapper> etatsDestinataireWrapper = historiqueDestinataireWrapper.getEtats();
		final List<EtatDestinataire> etats = new ArrayList<EtatDestinataire>();
		for (EtatDestinataireWrapper etatDestinataireWrapper : etatsDestinataireWrapper) {
			EtatDestinataire etatDestinataire = getEtatDestinataire(etatDestinataireWrapper);
			etats.add(etatDestinataire);
		}
		historiqueDestinataire.setEtats(etats);

		historiqueDestinataire.setEtats(revertList(historiqueDestinataire.getEtats()));
		historiqueDestinataire.setDemandes(revertList(historiqueDestinataire.getDemandes()));
		historiqueDestinataire.setActivable(historiqueDestinataireWrapper.isActivable());
		historiqueDestinataire.setSuspendable(historiqueDestinataireWrapper.isSuspendable());
		for (HistoriqueDemande demande : historiqueDestinataire.getDemandes()) {
			demande.setEtats(revertList(demande.getEtats()));
		}
		return historiqueDestinataire;
	}

	private EtatDestinataire getEtatDestinataire(EtatDestinataireWrapper etatDestinataireWrapper) {
		final RegDate dateObtention = etatDestinataireWrapper.getDateObtention();
		final String motifObtention = etatDestinataireWrapper.getDescriptionRaison();
		final String key = etatDestinataireWrapper.getChampLibre();
		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(etatDestinataireWrapper.getCodeRaison());
		final String descriptionEtat = etatDestinataireWrapper.getStatusDestinataire()!=null? etatDestinataireWrapper.getStatusDestinataire().getDescription():null;
		final ArchiveKey archiveKey = new ArchiveKey(typeDocumentEditique,key);
		return new EtatDestinataire(dateObtention,motifObtention,archiveKey,descriptionEtat);
	}

	private EtatDemande getEtatDemande(EtatDemandeWrapper etatDemandeWrapper) {
		final RegDate dateObtention = etatDemandeWrapper.getDate();
		final String motifObtention = etatDemandeWrapper.getDescriptionRaison();
		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(etatDemandeWrapper.getCodeRaison());
		final String key = etatDemandeWrapper.getChampLibre();
		final String descriptionEtat = etatDemandeWrapper.getStatusDemande().getDescription();
		final TypeEtatDemande typeEtatDemande = determinerTypeEtatDemande(etatDemandeWrapper.getStatusDemande(),etatDemandeWrapper.getCodeRaison());
		final ArchiveKey archiveKey = (key ==null || typeDocumentEditique ==null) ?null : new ArchiveKey(typeDocumentEditique,key);

		return new EtatDemande(dateObtention,motifObtention,archiveKey,descriptionEtat,typeEtatDemande);
	}

	private TypeEtatDemande determinerTypeEtatDemande(TypeStatusDemande statusDemande,Integer codeRaison) {
		switch (statusDemande){
		 case A_TRAITE:
			 return TypeEtatDemande.RECUE;
		 case IGNOREE:
			 return TypeEtatDemande.IGNOREE;
		 case REFUSEE:
			 return TypeEtatDemande.REFUSEE;
		 case VALIDATION_EN_COURS:
			 if(TypeAttenteEFacture.EN_ATTENTE_SIGNATURE.getCode() == codeRaison ){
				 return TypeEtatDemande.EN_ATTENTE_SIGNATURE;
			 }
			 else if(TypeAttenteEFacture.EN_ATTENTE_CONTACT.getCode() == codeRaison ){
				 return TypeEtatDemande.EN_ATTENTE_CONTACT;
			 }
			 break;

		 case VALIDEE:
			 return TypeEtatDemande.VALIDEE;
		}
		//TODO le type de demande ne match pas avec ce que renvoie le web service efacture
		//A clarifier avant d epoursuivre l'implementation
		//TODO C'est temporaire pour le testing, à supprimer lorsque l'on aura plus d'info sur les correspondances
		return TypeEtatDemande.EN_ATTENTE_SIGNATURE;  //To change body of created methods use File | Settings | File Templates.
	}

	private TypeDocumentEditique determineTypeDocumentEditique(Integer codeRaison) {
		if(codeRaison == TypeAttenteEFacture.EN_ATTENTE_CONTACT.getCode()){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_CONTACT;
		}
		else if(codeRaison == TypeAttenteEFacture.EN_ATTENTE_SIGNATURE.getCode()){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_SIGNATURE;
		}
		return null;
	}



	private static <T> List<T> revertList(List<T> source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		else {
			final List<T> dest = new ArrayList<T>(source.size());
			final ListIterator<T> iterator = source.listIterator(source.size());
			while (iterator.hasPrevious()) {
				dest.add(iterator.previous());
			}
			return dest;
		}
	}

	public void seteFactureService(EFactureService eFactureService) {
		this.eFactureService = eFactureService;
	}

	public void seteFactureResponseService(EFactureResponseService eFactureResponseService) {
		this.eFactureResponseService = eFactureResponseService;
	}

	public void setTimeOutForReponse(long timeOutForReponse) {
		this.timeOutForReponse = timeOutForReponse;
	}
}
