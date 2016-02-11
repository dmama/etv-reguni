package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;

public class AllegementFiscalConfederationValidatorTest extends AllegementFiscalValidatorTest {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalConfederationValidator";
	}

	protected AllegementFiscalConfederation build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage) {
		return new AllegementFiscalConfederation(dateDebut, dateFin, pourcentage, typeImpot, AllegementFiscalConfederation.Type.EXONERATION_SPECIALE);
	}
}
