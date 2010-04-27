package ch.vd.uniregctb.webservice.acicom;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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


public class AcicomClientImpl implements AcicomClient {

	private static final Logger LOGGER = Logger.getLogger(AcicomClientImpl.class);

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

	public ContenuMessage recupererMessage(RecupererContenuMessage infosMessage) throws AciComException_Exception, DocumentNotFoundException_Exception {
		initService();

		ContenuMessage contenuMessage;
		final String businessId = infosMessage.getMessageId();
		try {

			contenuMessage = this.service.recupererContenuMessage(businessId);

		}
		catch (WebServiceException e) {
			throw new AciComException_Exception(e.getMessage(), e);
		}


		if (contenuMessage != null) {
			final String extension = contenuMessage.getExtension();
			LOGGER.info("Type de document : " + extension);

			try {
				FileOutputStream fos = new FileOutputStream("messages."+extension);
				fos.write(contenuMessage.getContent());
				fos.close();
			}
			catch (IOException e) {
				throw new AciComException_Exception(e.getMessage(), e);
			}
			return contenuMessage;
		}
		else {

			throw new DocumentNotFoundException_Exception("Le message ayant le business ID"+businessId+"n'a pas été trouvé chez ACICOM");

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

