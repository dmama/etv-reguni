package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.validation.AbstractValidatorTest;

public abstract class AllegementFiscalValidatorTest<T extends AllegementFiscal> extends AbstractValidatorTest<T> {

	/**
	 * Utilisé pour les tests génériques
	 * @param dateDebut la date de début
	 * @param dateFin la date de fin
	 * @param typeImpot le type d'impôt
	 * @param pourcentage le pourcentage d'allègement (si null, 'flag montant')
	 * @return une instance d'allègement fiscal (les autres éventuels attributs ne doivent pas causer d'erreur de validation)
	 */
	protected abstract T build(RegDate dateDebut, @Nullable RegDate dateFin, AllegementFiscal.TypeImpot typeImpot, @Nullable BigDecimal pourcentage);

	@Test
	public void testPourcentage() throws Exception {
		// valeur absente -> ok (c'est le flag 'montant')
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, null);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur hors des clous -> -0.01
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.valueOf(-1, 2));
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals(String.format("L'allègement fiscal %s BENEFICE (01.01.2000 - 31.12.2005) a un pourcentage d'allègement hors des limites admises (0-100) : -0.01%%.", af.getClass().getSimpleName()), error);
		}

		// valeur hors des clous -> 100.01
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.valueOf(10001, 2));
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals(String.format("L'allègement fiscal %s BENEFICE (01.01.2000 - 31.12.2005) a un pourcentage d'allègement hors des limites admises (0-100) : 100.01%%.", af.getClass().getSimpleName()), error);
		}

		// valeur autorisée limite : 0
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.ZERO);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur autorisée limite : 100
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.valueOf(100L));
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		// valeur autorisée au milieu de la plage de validité
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.valueOf(33333, 3));
			final ValidationResults vr = validate(af);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	public void testTypeImpot() throws Exception {
		// null -> refusé !
		{
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), null, BigDecimal.TEN);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals(String.format("L'allègement fiscal %s (01.01.2000 - 31.12.2005) n'a pas de type d'impôt assigné.", af.getClass().getSimpleName()), error);
		}

		// sinon, ok
		for (AllegementFiscal.TypeImpot typeImpot : AllegementFiscal.TypeImpot.values()) {
			final T af = build(date(2000, 1, 1), date(2005, 12, 31), typeImpot, BigDecimal.TEN);
			final ValidationResults vr = validate(af);
			Assert.assertEquals(typeImpot.name(), 0, vr.errorsCount());
			Assert.assertEquals(typeImpot.name(), 0, vr.warningsCount());
		}
	}
}
