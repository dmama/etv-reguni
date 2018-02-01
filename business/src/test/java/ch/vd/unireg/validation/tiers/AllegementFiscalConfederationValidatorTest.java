package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalConfederation;

public class AllegementFiscalConfederationValidatorTest extends AllegementFiscalValidatorTest {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalConfederationValidator";
	}

	protected AllegementFiscalConfederation build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage) {
		return new AllegementFiscalConfederation(dateDebut, dateFin, pourcentage, typeImpot, AllegementFiscalConfederation.Type.EXONERATION_SPECIALE);
	}
}
