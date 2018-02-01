package ch.vd.unireg.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.Rerangeable;

public class CapitalHisto implements Sourced<Source>, CollatableDateRange<CapitalHisto>, Duplicable<CapitalHisto>, Annulable, Rerangeable<CapitalHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final MontantMonetaire montant;
	private final Source source;

	public CapitalHisto(CapitalFiscalEntreprise capital) {
		this(capital.getId(), capital.isAnnule(), capital.getDateDebut(), capital.getDateFin(), capital.getMontant().duplicate(), Source.FISCALE);
	}

	public CapitalHisto(Capital capital) {
		// [SIFISC-19942] si RCEnt ne fournit pas de devise, on supposera CHF...
		this(null, false, capital.getDateDebut(), capital.getDateFin(), new MontantMonetaire(capital.getCapitalLibere().longValue(), capital.getDevise() == null ? MontantMonetaire.CHF : capital.getDevise()), Source.CIVILE);
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
		return new CapitalHisto(id, annule, range.getDateDebut() != null ? range.getDateDebut() : dateDebut, range.getDateFin() != null ? range.getDateFin() : dateFin, montant, source);
	}

	@Override
	public boolean isCollatable(CapitalHisto next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next);
		if (collatable) {
			collatable = next.montant.equals(montant)
					&& next.source == source
					&& next.annule == annule
					&& ((next.id == null && id == null) || (next.id != null && id != null && next.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public CapitalHisto collate(CapitalHisto next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new CapitalHisto(id, annule, dateDebut, next.getDateFin(), montant, source);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && CollatableDateRange.super.isValidAt(date);
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
