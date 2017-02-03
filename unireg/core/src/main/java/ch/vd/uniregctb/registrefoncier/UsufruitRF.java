package ch.vd.uniregctb.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Droit de type usufruit sur un immeuble. L'ayant-droit d'un droit habitation est soit une personne morale, soit une personne physique.
 */
@Entity
@DiscriminatorValue("Usufruit")
public class UsufruitRF extends ServitudeRF {

}
