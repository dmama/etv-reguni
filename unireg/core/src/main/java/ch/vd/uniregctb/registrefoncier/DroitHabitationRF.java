package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Droit d'habitation sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
@DiscriminatorValue("DroitHabitation")
public class DroitHabitationRF extends ServitudeRF {

}
