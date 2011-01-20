package ch.vd.uniregctb.editique.impl;

import javax.activation.DataHandler;

import java.io.InputStream;

import org.apache.log4j.Logger;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.http.EsbHttpTemplate;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.editique.EditiqueCopieConformeService;
import ch.vd.uniregctb.editique.EditiqueException;

/**
 * Implémentation du service d'obtention de copies conformes
 */
public class EditiqueCopieConformeServiceImpl implements EditiqueCopieConformeService {

	public static final Logger LOGGER = Logger.getLogger(EditiqueCopieConformeServiceImpl.class);

	private static final String PDF = "pdf";

	private EsbMessageFactory esbMessageFactory;
	private EsbHttpTemplate esbHttpTemplate;
	private String serviceDestination;
	private String domain;
	private String application;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbMessageFactory(EsbMessageFactory esbMessageFactory) {
		this.esbMessageFactory = esbMessageFactory;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEsbHttpTemplate(EsbHttpTemplate esbHttpTemplate) {
		this.esbHttpTemplate = esbHttpTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceDestination(String serviceDestination) {
		this.serviceDestination = serviceDestination;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDomain(String domain) {
		this.domain = domain;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setApplication(String application) {
		this.application = application;
	}

	public InputStream getPdfCopieConforme(long numeroTiers, String typeDocument, String nomDocument, String contexte) throws EditiqueException {

		try {
			final EsbMessage demande = createEsbMessageDemande(numeroTiers, typeDocument, nomDocument, contexte);
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
		catch (Exception e) {
			LOGGER.error("Erreur lors de la demande de copie conforme", e);
			throw new EditiqueException(e);
		}
	}

	private EsbMessage createEsbMessageDemande(long numeroTiers, String typeDocument, String nomDocument, String contexte) throws Exception {

		final String user = AuthenticationHelper.getCurrentPrincipal();
		final EsbMessage m = esbMessageFactory.createMessage();

		m.setDomain(domain);
		m.setContext(contexte);
		m.setApplication(application);
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setBusinessUser(user);
		m.setServiceDestination(serviceDestination);
		m.setBody("<empty/>");

		// paramètres d'entrée
		m.addHeader("typDossier", EditiqueServiceImpl.TYPE_DOSSIER_UNIREG);
		m.addHeader("nomDossier", FormatNumeroHelper.numeroCTBToDisplay(numeroTiers));
		m.addHeader("typDocument", typeDocument);
		m.addHeader("idDocument", nomDocument);
		m.addHeader("typFormat", PDF);

		return m;
	}
}
