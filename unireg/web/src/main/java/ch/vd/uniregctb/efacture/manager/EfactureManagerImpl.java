package ch.vd.uniregctb.efacture.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.efacture.ArchiveKey;
import ch.vd.uniregctb.efacture.DemandeAvecHistoView;
import ch.vd.uniregctb.efacture.DestinataireAvecHistoView;
import ch.vd.uniregctb.efacture.EFactureResponseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.efacture.EtatDemandeView;
import ch.vd.uniregctb.efacture.EtatDestinataireView;
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
		final String user = AuthenticationHelper.getCurrentPrincipal();
		return String.format("Traitement manuel par %s", user);
	}

	@Override
	@Nullable
	public DestinataireAvecHistoView getDestinataireAvecSonHistorique(long ctbId) {
		final DestinataireAvecHisto destinataire = eFactureService.getDestinataireAvecSonHistorique(ctbId);
		return destinataire != null ? buildDestinataireAvecHistoView(destinataire) : null;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String suspendreContribuable(long ctbId) throws EvenementEfactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.suspendreContribuable(ctbId, true, description);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String activerContribuable(long ctbId) throws EvenementEfactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.activerContribuable(ctbId, true, description);
	}

	@Override
	public boolean isReponseReçuDeEfacture(String businessId) {
		return eFactureResponseService.waitForResponse(businessId, timeOutForReponse);
	}

	@Override
	public String accepterDemande(String idDemande) throws EvenementEfactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.accepterDemande(idDemande, true, description);
	}

	@Override
	public String refuserDemande(String idDemande) throws EvenementEfactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.refuserDemande(idDemande,true, description);
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EvenementEfactureException {
		return eFactureService.quittancer(noCtb);
	}

	@Override
	public String getMessageQuittancement(ResultatQuittancement resultatQuittancement, long noCtb) {
		// Note: d'un point de vue logique on pourrait simplifier avec un seul 'if' mais on evite
		//       d'appeler waitForResponse() pour rien (3 secondes de délai si le resultat n'est pas ok)
		if (resultatQuittancement.isOk()) {
			if (resultatQuittancement.equals(ResultatQuittancement.dejaInscrit())) {
				return ResultatQuittancement.dejaInscrit().getDescription(noCtb);
			} else if (eFactureResponseService.waitForResponse(resultatQuittancement.getBusinessId(), timeOutForReponse)) {
				return ResultatQuittancement.ok(resultatQuittancement).getDescription(noCtb);
			}
		}
		return resultatQuittancement.getDescription(noCtb);
	}


	private DestinataireAvecHistoView buildDestinataireAvecHistoView(DestinataireAvecHisto destinataire) {
		final DestinataireAvecHistoView res = new DestinataireAvecHistoView();
		res.setCtbId(destinataire.getCtbId());
		res.setActivable(destinataire.isActivable());
		res.setSuspendable(destinataire.isSuspendable());

		//On charge l'historique des demandes.
		final int sizeDemandes = destinataire.getHistoriqueDemandes().size();
		final List<DemandeAvecHistoView> demandes = new ArrayList<DemandeAvecHistoView>(sizeDemandes);
		for (ListIterator<DemandeAvecHisto> it = destinataire.getHistoriqueDemandes().listIterator(sizeDemandes); it.hasPrevious(); ) {
			final DemandeAvecHisto demande = it.previous();
			final DemandeAvecHistoView view  = new DemandeAvecHistoView();
			view.setIdDemande(demande.getIdDemande());
			view.setDateDemande(demande.getDateDemande());
			final int sizeEtatDemandes = demande.getHistoriqueEtats().size();
			final List<EtatDemandeView> etatsDemande = new ArrayList<EtatDemandeView>(sizeEtatDemandes);
			for(ListIterator<EtatDemande> jt = demande.getHistoriqueEtats().listIterator(sizeEtatDemandes); jt.hasPrevious(); ){
				final EtatDemandeView etatView = getEtatDemande(jt.previous());
				etatsDemande.add(etatView);
			}
			view.setEtats(etatsDemande);
			demandes.add(view);
		}
		res.setDemandes(demandes);

		//Chargement de l'historique des états du destinataire
		final int sizeEtatDestinataire = destinataire.getEtats().size();
		final List<EtatDestinataireView> etats = new ArrayList<EtatDestinataireView>(sizeEtatDestinataire);
		for (ListIterator<EtatDestinataire> it = destinataire.getEtats().listIterator(sizeEtatDestinataire); it.hasPrevious(); ) {
			etats.add(getEtatDestinataire(it.previous()));
		}
		res.setEtats(etats);

		return res;
	}

	private EtatDestinataireView getEtatDestinataire(EtatDestinataire etat) {
		return new EtatDestinataireView(
				etat.getDateObtention(),
				etat.getDescriptionRaison(),
				null, // pas de document relatif au destinataire, prévu dans un futur proche
				messageSource.getMessage("label.efacture.etat.destinataire."+ etat.getType(), null, WebContextUtils.getDefaultLocale()));
	}

	private EtatDemandeView getEtatDemande(EtatDemande etat) {
		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(etat.getType());
		String descriptionEtat = messageSource.getMessage(
				"label.efacture.etat.demande."+ etat.getType(),
				null,
				WebContextUtils.getDefaultLocale());
		// TODO A supprimer à terme
		// Utile pour l'instant car les données de tests e-facture non pas les descriptions adéquates
		// pour differencier les differents "sous-états" de validation en cours
		if ("true".equals(System.getProperty("debug-efacture"))) {
				if (etat.getType().isEnAttente()) {
					descriptionEtat += " " + etat.getType().name();
				}
		}
		final String key = etat.getChampLibre();
		final ArchiveKey archiveKey = (key ==null || typeDocumentEditique ==null) ?null : new ArchiveKey(typeDocumentEditique,key);
		return new EtatDemandeView(etat.getDate(), etat.getDescriptionRaison(),archiveKey,descriptionEtat,etat.getType());
	}


	private static TypeDocumentEditique determineTypeDocumentEditique(TypeEtatDemande typeEtat) {
		if(typeEtat.getTypeAttente() == TypeAttenteDemande.EN_ATTENTE_CONTACT){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_CONTACT;
		}
		else if(typeEtat.getTypeAttente() == TypeAttenteDemande.EN_ATTENTE_SIGNATURE){
			return TypeDocumentEditique.E_FACTURE_ATTENTE_SIGNATURE;
		}
		return null;
	}

	private static TypeAttenteDemande determineTypeAttenteEfacture(TypeDocument typeDocument) throws IllegalArgumentException {
		switch (typeDocument) {
		case E_FACTURE_ATTENTE_CONTACT:
			return TypeAttenteDemande.EN_ATTENTE_CONTACT;
		case E_FACTURE_ATTENTE_SIGNATURE:
			return TypeAttenteDemande.EN_ATTENTE_SIGNATURE;
		default:
			throw new IllegalArgumentException("Le type de document " + typeDocument.name() + " est inconnue");
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
