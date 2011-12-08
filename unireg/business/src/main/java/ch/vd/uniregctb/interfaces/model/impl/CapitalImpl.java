package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class CapitalImpl implements Capital, Serializable {

	private static final long serialVersionUID = 4245185162092655371L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Long capitalAction;
	private final Long capitalLibere;
	private final EditionFosc editionFosc;

	public static CapitalImpl get(ch.vd.registre.pm.model.Capital target) {
		if (target == null) {
			return null;
		}
		return new CapitalImpl(target);
	}

	public CapitalImpl(ch.vd.registre.pm.model.Capital target) {
		this.dateDebut = RegDate.get(target.getDateDebut());
		this.dateFin = RegDate.get(target.getDateFin());
		this.capitalAction = target.getCapitalAction();
		this.capitalLibere = target.getCapitalLibere();
		this.editionFosc = EditionFoscImpl.get(target.getEditionFosc());
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
	public Long getCapitalAction() {
		return capitalAction;
	}

	@Override
	public Long getCapitalLibere() {
		return capitalLibere;
	}

	@Override
	public EditionFosc getEditionFosc() {
		return editionFosc;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
