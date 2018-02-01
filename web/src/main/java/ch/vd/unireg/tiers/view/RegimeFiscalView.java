package ch.vd.unireg.tiers.view;

import java.util.List;
import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.PlageExonerationFiscale;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.common.Annulable;

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

	public boolean isExonerantIBC() {
		return isExonerant(GenreImpotExoneration.IBC);
	}

	public boolean isExonerantICI() {
		return isExonerant(GenreImpotExoneration.ICI);
	}

	public boolean isExonerantIFONC() {
		return isExonerant(GenreImpotExoneration.IFONC);
	}

	private boolean isExonerant(GenreImpotExoneration genreImpot) {
		// pas de type, pas d'exonération
		if (type == null) {
			return false;
		}

		// pas d'exonération, pas d'exonération
		final List<PlageExonerationFiscale> exonerations = type.getExonerations(genreImpot);
		if (exonerations.isEmpty()) {
			return false;
		}

		// il faut que les exonérations intersectent la période du régime fiscal pour que ça fonctionne
		return exonerations.stream()
				.map(exo -> new DateRangeHelper.Range(RegDate.get(exo.getPeriodeDebut(), 1, 1),
				                                      Optional.ofNullable(exo.getPeriodeFin()).map(pf -> RegDate.get(pf, 12, 31)).orElse(null)))
				.anyMatch(exo -> DateRangeHelper.intersect(exo, this));
	}
}
