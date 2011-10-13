package ch.vd.uniregctb.editique.impl;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.editique.EditiqueCopieConformeService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;
import ch.vd.uniregctb.editique.EditiqueResultatTimeout;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.EvenementEditiqueSender;
import ch.vd.uniregctb.editique.RetourImpressionToInboxTrigger;
import ch.vd.uniregctb.editique.RetourImpressionTrigger;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.inbox.InboxService;

/**
 * Implémentation standard de {@link EditiqueService}.
 */
public final class EditiqueServiceImpl implements EditiqueService, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EditiqueServiceImpl.class);

	/** Le type de document à transmettre au service pour UNIREG */
	public static final String TYPE_DOSSIER_UNIREG = "003";

	private EvenementEditiqueSender sender;

	private EditiqueRetourImpressionStorageService retourImpressionStorage;

	private EditiqueCopieConformeService copieConformeService;

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

	private static interface TimeoutManager {
		EditiqueResultat onTimeout(EditiqueResultat src);
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuInbox(final String nomDocument, final TypeDocumentEditique typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive, final String description) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, asyncReceiveDelay, new TimeoutManager() {
			@Override
			public EditiqueResultat onTimeout(EditiqueResultat src) {
				final String visa = AuthenticationHelper.getCurrentPrincipal();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Retour d'impression un peu lent pour le document '%s', routage demandé vers l'inbox de l'utilisateur %s", nomDocument, visa));
				}

				final RetourImpressionTrigger trigger = new RetourImpressionToInboxTrigger(inboxService, visa, description, hoursRetourImpressionExpiration);
				retourImpressionStorage.registerTrigger(nomDocument, trigger);
				return new EditiqueResultatReroutageInboxImpl(src.getIdDocument());
			}
		});
	}

	@Override
	public EditiqueResultat creerDocumentImmediatementSynchroneOuRien(final String nomDocument, final TypeDocumentEditique typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException {
		return creerDocumentImmediatement(nomDocument, typeDocument, typeFormat, document, archive, syncReceiveTimeout, new TimeoutManager() {
			@Override
			public EditiqueResultat onTimeout(EditiqueResultat src) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(String.format("Retour d'impression locale non-reçu pour document %s (%s) : Time-out", nomDocument, typeDocument));
				}
				return src;
			}
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
	private EditiqueResultat creerDocumentImmediatement(String nomDocument, TypeDocumentEditique typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive, int secTimeout, TimeoutManager timeoutManager) throws EditiqueException {

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

		EditiqueResultat resultat;
		try {
			resultat = retourImpressionStorage.getDocument(nomDocument, secTimeout * 1000L);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		// si on n'a rien reçu dans le temps imparti, il faut faire quelque chose de spécifique ?
		if (resultat instanceof EditiqueResultatTimeout) {
			resultat = timeoutManager.onTimeout(resultat);
		}
		else if (LOGGER.isDebugEnabled()) {
			// log de l'état de la réponse
			final String statut;
			if (resultat instanceof EditiqueResultatDocument) {
				statut = "OK";
			}
			else if (resultat instanceof EditiqueResultatErreur) {
				statut = String.format("Erreur (%s), ", ((EditiqueResultatErreur) resultat).getError());
			}
			else {
				statut = String.format("Erreur inconnue '%s'", resultat);
			}
			final String msg = String.format("Retour d'impression locale reçu pour document %s (%s) : %s", nomDocument, typeDocument, statut);
			LOGGER.debug(msg);
		}
		return resultat;
	}

	@Override
	public void creerDocumentParBatch(String nomDocument, TypeDocumentEditique typeDocument, XmlObject document, boolean archive) throws EditiqueException {
		sender.envoyerDocument(nomDocument, typeDocument, document, null, archive);
	}

	@Override
	public InputStream getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument, String contexte) throws EditiqueException {
		return copieConformeService.getPdfCopieConforme(noContribuable, typeDocument, nomDocument, contexte);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCopieConformeService(EditiqueCopieConformeService copieConformeService) {
		this.copieConformeService = copieConformeService;
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
