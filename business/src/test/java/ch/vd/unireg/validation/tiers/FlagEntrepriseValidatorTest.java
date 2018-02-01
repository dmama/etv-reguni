package ch.vd.uniregctb.validation.tiers;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class FlagEntrepriseValidatorTest extends AbstractValidatorTest<FlagEntreprise> {

	@Override
	protected String getValidatorBeanName() {
		return "flagEntrepriseValidator";
	}

	@Test
	public void testAnnule() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		final ValidationResults vrNonAnnule = validate(flag);
		Assert.assertTrue(vrNonAnnule.hasErrors());     // il manque au moins le type...

		flag.setAnnule(true);
		final ValidationResults vrAnnule = validate(flag);
		Assert.assertFalse(vrAnnule.hasErrors());
	}

	@Test
	public void testTypeManquant() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise(null, date(2000, 4, 12), null);       // sans type
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("Le type de flag entreprise est obligatoire.", error);
		}

		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}

	@Test
	public void testAnneeDebut() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("Le flag entreprise FlagEntreprise (? - ?) possède une date de début nulle", error);
		}

		flag.setDateDebut(date(2014, 5, 12));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}

	@Test
	public void testAnneeFin() throws Exception {
		final FlagEntreprise flag = new FlagEntreprise();
		flag.setType(TypeFlagEntreprise.UTILITE_PUBLIQUE);
		flag.setDateDebut(date(2000, 2, 4));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}

		flag.setDateFin(date(1289, 5, 21));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("Le flag entreprise FlagEntreprise (04.02.2000 - 21.05.1289) possède une date de début qui est après la date de fin: début = 04.02.2000, fin = 21.05.1289", error);
		}

		flag.setDateFin(date(1999, 4, 13));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());

			Assert.assertEquals(1, vr.getErrors().size());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("Le flag entreprise FlagEntreprise (04.02.2000 - 13.04.1999) possède une date de début qui est après la date de fin: début = 04.02.2000, fin = 13.04.1999", error);
		}

		flag.setDateFin(date(2000, 3, 6));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}

		flag.setDateFin(date(2006, 12, 4));
		{
			final ValidationResults vr = validate(flag);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
		}
	}
}