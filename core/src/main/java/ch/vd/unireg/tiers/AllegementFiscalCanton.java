package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

@Entity
@DiscriminatorValue("CANTON")
public class AllegementFiscalCanton extends AllegementFiscalCantonCommune {

	public AllegementFiscalCanton() {
	}

	public AllegementFiscalCanton(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable BigDecimal pourcentageAllegement, TypeImpot typeImpot, Type type) {
		super(dateDebut, dateFin, pourcentageAllegement, typeImpot, type);
	}

	public AllegementFiscalCanton(AllegementFiscalCanton src) {
		super(src);
	}

	@Transient
	@Override
	public TypeCollectivite getTypeCollectivite() {
		return TypeCollectivite.CANTON;
	}

	@Transient
	@Override
	public AllegementFiscalCanton duplicate() {
		return new AllegementFiscalCanton(this);
	}
}
