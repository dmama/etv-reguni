package ch.vd.uniregctb.editique.impl;

import javax.activation.DataHandler;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageException;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.http.EsbHttpTemplate;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueCopieConformeService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueHelper;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

/**
 * Implémentation du service d'obtention de copies conformes
 */
public class EditiqueCopieConformeServiceImpl implements EditiqueCopieConformeService {

	public static final Logger LOGGER = LoggerFactory.getLogger(EditiqueCopieConformeServiceImpl.class);

	private static final String PDF = "pdf";

	private EsbHttpTemplate esbHttpTemplate;
	private String serviceDestination;
	private EditiqueHelper editiqueHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbHttpTemplate(EsbHttpTemplate esbHttpTemplate) {
		this.esbHttpTemplate = esbHttpTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	public void setEditiqueHelper(EditiqueHelper editiqueHelper) {
		this.editiqueHelper = editiqueHelper;
	}

	@Override
	public InputStream getPdfCopieConforme(long numeroTiers, TypeDocumentEditique typeDocument, String nomDocument) throws EditiqueException {

		try {
			final EsbMessage demande = createEsbMessageDemande(numeroTiers, typeDocument, nomDocument);
			final EsbMessage reponse = esbHttpTemplate.sendSync(demande);

			final InputStream pdf;
			final EsbDataHandler edh = reponse.getAttachment(PDF);
			if (edh != null && edh.getDataHandler() != null) {
				final DataHandler dh = edh.getDataHandler();
				pdf = dh.getInputStream();
			}
			else {
				pdf = null;
			}
			return pdf;
		}
		catch (EsbMessageException e) {
			if (e.getErrorCode() != null && Integer.parseInt(e.getErrorCode()) == 404) {
				LOGGER.error(String.format("Not found : %s (document '%s')", e.getMessage(), nomDocument), e);
				return null;
			}
			else {
				LOGGER.error(String.format("Erreur lors de la demande de copie conforme '%s' (%s)", nomDocument, e.getErrorCode()), e);
				throw new EditiqueException(e);
			}
		}
		catch (Exception e) {
			LOGGER.error(String.format("Erreur lors de la demande de copie conforme '%s'", nomDocument), e);
			throw new EditiqueException(e);
		}
	}

	private EsbMessage createEsbMessageDemande(long numeroTiers, TypeDocumentEditique typeDocument, String nomDocument) throws Exception {
		if (typeDocument.getCodeDocumentArchivage() == null) {
			throw new IllegalArgumentException("Archivage non-supporté pour document de type " + typeDocument);
		}

		final String user = AuthenticationHelper.getCurrentPrincipal();
		final EsbMessage m = EsbMessageFactory.createMessage();

		m.setContext(typeDocument.getContexteImpression());
		m.setBusinessId(nomDocument);
		m.setBusinessUser(user);
		m.setServiceDestination(serviceDestination);
		m.setBody("<empty/>");

		// paramètres d'entrée
		m.addHeader("typDossier", editiqueHelper.getTypeDossierArchivage());
		m.addHeader("nomDossier", FormatNumeroHelper.numeroCTBToDisplay(numeroTiers));
		m.addHeader("typDocument", typeDocument.getCodeDocumentArchivage());
		m.addHeader("idDocument", nomDocument);
		m.addHeader("typFormat", PDF);

		return m;
	}
}
