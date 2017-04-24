package ch.vd.uniregctb.regimefiscal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-04-21, <raphael.marmier@vd.ch>
 */
class ServiceRegimeFiscalConfigurationImpl implements ServiceRegimeFiscalConfiguration, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegimeFiscalConfigurationImpl.class);

	private static final String WHITESPACE_CHARACTER = "\\s";
	private static final String WHITESPACE_REGEXP = "[" + WHITESPACE_CHARACTER + "]+";
	private static final String RECORD_SEPARATOR = ",";
	private static final String RECORD_SEPARATOR_REGEXP = "[,]";
	private static final String TUPLE_OPERATOR = "=";
	private static final String CODE_FORME_JURIDIQUE_REGEXP = "[0-9]{4}";
	private static final String ALLOWED_CHARACTERS_CODE_REGIME = "-A-Za-z0-9";
	private static final String CODE_REGIME_REGEXP = "[" + ALLOWED_CHARACTERS_CODE_REGIME + "]+";
	private static final String VALID_TUPLE_REGEXP = CODE_FORME_JURIDIQUE_REGEXP + TUPLE_OPERATOR + CODE_REGIME_REGEXP;
	private static final String ALLOWED_CHARACTERS_REGEXP = "[" + ALLOWED_CHARACTERS_CODE_REGIME + RECORD_SEPARATOR + TUPLE_OPERATOR + WHITESPACE_CHARACTER + "]+";

	private Map<FormeJuridiqueEntreprise, String> regimesParDefautMap;
	private Set<String> regimesDiOptionnelleVd;

	private String configTableFormesJuridiquesDefauts;
	private String configRegimesDiOptionnelleVd;

	public void setConfigTableFormesJuridiquesDefauts(String configTableFormesJuridiquesDefauts) {
		this.configTableFormesJuridiquesDefauts = configTableFormesJuridiquesDefauts;
	}

	public void setConfigRegimesDiOptionnelleVd(String configRegimesDiOptionnelleVd) {
		this.configRegimesDiOptionnelleVd = configRegimesDiOptionnelleVd;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		regimesParDefautMap = parseConfigTableFormesJuridiquesDefauts(configTableFormesJuridiquesDefauts);
		regimesDiOptionnelleVd = parseConfigRegimesDiOptionnelleVd(configRegimesDiOptionnelleVd);
	}

	@Override
	@Nullable
	public String getCodeTypeRegimeFiscal(FormeJuridiqueEntreprise formeJuridique) {
		return regimesParDefautMap.get(formeJuridique);
	}

	@Override
	public boolean isRegimeFiscalDiOptionnelleVd(String codeTypeRegimeFiscal) {
		return regimesDiOptionnelleVd.contains(codeTypeRegimeFiscal);
	}

	/**
	 * <p>Interprête la configuration de la table de correspondance des régimes fiscaux à utiliser par défaut pour les formes juridiques d'entreprise.</p>
	 *
	 * <p>La table doit être encodée dans une chaîne de caractère selon les règles suivantes:</p>
	 * <ul>
	 *     <li>La configuration consiste en une liste de paires (tuples) faisant correspondre une forme juridique à un type de régime fiscal au moyen du signe <code>=</code>.</li>
	 *     <li>Chaque tuple est séparé par une virgule.</li>
	 *     <li>Les caractères permis sont <code>a-zA-Z0-9</code> et <code>-=,</code>. Les espaces sont enlevés du texte avant interprétation. On est donc libre d'ajouter
	 *     des espaces pour rendre la configuration plus lisible.</li>
	 *     <li>La clé est constituée du code à 4 chiffres de la forme juridique.</li>
	 *     <li>La valeur est constituée du code alpha-numérique du type de régime fiscal.</li>
	 * </ul>
	 *
	 * <p>Exemple: 0103=80, 0104=80, 0105=01, 0106=01, 0107=01, 0108=01, 0109=70, 0110=70, 0111=01, 0151=01, 0312=01</p>
	 * @param config la chaîne de caractères contenant la table encodée.
	 * @return la Map de la table de correspondance forme juridique vers code de type régime fiscal par défaut.
	 */
	@NotNull
	Map<FormeJuridiqueEntreprise, String> parseConfigTableFormesJuridiquesDefauts(String config) {
		if (StringUtils.isBlank(config)) {
			return Collections.emptyMap();
		}
		else if (!config.matches(ALLOWED_CHARACTERS_REGEXP)) {
			throw new IllegalArgumentException(String.format("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts malformée: [%s]", config));
		}
		final String mappingString = stripWhiteSpace(config);
		return Arrays.stream(mappingString.split(RECORD_SEPARATOR_REGEXP))
				.map(this::checkAndSplitTupleFormesJuridiquesDefauts)
				.collect(Collectors.toMap(t -> toFormeJuridiqueEntreprise(t[0]), t -> t[1]));
	}

	@NotNull
	private String[] checkAndSplitTupleFormesJuridiquesDefauts(String e) {
		if (!e.matches(VALID_TUPLE_REGEXP)) {
			throw new IllegalArgumentException(String.format("Propriété de configuration extprop.regimesfiscaux.defauts.formesjuridiques.map invalide: paire malformée [%s]", e));
		}
		return e.split(TUPLE_OPERATOR);
	}

	@NotNull
	private FormeJuridiqueEntreprise toFormeJuridiqueEntreprise(String codeECH) {
		try {
			return FormeJuridiqueEntreprise.fromCode(codeECH);
		}
		catch (RuntimeException e) {
			throw new IllegalArgumentException(String.format("Configuration extprop.regimesfiscaux.defauts.formesjuridiques.map potentiellement erronnée: %s", e.getMessage()));
		}
	}

	/**
	 * <p>Interprête la configuration de la liste des régimes fiscaux entraînant une DI optionnelle pour les entités vaudoises.</p>
	 *
	 * <p>La liste doit être encodée dans une chaîne de caractère selon les règles suivantes:</p>
	 * <ul>
	 *     <li>La configuration consiste en une liste de code de type de régime fiscal.</li>
	 *     <li>Chaque code est séparé par une virgule.</li>
	 *     <li>Les caractères permis sont <code>a-zA-Z0-9</code> et <code>-,</code>. Les espaces sont enlevés du texte avant interprétation. On est donc libre d'ajouter
	 *     des espaces pour rendre la configuration plus lisible.</li>
	 *     <li>Le code du type de régime fiscal est alpha-numérique.</li>
	 * </ul>
	 *
	 * <p>Exemple: 190-2, 739</p>
	 * @param config la chaîne de caractères contenant la liste encodée.
	 * @return la liste de type régime fiscal sous forme de Map.
	 */
	@NotNull
	Set<String> parseConfigRegimesDiOptionnelleVd(String config) {
		if (StringUtils.isBlank(config)) {
			return Collections.emptySet();
		}
		else if (!config.matches(ALLOWED_CHARACTERS_REGEXP)) {
			throw new IllegalArgumentException(String.format("Propriété de configuration extprop.regimesfiscaux.regimes.di.optionnelle.vd malformée: [%s]", config));
		}
		final String listString = stripWhiteSpace(config);
		return Arrays.stream(listString.split(RECORD_SEPARATOR_REGEXP))
				.collect(Collectors.toSet());
	}

	private String stripWhiteSpace(String regimesParDefautMapString) {
		return regimesParDefautMapString.replaceAll(WHITESPACE_REGEXP, "");
	}
}
