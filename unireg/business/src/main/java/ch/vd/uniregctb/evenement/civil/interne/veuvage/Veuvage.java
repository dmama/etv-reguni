package ch.vd.uniregctb.evenement.civil.interne.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Modélise un événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public interface Veuvage extends EvenementCivilInterne {

	/**
	 * Renvoie la date à laquelle le conjoint est décédé.
	 * @return
	 */
	public RegDate getDateVeuvage();
}
