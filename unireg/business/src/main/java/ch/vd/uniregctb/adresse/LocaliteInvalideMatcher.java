package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.utils.UniregProperties;

/**
 * <p>Cette classe possède une unique méthode {@link LocaliteInvalideMatcher#match} qui permet de controler si un libellé de commune est valide.
 * En effet, certains libellés de commune dans les adresses renseignées par l'utilisateur sont là pour dénoter le fait que l'ont ne
 * connait pas l'adresse de la personne. On retrouvera donc des libellés comme "Sans Adresse", "Inconnu", "parti à l'étranger" dans
 * le champs normalement dédiés au libellé de la commune.
 *
 * <p>De plus ce champ étant libre, nous ne sommes pas à l'abris de faute de typo et de certaines fantaisies dans la saisie...
 * Cette classe est donc là pour essayer de detecter ces cas.
 *
 * <p>La méthode de detection est basée sur une analyse empirique des données présentes en production.
 * A partir de certain termes dénotant une adresse inconnue, on génère une expression régulière qui tentera de reconnaitre des dérivés proches
 * de cette expression. Cette méthode n'est donc pas parfaite et peut même à l'occasion signalé des libellés
 * comme étant faussement invalide (des faux-positifs)
 *
 * <p>Les propriétés décrites ci-dessous permettent d'affiner les resultats obtenues. (à renseigner dans unireg.properties)
 *
 * <ul>
 * 	<li><code>extprop.localite.invalide.regexp.enabled</code>: true ou false, active ou desactive de maniere globale le contrôle.
 *
 *  <li><code>extprop.localite.invalide.regexp.patterns</code>:
 *      la liste des termes, séparés par des virgules, utilisés pour construire les regexp qui vont servir à matcher les localités invalides
 *      <p>par exemple: <strong>"inconu,adrese"</strong> matchera <strong>"Inconnu", "Adressssse innnnccconnue", "S A N S - A D R E S S E",
 *      "parti sans laisser d'adresse"</strong> mais ne matchera pas <strong>"no address"</strong> (il faudrait enlever le 'e' à la fin du pattern)
 *
 *  <li><code>extprop.localite.invalide.regexp.faux.positifs</code>:
 *      la liste des localités valides connues, séparées par des virgules, matchant les patterns d'invalidité.
 *      par exemple: la commune de "Sainte-Adresse" en France
 *      En les renseignant ici, elles ne resortiront plus comme étant invalides
 * </ul>
 *
 * Les regexp générées sont cases insensitves
 *
 * <h1>Notes concernant l'implémentation
 *
 * <p>Les membres static de la classe et donc les paramètres du match sont initialisés
 * lors de l'instantiation d'un objet de cette classe par le conteneur spring.
 *
 * <p>Cette mécanique est un peu bizarre mais pratique pour pouvoir benéficier des propriétés de unireg.properties
 * et de l'acces par membre static sans trop se casser la tête!

 */
public class LocaliteInvalideMatcher implements InitializingBean {

	private static Logger LOGGER = Logger.getLogger(LocaliteInvalideMatcher.class);

	private static LocaliteInvalideMatcherProperties actualProperties;
	private static UniregProperties uniregProperties;

	private static Map<String, String> conversionSpeciales = new HashMap<String, String>();
	private static List<Pattern> patternsLocaliteInvalide = new ArrayList<Pattern>();
	private static List<Pattern> patternsFauxPositif  = new ArrayList<Pattern>();

	private static boolean initialized = false;

	public void setUniregProperties(UniregProperties properties) {
		LocaliteInvalideMatcher.uniregProperties = properties;
	}

	/**
	 * Utile pour éviter les appels à la méthode match pendant que le bean initialise les champs statics
	 */
	private static ReadWriteLock lockInit = new ReentrantReadWriteLock();

	/**
	 * <p>Analyse si le paramètre ressemble à une localité invalide, résutat non-garantie!
	 * Mais ça permet de filtrer les cas les plus courant.</p>
	 *
	 * <p>A noter que cette méthode renverra toujours <code>false</code> si les propriétés attendues
	 * dans <code>unireg.properties</code> sont absentes.</p>
	 *
	 * @param localite la désignation de la localité à analyser
	 * @return <code>true</code> si la localité correspond à un pattern de localité invalide
	 */
	public static boolean match(String localite) {
		if (!initialized) {
			try {
				// Initialisation automatique
				LOGGER.warn("LocaliteInvalideMatcher n'a pas été initialisé par Spring, initialisation automatique avec les paramètres par défaut");
				new LocaliteInvalideMatcher().afterPropertiesSet();
			}
			catch (Exception e) {
				LOGGER.error("Erreur lors de l'initialisation de LocaliteInvalideMatcher, service désactivé", e);
				initialized = true;
				disable();
			}
		}
		lockInit.readLock().lock();
		try {
			if (!isEnabled()) {
				return false;
			}
			for1: for (Pattern p: patternsLocaliteInvalide) {
				if (p.matcher(localite).find()) {
					// la localité match un pattern localité invalide
					for (Pattern q: patternsFauxPositif) {
						if (q.matcher(localite).matches()) {
							// C'est un faux positif
							continue for1;
						}
					}
					// A ce stade, la localite match une localité invalide et n'est pas un faux positif
					return true;
				}
			}
			return false;
		} finally {
			lockInit.readLock().unlock();
		}
	}

	private static boolean isEnabled() {
		return actualProperties.isEnabled();
	}

	private static void disable() {
		actualProperties.setEnabled(false);
	}

	// expression régulière mappant les caractères qui peuvent être utilisé comme séparateur dans les localités invalides
	// On match ainsi les strings du type: "I N C O N N U","Non-Indiqué", "E_T_R_A_N_G_E_R", "S/DC"
	private static final String REGEXP_SEPARATEURS = "[ -./_,\\\\:;|*]*";

	private static String buildRegExpLocaliteInvalide (String terme) {
		StringBuilder sb = new StringBuilder();
		for(Character c: terme.toCharArray()) {
			sb.append("([");
			if (conversionSpeciales.containsKey(c.toString())) {
				sb.append(conversionSpeciales.get(c.toString()));
			}
			else {
				sb.append(c.toString().toLowerCase()).append(c.toString().toUpperCase());
			}
			sb.append("]+");
			sb.append(REGEXP_SEPARATEURS);
			sb.append(")+");
		}
		LOGGER.debug("regexp localité invalide pour " + terme + " = " + sb.toString());
		return sb.toString();
	}

	private static String buildRegExpFauxPositif (String terme) {
		StringBuilder sb = new StringBuilder();
		for(Character c: terme.toCharArray()) {
			sb.append("[");
			sb.append(c.toString().toLowerCase()).append(c.toString().toUpperCase());
			sb.append("]");
		}
		LOGGER.debug("regexp faux-positif" + terme + " = " + sb.toString());
		return sb.toString();
	}

	/**
	 * Utile pour les tests, ne devrait pas être utiliser dans le code de prod pour l'instant
	 * cependant dans le futur on pourrait imaginer un rechargement à chaud des parametres et donc l'utilité de cette méthode
	 */
	static void reset() {
		lockInit.writeLock().lock();
		try {
			uniregProperties = null;
			actualProperties = null;
			conversionSpeciales = new HashMap<String, String>();
			patternsLocaliteInvalide = new ArrayList<Pattern>();
			patternsFauxPositif  = new ArrayList<Pattern>();
			initialized = false;
		} finally {
			lockInit.writeLock().unlock();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		lockInit.writeLock().lock();
		try {
			initialized = false;
			initProperties();
			if(!isEnabled()) return;
            initConversionsSpeciales();
            initPatternsLocalitesInvalides();
			if(!isEnabled()) return;
			initPatternsFauxPositifs();
		}
		finally {
			initialized = true;
			lockInit.writeLock().unlock();
		}
	}

	private void initProperties() {
		if (uniregProperties != null) {
			actualProperties = new LocaliteInvalideMatcherProperties(uniregProperties);
		} else {
			actualProperties = new LocaliteInvalideMatcherProperties(null);
		}
	}

	private void initPatternsFauxPositifs() {
		patternsFauxPositif.clear();
		String[] fauxPositifs = actualProperties.getPatternsFauxPositifs();
		if (fauxPositifs.length > 0) {
			for(String fauxPositif : fauxPositifs) {
				patternsFauxPositif.add(Pattern.compile(buildRegExpFauxPositif(fauxPositif)));
			}
		}
	}

	private void initPatternsLocalitesInvalides() {
		patternsLocaliteInvalide.clear();
		String[] termesInvalides = actualProperties.getPatternsInvalides();
		for(String termeInvalide : termesInvalides) {
			patternsLocaliteInvalide.add(Pattern.compile(buildRegExpLocaliteInvalide(termeInvalide)));
		}
	}

	private void initConversionsSpeciales() {
		conversionSpeciales.clear();
		conversionSpeciales.put("a", "aäàáâãAÄÀÁÂÃ");
		conversionSpeciales.put("e", "eëèéêẽEËÈÉÊẼ");
		conversionSpeciales.put("i", "iïìíîĩIÏÌÍÎĨ");
		conversionSpeciales.put("o", "oöòóôõOÖÒÓÔÕ");
		conversionSpeciales.put("u", "uüùúûũUÜÙÚÛŨ");
		conversionSpeciales.put("n", "nñNÑ");
	}

}
