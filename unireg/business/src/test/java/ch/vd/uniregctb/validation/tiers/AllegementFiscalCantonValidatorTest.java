package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCanton;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;

public class AllegementFiscalCantonValidatorTest extends AllegementFiscalCantonCommuneValidatorTest<AllegementFiscalCanton> {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalCantonValidator";
	}

	@Override
	protected AllegementFiscalCanton build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage) {
		return new AllegementFiscalCanton(dateDebut, dateFin, pourcentage, typeImpot, AllegementFiscalCantonCommune.Type.ARTICLE_91_LI);
	}
}
