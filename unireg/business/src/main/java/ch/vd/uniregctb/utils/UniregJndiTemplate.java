package ch.vd.uniregctb.utils;

import java.util.Enumeration;
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

		Properties newEnv = new Properties();

		Enumeration<?> iter = environment.keys();
		while (iter.hasMoreElements()) {
			String key = (String)iter.nextElement();
			String value = environment.getProperty(key);

			if (!value.equals("")) {
				newEnv.setProperty(key, value);
				LOGGER.info(" * "+key+" => "+value);
			}
			else {
				LOGGER.info(" Suppression de la propriété "+key);
			}
		}

		super.setEnvironment(newEnv);
	}

}
