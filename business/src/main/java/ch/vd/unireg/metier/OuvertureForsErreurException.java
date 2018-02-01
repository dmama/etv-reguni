package ch.vd.unireg.metier;

import ch.vd.unireg.metier.OuvertureForsResults.ErreurType;
import ch.vd.unireg.tiers.PersonnePhysique;

/**
 * Exception levée dans le traitement de l'ouverture des fors des contribuables majeurs, et qui contient les informations nécessaires pour
 * renseigner le rapport d'erreur du batch.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
class OuvertureForsErreurException extends Exception {

	private static final long serialVersionUID = 7991504624176985007L;

	private final PersonnePhysique habitant;
	private final ErreurType type;

	public OuvertureForsErreurException(PersonnePhysique habitant, ErreurType type) {
		this.habitant = habitant;
		this.type = type;
	}

	public OuvertureForsErreurException(PersonnePhysique habitant, ErreurType type, Throwable cause) {
		super(cause);
		this.habitant = habitant;
		this.type = type;
	}

	public OuvertureForsErreurException(PersonnePhysique habitant, ErreurType type, String message) {
		super(message);
		this.habitant = habitant;
		this.type = type;
	}

	public PersonnePhysique getHabitant() {
		return habitant;
	}

	public ErreurType getType() {
		return type;
	}
}
