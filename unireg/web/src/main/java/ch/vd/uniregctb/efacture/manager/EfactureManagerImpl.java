package ch.vd.uniregctb.efacture.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeHistorisee;
import ch.vd.unireg.interfaces.efacture.data.DestinataireHistorise;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.efacture.ArchiveKey;
import ch.vd.uniregctb.efacture.EFactureResponseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.efacture.EvenementEfactureException;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.WebContextUtils;

public class EfactureManagerImpl implements EfactureManager {

	private EFactureService eFactureService;
	private EFactureResponseService eFactureResponseService;
	private MessageSource messageSource;

	private long timeOutForReponse;

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande) throws EditiqueException, EvenementEfactureException {
		final String idArchivage = eFactureService.imprimerDocumentEfacture(ctbId, typeDocument, dateDemande);
		final TypeAttenteDemande typeAttenteEFacture = determineTypeAttenteEfacture(typeDocument);
		final String messageAvecVisaUser = getMessageAvecVisaUser();
		final String description = typeAttenteEFacture.getDescription()+" "+messageAvecVisaUser;
		return eFactureService.notifieMiseEnattenteInscription(idDemande, typeAttenteEFacture, description, idArchivage, true);
	}

	private String getMessageAvecVisaUser() {
		String user = AuthenticationHelper.getCurrentPrincipal();
		return String.format("Traitement manuel par %s", user);
	}

	@Override
	public ch.vd.uniregctb.efacture.HistoriqueDestinataire getHistoriqueDestinataire(long ctbId) {
		DestinataireHistorise historiqueDestinataireWrapper = eFactureService.getHistoriqueDestinataire(ctbId);
		if(historiqueDestinataireWrapper == null){
			return new ch.vd.uniregctb.efacture.HistoriqueDestinataire();
		}
		return getHistoriqueDestinataire(historiqueDestinataireWrapper);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String suspendreContribuable(long ctbId) throws EvenementEfactureException {
		String description = getMessageAvecVisaUser();
		return eFactureService.suspendreContribuable(ctbId, true, description);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String activerContribuable(long ctbId) throws EvenementEfactureException {
		String description = getMessageAvecVisaUser();
		return eFactureService.activerContribuable(ctbId, true, description);
	}

	@Override
	public boolean isReponseReçuDeEfacture(String businessId) {
		return eFactureResponseService.waitForResponse(businessId, timeOutForReponse);
	}

	@Override
	public String accepterDemande(String idDemande) throws EvenementEfactureException {
		String description = getMessageAvecVisaUser();
		return eFactureService.accepterDemande(idDemande, true, description);
	}

	@Override
	public String refuserDemande(String idDemande) throws EvenementEfactureException {
		String description = getMessageAvecVisaUser();
		return eFactureService.refuserDemande(idDemande,true, description);
	}


	private ch.vd.uniregctb.efacture.HistoriqueDestinataire getHistoriqueDestinataire(DestinataireHistorise destinataire) {
		final ch.vd.uniregctb.efacture.HistoriqueDestinataire historiqueDestinataire = new ch.vd.uniregctb.efacture.HistoriqueDestinataire();
		historiqueDestinataire.setCtbId(destinataire.getCtbId());
		final List<ch.vd.uniregctb.efacture.HistoriqueDemande> demandes = new ArrayList<ch.vd.uniregctb.efacture.HistoriqueDemande>();
		//On charge l'historique des demandes.
		for (DemandeHistorisee historiqueDemandeWrapper : destinataire.getHistoriqueDemandes()) {
			ch.vd.uniregctb.efacture.HistoriqueDemande historiqueDemande  = new ch.vd.uniregctb.efacture.HistoriqueDemande();
			historiqueDemande.setIdDemande(historiqueDemandeWrapper.getIdDemande());
			historiqueDemande.setDateDemande(historiqueDemandeWrapper.getDateDemande());
			final List<ch.vd.uniregctb.efacture.EtatDemande>etatsDemande = new ArrayList<ch.vd.uniregctb.efacture.EtatDemande>();
			for(EtatDemande etatDemandeWrapper: historiqueDemandeWrapper.getHistoriqueEtats()){
				ch.vd.uniregctb.efacture.EtatDemande etatDemande = getEtatDemande(etatDemandeWrapper);
				etatsDemande.add(etatDemande);
			}
			historiqueDemande.setEtats(etatsDemande);
			demandes.add(historiqueDemande);

		}
		historiqueDestinataire.setDemandes(demandes);

		//Chargement de l'historique des états du déstinataire
		List<EtatDestinataire> etatsDestinataireWrapper = destinataire.getEtats();
		final List<ch.vd.uniregctb.efacture.EtatDestinataire> etats = new ArrayList<ch.vd.uniregctb.efacture.EtatDestinataire>();
		for (EtatDestinataire etatDestinataireWrapper : etatsDestinataireWrapper) {
			ch.vd.uniregctb.efacture.EtatDestinataire etatDestinataire = getEtatDestinataire(etatDestinataireWrapper);
			etats.add(etatDestinataire);
		}
		historiqueDestinataire.setEtats(etats);

		historiqueDestinataire.setEtats(revertList(historiqueDestinataire.getEtats()));
		historiqueDestinataire.setDemandes(revertList(historiqueDestinataire.getDemandes()));
		historiqueDestinataire.setActivable(destinataire.isActivable());
		historiqueDestinataire.setSuspendable(destinataire.isSuspendable());
		for (ch.vd.uniregctb.efacture.HistoriqueDemande demande : historiqueDestinataire.getDemandes()) {
			demande.setEtats(revertList(demande.getEtats()));
		}
		return historiqueDestinataire;
	}

	private ch.vd.uniregctb.efacture.EtatDestinataire getEtatDestinataire(EtatDestinataire etatDestinataireWrapper) {
		final RegDate dateObtention = etatDestinataireWrapper.getDateObtention();
		final String motifObtention = etatDestinataireWrapper.getDescriptionRaison();
		final String key = etatDestinataireWrapper.getChampLibre();
		final TypeAttenteDemande typeAttenteEFacture = TypeAttenteDemande.valueOf(etatDestinataireWrapper.getCodeRaison());
		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(typeAttenteEFacture);
		final String label  = "label.efacture.etat.destinataire."+etatDestinataireWrapper.getEtatDestinataire();
		final String descriptionEtat = messageSource.getMessage(label,null, WebContextUtils.getDefaultLocale());
		final ArchiveKey archiveKey = new ArchiveKey(typeDocumentEditique,key);
		return new ch.vd.uniregctb.efacture.EtatDestinataire(dateObtention,motifObtention,archiveKey,descriptionEtat);
	}

	private ch.vd.uniregctb.efacture.EtatDemande getEtatDemande(EtatDemande etatDemandeWrapper) {
		final RegDate dateObtention = etatDemandeWrapper.getDate();
		final String motifObtention = etatDemandeWrapper.getDescriptionRaison();
		final TypeAttenteDemande typeAttenteEFacture = TypeAttenteDemande.valueOf(etatDemandeWrapper.getCodeRaison());

		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(typeAttenteEFacture);
		final String key = etatDemandeWrapper.getChampLibre();
		final String label  = "label.efacture.etat.demande."+etatDemandeWrapper.getTypeEtatDemande();
		final String descriptionEtat = messageSource.getMessage(label,null, WebContextUtils.getDefaultLocale());

		final TypeEtatDemande typeEtatDemande = etatDemandeWrapper.getTypeEtatDemande();
		final ArchiveKey archiveKey = (key ==null || typeDocumentEditique ==null) ?null : new ArchiveKey(typeDocumentEditique,key);
		return new ch.vd.uniregctb.efacture.EtatDemande(dateObtention,motifObtention,archiveKey,descriptionEtat,typeEtatDemande, typeAttenteEFacture);
	}


	private TypeDocumentEditique determineTypeDocumentEditique(TypeAttenteDemande typeAttenteEFacture) {
		if(typeAttenteEFacture == TypeAttenteDemande.EN_ATTENTE_CONTACT){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_CONTACT;
		}
		else if(typeAttenteEFacture == TypeAttenteDemande.EN_ATTENTE_SIGNATURE){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_SIGNATURE;
		}
		return null;
	}

	private TypeAttenteDemande determineTypeAttenteEfacture(TypeDocument typeDocument) throws IllegalArgumentException {
		switch (typeDocument) {
		case E_FACTURE_ATTENTE_CONTACT:
			return TypeAttenteDemande.EN_ATTENTE_CONTACT;
		case E_FACTURE_ATTENTE_SIGNATURE:
			return TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
		default:
			throw new IllegalArgumentException("Le type de document " + typeDocument.name() + " est inconnue");
		}
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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
