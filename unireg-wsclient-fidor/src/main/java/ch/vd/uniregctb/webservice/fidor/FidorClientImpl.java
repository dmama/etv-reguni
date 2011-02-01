package ch.vd.uniregctb.webservice.fidor;

import javax.xml.ws.BindingProvider;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.FidorPortType;
import ch.vd.fidor.ws.v2.FidorService;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.ParameterMap;
import ch.vd.fidor.ws.v2.Pays;

public class FidorClientImpl implements FidorClient {

	private static final Logger LOGGER = Logger.getLogger(FidorClientImpl.class);

	private String serviceUrl;
	private String username;
	private String password;

	private String patternTaoPP;
	private String patternTaoBA;
	private String patternTaoIS;
	private String patternSipf;

	private FidorPortType service;

	private long lastTentative = 0;
	private static final long fiveMinutes = 5L * 60L * 1000000000L; // en nanosecondes

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setUsername(String username) {
		this.username = username;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPassword(String password) {
		this.password = password;
	}


	public Logiciel getLogicielDetail(long logicielId) {
		initWebService();
		final Logiciel logiciel = service.getLogicielDetail(logicielId);
		if (logiciel == null) {
			LOGGER.error(String.format("Il manque le logiciel dont l'id est %s dans FiDoR !", logicielId));
		}
		return logiciel;
	}

	public Pays getPaysDetail(long ofsId) {
		initWebService();
		final Pays pays = service.getPaysDetail(ofsId);
		if (pays == null) {
			LOGGER.error(String.format("Il manque le pays dont l'id est %s dans FiDoR !", ofsId));
		}
		return pays;
	}

	public Collection<Logiciel> getTousLesLogiciels() {
		initWebService();
		final Collection<Logiciel> logiciels = service.getTousLesLogiciels();
		if (logiciels == null || logiciels.isEmpty()) {
			LOGGER.error("Il manque la liste des logiciels dans FiDoR !");

		}
		return logiciels;
	}

	public Collection<Pays> getTousLesPays() {
		initWebService();
		final Collection<Pays> listePays = service.getTousLesPays();
		if (listePays == null || listePays.isEmpty()) {
			LOGGER.error("Il manque la liste des pays dans FiDoR !");

		}
		return listePays;
	}

	public String getUrl(String app, Acces acces, String targetType, ParameterMap map) {
		initWebService();
		final String url = service.getUrl(app, acces, targetType, map);
		if (url == null) {
			LOGGER.error(String.format("Il manque l'url d'accès à %s (target %s) dans FiDoR !", app, targetType));
		}
		return url;
	}


	private void initWebService() {

		if (service == null) {
			final URL wsdlUrl;
			try {
				wsdlUrl = ResourceUtils.getURL("classpath:fidor-v2.wsdl");
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			final FidorService ts = new FidorService(wsdlUrl);
			service = ts.getFidorPort();
			final Map<String, Object> context = ((BindingProvider) service).getRequestContext();
			if (StringUtils.isNotBlank(username)) {
				context.put(BindingProvider.USERNAME_PROPERTY, username);
				context.put(BindingProvider.PASSWORD_PROPERTY, password);
			}
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);

			// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
			context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
			context.put("set-jaxb-validation-event-handler", false);

		}
	}


}
