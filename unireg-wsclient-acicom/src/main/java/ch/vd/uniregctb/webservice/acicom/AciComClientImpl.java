package ch.vd.uniregctb.webservice.acicom;

import javax.xml.ws.BindingProvider;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.ResourceUtils;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.AciComException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.DocumentNotFoundException_Exception;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.MeldewesenConsultationServiceImpl;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.MeldewesenConsultationServiceImplPortType;
import ch.vd.dfin.acicom.web.services.meldewesen.impl.RecupererContenuMessage;


public class AciComClientImpl implements AciComClient {

	private static final Logger LOGGER = Logger.getLogger(AciComClientImpl.class);

	private MeldewesenConsultationServiceImplPortType service;

	private String serviceUrl;

	private String username;

	private String password;

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ContenuMessage recupererMessage(RecupererContenuMessage infosMessage) throws AciComClientException {
		initService();

		ContenuMessage contenuMessage;
		final String businessId = infosMessage.getMessageId();
		try {

			contenuMessage = this.service.recupererContenuMessage(businessId);

		}
		catch (AciComException_Exception e) {
			final String message = "Erreur technique d'accès à ACICOM: "+e.getMessage();
			throw new AciComClientTechniqueException(message);
		}
		catch (RuntimeException e) {
			final String message = "Erreur technique d'accès à ACICOM: "+e.getMessage();
			throw new AciComClientTechniqueException(message);
		}
			catch (DocumentNotFoundException_Exception e) {
			final String message = "Erreur suite à la recherche du message : "+e.getMessage();
			throw new AciComClientDocumentNotFoundException(message);
		}


		if (contenuMessage != null) {
			final String extension = contenuMessage.getExtension();
			LOGGER.info("Type de document : " + extension);
			return contenuMessage;
		}
		else {

			throw new AciComClientTechniqueException("La recherche du message ayant le business ID"+businessId+" a renvoyé un contenu nul");

		}


	}

	public void initService() {
		if (this.service == null) {
			URL wsdlUrl;
			try {
				wsdlUrl = ResourceUtils.getURL("classpath:serviceMeldewesen-v1.1.wsdl");
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			MeldewesenConsultationServiceImpl MeldewesenService = new MeldewesenConsultationServiceImpl(wsdlUrl);
			this.service = MeldewesenService.getMeldewesenConsultationServiceImplPort();
			Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (StringUtils.isNotBlank(this.username)) {
				context.put(BindingProvider.USERNAME_PROPERTY, this.username);
				context.put(BindingProvider.PASSWORD_PROPERTY, this.password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, this.serviceUrl);
		}
	}
}

