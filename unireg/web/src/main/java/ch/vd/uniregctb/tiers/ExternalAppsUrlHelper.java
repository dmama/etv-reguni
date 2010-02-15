package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.utils.UniregProperties;

public class ExternalAppsUrlHelper {

	// private static final Logger LOGGER = Logger.getLogger(ExternalAppsUrlHelper.class);

	private final String patternSipf;
	private final String patternTaoIS;
	private final String patternTaoBA;
	private final String patternTaoPP;

	public ExternalAppsUrlHelper(UniregProperties uniregProperties) {
		this.patternTaoPP = getProperty(uniregProperties, "extprop.url.taopp");
		this.patternTaoBA = getProperty(uniregProperties, "extprop.url.taoba");
		this.patternTaoIS = getProperty(uniregProperties, "extprop.url.taois");
		this.patternSipf = getProperty(uniregProperties, "extprop.url.sipf");
	}

	private static String getProperty(UniregProperties uniregProperties, final String url) {
		String value = uniregProperties.getProperty(url);
		Assert.notNull(value, "La propriété '" + url + "' n'est pas définie.");
		return value;
	}

	private String replace(String url, String numero, Integer oid) {
		Assert.notNull(url);
		Assert.notNull(numero);
		Assert.notNull(oid);

		url = url.replace("_NOCTB_", numero);
		url = url.replace("_OID_", oid.toString());
		return url;
	}

	public String getUrlTaoPP(Long numero) {
		Assert.notNull(numero);
		return getUrlTaoPP(numero.toString());
	}

	public String getUrlTaoPP(String numero) {
		return replace(patternTaoPP, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlTaoBA(Long numero) {
		Assert.notNull(numero);
		return getUrlTaoBA(numero.toString());
	}

	public String getUrlTaoBA(String numero) {
		return replace(patternTaoBA, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlTaoIS(Long numero) {
		Assert.notNull(numero);
		return getUrlTaoIS(numero.toString());
	}

	public String getUrlTaoIS(String numero) {
		return replace(patternTaoIS, numero, AuthenticationHelper.getCurrentOID());
	}

	public String getUrlSipf(Long numero) {
		Assert.notNull(numero);
		return getUrlSipf(numero.toString());
	}

	public String getUrlSipf(String numero) {
		return replace(patternSipf, numero, AuthenticationHelper.getCurrentOID());
	}

}
