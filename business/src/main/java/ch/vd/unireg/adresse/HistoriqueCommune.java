package ch.vd.unireg.adresse;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;

/**
 * Décrit la présence dans une commune pendant une période
 */
public class HistoriqueCommune implements CollatableDateRange<HistoriqueCommune> {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Commune commune;

	public HistoriqueCommune(RegDate dateDebut, RegDate dateFin, Commune commune) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.commune = commune;
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
	public boolean isCollatable(HistoriqueCommune next) {
		return DateRangeHelper.isCollatable(this, next) && sameCommune(commune, next.commune);
	}

	@SuppressWarnings({"SimplifiableIfStatement"})
	private static boolean sameCommune(Commune commune1, Commune commune2) {
		if (commune1 == commune2) {
			return true;
		}
		else if (commune1 == null || commune2 == null) {
			return false;
		}
		else {
			return commune1.getNoOFS() == commune2.getNoOFS();
		}
	}

	@Override
	public HistoriqueCommune collate(HistoriqueCommune next) {
		return new HistoriqueCommune(dateDebut, next.getDateFin(), commune);
	}

	public Commune getCommune() {
		return commune;
	}
}
