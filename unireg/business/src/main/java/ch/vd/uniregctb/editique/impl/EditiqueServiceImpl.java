package ch.vd.uniregctb.editique.impl;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.uniregctb.editique.EditiqueCopieConformeService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueRetourImpressionStorageService;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.editique.EvenementEditiqueSender;

/**
 * Implémentation standard de {@link EditiqueService}.
 */
public final class EditiqueServiceImpl implements EditiqueService {

	private static final Logger LOGGER = Logger.getLogger(EditiqueServiceImpl.class);

	/** Le type de document à transmettre au service pour UNIREG */
	public static final String TYPE_DOSSIER_UNIREG = "003";

	private EvenementEditiqueSender sender;

	private EditiqueRetourImpressionStorageService retourImpressionStorage;

	private EditiqueCopieConformeService copieConformeService;

	/**
	 * Temps d'attente (en secondes) du retour du document PDF / PCL lors d'une impression locale.
	 */
	private int receiveTimeout = 120;

	/**
	 * {@inheritDoc}
	 */
	public EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, XmlObject document, boolean archive) throws EditiqueException {

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

		final EditiqueResultat resultat;
		try {
			resultat = retourImpressionStorage.getDocument(nomDocument, receiveTimeout * 1000L);
		}
		catch (Exception e) {
			throw new EditiqueException(e);
		}

		// log de l'état de la réponse
		if (LOGGER.isDebugEnabled()) {
			final String statut;
			if (resultat == null) {
				statut = "Time-out";
			}
			else if (resultat.getDocument() == null) {
				statut = String.format("Erreur (%s), ", resultat.getError());
			}
			else {
				statut = "OK";
			}
			final String msg = String.format("Retour d'impression locale reçu pour document %s (%s) : %s", nomDocument, typeDocument, statut);
			LOGGER.debug(msg);
		}
		return resultat;
	}

	/**
	 * {@inheritDoc}
	 */
	public void creerDocumentParBatch(String nomDocument, String typeDocument, XmlObject document, boolean archive) throws EditiqueException {
		sender.envoyerDocument(nomDocument, typeDocument, document, null, archive);
	}

	/**
	 * {@inheritDoc}
	 */
	public InputStream getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument, String contexte) throws EditiqueException {
		return copieConformeService.getPdfCopieConforme(noContribuable, typeDocument, nomDocument, contexte);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCopieConformeService(EditiqueCopieConformeService copieConformeService) {
		this.copieConformeService = copieConformeService;
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
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
}