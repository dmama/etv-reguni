package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCanton;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;

public class AllegementFiscalCantonValidatorTest extends AllegementFiscalCantonCommuneValidatorTest<AllegementFiscalCanton> {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalCantonValidator";
	}

	@Override
	protected AllegementFiscalCanton build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage) {
		return new AllegementFiscalCanton(dateDebut, dateFin, pourcentage, typeImpot, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI);
	}
}
