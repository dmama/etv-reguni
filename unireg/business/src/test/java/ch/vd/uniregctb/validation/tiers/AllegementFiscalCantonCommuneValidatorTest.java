package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;

public abstract class AllegementFiscalCantonCommuneValidatorTest<T extends AllegementFiscalCantonCommune> extends AllegementFiscalValidatorTest<T> {

	@Override
	protected abstract T build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage);

	protected T build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage, AllegementFiscalCantonCommune.Type type) {
		final T instance = build(dateDebut, dateFin, typeImpot, pourcentage);
		instance.setType(type);
		return instance;
	}

	@Test
	public void testType() throws Exception {
		// valeur absente -> ko
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals(String.format("L'allègement fiscal %s BENEFICE (01.01.2000 - 31.12.2005) n'a pas de type d'allègement fixé.", af.getClass().getSimpleName()), error);
		}

		// valeur présente -> ok
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, null, AllegementFiscalCantonCommune.Type.ARTICLE_91_LI);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}
}