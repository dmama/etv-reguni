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
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.uniregctb.common.MontantMonetaireView;
import ch.vd.uniregctb.tiers.MontantMonetaire;

public class CapitalView implements CollatableDateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaireView capitalLibere;

	public CapitalView(Capital capital) {
		this(capital.getDateDebut(),
		     capital.getDateFin(),
		     buildMontantMonetaire(capital.getCapitalLibere(), capital.getDevise()));
	}

	public CapitalView(RegDate dateDebut, RegDate dateFin, MontantMonetaire capitalLibere) {
		this(dateDebut, dateFin, buildMontantMonetaire(capitalLibere));
	}

	public CapitalView(RegDate dateDebut, RegDate dateFin, MontantMonetaireView capitalLibere) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.capitalLibere = capitalLibere;
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

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next)
				&& next instanceof CapitalView
				&& isSameValue(capitalLibere, ((CapitalView) next).capitalLibere);
	}

	private static boolean isSameValue(MontantMonetaireView one, MontantMonetaireView two) {
		return one == two || (one != null && two != null && one.equals(two));
	}

	@Override
	public CapitalView collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Ranges are not collatable!");
		}
		return new CapitalView(dateDebut, next.getDateFin(), capitalLibere);
	}
}
