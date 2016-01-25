package ch.vd.uniregctb.tiers.view;

import java.math.BigDecimal;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.AllegementFiscal;

public class AllegementFiscalView implements DateRange, Annulable {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final AllegementFiscal.TypeImpot typeImpot;
	private final AllegementFiscal.TypeCollectivite typeCollectivite;
	private final Integer noOfsCommune;
	private final BigDecimal pourcentage;
	private final boolean annule;

	public AllegementFiscalView(AllegementFiscal af) {
		this.id = af.getId();
		this.dateDebut = af.getDateDebut();
		this.dateFin = af.getDateFin();
		this.typeImpot = af.getTypeImpot();
		this.typeCollectivite = af.getTypeCollectivite();
		this.noOfsCommune = af.getNoOfsCommune();
		this.pourcentage = af.getPourcentageAllegement();
		this.annule = af.isAnnule();
	}

	public Long getId() {
		return id;
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

	@Override
	public boolean isAnnule() {
		return annule;
	}
}
