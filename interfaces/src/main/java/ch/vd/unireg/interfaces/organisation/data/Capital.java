package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ComparisonHelper;

public class Capital implements Serializable, DateRange, DateRangeLimitable<Capital> {

	private static final long serialVersionUID = 5621443460846552394L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeDeCapital typeDeCapital;
    private final String devise;
    private final BigDecimal capitalLibere;
	private final String repartition;

	public Capital(RegDate dateDebut, @Nullable RegDate dateFin, TypeDeCapital typeDeCapital, String devise, BigDecimal capitalLibere, String repartition) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeDeCapital = typeDeCapital;
		this.devise = devise;
		this.capitalLibere = capitalLibere;
		this.repartition = repartition;
	}

	public Capital limitTo(@Nullable RegDate dateDebut, @Nullable RegDate dateFin) {
		return new Capital(dateDebut == null ? this.dateDebut : dateDebut,
		                   dateFin == null ? this.dateFin : dateFin,
		                   this.typeDeCapital,
		                   this.devise,
		                   this.capitalLibere,
		                   this.repartition);
	}

	/**
	 * Teste l'identité de valeur avec un autre capital. Les dates de début et de fin sont ignorées. C'est un equals() sans la notion de temps.
	 * @param capital Le capital à comparer
	 * @return
	 */
	public boolean identicalTo(@Nullable Capital capital) {
		if (capital == this) {
			return true;
		}
		if (capital == null) {
			return false;
		}
		return ComparisonHelper.areEqual(capitalLibere, capital.capitalLibere)
				&& ComparisonHelper.areEqual(devise, capital.devise)
				&& ComparisonHelper.areEqual(typeDeCapital, capital.typeDeCapital)
				&& ComparisonHelper.areEqual(repartition, capital.repartition);
	}

	public BigDecimal getCapitalLibere() {
		return capitalLibere;
	}

	public String getDevise() {
		return devise;
	}

	public TypeDeCapital getTypeDeCapital() {
		return typeDeCapital;
	}

	public String getRepartition() {
		return repartition;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
