package ch.vd.unireg.interfaces.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.TexteCasePostale;

/**
 * Contient les informations (texte + numéro) pour l'adressage d'une boîte postale.
 *
 * le npa est optionnel, il est renseigné lorsque le npa de la case postale diffère de celui de la localité
 */
public class CasePostale implements Serializable {

	private static final long serialVersionUID = 8748093849874697183L;

	public static final CasePostale VIDE = new CasePostale(TexteCasePostale.CASE_POSTALE,null,null);
	private final TexteCasePostale type;
	private final Integer numero;
	private final Integer npa;

	public CasePostale(TexteCasePostale type, Integer numero) {
		this(type, numero, null);
	}

	public CasePostale(String text, Number numero) {
		this(text, numero, null);
	}

	public CasePostale(TexteCasePostale type, Integer numero, Integer npa) {
		this.type = type;
		this.numero = numero;
		this.npa = npa;
	}

	public CasePostale(String text, Number numero, Integer npa) {
		this.type = TexteCasePostale.parse(text);
		this.numero = (numero == null ? null : numero.intValue());
		this.npa = npa;
	}

	public TexteCasePostale getType() {
		return type;
	}

	public Integer getNumero() {
		return numero;
	}

	public Integer getNpa() {
		return npa;
	}

	public String toString() {
		if (numero == null) {
			return type.format();
		}
		else {
			return type.format(numero);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final CasePostale that = (CasePostale) o;

		if (npa != null ? !npa.equals(that.npa) : that.npa != null) return false;
		if (numero != null ? !numero.equals(that.numero) : that.numero != null) return false;
		if (type != that.type) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = type != null ? type.hashCode() : 0;
		result = 31 * result + (numero != null ? numero.hashCode() : 0);
		result = 31 * result + (npa != null ? npa.hashCode() : 0);
		return result;
	}

	private static final List<Pair<TexteCasePostale, Pattern>> PARSING_PATTERNS = buildParsingPatterns();

	private static List<Pair<TexteCasePostale, Pattern>> buildParsingPatterns() {
		final List<Pair<TexteCasePostale, Pattern>> patterns = new ArrayList<>();
		patterns.add(Pair.of(TexteCasePostale.CASE_POSTALE, Pattern.compile("^(?:case|cases)\\s(?:postale|postales)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		patterns.add(Pair.of(TexteCasePostale.CASE_POSTALE, Pattern.compile("^(?:CP)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		patterns.add(Pair.of(TexteCasePostale.BOITE_POSTALE, Pattern.compile("^(?:boite|boîte|boites|boîtes)\\s(?:postale|postales)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		patterns.add(Pair.of(TexteCasePostale.POSTFACH, Pattern.compile("^(?:postfach)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		patterns.add(Pair.of(TexteCasePostale.CASELLA_POSTALE, Pattern.compile("^(?:casella)\\s(?:postale)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		patterns.add(Pair.of(TexteCasePostale.PO_BOX, Pattern.compile("^(?:po)\\s(?:box)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE)));
		return Collections.unmodifiableList(patterns);
	}

	/**
	 * Converti une case postale exprimée sous forme de chaîne de caractères en un objet composé. <p> Exemples de case postales vus en production :
	 * <pre>
	 * - "Case postale"
	 * - "Case postale 121"
	 * - "Cases postales 12/345"
	 * - "Postfach"
	 * - "Postfach 31"
	 * - "Case Postale 1245"
	 * - "CP 711"
	 * - "Casella postale 32"
	 * </pre>
	 * </p>
	 *
	 * @param string la case postale sous forme de string
	 * @return la case postale sous forme détaillée
	 */
	public static CasePostale parse(@Nullable String string) {

		if (StringUtils.isBlank(string)) {
			return null;
		}

		for (Pair<TexteCasePostale, Pattern> parsingPattern : PARSING_PATTERNS) {
			final Matcher matcher = parsingPattern.getRight().matcher(string);
			if (matcher.find()) {
				final String numero = matcher.groupCount() > 0 ? matcher.group(1) : null;
				return new CasePostale(parsingPattern.getLeft(), parseInt(numero));
			}
		}

		return null;
	}

	private static Integer parseInt(String n) {
		Integer number;
		try {
			number = Integer.parseInt(n);
		}
		catch (NumberFormatException ignored) {
			number = null;
		}
		return number;
	}

}
