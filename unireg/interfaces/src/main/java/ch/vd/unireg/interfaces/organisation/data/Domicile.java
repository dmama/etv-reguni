package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class Domicile implements Serializable, CollatableDateRange, DateRangeLimitable<Domicile>, LocalizedDateRange {

	private static final long serialVersionUID = -1655521082019660148L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final int numeroOfsAutoriteFiscale;

	public Domicile(RegDate dateDebut, @Nullable RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
	}

	public Domicile(RegDate dateDebut, @Nullable RegDate dateFin, Commune commune) {
		this(dateDebut, dateFin, commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC, commune.getNoOFS());
	}

	public Domicile limitTo(RegDate dateDebut, RegDate dateFin) {
		return new Domicile(dateDebut == null ? this.dateDebut : dateDebut,
		                    dateFin == null ? this.dateFin : dateFin,
		                    this.typeAutoriteFiscale,
		                    this.numeroOfsAutoriteFiscale);
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
				&& ((Domicile) next).numeroOfsAutoriteFiscale == this.numeroOfsAutoriteFiscale;
	}

	@Override
	public Domicile collate(DateRange next) {
		Assert.isTrue(isCollatable(next));
		return new Domicile(this.dateDebut, next.getDateFin(), this.typeAutoriteFiscale, this.numeroOfsAutoriteFiscale);
	}

	@Override
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	@Override
	public Integer getNumeroOfsAutoriteFiscale() {
		return numeroOfsAutoriteFiscale;
	}
}
