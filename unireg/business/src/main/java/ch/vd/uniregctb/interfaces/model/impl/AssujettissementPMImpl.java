package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.AssujettissementPM;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AssujettissementPMImpl implements AssujettissementPM, Serializable {

	private static final long serialVersionUID = -5318696556204038996L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noSequence;
	private final Type type;

	public static AssujettissementPMImpl get(ch.vd.registre.fiscal.model.Assujettissement target) {
		if (target == null) {
			return null;
		}
		return new AssujettissementPMImpl(target);
	}

	private AssujettissementPMImpl(ch.vd.registre.fiscal.model.Assujettissement target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.noSequence = target.getNoSequence();
		this.type = Type.valueOf(target.getTypeAssujettissement().getName());
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
