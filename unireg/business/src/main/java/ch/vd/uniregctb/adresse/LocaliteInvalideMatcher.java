package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

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
 * de cette expression. Cette méthode n'est donc pas parfaite et peut même à l'occasion signaler des libellés
 * comme étant faussement invalide (des faux-positifs)
 *
 * <p>Les propriétés décrites ci-dessous permettent d'affiner les resultats obtenues. (à renseigner dans unireg.properties)
 *
 * <ul>
 * 	<li><code>enabled</code>: true ou false, active ou desactive de maniere globale le contrôle.
 *
 *  <li><code>localitesInvalides</code>:
 *      la liste des termes, séparés par des virgules, utilisés pour construire les regexp qui vont servir à matcher les localités invalides
 *      <p>par exemple: <strong>"inconu,adrese"</strong> matchera <strong>"Inconnu", "Adressssse innnnccconnue", "S A N S - A D R E S S E",
 *      "parti sans laisser d'adresse"</strong> mais ne matchera pas <strong>"no address"</strong> (il faudrait enlever le 'e' à la fin du pattern)
 *
 *  <li><code>fauxPositifs</code>:
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
 * lors de l'instantiation d'un objet par le conteneur spring (cf. {@link ch.vd.uniregctb.adresse.LocaliteInvalideMatcher#afterPropertiesSet()}).
 *
 * <p>Cette mécanique est un peu bizarre mais pratique. Elle permet de paramètrer facilement le composant en utilisant des propriétés définies dans unireg.properties et
 * d'accéder à la fonction {@link LocaliteInvalideMatcher#match(String) match()}  par l'intermédiaire d'un membre statique.
 * match() est donc utilisable la méthode dans les classes qui ne sont pas instanciées par Spring, comme c'est le cas ici dans {@link AdresseEnvoiDetaillee}
 *
 * <p>Le pendant de cette façon de faire est qu'on ne peut pas instancier 2 LocaliteInvalideMatcher car l'instantiation du deuxieme ecraserait les paramètres
 * initialisé par le premier.

 *
 */
public class LocaliteInvalideMatcher implements InitializingBean {

	private static Logger LOGGER = Logger.getLogger(LocaliteInvalideMatcher.class);

	private static boolean enabled;
	private static String localitesInvalides;
	private static String fauxPositifs;

	private static Map<Character, String> conversionSpeciales = new HashMap<Character, String>();
	private static List<Pattern> patternsLocaliteInvalide = new ArrayList<Pattern>();
	private static List<Pattern> patternsFauxPositif  = new ArrayList<Pattern>();

	private static boolean initialized = false;


	/**
	 * Utile pour éviter les appels à la méthode match pendant que le bean initialise les champs statics
	 */
	private static ReadWriteLock lockInit = new ReentrantReadWriteLock();

	public void setEnabled(boolean enabled) {
		LocaliteInvalideMatcher.enabled = enabled;
	}

	public void setLocalitesInvalides(String propertyPatterns) {
		LocaliteInvalideMatcher.localitesInvalides = propertyPatterns;
	}

	public void setFauxPositifs(String propertyFauxPositifs) {
		LocaliteInvalideMatcher.fauxPositifs = propertyFauxPositifs;
	}

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
		lockInit.readLock().lock();
		try {
			if (!initialized) {
				LOGGER.warn("LocaliteInvalideMatcher n'a pas été initialisé. match() renvoie toujours 'false'");
				return false;
			}
			if (!enabled) {
				return false;
			}
			if (StringUtils.isBlank(localite)) {
				// Une chaine vide ou nulle n'est jamais une localité valide et donc "matche"
				return true;
			}
			for1: for (Pattern p: patternsLocaliteInvalide) {
				if (p.matcher(localite).find()) {
					// la localité match un pattern localité invalide
					for (Pattern q: patternsFauxPositif) {
						if (q.matcher(localite).find()) {
							// C'est un faux positif
							continue for1;
						}
					}
					// A ce stade, la localite match une localité invalide et n'est pas un faux positif
					return true;
				}
			}
			return false;
		}
		finally {
			lockInit.readLock().unlock();
		}
	}

	// expression régulière mappant les caractères qui peuvent être utilisé comme séparateur dans les localités invalides
	// On match ainsi les strings du type: "I N C O N N U","Non-Indiqué", "E_T_R_A_N_G_E_R", "S/DC"
	private static final String REGEXP_SEPARATEURS = "[ -./_,\\\\:;|*]*";

	private static String buildRegExpLocaliteInvalide (String terme) {
		StringBuilder sb = new StringBuilder();
		for(Character c: terme.toCharArray()) {
			sb.append("([");
			if (conversionSpeciales.containsKey(c)) {
				sb.append(conversionSpeciales.get(c));
			}
			else {
				sb.append(Character.toLowerCase(c)).append(Character.toUpperCase(c));
			}
			sb.append("]+");
			sb.append(REGEXP_SEPARATEURS);
			sb.append(")+");
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("regexp localité invalide pour " + terme + " = " + sb.toString());
		}
		return sb.toString();
	}

	private static String buildRegExpFauxPositif (String terme) {
		StringBuilder sb = new StringBuilder();
		for(Character c: terme.toCharArray()) {
			sb.append("[");
			sb.append(Character.toLowerCase(c)).append(Character.toUpperCase(c));
			sb.append("]");
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("regexp faux-positif pour " + terme + " = " + sb.toString());
		}
		return sb.toString();
	}

	/**
	 * Utile pour les tests, ne devrait pas être utiliser dans le code de prod pour l'instant
	 * cependant dans le futur on pourrait imaginer un rechargement à chaud des parametres et donc l'utilité de cette méthode
	 */
	static void reset() {
		lockInit.writeLock().lock();
		try {
			conversionSpeciales = new HashMap<Character, String>();
			patternsLocaliteInvalide = new ArrayList<Pattern>();
			patternsFauxPositif  = new ArrayList<Pattern>();
			localitesInvalides = null;
			fauxPositifs = null;
			enabled = false;
			initialized = false;
		}
		finally {
			lockInit.writeLock().unlock();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		lockInit.writeLock().lock();
		try {
			initialized = false;
            initConversionsSpeciales();
            initPatternsLocalitesInvalides();
			initPatternsFauxPositifs();
			initialized = true;
		}
		finally {
			lockInit.writeLock().unlock();
		}
	}

	private void initPatternsFauxPositifs() {
		patternsFauxPositif.clear();
		if (StringUtils.isNotBlank(fauxPositifs)) {
			String[] arrayFauxPositifs = fauxPositifs.split(",");
			for(String fauxPositif : arrayFauxPositifs) {
				patternsFauxPositif.add(Pattern.compile(buildRegExpFauxPositif(fauxPositif.trim())));
			}
			LOGGER.info("les " + arrayFauxPositifs.length + " termes suivants sont utilisés pour détecter les faux positifs : " + fauxPositifs);
		} else {
			LOGGER.warn("Aucun faux-positif n'est paramétré");
		}
	}

	private void initPatternsLocalitesInvalides() {
		patternsLocaliteInvalide.clear();
		if (StringUtils.isNotBlank(localitesInvalides)) {
			String[] arraylocalitesInvalides = localitesInvalides.split(",");
			for(String termeInvalide : arraylocalitesInvalides) {
				patternsLocaliteInvalide.add(Pattern.compile(buildRegExpLocaliteInvalide(termeInvalide.trim())));
			}
			LOGGER.info("les " + arraylocalitesInvalides.length + " termes suivants sont utilisés pour détecter les libellés de localité invalide : " + localitesInvalides );
		} else {
			LOGGER.warn("Aucun libellé de localité n'est considéré invalide");
		}

	}

	private void initConversionsSpeciales() {
		conversionSpeciales.clear();
		conversionSpeciales.put('a', "aäàáâãAÄÀÁÂÃ");
		conversionSpeciales.put('e', "eëèéêẽEËÈÉÊẼ");
		conversionSpeciales.put('i', "iïìíîĩIÏÌÍÎĨ");
		conversionSpeciales.put('o', "oöòóôõOÖÒÓÔÕ");
		conversionSpeciales.put('u', "uüùúûũUÜÙÚÛŨ");
		conversionSpeciales.put('n', "nñNÑ");
		conversionSpeciales.put('c', "cçCÇ");
	}
}
