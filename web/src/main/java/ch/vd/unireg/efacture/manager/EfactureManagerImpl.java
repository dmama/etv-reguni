package ch.vd.unireg.efacture.manager;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.efacture.ArchiveKey;
import ch.vd.unireg.efacture.DemandeAvecHistoView;
import ch.vd.unireg.efacture.DestinataireAvecHistoView;
import ch.vd.unireg.efacture.DocumentEFacture;
import ch.vd.unireg.efacture.DocumentEFactureDAO;
import ch.vd.unireg.efacture.EFactureException;
import ch.vd.unireg.efacture.EFactureHelper;
import ch.vd.unireg.efacture.EFactureResponseService;
import ch.vd.unireg.efacture.EFactureService;
import ch.vd.unireg.efacture.EtatDemandeView;
import ch.vd.unireg.efacture.EtatDestinataireView;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.EtatDestinataire;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.utils.WebContextUtils;

public class EfactureManagerImpl implements EfactureManager {

	private EFactureService eFactureService;
	private EFactureResponseService eFactureResponseService;
	private MessageSource messageSource;
	private ServiceInfrastructureService infraService;
	private DocumentEFactureDAO documentEFactureDAO;

	private long timeOutForReponse;

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String envoyerDocumentAvecNotificationEFacture(long ctbId, TypeDocument typeDocument, String idDemande, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException,
			EFactureException {
		final String idArchivage = eFactureService.imprimerDocumentEfacture(ctbId, typeDocument, dateDemande, noAdherent, dateDemandePrecedente, noAdherentPrecedent);
		final TypeAttenteDemande typeAttenteEFacture = determineTypeAttenteEfacture(typeDocument);
		final String messageAvecVisaUser = getMessageAvecVisaUser();
		final String description = String.format("%s %s", typeAttenteEFacture.getDescription(), messageAvecVisaUser);
		return eFactureService.notifieMiseEnAttenteInscription(idDemande, typeAttenteEFacture, description, idArchivage, true);
	}

	private static String getMessageAvecVisaUser() {
		final String user = AuthenticationHelper.getCurrentPrincipal();
		return String.format("Traitement manuel par %s.", user);
	}

	private static String getMessageAvecVisaUser(@Nullable String comment) {
		if (StringUtils.isBlank(comment)) {
			return getMessageAvecVisaUser();
		}
		else {
			final String user = AuthenticationHelper.getCurrentPrincipal();
			return String.format("[%s] %s", user, StringUtils.abbreviate(comment, getMaxLengthForManualComment(user)));
		}
	}

	/**
	 * @param user le visa utilisé
	 * @return taille maximale du commentaire manuel autorisé avec le visa donné
	 */
	private static int getMaxLengthForManualComment(String user) {
		return 252 - user.length();     // 255 - ('[' + visa + ']' + ' ')
	}

	@Override
	public int getMaxLengthForManualComment() {
		final String user = AuthenticationHelper.getCurrentPrincipal();
		return getMaxLengthForManualComment(user);
	}

	@Override
	@Nullable
	@Transactional(rollbackFor = Throwable.class, readOnly = true)
	public DestinataireAvecHistoView getDestinataireAvecSonHistorique(long ctbId) {
		final DestinataireAvecHisto destinataire = eFactureService.getDestinataireAvecSonHistorique(ctbId);
		return destinataire != null ? buildDestinataireAvecHistoView(destinataire) : null;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String suspendreContribuable(long ctbId, @Nullable String comment) throws EFactureException {
		final String description = getMessageAvecVisaUser(comment);
		return eFactureService.suspendreContribuable(ctbId, true, description);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String activerContribuable(long ctbId, @Nullable String comment) throws EFactureException {
		final String description = getMessageAvecVisaUser(comment);
		return eFactureService.activerContribuable(ctbId, true, description);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public String modifierEmail(long noCtb, String newEmail) throws EFactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.modifierEmailContribuable(noCtb, newEmail, true, description);
	}

	@Override
	public boolean isReponseRecueDeEfacture(String businessId) {
		return eFactureResponseService.waitForResponse(businessId, Duration.ofMillis(timeOutForReponse));
	}

	@Override
	public String accepterDemande(String idDemande) throws EFactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.accepterDemande(idDemande, true, description);
	}

	@Override
	public String refuserDemande(String idDemande) throws EFactureException {
		final String description = getMessageAvecVisaUser();
		return eFactureService.refuserDemande(idDemande, true, description);
	}

	@Transactional(rollbackFor = Throwable.class)
	@Override
	public ResultatQuittancement quittancer(Long noCtb) throws EFactureException {
		return eFactureService.quittancer(noCtb);
	}

	@Override
	public String getMessageQuittancement(ResultatQuittancement resultatQuittancement, long noCtb) {
		// Note: d'un point de vue logique on pourrait simplifier avec un seul 'if' mais on evite
		//       d'appeler waitForResponse() pour rien (3 secondes de délai si le resultat n'est pas ok)
		if (resultatQuittancement.isOk()) {
			if (resultatQuittancement.equals(ResultatQuittancement.dejaInscrit())) {
				return ResultatQuittancement.dejaInscrit().getDescription(noCtb);
			}
			else if (eFactureResponseService.waitForResponse(resultatQuittancement.getBusinessId(), Duration.ofMillis(timeOutForReponse))) {
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
		res.setInscrit(destinataire.isInscrit());

		//On charge l'historique des demandes.
		final List<DemandeAvecHisto> historiqueDemandes = destinataire.getHistoriqueDemandes();
		final List<DemandeAvecHistoView> demandes = new ArrayList<>(historiqueDemandes.size());
		for (DemandeAvecHisto demande : CollectionsUtils.revertedOrder(historiqueDemandes)) {
			final List<EtatDemande> historiqueEtats = demande.getHistoriqueEtats();
			final List<EtatDemandeView> etatsDemande = new ArrayList<>(historiqueEtats.size());
			for (EtatDemande etat : CollectionsUtils.revertedOrder(historiqueEtats)) {
				final EtatDemandeView etatView = getEtatDemande(etat, destinataire.getCtbId());
				etatsDemande.add(etatView);
			}

			final String canonicalNoAvs = AvsHelper.removeSpaceAndDash(demande.getNoAvs());
			final String noAvs = EFactureHelper.isNavs13(canonicalNoAvs) ? canonicalNoAvs : null;
			final DemandeAvecHistoView view = new DemandeAvecHistoView(demande.getIdDemande(), demande.getDateDemande(), demande.getNoAdherent(), FormatNumeroHelper.formatNumAVS(noAvs),
			                                                           demande.getEmail(), demande.getAction(), etatsDemande);
			demandes.add(view);
		}
		res.setDemandes(demandes);

		//Chargement de l'historique des états du destinataire
		final List<EtatDestinataire> historiquesEtats = destinataire.getHistoriquesEtats();
		final List<EtatDestinataireView> etats = new ArrayList<>(historiquesEtats.size());
		for (EtatDestinataire etat : CollectionsUtils.revertedOrder(historiquesEtats)) {
			final EtatDestinataireView etatView = getEtatDestinataire(etat);
			etats.add(etatView);
		}
		res.setEtats(etats);

		return res;
	}

	private EtatDestinataireView getEtatDestinataire(EtatDestinataire etat) {
		return new EtatDestinataireView(etat.getDateObtention(),
		                                etat.getDescriptionRaison(),
		                                messageSource.getMessage("label.efacture.etat.destinataire." + etat.getType(), null, WebContextUtils.getDefaultLocale()),
		                                etat.getEmail());
	}

	private EtatDemandeView getEtatDemande(EtatDemande etat, Long ctbId) {
		final TypeDocumentEditique typeDocumentEditique = determineTypeDocumentEditique(etat.getType());
		final String descriptionEtat = messageSource.getMessage("label.efacture.etat.demande." + etat.getType(), null, WebContextUtils.getDefaultLocale());
		final String key = etat.getChampLibre();

		final String urlVisualisationExerneDocument = Optional.ofNullable(key)
				.map(cleArchivage -> getCleDocument(ctbId, cleArchivage))
				.map(cleDocument -> infraService.getUrlVisualisationDocument(ctbId, null, cleDocument))
				.orElse(null);

		final ArchiveKey archiveKey = (key == null || typeDocumentEditique == null) ? null : new ArchiveKey(typeDocumentEditique, key);
		return new EtatDemandeView(etat.getDate(), etat.getDescriptionRaison(), archiveKey, urlVisualisationExerneDocument, descriptionEtat, etat.getType());
	}

	@Nullable
	private String getCleDocument(Long ctbId, String cleArchivage) {
		final DocumentEFacture doc = documentEFactureDAO.findByTiersEtCleArchivage(ctbId, cleArchivage);
		return doc != null ? doc.getCleDocument() : null;
	}

	private static TypeDocumentEditique determineTypeDocumentEditique(TypeEtatDemande typeEtat) {
		if (typeEtat.getTypeAttente() == TypeAttenteDemande.EN_ATTENTE_CONTACT) {
			return TypeDocumentEditique.E_FACTURE_ATTENTE_CONTACT;
		}
		else if (typeEtat.getTypeAttente() == TypeAttenteDemande.EN_ATTENTE_SIGNATURE) {
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

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setDocumentEFactureDAO(DocumentEFactureDAO documentEFactureDAO) {
		this.documentEFactureDAO = documentEFactureDAO;
	}
}
