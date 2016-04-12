package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class Domicile implements Serializable, CollatableDateRange, DateRangeLimitable<Domicile> {

	private static final long serialVersionUID = -3128523884534860892L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final int noOfs;

	public Domicile(RegDate dateDebut, @Nullable RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int noOfs) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
	}

	public Domicile(RegDate dateDebut, @Nullable RegDate dateFin, Commune commune) {
		this(dateDebut, dateFin, commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC, commune.getNoOFS());
	}

	public Domicile limitTo(RegDate dateDebut, RegDate dateFin) {
		return new Domicile(dateDebut == null ? this.dateDebut : dateDebut,
		                    dateFin == null ? this.dateFin : dateFin,
		                    this.typeAutoriteFiscale,
		                    this.noOfs);
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

	@Override
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next)
				&& next instanceof Domicile
				&& ((Domicile) next).typeAutoriteFiscale == this.typeAutoriteFiscale
				&& ((Domicile) next).noOfs == this.noOfs;
	}

	@Override
	public Domicile collate(DateRange next) {
		Assert.isTrue(isCollatable(next));
		return new Domicile(this.dateDebut, next.getDateFin(), this.typeAutoriteFiscale, this.noOfs);
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public int getNoOfs() {
		return noOfs;
	}
}
