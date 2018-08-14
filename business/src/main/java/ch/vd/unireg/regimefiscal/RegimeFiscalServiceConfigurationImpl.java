package ch.vd.unireg.regimefiscal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-04-21, <raphael.marmier@vd.ch>
 */
class RegimeFiscalServiceConfigurationImpl implements RegimeFiscalServiceConfiguration, InitializingBean {

	private static final String WHITESPACE_CHARACTER = "\\s";
	private static final String WHITESPACE_REGEXP = "[" + WHITESPACE_CHARACTER + "]+";
	private static final String RECORD_SEPARATOR = ",";
	private static final String RECORD_SEPARATOR_REGEXP = ",";
	private static final String TUPLE_OPERATOR = "=>";
	private static final String DATE_SEPARATOR = "\\{}";
	private static final String CODE_FORME_JURIDIQUE_REGEXP = "[0-9]{4}";
	private static final String ALLOWED_CHARACTERS_CODE_REGIME = "-A-Za-z0-9";
	private static final String CODE_REGIME_REGEXP = "[" + ALLOWED_CHARACTERS_CODE_REGIME + "]+";
	private static final Pattern FORME_JURIDIQUE_MAPPING_REGEXP = Pattern.compile("(" + CODE_FORME_JURIDIQUE_REGEXP + ")" + TUPLE_OPERATOR + "(" + CODE_REGIME_REGEXP + ")(?:\\{([0-9]*)=>([0-9]*)})?"); // e.g. 0107=>01 ou 0107=>01{19700101=>20180203}
	private static final String ALLOWED_CHARACTERS_REGEXP = "[" + ALLOWED_CHARACTERS_CODE_REGIME + RECORD_SEPARATOR + TUPLE_OPERATOR + DATE_SEPARATOR + WHITESPACE_CHARACTER + "]+";

	private List<FormeJuridiqueMapping> formeJuridiquesMappings;
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
		formeJuridiquesMappings = parseConfigFormesJuridiquesMapping(configTableFormesJuridiquesDefauts);
		regimesDiOptionnelleVd = parseConfigRegimesDiOptionnelleVd(configRegimesDiOptionnelleVd);
	}

	@NotNull
	@Override
	public List<FormeJuridiqueMapping> getMapping(FormeJuridiqueEntreprise formeJuridique) {
		return formeJuridiquesMappings.stream()
				.filter(m -> m.getFormeJuridique() == formeJuridique)
				.collect(Collectors.toList());
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
	 * <p>Exemple: 0103=>80, 0104=>80, 0105=>01, 0106=>01, 0107=>01, 0108=>01, 0109=>70{=>20171231}, 0109=>703{20180101=>}, 0110=>70{=>20171231}, 0110=>703{20180101=>}, 0111=>01, 0151=>01, 0312=>01</p>
	 * @param config la chaîne de caractères contenant la table encodée.
	 * @return la Map de la table de correspondance forme juridique vers code de type régime fiscal par défaut.
	 */
	List<FormeJuridiqueMapping> parseConfigFormesJuridiquesMapping(String config) {
		if (StringUtils.isBlank(config)) {
			return Collections.emptyList();
		}
		else if (!config.matches(ALLOWED_CHARACTERS_REGEXP)) {
			throw new IllegalArgumentException(String.format("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts malformée: [%s]", config));
		}
		final String mappingString = stripWhiteSpace(config);
		return Arrays.stream(mappingString.split(RECORD_SEPARATOR_REGEXP))
				.map(this::parseFormeJuridiqueMapping)
				.collect(Collectors.toList());
	}

	@NotNull
	private FormeJuridiqueMapping parseFormeJuridiqueMapping(String mappingAsString) {

		final Matcher matcher = FORME_JURIDIQUE_MAPPING_REGEXP.matcher(mappingAsString);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format("Propriété de configuration extprop.regimesfiscaux.table.formesjuridiques.defauts invalide: paire malformée [%s]", mappingAsString));
		}
		final String codeFormeJuridique = matcher.group(1);
		final String codeRegime = matcher.group(2);
		final String indexDateFrom = matcher.groupCount() > 2 ? matcher.group(3) : null;
		final String indexDateTo = matcher.groupCount() > 3 ? matcher.group(4) : null;

		return new FormeJuridiqueMapping(parseRegDate(indexDateFrom),
		                                 parseRegDate(indexDateTo),
		                                 toFormeJuridiqueEntreprise(codeFormeJuridique),
		                                 codeRegime);
	}

	private static RegDate parseRegDate(@Nullable String index) {
		if (StringUtils.isBlank(index)) {
			return null;
		}
		else {
			return RegDateHelper.indexStringToDate(index);
		}
	}

	@NotNull
	private FormeJuridiqueEntreprise toFormeJuridiqueEntreprise(String codeECH) {
		try {
			return FormeJuridiqueEntreprise.fromCode(codeECH);
		}
		catch (RuntimeException e) {
			throw new IllegalArgumentException(String.format("Configuration extprop.regimesfiscaux.table.formesjuridiques.defauts potentiellement erronée: %s", e.getMessage()));
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
