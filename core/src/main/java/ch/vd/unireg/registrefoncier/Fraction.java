package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Fraction représentant une partie d'un tout (par exemple: quote-part dans le cas d'un immeuble en PPE ou un part de propriété).
 */
@Embeddable
public class Fraction implements Comparable<Fraction> {

	private int numerateur;
	private int denominateur;

	public Fraction() {
	}

	public Fraction(int numerateur, int denominateur) {
		this.numerateur = numerateur;
		this.denominateur = denominateur;
	}

	@Column(name = "NUMERATEUR", nullable = false)
	public int getNumerateur() {
		return numerateur;
	}

	public void setNumerateur(int numerateur) {
		this.numerateur = numerateur;
	}

	@Column(name = "DENOMINATEUR", nullable = false)
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

	@Override
	public int compareTo(@NotNull Fraction other) {

		if (this == other) {
			return 0;
		}
		if (this.numerateur == other.numerateur && this.denominateur == other.denominateur) {
			return 0;
		}

		// otherwise see which is less
		long first = (long) this.numerateur * (long) other.denominateur;
		long second = (long) other.numerateur * (long) this.denominateur;
		return Long.compare(first, second);
	}
}
