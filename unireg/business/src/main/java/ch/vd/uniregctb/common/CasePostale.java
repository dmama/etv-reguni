package ch.vd.uniregctb.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.TexteCasePostale;

/**
 * Contient les informations (texte + numéro) pour l'adressage d'une boîte postale.
 */
public class CasePostale {
	private TexteCasePostale type;
	private Integer numero;

	public CasePostale(TexteCasePostale type, Integer numero) {
		this.type = type;
		this.numero = numero;
	}

	public TexteCasePostale getType() {
		return type;
	}

	public Integer getNumero() {
		return numero;
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

		return !(numero != null ? !numero.equals(that.numero) : that.numero != null) && type == that.type;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + (numero != null ? numero.hashCode() : 0);
		return result;
	}

	private static final Pattern CASE_POSTALE_PATTERN = Pattern.compile("^(?:case|cases)\\s(?:postale|postales)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern CASE_POSTALE_PATTERN_2 = Pattern.compile("^(?:CP)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern BOITE_POSTALE_PATTERN = Pattern.compile("^(?:boite|boîte|boites|boîtes)\\s(?:postale|postales)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern POSTFACH_PATTERN = Pattern.compile("^(?:postfach)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern CASELLA_POSTALE_PATTERN = Pattern.compile("^(?:casella)\\s(?:postale)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PO_BOX_PATTERN = Pattern.compile("^(?:po)\\s(?:box)[^\\d]*(\\d*)", Pattern.CASE_INSENSITIVE);

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

		final Matcher cp = CASE_POSTALE_PATTERN.matcher(string);
		if (cp.find()) {
			String numero = null;
			if (cp.groupCount() > 0) {
				numero = cp.group(1);
			}
			return new CasePostale(TexteCasePostale.CASE_POSTALE, parseInt(numero));
		}

		final Matcher cp2 = CASE_POSTALE_PATTERN_2.matcher(string);
		if (cp2.find()) {
			String numero = null;
			if (cp2.groupCount() > 0) {
				numero = cp2.group(1);
			}
			return new CasePostale(TexteCasePostale.CASE_POSTALE, parseInt(numero));
		}

		final Matcher bp = BOITE_POSTALE_PATTERN.matcher(string);
		if (bp.find()) {
			String numero = null;
			if (bp.groupCount() > 0) {
				numero = bp.group(1);
			}
			return new CasePostale(TexteCasePostale.BOITE_POSTALE, parseInt(numero));
		}

		final Matcher pf = POSTFACH_PATTERN.matcher(string);
		if (pf.find()) {
			String numero = null;
			if (pf.groupCount() > 0) {
				numero = pf.group(1);
			}
			return new CasePostale(TexteCasePostale.POSTFACH, parseInt(numero));
		}

		final Matcher ll = CASELLA_POSTALE_PATTERN.matcher(string);
		if (ll.find()) {
			String numero = null;
			if (ll.groupCount() > 0) {
				numero = ll.group(1);
			}
			return new CasePostale(TexteCasePostale.CASELLA_POSTALE, parseInt(numero));
		}

		final Matcher po = PO_BOX_PATTERN.matcher(string);
		if (po.find()) {
			String numero = null;
			if (po.groupCount() > 0) {
				numero = po.group(1);
			}
			return new CasePostale(TexteCasePostale.PO_BOX, parseInt(numero));
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
