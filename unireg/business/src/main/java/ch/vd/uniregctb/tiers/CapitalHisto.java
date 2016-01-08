package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.Rerangeable;

public class CapitalHisto implements Sourced<Source>, CollatableDateRange, Duplicable<CapitalHisto>, Annulable, Rerangeable<CapitalHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaire montant;
	private final Source source;

	public CapitalHisto(CapitalEntreprise capital) {
		this(capital.getId(), capital.isAnnule(), capital.getDateDebut(), capital.getDateFin(), capital.getMontant().duplicate(), Source.FISCALE);
	}

	public CapitalHisto(Capital capital) {
		this(null, false, capital.getDateDebut(), capital.getDateFin(), new MontantMonetaire(capital.getCapitalLibere().longValue(), capital.getDevise()), Source.CIVILE);
	}

	private CapitalHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, MontantMonetaire montant, Source source) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.montant = montant;
		this.source = source;
	}

	@Override
	public CapitalHisto rerange(DateRange range) {
		return new CapitalHisto(id, annule, range.getDateDebut(), range.getDateFin(), montant, source);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next) && next instanceof CapitalHisto;
		if (collatable) {
			final CapitalHisto nextCapital = (CapitalHisto) next;
			collatable = nextCapital.montant.equals(montant)
					&& nextCapital.source == source
					&& nextCapital.annule == annule
					&& ((nextCapital.id == null && id == null) || (nextCapital.id != null && id != null && nextCapital.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public CapitalHisto collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new CapitalHisto(id, annule, dateDebut, next.getDateFin(), montant, source);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
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
		return new CapitalHisto(id, annule, dateDebut, dateFin, montant.duplicate(), source);
	}

	@Override
	public Source getSource() {
		return source;
	}

	/**
	 * @return Identifiant de la donnée dans la base fiscale
	 */
	public Long getId() {
		return id;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}
}
