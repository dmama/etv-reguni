package ch.vd.uniregctb.tiers.view;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.AllegementFiscal;

public class AllegementFiscalView implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final AllegementFiscal.TypeImpot typeImpot;
	private final AllegementFiscal.TypeCollectivite typeCollectivite;
	private final Integer noOfsCommune;
	private final BigDecimal pourcentage;

	public AllegementFiscalView(RegDate dateDebut, RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, AllegementFiscal.TypeCollectivite typeCollectivite, @Nullable Integer noOfsCommune, BigDecimal pourcentage) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeImpot = typeImpot;
		this.typeCollectivite = typeCollectivite;
		this.noOfsCommune = noOfsCommune;
		this.pourcentage = pourcentage;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public AllegementFiscal.TypeImpot getTypeImpot() {
		return typeImpot;
	}

	public AllegementFiscal.TypeCollectivite getTypeCollectivite() {
		return typeCollectivite;
	}

	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public BigDecimal getPourcentage() {
		return pourcentage;
	}
}
