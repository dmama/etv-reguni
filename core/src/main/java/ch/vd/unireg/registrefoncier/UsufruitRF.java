package ch.vd.unireg.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.jetbrains.annotations.NotNull;

/**
 * Droit de type usufruit sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
@DiscriminatorValue("Usufruit")
public class UsufruitRF extends ServitudeRF {

	public UsufruitRF() {
	}

	public UsufruitRF(@NotNull UsufruitRF right) {
		super(right);
	}

	@Transient
	@Override
	public UsufruitRF duplicate() {
		return new UsufruitRF(this);
	}
}
