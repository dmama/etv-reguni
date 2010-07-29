package ch.vd.uniregctb.evenement.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementCivil;

/**
 * Modélise un événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public interface Veuvage extends EvenementCivil {

	/**
	 * Renvoie la date à laquelle le conjoint est décédé.
	 * @return
	 */
	public RegDate getDateVeuvage();
}
