package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AllegementFiscalCommune;

public class AllegementFiscalCommuneValidatorTest extends AllegementFiscalCantonCommuneValidatorTest<AllegementFiscalCommune> {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalCommuneValidator";
	}

	@Override
	protected AllegementFiscalCommune build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage) {
		return new AllegementFiscalCommune(dateDebut, dateFin, pourcentage, typeImpot, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI, null);
	}

	protected AllegementFiscalCommune build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage, AllegementFiscalCantonCommune.Type type, @Nullable MockCommune commune) {
		final AllegementFiscalCommune af = build(dateDebut, dateFin, typeImpot, pourcentage, type);
		if (commune != null) {
			af.setNoOfsCommune(commune.getNoOFS());
		}
		return af;
	}

	@Test
	public void testCommune() throws Exception {

		// valeur vide -> ok
		{
			final AllegementFiscalCommune af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.TEN);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur non vide vaudoise -> ok
		{
			final AllegementFiscalCommune af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.TEN, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI, MockCommune.Lausanne);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur non-vide non-vaudoise -> toujours refusée
		{
			final AllegementFiscalCommune af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.TEN, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI, MockCommune.Bern);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'allègement fiscal AllegementFiscalCommune BENEFICE (commune 351) (01.01.2000 - 31.12.2005) est sur une commune sise hors-canton (Bern (BE) - 351).", error);
		}
	}
}
