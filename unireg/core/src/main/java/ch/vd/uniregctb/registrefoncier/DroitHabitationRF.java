package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;

/**
 * Droit d'habitation sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
@DiscriminatorValue("DroitHabitation")
public class DroitHabitationRF extends ServitudeRF {

	public DroitHabitationRF() {
	}

	public DroitHabitationRF(@NotNull DroitHabitationRF right) {
		super(right);
	}

	@Transient
	@Override
	public DroitHabitationRF duplicate() {
		return new DroitHabitationRF(this);
	}
}
