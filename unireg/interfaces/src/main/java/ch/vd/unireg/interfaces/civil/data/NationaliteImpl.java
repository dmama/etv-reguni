package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;

public class NationaliteImpl implements Nationalite, Serializable {

	private static final long serialVersionUID = -6860695089410911036L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Pays pays;

	public static NationaliteImpl get(ch.vd.registre.civil.model.Nationalite target) {
		if (target == null) {
			return null;
		}
		return new NationaliteImpl(target);
	}

	private NationaliteImpl(ch.vd.registre.civil.model.Nationalite target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.pays = PaysImpl.get(target.getPays());
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

	@Override
	public Pays getPays() {
		return pays;
	}
}
