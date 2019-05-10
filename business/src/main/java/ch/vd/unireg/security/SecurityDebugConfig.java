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
	private static boolean securityDebug;
	private static String securityBypassOID;
	private static String securityBypassOIDSigle;
	private static String securityBypassProcedures;
	private static boolean securityBypassUnitTest;

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
	public static boolean isSecurityDebug() {
		return securityDebug;
	}

	public static String getSecurityBypassOID() {
		return securityBypassOID;
	}

	public static String getSecurityBypassOIDSigle() {
		return securityBypassOIDSigle;
	}

	public static boolean isSecurityBypassUnitTest() {
		return securityBypassUnitTest;
	}

	public static String getSecurityBypassProcedures(String user) {
		String bypass = null;
		if (properties != null) {
			bypass = properties.getProperty("extprop.security.bypass.procedures." + user);
		}
		if (bypass == null) {
			bypass = securityBypassProcedures;
		}
		return bypass;
	}

	/**
	 * Setter réservé à Spring. Ne pas l'utiliser depuis du code Java !
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setSecurityBypassUnitTest(boolean value) {
		SecurityDebugConfig.securityBypassUnitTest = value;
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
			securityDebug = getBooleanProp("extprop.security.debug");

			if (securityDebug && securityBypass == null) {
				LOGGER.warn("\n************************************************ Attention ********************************************************\n" +
						            "* le debug de la sécurité est activé, mais l'application n'a pas été compilée avec le profil 'dev' => pas d'effet *\n" +
						            "*******************************************************************************************************************");
				securityDebug = false;
			}

			if (securityDebug && securityBypass != null) {
				securityBypassOID = getStringProp("extprop.security.bypass.oid.no");
				securityBypassOIDSigle = getStringProp("extprop.security.bypass.oid.sigle");
				securityBypassProcedures = getStringProp("extprop.security.bypass.procedures");
				securityBypassUnitTest = getBooleanProp("extprop.security.bypass.unittest");

				// Bypass global
				int oid = 0;
				try {
					oid = Integer.parseInt(securityBypassOID);
				}
				catch (NumberFormatException e) {
					// on ignore
				}
				SecurityBypass globalBypass = new SecurityBypass(oid, securityBypassOIDSigle, securityBypassProcedures);
				securityBypass.addBypass(globalBypass);

				// Bypass par user
				final Map<String, String> all = properties.getAllProperties();
				for (Map.Entry<String, String> entry : all.entrySet()) {
					if (entry.getKey().startsWith("extprop.security.bypass.procedures.")) {
						String user = entry.getKey().substring("extprop.security.bypass.procedures.".length());
						String procedures = entry.getValue();
						SecurityBypass bypass = new SecurityBypass(user, oid, securityBypassOIDSigle, procedures);
						securityBypass.addBypass(bypass);
					}
				}
			}
		}
	}
}
