package ch.vd.unireg.security;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.common.EncodingFixHelper;
import ch.vd.unireg.interfaces.service.ServiceSecuriteBypass;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.utils.UniregProperties;

/**
 * Permet de récupérer les informations de debug liées à la sécurité. Ces informations doivent être stockées dans un fichier properties.
 *
 * TODO (msi) à fusionner dans le ServiceSecuriteDebug
 */
public class SecurityDebugConfig implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityDebugConfig.class);

	public static UniregProperties properties;
	private static boolean iamDebug;
	private static String iamBypassApplication;
	private static String iamBypassUser;
	private static String iamBypassFirstName;
	private static String iamBypassLastName;
	private static String iamBypassRoles;
	private static boolean ifoSecDebug;
	private static String ifoSecBypassOID;
	private static String ifoSecBypassOIDSigle;
	private static String ifoSecBypassProcedures;
	private static boolean ifoSecBypassUnitTest;

	@Nullable
	private ServiceSecuriteBypass securityBypass;

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
	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		if (serviceSecurite instanceof ServiceSecuriteBypass) {
			this.securityBypass = (ServiceSecuriteBypass) serviceSecurite;
		}
	}

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
		return Boolean.parseBoolean(value);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (properties != null) {
			iamDebug = getBooleanProp("extprop.iam.debug");
			iamBypassApplication = getStringProp("extprop.iam.bypass.application");
			iamBypassUser = getStringProp("extprop.iam.bypass.user");
			iamBypassFirstName = getStringProp("extprop.iam.bypass.firstname");
			iamBypassLastName = getStringProp("extprop.iam.bypass.lastname");
			iamBypassRoles = getStringProp("extprop.iam.bypass.roles");
			ifoSecDebug = getBooleanProp("extprop.ifosec.debug");

			if (ifoSecDebug && securityBypass == null) {
				LOGGER.warn("\n************************************************ Attention ********************************************************\n" +
						            "* le debug de la sécurité est activé, mais l'application n'a pas été compilée avec le profil 'dev' => pas d'effet *\n" +
						            "*******************************************************************************************************************");
			}

			if (ifoSecDebug && securityBypass != null) {
				ifoSecBypassOID = getStringProp("extprop.ifosec.bypass.oid.no");
				ifoSecBypassOIDSigle = getStringProp("extprop.ifosec.bypass.oid.sigle");
				ifoSecBypassProcedures = getStringProp("extprop.ifosec.bypass.procedures");
				ifoSecBypassUnitTest = getBooleanProp("extprop.ifosec.bypass.unittest");

				// Bypass global
				int oid = 0;
				try {
					oid = Integer.parseInt(ifoSecBypassOID);
				}
				catch (NumberFormatException e) {
					// on ignore
				}
				IfoSecBypass globalBypass = new IfoSecBypass(oid, ifoSecBypassOIDSigle, ifoSecBypassProcedures);
				securityBypass.addBypass(globalBypass);

				// Bypass par user
				final Map<String, String> all = properties.getAllProperties();
				for (Map.Entry<String, String> entry : all.entrySet()) {
					if (entry.getKey().startsWith("extprop.ifosec.bypass.procedures.")) {
						String user = entry.getKey().substring("extprop.ifosec.bypass.procedures.".length());
						String procedures = entry.getValue();
						IfoSecBypass bypass = new IfoSecBypass(user, oid, ifoSecBypassOIDSigle, procedures);
						securityBypass.addBypass(bypass);
					}
				}
			}
		}
	}
}
