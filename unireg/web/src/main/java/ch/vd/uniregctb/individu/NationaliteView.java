package ch.vd.uniregctb.individu;

import java.io.Serializable;
import java.util.Comparator;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;

public class NationaliteView implements DateRange, Serializable {

	public static final Comparator<NationaliteView> COMPARATOR = new DateRangeComparator<>();

	private static final long serialVersionUID = -9166765391923515888L;

	private final DateRange validite;
	private final String pays;

	public NationaliteView(Nationalite src) {
		this.validite = new DateRangeHelper.Range(src.getDateDebut(), src.getDateFin());
		this.pays = src.getPays().getNomCourt();
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return validite.isValidAt(date);
	}

	@Override
	public RegDate getDateDebut() {
		return validite.getDateDebut();
	}

	@Override
	public RegDate getDateFin() {
		return validite.getDateFin();
	}

	public String getPays() {
		return pays;
	}
}
