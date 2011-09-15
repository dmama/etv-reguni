package ch.vd.uniregctb.rf;

import javax.persistence.Embeddable;

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
	public String toString() {
		if (denominateur == 1) {
			return String.valueOf(numerateur);
		}
		else {
			return String.format("%d/%d", numerateur, denominateur);
		}
	}
}
