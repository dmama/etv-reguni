package ch.vd.uniregctb.wsclient.fidor;

import javax.xml.ws.BindingProvider;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.FidorPortType;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.webservice.fidor.FidorClient;

/**
 * [UNIREG-2187]
 */
public class FidorServiceImpl implements FidorService {

	private static final Logger LOGGER = Logger.getLogger(FidorServiceImpl.class);


	private FidorClient fidorClient;

	private String patternTaoPP;
	private String patternTaoBA;
	private String patternTaoIS;
	private String patternSipf;

	private long lastTentative = 0;
	private static final long fiveMinutes = 5L * 60L * 1000000000L; // en nanosecondes


	public void setFidorClient(FidorClient fidorClient) {
		this.fidorClient = fidorClient;
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
			final long now = System.nanoTime();
			if (lastTentative > 0 && lastTentative + fiveMinutes > now) {
				// on attend cinq minutes avant d'essayer de recontacter FiDoR, pour éviter de remplir les logs pour rien
				return;
			}
			synchronized (this) {
				try {
					if (patternSipf == null) {
						patternTaoPP = getUrl("TAOPP", "synthese");
						patternTaoBA = getUrl("TAOBA", "dossier");
						patternTaoIS = getUrl("TAOIS", "default");
						patternSipf = getUrl("SIPF", "explorer"); // [UNIREG-2409]
						LOGGER.info("URLs externes (FiDoR) :\n" +
								" * TAOPP = " + patternTaoPP + "\n" +
								" * TAOBA = " + patternTaoBA + "\n" +
								" * TAOIS = " + patternTaoIS + "\n" +
								" * SIPF = " + patternSipf);
					}
				}
				catch (Exception e) {
					LOGGER.error("Impossible de contacter FiDoR : allez lui donner un coup de pied !");
					lastTentative = now;
				}
			}
		}
	}

	private String getUrl(String app, String target) {
		final String url = fidorClient.getUrl(app, Acces.INTERNE, target, null);
		if (url == null) {
			LOGGER.error(String.format("Il manque l'url d'accès à %s (target %s) dans FiDoR !", app, target));
		}
		return url;
	}

}
