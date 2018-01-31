package ch.vd.uniregctb.registrefoncier.allegement;

import java.math.BigDecimal;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.foncier.ExonerationIFONC;

public class ExonerationIFONCView implements Annulable, DateRange {

	private final long idExoneration;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final BigDecimal pourcentageExoneration;

	public ExonerationIFONCView(ExonerationIFONC data) {
		this.idExoneration = data.getId();
		this.annule = data.isAnnule();
		this.dateDebut = data.getDateDebut();
		this.dateFin = data.getDateFin();
		this.pourcentageExoneration = data.getPourcentageExoneration();
	}

	public long getIdExoneration() {
		return idExoneration;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public BigDecimal getPourcentageExoneration() {
		return pourcentageExoneration;
	}

	/**
	 * @return <code>true</code> si le dégrèvement est complètement dans le passé
	 */
	public boolean isPast() {
		return dateFin != null && dateFin.isBefore(RegDate.get());
	}
}
