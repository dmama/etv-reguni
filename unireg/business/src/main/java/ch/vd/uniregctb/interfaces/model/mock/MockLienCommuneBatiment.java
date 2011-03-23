package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class MockLienCommuneBatiment implements DateRange {

	private MockCommune commune;
	private MockBatiment batiment;
	private RegDate dateDebut;
	private RegDate dateFin;

	MockLienCommuneBatiment(MockCommune commune, MockBatiment batiment, RegDate dateDebut, RegDate dateFin) {
		this.commune = commune;
		this.batiment = batiment;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public MockCommune getCommune() {
		return commune;
	}

	public MockBatiment getBatiment() {
		return batiment;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
