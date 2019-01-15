package ch.vd.unireg.editique.impl;

import java.time.Duration;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatTimeout;
import ch.vd.unireg.editique.EditiqueRetourImpressionStorageService;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.editique.EvenementEditiqueSender;
import ch.vd.unireg.editique.FormatDocumentEditique;
import ch.vd.unireg.editique.RetourImpressionToInboxTrigger;
import ch.vd.unireg.editique.RetourImpressionTrigger;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.inbox.InboxService;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

/**
 * Implémentation standard de {@link EditiqueService}.
 */
public final class EditiqueServiceImpl implements EditiqueService, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditiqueServiceImpl.class);

	private EvenementEditiqueSender sender;

	private EditiqueRetourImpressionStorageService retourImpressionStorage;

	private InboxService inboxService;

	/**
	 * Temps d'attente (en secondes) du retour du document PDF / PCL lors d'une impression locale synchrone
	 */
	private int syncReceiveTimeout = 120;

	/**
	 * Temps d'attente (en secondes) du retour d'impression locale pas nécessairement synchrone avant de
	 * partir en mode asynchrone en re-routant le résultat sur l'inbox du demandeur
	 */
	private int asyncReceiveDelay = 15;

	/**
	 * Délai de conservation (en heures) des retours d'impression reroutés par l'inbox
	 */
	private int hoursRetourImpressionExpiration = 2;

	private interface TimeoutManager {
		EditiqueResultat onTimeout(EditiqueResultatTimeout src);
	}

	private final class InboxRoutingTimeoutManager implements TimeoutManager  {
		private final String nomDocument;
		private final String descriptionDocument;

		private InboxRoutingTimeoutManager(String nomDocument, String descriptionDocument) {
			this.nomDocument = nomDocument;
			this.descriptionDocument = descriptionDocument;
		}

		@Override
		public EditiqueResultat onTimeout(EditiqueResultatTimeout src) {
			final String visa = AuthenticationHelper.getCurrentPrincipal();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Retour éditique un peu lent pour le document '%s', routage demandé vers l'inbox de l'utilisateur %s", nomDocument, visa));
			}

			final RetourImpressionTrigger trigger = new RetourImpressionToInboxTrigger(inboxService, visa, descriptionDocument, hoursRetourImpressionExpiration);
			retourImpressionStorage.registerTrigger(nomDocument, trigger);
			return new EditiqueResultatReroutageInboxImpl(src.getIdDocument());
		}
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(final String nomDocument, final TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive, final String description) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, asyncReceiveDelay, new InboxRoutingTimeoutManager(nomDocument, description));
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, FichierImpression document, boolean archive, String description) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, asyncReceiveDelay, new InboxRoutingTimeoutManager(nomDocument, description));
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, ch.vd.unireg.xml.editique.pp.FichierImpression document, boolean archive,
	                                                                   String description) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, asyncReceiveDelay, new InboxRoutingTimeoutManager(nomDocument, description));
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(final String nomDocument, final TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, syncReceiveTimeout, src -> {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Retour d'impression locale non-reçu pour document %s (%s) : Time-out", nomDocument, typeDocument));
			}
			return src;
		});
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(final String nomDocument, final TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, FichierImpression document, boolean archive) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, syncReceiveTimeout, src -> {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Retour d'impression locale non-reçu pour document %s (%s) : Time-out", nomDocument, typeDocument));
			}
			return src;
		});
	}

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe. La gestion du timeout
	 * est faite par le timeoutManager passé en paramètre
	 *
	 * @param nomDocument le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat le format souhaité
	 * @param document document XML à envoyer à éditique
	 * @param archive indicateur d'archivage
	 * @param secTimeout timeout à utiliser, en secondes
	 * @param timeoutManager sera lancé en cas de timeout
	 * @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	private EditiqueResultat creerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, XmlObject document, boolean archive, int secTimeout, TimeoutManager timeoutManager) throws EditiqueException {

		// envoi de la demande
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s)", nomDocument, typeDocument);
			LOGGER.debug(msg);
		}
		final String id = sender.envoyerDocumentImmediatement(nomDocument, typeDocument, document, typeFormat, archive);

		// demande envoyée, attente de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s) envoyée : %s", nomDocument, typeDocument, id);
			LOGGER.debug(msg);
		}

		return getRetourEditique(nomDocument, typeDocument, secTimeout, timeoutManager);
	}

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe. La gestion du timeout
	 * est faite par le timeoutManager passé en paramètre
	 *
	 * @param nomDocument le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat le format souhaité
	 * @param document document XML à envoyer à éditique
	 * @param archive indicateur d'archivage
	 * @param secTimeout timeout à utiliser, en secondes
	 * @param timeoutManager sera lancé en cas de timeout
	 * @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	private EditiqueResultat creerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, FichierImpression document, boolean archive, int secTimeout, TimeoutManager timeoutManager) throws EditiqueException {

		// envoi de la demande
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s)", nomDocument, typeDocument);
			LOGGER.debug(msg);
		}
		final String id = sender.envoyerDocumentImmediatement(nomDocument, typeDocument, document, typeFormat, archive);

		// demande envoyée, attente de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s) envoyée : %s", nomDocument, typeDocument, id);
			LOGGER.debug(msg);
		}

		return getRetourEditique(nomDocument, typeDocument, secTimeout, timeoutManager);
	}

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe. La gestion du timeout
	 * est faite par le timeoutManager passé en paramètre
	 *
	 * @param nomDocument le nom du document à transmettre à Editique.
	 * @param typeDocument le type de document
	 * @param typeFormat le format souhaité
	 * @param document document XML à envoyer à éditique
	 * @param archive indicateur d'archivage
	 * @param secTimeout timeout à utiliser, en secondes
	 * @param timeoutManager sera lancé en cas de timeout
	 * @return le document imprimé ou <b>null</b> si éditique n'a pas répondu dans les temps
	 * @throws EditiqueException si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	private EditiqueResultat creerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, FormatDocumentEditique typeFormat, ch.vd.unireg.xml.editique.pp.FichierImpression document, boolean archive, int secTimeout, TimeoutManager timeoutManager) throws EditiqueException {

		// envoi de la demande
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s)", nomDocument, typeDocument);
			LOGGER.debug(msg);
		}
		final String id = sender.envoyerDocumentImmediatement(nomDocument, typeDocument, document, typeFormat, archive);

		// demande envoyée, attente de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande d'impression locale du document %s (%s) envoyée : %s", nomDocument, typeDocument, id);
			LOGGER.debug(msg);
		}

		return getRetourEditique(nomDocument, typeDocument, secTimeout, timeoutManager);
	}

	private EditiqueResultat getRetourEditique(String nomDocument, TypeDocumentEditique typeDocument, int secTimeout, TimeoutManager timeoutManager) throws EditiqueException {
		EditiqueResultat resultat;
		try {
			resultat = retourImpressionStorage.getDocument(nomDocument, Duration.ofSeconds(secTimeout));
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		// si on n'a rien reçu dans le temps imparti, il faut faire quelque chose de spécifique ?
		if (resultat instanceof EditiqueResultatTimeout) {
			resultat = timeoutManager.onTimeout((EditiqueResultatTimeout) resultat);
		}
		else if (LOGGER.isDebugEnabled()) {
			// log de l'état de la réponse
			final String statut;
			if (resultat instanceof EditiqueResultatDocument) {
				statut = "OK";
			}
			else if (resultat instanceof EditiqueResultatErreur) {
				final EditiqueResultatErreur erreur = (EditiqueResultatErreur) resultat;
				statut = String.format("Erreur (%s/%s/%s), ", erreur.getErrorType(), erreur.getErrorCode(), erreur.getErrorMessage());
			}
			else {
				statut = String.format("Erreur inconnue '%s'", resultat);
			}
			final String msg = String.format("Réponse éditique reçue pour document %s (%s) : %s", nomDocument, typeDocument, statut);
			LOGGER.debug(msg);
		}
		return resultat;
	}

	@Override
	public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
		sender.envoyerDocument(nomDocument, typeDocument, document, null, archive);
	}

	@Override
	public String creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, FichierImpression document, boolean archive) throws EditiqueException {
		return sender.envoyerDocument(nomDocument, typeDocument, document, null, archive);
	}

	@Override
	public String creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, ch.vd.unireg.xml.editique.pp.FichierImpression document, boolean archive) throws EditiqueException {
		return sender.envoyerDocument(nomDocument, typeDocument, document, null, archive);
	}

	@Override
	public EditiqueResultat getPDFDeDocumentDepuisArchive(long noContribuable, TypeDocumentEditique typeDocument, String cleArchivage) throws EditiqueException {

		// envoi de la demande
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande de récupération de l'archive '%s' (%s) pour le contribuable %s", cleArchivage, typeDocument, FormatNumeroHelper.numeroCTBToDisplay(noContribuable));
			LOGGER.debug(msg);
		}
		final Pair<String, String> ids = sender.envoyerDemandeCopieConforme(cleArchivage, typeDocument, noContribuable);

		// demande envoyée, attente de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String msg = String.format("Demande de récupération de l'archive '%s' (%s) envoyée : %s", cleArchivage, typeDocument, ids.getLeft());
			LOGGER.debug(msg);
		}

		return getRetourEditique(ids.getRight(), typeDocument, asyncReceiveDelay, new InboxRoutingTimeoutManager(cleArchivage, String.format("Copie conforme de document pour le tiers %s", FormatNumeroHelper.numeroCTBToDisplay(noContribuable))));
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSender(EvenementEditiqueSender sender) {
		this.sender = sender;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRetourImpressionStorage(EditiqueRetourImpressionStorageService retourImpressionStorage) {
		this.retourImpressionStorage = retourImpressionStorage;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSyncReceiveTimeout(int syncReceiveTimeout) {
		this.syncReceiveTimeout = syncReceiveTimeout;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAsyncReceiveDelay(int asyncReceiveDelay) {
		this.asyncReceiveDelay = asyncReceiveDelay;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHoursRetourImpressionExpiration(int hoursRetourImpressionExpiration) {
		if (hoursRetourImpressionExpiration < 0) {
			throw new IllegalArgumentException("La valeur doit être positive ou zéro (= pas d'expiration)");
		}
		this.hoursRetourImpressionExpiration = hoursRetourImpressionExpiration;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Service de demandes d'impression initialisé (time-out synchrone : %ds, time-out asynchrone : %ds)", syncReceiveTimeout, asyncReceiveDelay));
		}
	}
}
