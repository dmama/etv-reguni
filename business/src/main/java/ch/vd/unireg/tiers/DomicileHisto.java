package ch.vd.unireg.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.Rerangeable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-01-07, <raphael.marmier@vd.ch>
 */
public class DomicileHisto implements Sourced<Source>, CollatableDateRange<DomicileHisto>, Duplicable<DomicileHisto>, Annulable, Rerangeable<DomicileHisto>, LocalizedDateRange {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final int noOfs;
	private final Source source;

	public DomicileHisto(Domicile source) {
		this(null, false, source.getDateDebut(), source.getDateFin(), source.getTypeAutoriteFiscale(), source.getNumeroOfsAutoriteFiscale(), Source.CIVILE);
	}

	public DomicileHisto(DomicileEtablissement source) {
		this(source.getId(), source.isAnnule(), source.getDateDebut(), source.getDateFin(), source.getTypeAutoriteFiscale(), source.getNumeroOfsAutoriteFiscale(), Source.FISCALE);
	}

	private DomicileHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int noOfs, Source source) {
		this.id = id;
		this.annule = annule;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	@Override
	public Integer getNumeroOfsAutoriteFiscale() {
		return noOfs;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isCollatable(DomicileHisto next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next);
		if (collatable) {
			collatable = next.typeAutoriteFiscale == typeAutoriteFiscale
					&& next.noOfs == noOfs
					&& next.source == source
					&& next.annule == annule
					&& ((next.id == null && id == null) || (next.id != null && id != null && next.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public DomicileHisto collate(DomicileHisto next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new DomicileHisto(id, annule, dateDebut, next.getDateFin(), typeAutoriteFiscale, noOfs, source);
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
	public DomicileHisto duplicate() {
		return new DomicileHisto(id, annule, dateDebut, dateFin, typeAutoriteFiscale, noOfs, source);
	}

	@Override
	public DomicileHisto rerange(DateRange range) {
		return new DomicileHisto(id, annule, range.getDateDebut() != null ? range.getDateDebut() : dateDebut, range.getDateFin() != null ? range.getDateFin() : dateFin, typeAutoriteFiscale, noOfs, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
