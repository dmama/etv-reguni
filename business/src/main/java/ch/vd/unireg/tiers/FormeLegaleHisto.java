package ch.vd.unireg.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.Rerangeable;

/**
 * @author Raphaël Marmier, 2016-01-07, <raphael.marmier@vd.ch>
 */
public class FormeLegaleHisto implements Sourced<Source>, CollatableDateRange<FormeLegaleHisto>, Duplicable<FormeLegaleHisto>, Annulable, Rerangeable<FormeLegaleHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final FormeLegale formeLegale;
	private final Source source;

	public FormeLegaleHisto(DateRanged<FormeLegale> source) {
		this(null, false, source.getDateDebut(), source.getDateFin(), source.getPayload(), Source.CIVILE);
	}

	public FormeLegaleHisto(FormeJuridiqueFiscaleEntreprise source) {
		this(source.getId(), source.isAnnule(), source.getDateDebut(), source.getDateFin(), FormeLegale.fromCode(source.getFormeJuridique().getCodeECH()), Source.FISCALE);
	}

	private FormeLegaleHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, FormeLegale formeLegale, Source source) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.formeLegale = formeLegale;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public FormeLegale getFormeLegale() {
		return formeLegale;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isCollatable(FormeLegaleHisto next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next);
		if (collatable) {
			collatable = next.formeLegale.equals(formeLegale)
					&& next.source == source
					&& next.annule == annule
					&& ((next.id == null && id == null) || (next.id != null && id != null && next.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public FormeLegaleHisto collate(FormeLegaleHisto next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new FormeLegaleHisto(id, annule, dateDebut, next.getDateFin(), formeLegale, source);
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
	public FormeLegaleHisto duplicate() {
		return new FormeLegaleHisto(id, annule, dateDebut, dateFin, formeLegale, source);
	}

	@Override
	public FormeLegaleHisto rerange(DateRange range) {
		return new FormeLegaleHisto(id, annule, range.getDateDebut() != null ? range.getDateDebut() : dateDebut, range.getDateFin() != null ? range.getDateFin() : dateFin, formeLegale, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
