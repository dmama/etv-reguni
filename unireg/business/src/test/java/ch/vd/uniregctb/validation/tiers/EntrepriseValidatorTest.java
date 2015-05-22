package ch.vd.uniregctb.validation.tiers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.type.TypeRegimeFiscal;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class EntrepriseValidatorTest extends AbstractValidatorTest<Entreprise> {

	@Override
	protected String getValidatorBeanName() {
		return "entrepriseValidator";
	}

	@Test
	public void testChevauchementRegimesFiscaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), date(2005, 12, 31), RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), null, RegimeFiscal.Portee.CH, TypeRegimeFiscal.ORDINAIRE));

		// ici, tout va bien, les différentes portées ne se marchent pas dessus
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui ne chevauche pas -> pas de souci
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2007, 1, 1), date(2009, 12, 1), RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui chevauche -> rien ne va plus
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2005, 1, 1), date(2007, 12, 31), RegimeFiscal.Portee.VD, TypeRegimeFiscal.ORDINAIRE));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(2, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs régimes fiscaux VD", errors.get(0));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs régimes fiscaux VD", errors.get(1));
		}
	}
}
