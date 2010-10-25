package ch.vd.uniregctb.interfaces.model.wrapper.hostinterfaces;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AssujettissementPMWrapper implements AssujettissementPM, Serializable {

	private static final long serialVersionUID = -5318696556204038996L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noSequence;
	private final Type type;

	public static AssujettissementPMWrapper get(ch.vd.registre.fiscal.model.Assujettissement target) {
		if (target == null) {
			return null;
		}
		return new AssujettissementPMWrapper(target);
	}

	private AssujettissementPMWrapper(ch.vd.registre.fiscal.model.Assujettissement target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.noSequence = target.getNoSequence();
		this.type = Type.valueOf(target.getTypeAssujettissement().getName());
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public Type getType() {
		return type;
	}
}
