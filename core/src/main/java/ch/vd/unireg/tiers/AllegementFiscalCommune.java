package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("COMMUNE")
public class AllegementFiscalCommune extends AllegementFiscalCantonCommune {

	private Integer noOfsCommune;

	public AllegementFiscalCommune() {
	}

	public AllegementFiscalCommune(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable BigDecimal pourcentageAllegement, TypeImpot typeImpot, Type type, @Nullable Integer noOfsCommune) {
		super(dateDebut, dateFin, pourcentageAllegement, typeImpot, type);
		this.noOfsCommune = noOfsCommune;
	}

	public AllegementFiscalCommune(AllegementFiscalCommune src) {
		super(src);
		this.noOfsCommune = src.noOfsCommune;
	}

	@Transient
	@Override
	public TypeCollectivite getTypeCollectivite() {
		return TypeCollectivite.COMMUNE;
	}

	@Transient
	@Override
	public AllegementFiscalCommune duplicate() {
		return new AllegementFiscalCommune(this);
	}

	@Transient
	@Override
	protected String getBusinessName() {
		final String name = super.getBusinessName();
		if (noOfsCommune != null) {
			return String.format("%s (commune %d)", name, noOfsCommune);
		}
		else {
			return name;
		}
	}

	@Column(name = "NO_OFS_COMMUNE", nullable = true)
	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(Integer noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}
}
