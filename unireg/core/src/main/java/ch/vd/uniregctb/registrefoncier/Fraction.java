package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Embeddable;
import java.util.Objects;

/**
 * Fraction représentant une partie d'un tout (par exemple: quote-part dans le cas d'un immeuble en PPE ou un part de propriété).
 */
@Embeddable
public class Fraction {

	private int numerateur;
	private int denominateur;

	public Fraction() {
	}

	public Fraction(int numerateur, int denominateur) {
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
		final Fraction fraction = (Fraction) o;
		return numerateur == fraction.numerateur && denominateur == fraction.denominateur;
	}

	@Override
	public int hashCode() {
		return Objects.hash(numerateur, denominateur);
	}

	@Override
	public String toString() {
		return String.valueOf(numerateur) + "/" + denominateur;
	}
}
