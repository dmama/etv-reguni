package ch.vd.uniregctb.rf;

import javax.persistence.Embeddable;

import org.apache.commons.lang.StringUtils;

/**
 * Représente une part de propriété sous forme de fraction. Cette part est logiquement comprise entre 0 et 1.
 */
@Embeddable
public class PartPropriete {

	private int numerateur;
	private int denominateur;

	public PartPropriete() {
	}

	public PartPropriete(int numerateur, int denominateur) {
		this.numerateur = numerateur;
		this.denominateur = denominateur;
	}

	public int getNumerateur() {
		return numerateur;
	}

	public void setNumerateur(int numerateur) {
		this.numerateur = numerateur;
	}

	public int getDenominateur() {
		return denominateur;
	}

	public void setDenominateur(int denominateur) {
		this.denominateur = denominateur;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final PartPropriete that = (PartPropriete) o;
		return denominateur == that.denominateur && numerateur == that.numerateur;

	}

	@Override
	public int hashCode() {
		int result = numerateur;
		result = 31 * result + denominateur;
		return result;
	}

	@Override
	public String toString() {
		return String.format("%d/%d", numerateur, denominateur);
	}

	public static PartPropriete parse(String s) {
		final String str = StringUtils.trimToNull(s);
		if (str == null) {
			return null;
		}
		if ("1".equals(str)) {
			return new PartPropriete(1, 1);
		}
		else {
			final String[] tokens = str.split("/");
			if (tokens.length != 2) {
				throw new IllegalArgumentException("La string [" + str + "] ne représente par une part de propriété valide");
			}
			int numerateur = Integer.parseInt(StringUtils.trim(tokens[0]));
			int denominateur = Integer.parseInt(StringUtils.trim(tokens[1]));
			return new PartPropriete(numerateur, denominateur);
		}
	}
}
