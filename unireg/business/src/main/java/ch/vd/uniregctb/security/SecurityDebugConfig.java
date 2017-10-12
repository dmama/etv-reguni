package ch.vd.uniregctb.security;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.EncodingFixHelper;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Permet de récupérer les informations de debug liées à la sécurité. Ces informations doivent être stockées dans un fichier properties.
 *
 * @author <a href="mailto:abenaissi@cross-systems.ch">Akram BEN AISSI</a>
 */
public class SecurityDebugConfig implements InitializingBean {

	public static UniregProperties properties;
	private static String reloadEachTime;
	private static boolean iamDebug;
	private static String iamBypassApplication;
	private static String iamBypassUser;
	private static String iamBypassFirstName;
	private static String iamBypassLastName;
	private static String iamBypassRoles;
	private static Portail iamBypassAccessPortail;
	private static boolean ifoSecDebug;
	private static String ifoSecBypassOID;
	private static String ifoSecBypassOIDSigle;
	private static String ifoSecBypassProcedures;
	private static boolean ifoSecBypassUnitTest;

	private IfoSecService ifoSecService;

	// Reload la sécurité
	public static boolean isReloadEachTime() {
		boolean reload = false;
		if (reloadEachTime != null) {
			reload = reloadEachTime.equals("true");
		}
		return reload;
	}

	// IAM
	public static boolean isIamDebug() {
		return iamDebug;
	}

	public static String getIamBypassApplication() {
		return iamBypassApplication;
	}

	public static String getIamBypassUser() {
		return iamBypassUser;
	}

	public static String getIamBypassFirstName() {
		return iamBypassFirstName;
	}

	public static String getIamBypassLastName() {
		return iamBypassLastName;
	}

	/**
	 * Exemple de rôles reçus depuis from IAM: Les rôles doivent être séparés par le caractère '|'
	 * <p>
	 * cn=finances-demo-comptable,dc=etat-de-vaud,dc=ch cn=finances-demo-secretaire_d_office,dc=etat-de-vaud,dc=ch
	 * cn=finances-demo-juriste,dc=etat-de-vaud,dc=ch cn=finances-demo-prepose,dc=etat-de-vaud,dc=ch
	 */
	public static String getIamBypassRoles() {
		return iamBypassRoles;
	}

	// IFO-SEC
	public static Portail getIamBypassAccessPortail() {
		return iamBypassAccessPortail;
	}

	public static boolean isIfoSecDebug() {
		return ifoSecDebug;
	}

	public static String getIfoSecBypassOID() {
		return ifoSecBypassOID;
	}

	public static String getIfoSecBypassOIDSigle() {
		return ifoSecBypassOIDSigle;
	}

	public static boolean isIfoSecBypassUnitTest() {
		return ifoSecBypassUnitTest;
	}

	public static String getIfoSecBypassProcedures(String user) {
		String bypass = null;
		if (properties != null) {
			bypass = properties.getProperty("extprop.ifosec.bypass.procedures." + user);
		}
		if (bypass == null) {
			bypass = ifoSecBypassProcedures;
		}
		return bypass;
	}

	/**
	 * Setter réservé à Spring. Ne pas l'utiliser depuis du code Java !
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setIfoSecBypassUnitTest(boolean ifoSecBypassUnitTest) {
		SecurityDebugConfig.ifoSecBypassUnitTest = ifoSecBypassUnitTest;
	}

	/**
	 * Setter réservé à Spring. Ne pas l'utiliser depuis du code Java !
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setProperties(UniregProperties properties) {
		SecurityDebugConfig.properties = properties;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIfoSecService(IfoSecService ifoSecService) {
		this.ifoSecService = ifoSecService;
	}

	@NotNull
	private String getStringProp(String key) {
		String value = properties.getProperty(key);
		if (value == null) {
			return "";
		}

		// les fichiers de propriétés sont toujours lus en ISO-8859-1
		return EncodingFixHelper.fixFromIso(value);
	}

	private boolean getBooleanProp(String key) {
		String value = properties.getProperty(key);
		return value != null && Boolean.parseBoolean(value);
	}

	@NotNull
	private Portail getPortailProp(String key) {
		final String s = getStringProp(key);
		if (Portail.CYBER.name().equalsIgnoreCase(s)) {
			return Portail.CYBER;
		}
		else {
			// valeur par défaut
			return Portail.IAM;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (properties != null) {
			reloadEachTime = getStringProp("extprop.security.reload.each.time");
			iamDebug = getBooleanProp("extprop.iam.debug");
			iamBypassApplication = getStringProp("extprop.iam.bypass.application");
			iamBypassUser = getStringProp("extprop.iam.bypass.user");
			iamBypassFirstName = getStringProp("extprop.iam.bypass.firstname");
			iamBypassLastName = getStringProp("extprop.iam.bypass.lastname");
			iamBypassAccessPortail = getPortailProp("extprop.iam.bypass.portail");
			iamBypassRoles = getStringProp("extprop.iam.bypass.roles");
			ifoSecDebug = getBooleanProp("extprop.ifosec.debug");

			if (ifoSecDebug) {
				ifoSecBypassOID = getStringProp("extprop.ifosec.bypass.oid.no");
				ifoSecBypassOIDSigle = getStringProp("extprop.ifosec.bypass.oid.sigle");
				ifoSecBypassProcedures = getStringProp("extprop.ifosec.bypass.procedures");
				ifoSecBypassUnitTest = getBooleanProp("extprop.ifosec.bypass.unittest");

				// Bypass global
				Integer oid = 0;
				try {
					oid = Integer.valueOf(ifoSecBypassOID);
				}
				catch (NumberFormatException e) {
					// on ignore
				}
				IfoSecBypass globalBypass = new IfoSecBypass(oid, ifoSecBypassOIDSigle, ifoSecBypassProcedures);
				ifoSecService.addBypass(globalBypass);

				// Bypass par user
				final Map<String, String> all = properties.getAllProperties();
				for (Map.Entry<String, String> entry : all.entrySet()) {
					if (entry.getKey().startsWith("extprop.ifosec.bypass.procedures.")) {
						String user = entry.getKey().substring("extprop.ifosec.bypass.procedures.".length());
						String procedures = entry.getValue();
						IfoSecBypass bypass = new IfoSecBypass(user, oid, ifoSecBypassOIDSigle, procedures);
						ifoSecService.addBypass(bypass);
					}
				}
			}
		}
	}
}
