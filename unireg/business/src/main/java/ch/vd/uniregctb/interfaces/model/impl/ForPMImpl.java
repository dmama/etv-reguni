package ch.vd.uniregctb.interfaces.model.impl;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.ForPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ForPMImpl implements ForPM {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final int noOfsAutoriteFiscale;
	private final TypeNoOfs typeAutoriteFiscale;

	public static ForPMImpl get(ch.vd.registre.pm.model.ForPM target) {
		if (target == null) {
			return null;
		}
		return new ForPMImpl(target);
	}

	public ForPMImpl(ch.vd.registre.pm.model.ForPM target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.noOfsAutoriteFiscale = target.getNoOfsAutoriteFiscale();
		this.typeAutoriteFiscale = TypeNoOfs.valueOf(target.getTypeAutoriteFiscale().name());
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
	public int getNoOfsAutoriteFiscale() {
		return noOfsAutoriteFiscale;
	}

	@Override
	public TypeNoOfs getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}
}