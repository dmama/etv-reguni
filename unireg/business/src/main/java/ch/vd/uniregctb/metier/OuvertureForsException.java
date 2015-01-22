package ch.vd.uniregctb.metier;

import ch.vd.uniregctb.metier.OuvertureForsResults.ErreurType;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Exception levée dans le traitement de l'ouverture des fors des contribuables majeurs, et qui contient les informations nécessaires pour
 * renseigner le rapport d'erreur du batch.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class OuvertureForsException extends Exception {

	private static final long serialVersionUID = 9209211678473408389L;

	private PersonnePhysique habitant;
	private ErreurType type;

	public OuvertureForsException() {
	}

	public OuvertureForsException(String message, Throwable cause) {
		super(message, cause);
	}

	public OuvertureForsException(String message) {
		super(message);
	}

	public OuvertureForsException(Throwable cause) {
		super(cause);
	}

	public OuvertureForsException(PersonnePhysique habitant, ErreurType type) {
		this.habitant = habitant;
		this.type = type;
	}

	public OuvertureForsException(PersonnePhysique habitant, ErreurType type, Throwable cause) {
		super(cause);
		this.habitant = habitant;
		this.type = type;
	}

	public OuvertureForsException(PersonnePhysique habitant, ErreurType type, String message) {
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
