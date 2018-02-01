package ch.vd.uniregctb.validation.tiers;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class RegimeFiscalValidatorTest extends AbstractValidatorTest<RegimeFiscal> {

	@Override
	protected String getValidatorBeanName() {
		return "regimeFiscalValidator";
	}

	@Test
	public void testPorteeAbsente() {
		final RegimeFiscal rf = new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_APM.getCode());
		assertValidation(Collections.emptyList(), Collections.emptyList(), validate(rf));

		rf.setPortee(null);
		assertValidation(Collections.singletonList("Le régime fiscal (01.01.2009 - ?) n'a pas de portée (VD/CH)."), Collections.emptyList(), validate(rf));
	}

	@Test
	public void testCode() {
		final RegimeFiscal rf = new RegimeFiscal(date(2010, 6, 2), null, RegimeFiscal.Portee.CH, MockTypeRegimeFiscal.PARTICIPATIONS.getCode());
		assertValidation(Collections.emptyList(), Collections.emptyList(), validate(rf));

		rf.setCode(null);
		assertValidation(Collections.singletonList("Le régime fiscal de portée CH (02.06.2010 - ?) doit être associé à un code."), Collections.emptyList(), validate(rf));

		rf.setCode("TRALALA");      // si ce code finit par exister dans nos Mock, et bien il faudra le changer ici...
		assertValidation(Collections.singletonList("Régime fiscal de portée CH (02.06.2010 - ?) : Aucun type de régime fiscal ne correspond au code fourni 'TRALALA'. Soit le code est erroné, soit il manque des données dans FiDoR."), Collections.emptyList(), validate(rf));
	}

	@Test
	public void testDepassementPertinentValidite() throws Exception {
		Assert.assertEquals((Integer) 2016, MockTypeRegimeFiscal.COMMUNAUTE_PERSONNES_ETRANGERES_PM.getPremierePeriodeFiscaleValidite());
		final RegimeFiscal rf = new RegimeFiscal(date(2010, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.COMMUNAUTE_PERSONNES_ETRANGERES_PM.getCode());
		final ValidationResults vr = validate(rf);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.errorsCount());
		Assert.assertEquals(1, vr.warningsCount());
		{
			final String warning  = vr.getWarnings().get(0);
			Assert.assertEquals("Le régime fiscal de portée VD (01.01.2010 - ?) déborde de la plage de validité du type associé '13 - Communauté de pers. étrangères - PM (Art. 84 LI)' (01.01.2016 - ?).", warning);
		}
	}

	@Test
	public void testDepassementNonPertinentValidite() throws Exception {
		Assert.assertEquals((Integer) 1994, MockTypeRegimeFiscal.ORDINAIRE_PM.getPremierePeriodeFiscaleValidite());
		final RegimeFiscal rf = new RegimeFiscal(date(1993, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode());
		final ValidationResults vr = validate(rf);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());
	}
}
