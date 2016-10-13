package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.Annulable;

public class RegimeFiscalView implements DateRange, Annulable {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeRegimeFiscal type;
	private final boolean annule;

	public RegimeFiscalView(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, TypeRegimeFiscal type) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
	}

	public Long getId() {
		return id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public TypeRegimeFiscal getType() {
		return type;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}
}
