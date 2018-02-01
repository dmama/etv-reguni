package ch.vd.unireg.regimefiscal;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;

/**
 * Période associée à in mode d'exonération
 */
public class ModeExonerationHisto implements CollatableDateRange<ModeExonerationHisto> {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final ModeExoneration modeExoneration;

	public ModeExonerationHisto(RegDate dateDebut, RegDate dateFin, ModeExoneration modeExoneration) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.modeExoneration = modeExoneration;
	}

	@Override
	public boolean isCollatable(ModeExonerationHisto next) {
		return DateRangeHelper.isCollatable(this, next) && next.modeExoneration == modeExoneration;
	}

	@Override
	public ModeExonerationHisto collate(ModeExonerationHisto next) {
		return new ModeExonerationHisto(dateDebut, next.getDateFin(), modeExoneration);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public ModeExoneration getModeExoneration() {
		return modeExoneration;
	}
}
