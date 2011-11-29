package ch.vd.uniregctb.utils;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.jndi.JndiTemplate;

/**
 * Cette classe filtre les proprétés passées à "Chaine vide"
 * Elle les transforme en "null" pour que si on donne un user/password à "chaine vide"
 * L'authentification martche quand même en passant "null" plus loin
 *
 * @author jec
 *
 */
public class UniregJndiTemplate extends JndiTemplate {

	private static final Logger LOGGER = Logger.getLogger(UniregJndiTemplate.class);

	@Override
	public void setEnvironment(Properties environment) {

		LOGGER.info("JNDI Properties");

		final Properties newEnv = new Properties();

		for (Map.Entry<Object, Object> entry : environment.entrySet()) {
			final String key = (String) entry.getKey();
			final String value = (String) entry.getValue();

			if (!value.isEmpty()) {
				newEnv.setProperty(key, value);

				final String valueToDisplay;
				if (key.contains("password") || key.contains("credentials")) {
					valueToDisplay = "*******";
				}
				else {
					valueToDisplay = value;
				}
				LOGGER.info(String.format(" * %s => %s", key, valueToDisplay));
			}
			else {
				LOGGER.info(" Suppression de la propriété "+key);
			}
		}

		super.setEnvironment(newEnv);
	}

}
