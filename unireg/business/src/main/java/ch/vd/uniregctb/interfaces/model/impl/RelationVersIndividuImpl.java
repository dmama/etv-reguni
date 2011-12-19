package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.RelationVersIndividu;

public class RelationVersIndividuImpl implements RelationVersIndividu {

	private long numeroAutreIndividu;
	private RegDate dateDebut;
	private RegDate dateFin;

	public RelationVersIndividuImpl(long numeroAutreIndividu, RegDate dateDebut, RegDate dateFin) {
		this.numeroAutreIndividu = numeroAutreIndividu;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public long getNumeroAutreIndividu() {
		return numeroAutreIndividu;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
