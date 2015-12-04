package ch.vd.uniregctb.entreprise;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.MontantMonetaireView;
import ch.vd.uniregctb.tiers.CapitalHisto;
import ch.vd.uniregctb.tiers.MontantMonetaire;

public class CapitalView implements CollatableDateRange {

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaireView capitalLibere;
	private final CapitalHisto.Source source;

	public CapitalView(CapitalHisto capital) {
		this(capital.getId(), capital.getDateDebut(), capital.getDateFin(), capital.getMontant(), capital.getSource());
	}

	public CapitalView(Long id, RegDate dateDebut, RegDate dateFin, MontantMonetaire capitalLibere, CapitalHisto.Source source) {
		this(id, dateDebut, dateFin, buildMontantMonetaire(capitalLibere), source);
	}

	public CapitalView(Long id, RegDate dateDebut, RegDate dateFin, MontantMonetaireView capitalLibere, CapitalHisto.Source source) {
		this.id = id;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.capitalLibere = capitalLibere;
		this.source = source;
	}

	@Nullable
	private static MontantMonetaireView buildMontantMonetaire(BigDecimal montant, String monnaie) {
		if (montant == null || StringUtils.isBlank(monnaie)) {
			return null;
		}
		return new MontantMonetaireView(montant.longValue(), monnaie);
	}
	
	@Nullable
	private static MontantMonetaireView buildMontantMonetaire(MontantMonetaire mm) {
		if (mm == null) {
			return null;
		}
		return new MontantMonetaireView(mm);
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

	public MontantMonetaireView getCapitalLibere() {
		return capitalLibere;
	}

	public CapitalHisto.Source getSource() {
		return source;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next)
				&& next instanceof CapitalView
				&& isSameValue(id, ((CapitalView) next).id)
				&& isSameValue(capitalLibere, ((CapitalView) next).capitalLibere)
				&& source == ((CapitalView) next).source;
	}

	private static <T> boolean isSameValue(T one, T two) {
		return one == two || (one != null && two != null && one.equals(two));
	}

	@Override
	public CapitalView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges are not collatable!");
		}
		return new CapitalView(id, dateDebut, next.getDateFin(), capitalLibere, source);
	}
}
