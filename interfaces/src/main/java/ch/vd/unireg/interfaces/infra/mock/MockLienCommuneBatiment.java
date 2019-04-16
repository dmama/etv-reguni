package ch.vd.unireg.interfaces.infra.mock;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public class MockLienCommuneBatiment implements DateRange {

	private final MockCommune commune;
	private final MockBatiment batiment;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	MockLienCommuneBatiment(MockCommune commune, MockBatiment batiment, RegDate dateDebut, RegDate dateFin) {
		if (commune == null) {
			throw new IllegalArgumentException();
		}
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

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
