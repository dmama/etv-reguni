package ch.vd.uniregctb.evenement.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

/**
 * Adapter de l'événement de veuvage.
 * 
 * @author Pavel BLANCO
 *
 */
public class VeuvageAdapter extends GenericEvenementAdapter implements Veuvage {

	public RegDate getDateVeuvage() {
		return getDate();
	}

}
