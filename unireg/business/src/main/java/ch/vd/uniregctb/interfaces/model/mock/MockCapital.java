package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Capital;
import ch.vd.uniregctb.interfaces.model.EditionFosc;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MockCapital implements Capital {

	private RegDate dateDebut;
	private RegDate dateFin;
	private Long capitalAction;
	private Long capitalLibere;
	private EditionFosc editionFosc;

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public Long getCapitalAction() {
		return capitalAction;
	}

	public void setCapitalAction(Long capitalAction) {
		this.capitalAction = capitalAction;
	}

	@Override
	public Long getCapitalLibere() {
		return capitalLibere;
	}

	public void setCapitalLibere(Long capitalLibere) {
		this.capitalLibere = capitalLibere;
	}

	@Override
	public EditionFosc getEditionFosc() {
		return editionFosc;
	}

	public void setEditionFosc(EditionFosc editionFosc) {
		this.editionFosc = editionFosc;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
