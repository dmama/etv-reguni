package ch.vd.uniregctb.adresse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.utils.UniregProperties;

/**
 *  Encapsule la configuration de la class {@link LocaliteInvalideMatcher}
 *
 *  2 origines possibles:
 *     - externe: le fichier, unireg.properties
 *     - interne: LocaliteInvalideMatcher.properties sur le classpath
 *
 *  unireg.properties est prioritaire
 *
 *  LocaliteInvalideMatcher.properties est utile lorsque la classe est chargée dans un contexte ou il n'y a pas de fichier unireg.properties.
 *
 */
class LocaliteInvalideMatcherProperties {

	private static final Logger LOGGER = Logger.getLogger(LocaliteInvalideMatcherProperties.class);

	static final String PROPERTY_ENABLED = "localite.invalide.regexp.enabled";
	static final String PROPERTY_PATTERNS = "localite.invalide.regexp.patterns";
	static final String PROPERTY_FAUX_POSITIFS = "localite.invalide.regexp.faux.positifs";
	private static final String SEP = ",";

	private String propertyEnabled;
	private String propertyPatterns;
	private String propertyFauxPositifs;

	LocaliteInvalideMatcherProperties(UniregProperties uniregProperties) {

		if (uniregProperties == null) {
			Properties defaultProperties = null;
			InputStream is = LocaliteInvalideMatcher.class.getResourceAsStream("LocaliteInvalideMatcher.properties");
			try {
				defaultProperties = new Properties();
				defaultProperties.load(is);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			finally {
				try {
					is.close();
				}
				catch (IOException ignored) {}
			}
			this.propertyEnabled = defaultProperties.getProperty(PROPERTY_ENABLED);
			this.propertyPatterns = defaultProperties.getProperty(PROPERTY_PATTERNS);
			this.propertyFauxPositifs = defaultProperties.getProperty(PROPERTY_FAUX_POSITIFS);
		} else {
			this.propertyEnabled = uniregProperties.getProperty("extprop." + PROPERTY_ENABLED);
			this.propertyPatterns = uniregProperties.getProperty("extprop." + PROPERTY_PATTERNS);
			this.propertyFauxPositifs = uniregProperties.getProperty("extprop." + PROPERTY_FAUX_POSITIFS);
		}

		if (propertyEnabled == null) {
			LOGGER.warn("propriété " + PROPERTY_ENABLED + " introuvable, LocaliteInvalideMatcher désactivé\n" +
					"unireg.properties renseigne-t-il :\n" +
					"  - " + "extprop." + PROPERTY_ENABLED + "\n" +
					"  - " + "extprop." + PROPERTY_PATTERNS + "\n" +
					"  - " + "extprop." + PROPERTY_FAUX_POSITIFS + "\n");
			return;
		}
		if (!isEnabled()) {
			LOGGER.info("LocaliteInvalideMatcher désactivé");
		}

		if (propertyPatterns == null || propertyPatterns.trim().isEmpty()) {
			LOGGER.warn("propriété " + PROPERTY_PATTERNS + " non-défini, LocaliteInvalideMatcher désactivé");
			setEnabled(false);
			return;
		}
		LOGGER.info("" + propertyPatterns.split(SEP).length + " terme(s) défini(s) pour les localités invalides: " + propertyPatterns);
		LOGGER.info("" + propertyFauxPositifs.split(SEP).length + " terme(s) défini(s) pour les faux positifs: " + propertyFauxPositifs);
	}

	void setEnabled (boolean enabled) {
		propertyEnabled = Boolean.toString(enabled);
	}


	boolean isEnabled () {
		return Boolean.parseBoolean(propertyEnabled);
	}

	String[] getPatternsInvalides() {
		if (propertyPatterns != null && propertyPatterns.trim().length() > 0) {
			return propertyPatterns.split(SEP);
		} else {
			return new String[] {};
		}
	}

	String[] getPatternsFauxPositifs() {
		if (propertyFauxPositifs != null && propertyFauxPositifs.trim().length() > 0) {
			return propertyFauxPositifs.split(SEP);
		} else {
			return new String[] {};
		}
	}

}
