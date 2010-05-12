package ch.vd.uniregctb.fidor;

import javax.xml.ws.BindingProvider;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import ch.vd.fidor.ws.v1.generated.Acces;
import ch.vd.fidor.ws.v1.generated.FidorPortType;
import ch.vd.uniregctb.common.AuthenticationHelper;

/**
 * [UNIREG-2187]
 */
public class FidorServiceImpl implements FidorService {

	private static final Logger LOGGER = Logger.getLogger(FidorServiceImpl.class);

	private String serviceUrl;
	private String username;
	private String password;

	private String patternTaoPP;
	private String patternTaoBA;
	private String patternTaoIS;
	private String patternSipf;

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

	public String getUrlTaoPP(Long numero) {
		lazyInit();
		return resolve(patternTaoPP, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlTaoBA(Long numero) {
		lazyInit();
		return resolve(patternTaoBA, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlTaoIS(Long numero) {
		lazyInit();
		return resolve(patternTaoIS, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlSipf(Long numero) {
		lazyInit();
		return resolve(patternSipf, numero, AuthenticationHelper.getCurrentOID());
	}

	private static String resolve(String url, Long numero, Integer oid) {
		if (url == null) {
			return null;
		}
		Assert.notNull(numero);
		Assert.notNull(oid);
		return url.replaceAll("\\{NOCTB\\}", numero.toString()).replaceAll("\\{OID\\}", oid.toString());
	}

	/**
	 * Initialise le client du web-service à la demande.
	 * <p/>
	 * <b>Note:</b> il est absolument nécessaire d'initialiser le client <i>après</i> le contexte Spring, car il y a une dépendence
	 * implicite sur le bus CXF qui risque d'être initialisé plus tard que ce bean. Dans ce dernier, cas on reçoit une NPE dans
	 * le constructeur du service.
	 */
	private void lazyInit() {
		if (patternSipf == null) {
			synchronized (this) {
				if (patternSipf == null) {
					final FidorPortType service = initWebService(serviceUrl, username, password);
					patternTaoPP = getUrl(service, "TAOPP", "synthese");
					patternTaoBA = getUrl(service, "TAOBA", "dossier");
					patternTaoIS = getUrl(service, "TAOIS", "default");
					patternSipf = getUrl(service, "SIPF", "default");
					LOGGER.info("URLs externes (FiDoR) :\n" +
							" * TAOPP = " + patternTaoPP + "\n" +
							" * TAOBA = " + patternTaoBA + "\n" +
							" * TAOIS = " + patternTaoIS + "\n" +
							" * SIPF = " + patternSipf);
				}
			}
		}
	}

	private String getUrl(FidorPortType service, String app, String target) {
		final String url = service.getUrl(app, Acces.INTERNE, target, null);
		if (url == null) {
			LOGGER.error(String.format("Il manque l'url d'accès à %s (target %s) dans FiDoR !", app, target));
		}
		return url;
	}

	private static FidorPortType initWebService(String serviceUrl, String username, String password) {

		final URL wsdlUrl;
		try {
			wsdlUrl = ResourceUtils.getURL("classpath:fidor_ws_v1.wsdl");
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		final ch.vd.fidor.ws.v1.generated.FidorService ts = new ch.vd.fidor.ws.v1.generated.FidorService(wsdlUrl);
		final FidorPortType service = ts.getFidorPort();
		final Map<String, Object> context = ((BindingProvider) service).getRequestContext();
		if (StringUtils.isNotBlank(username)) {
			context.put(BindingProvider.USERNAME_PROPERTY, username);
			context.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);

		return service;
	}
}
