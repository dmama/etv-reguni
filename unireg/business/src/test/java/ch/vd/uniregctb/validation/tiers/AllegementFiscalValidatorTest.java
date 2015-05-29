package ch.vd.uniregctb.validation.tiers;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class AllegementFiscalValidatorTest extends AbstractValidatorTest<AllegementFiscal> {

	@Override
	protected String getValidatorBeanName() {
		return "allegementFiscalValidator";
	}

	@Test
	public void testPourcentage() throws Exception {
		// valeur absente
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), null, null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'allègement fiscal AllegementFiscal universel (01.01.2000 - 31.12.2005) n'a pas de pourcentage d'allègement fixé.", error);
		}

		// valeur hors des clous -> -0.01
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(-1, 2), null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'allègement fiscal AllegementFiscal universel (01.01.2000 - 31.12.2005) a un pourcentage d'allègement hors des limites admises (0-100) : -0.01%.", error);
		}

		// valeur hors des clous -> 100.01
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(10001, 2), null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'allègement fiscal AllegementFiscal universel (01.01.2000 - 31.12.2005) a un pourcentage d'allègement hors des limites admises (0-100) : 100.01%.", error);
		}

		// valeur autorisée limite : 0
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.ZERO, null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur autorisée limite : 100
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(100L), null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur autorisée au milieu de la plage de validité
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(33333, 3), null, null, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	public void testCommune() throws Exception {

		// valeur vide quel que soit le type de collectivité -> ok
		for (AllegementFiscal.TypeCollectivite typeCollectivite : AllegementFiscal.TypeCollectivite.values()) {
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.TEN, null, typeCollectivite, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(typeCollectivite.name(), 0, vr.errorsCount());
			Assert.assertEquals(typeCollectivite.name(), 0, vr.warningsCount());
		}

		// valeur non vide vaudoise -> accepté seulement pour le type de collectivité COMMUNE
		for (AllegementFiscal.TypeCollectivite typeCollectivite : AllegementFiscal.TypeCollectivite.values()) {
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.TEN, null, typeCollectivite, MockCommune.Lausanne.getNoOFS());
			final ValidationResults vr = validate(af);
			if (typeCollectivite == AllegementFiscal.TypeCollectivite.COMMUNE) {
				Assert.assertEquals(typeCollectivite.name(), 0, vr.errorsCount());
				Assert.assertEquals(typeCollectivite.name(), 0, vr.warningsCount());
			}
			else {
				Assert.assertEquals(typeCollectivite.name(), 1, vr.errorsCount());
				Assert.assertEquals(typeCollectivite.name(), 0, vr.warningsCount());

				final String error = vr.getErrors().get(0);
				Assert.assertEquals("L'allègement fiscal AllegementFiscal " + typeCollectivite + " (commune 5586) (01.01.2000 - 31.12.2005) indique une commune alors que la collectivité associée n'est pas de type communal.", error);
			}
		}

		// valeur non-vide non-vaudoise -> toujours refusée
		{
			final AllegementFiscal af = new AllegementFiscal(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.TEN, null, AllegementFiscal.TypeCollectivite.COMMUNE, MockCommune.Bern.getNoOFS());
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("L'allègement fiscal AllegementFiscal COMMUNE (commune 351) (01.01.2000 - 31.12.2005) est sur une commune sise hors-canton (Bern (BE) - 351).", error);
		}
	}
}
