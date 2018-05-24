package ch.vd.unireg.xml.party.v5;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.taxresidence.v4.OperatingPeriod;

public abstract class OperatingPeriodBuilder {
	private OperatingPeriodBuilder() {
	}

	@NotNull
	public static OperatingPeriod newPeriod(@NotNull DateRange periodeExploitation) {
		final OperatingPeriod op = new OperatingPeriod();
		op.setDateFrom(DataHelper.coreToXMLv2(periodeExploitation.getDateDebut()));
		op.setDateTo(DataHelper.coreToXMLv2(periodeExploitation.getDateFin()));
		return op;
	}
}
