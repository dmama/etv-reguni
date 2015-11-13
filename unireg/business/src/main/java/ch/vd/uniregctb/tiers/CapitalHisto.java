package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;

public class CapitalHisto implements CollatableDateRange, Duplicable<CapitalHisto> {

	/**
	 * Source de la valeur du capital
	 */
	public enum Source {

		/**
		 * Registre civil = registre cantonal des entreprises
		 */
		CIVILE,

		/**
		 * Registre fiscal = unireg
		 */
		FISCALE
	}

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaire montant;
	private final Source source;

	public CapitalHisto(RegDate dateDebut, RegDate dateFin, @NotNull MontantMonetaire montant, Source source) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.montant = montant;
		this.source = source;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next)
				&& next instanceof CapitalHisto
				&& ((CapitalHisto) next).montant.equals(montant)
				&& ((CapitalHisto) next).source == source;
	}

	@Override
	public CapitalHisto collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new CapitalHisto(dateDebut, next.getDateFin(), montant, source);
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

	public MontantMonetaire getMontant() {
		return montant;
	}

	@Override
	public CapitalHisto duplicate() {
		return new CapitalHisto(dateDebut, dateFin, montant.duplicate(), source);
	}

	public Source getSource() {
		return source;
	}
}
