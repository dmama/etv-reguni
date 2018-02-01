package ch.vd.unireg.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.Rerangeable;

/**
 * @author Raphaël Marmier, 2016-01-07, <raphael.marmier@vd.ch>
 */
public class RaisonSocialeHisto implements Sourced<Source>, CollatableDateRange<RaisonSocialeHisto>, Duplicable<RaisonSocialeHisto>, Annulable, Rerangeable<RaisonSocialeHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String raisonSociale;
	private final Source source;

	public RaisonSocialeHisto(DateRanged<String> source) {
		this(null, false, source.getDateDebut(), source.getDateFin(), source.getPayload(), Source.CIVILE);
	}

	public RaisonSocialeHisto(RaisonSocialeFiscaleEntreprise source) {
		this(source.getId(), source.isAnnule(), source.getDateDebut(), source.getDateFin(), source.getRaisonSociale(), Source.FISCALE);
	}

	private RaisonSocialeHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, String raisonSociale, Source source) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.raisonSociale = raisonSociale;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isCollatable(RaisonSocialeHisto next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next);
		if (collatable) {
			collatable = next.raisonSociale.equals(raisonSociale)
					&& next.source == source
					&& next.annule == annule
					&& ((next.id == null && id == null) || (next.id != null && id != null && next.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public RaisonSocialeHisto collate(RaisonSocialeHisto next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new RaisonSocialeHisto(id, annule, dateDebut, next.getDateFin(), raisonSociale, source);
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

	@Override
	public RaisonSocialeHisto duplicate() {
		return new RaisonSocialeHisto(id, annule, dateDebut, dateFin, raisonSociale, source);
	}

	@Override
	public RaisonSocialeHisto rerange(DateRange range) {
		return new RaisonSocialeHisto(id, annule, range.getDateDebut() != null ? range.getDateDebut() : dateDebut, range.getDateFin() != null ? range.getDateFin() : dateFin, raisonSociale, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
