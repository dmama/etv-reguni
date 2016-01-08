package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.Siege;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.Rerangeable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * @author Raphaël Marmier, 2016-01-07, <raphael.marmier@vd.ch>
 */
public class SiegeHisto implements Sourced<Source>, CollatableDateRange, Duplicable<SiegeHisto>, Annulable, Rerangeable<SiegeHisto> {

	private final Long id;
	private final boolean annule;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final int noOfs;
	private final Source source;

	public SiegeHisto(Siege source) {
		this(null, false, source.getDateDebut(), source.getDateFin(), source.getTypeAutoriteFiscale(), source.getNoOfs(), Source.CIVILE);
	}

	public SiegeHisto(DomicileEtablissement source) {
		this(source.getId(), source.isAnnule(), source.getDateDebut(), source.getDateFin(), source.getTypeAutoriteFiscale(), source.getNumeroOfsAutoriteFiscale(), Source.FISCALE);
	}

	private SiegeHisto(Long id, boolean annule, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int noOfs, Source source) {
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

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public int getNoOfs() {
		return noOfs;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		boolean collatable = DateRangeHelper.isCollatable(this, next) && next instanceof SiegeHisto;
		if (collatable) {
			final SiegeHisto nextSiegeHisto = (SiegeHisto) next;
			collatable = nextSiegeHisto.typeAutoriteFiscale == typeAutoriteFiscale
					&& nextSiegeHisto.noOfs == noOfs
					&& nextSiegeHisto.source == source
					&& nextSiegeHisto.annule == annule
					&& ((nextSiegeHisto.id == null && id == null) || (nextSiegeHisto.id != null && id != null && nextSiegeHisto.id.equals(id)));
		}
		return collatable;
	}

	@Override
	public DateRange collate(DateRange next) {
		if (!isCollatable(next)) {
			throw new IllegalArgumentException("Les ranges ne sont pas collatables...");
		}
		return new SiegeHisto(id, annule, dateDebut, next.getDateFin(), typeAutoriteFiscale, noOfs, source);
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

	@Override
	public SiegeHisto duplicate() {
		return new SiegeHisto(id, annule, dateDebut, dateFin, typeAutoriteFiscale, noOfs, source);
	}

	@Override
	public SiegeHisto rerange(DateRange range) {
		return new SiegeHisto(id, annule, range.getDateDebut(), range.getDateFin(), typeAutoriteFiscale, noOfs, source);
	}

	@Override
	public Source getSource() {
		return source;
	}
}
