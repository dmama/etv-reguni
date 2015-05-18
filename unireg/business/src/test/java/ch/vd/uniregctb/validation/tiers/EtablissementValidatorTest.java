package ch.vd.uniregctb.validation.tiers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;

public class EtablissementValidatorTest extends AbstractValidatorTest<Etablissement> {

	@Override
	protected String getValidatorBeanName() {
		return "etablissementValidator";
	}

	@Test
	public void testChevauchementDomiciles() throws Exception {

		final Etablissement etb = new Etablissement();
		etb.addDomicile(new DomicileEtablissement(date(2000, 1, 1), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), etb));

		// ici, pas d'erreur
		{
			final ValidationResults results = validate(etb);
			Assert.assertFalse(results.hasErrors());
		}

		// rajoutons un autre établissement qui chevauche, et rien ne va plus
		etb.addDomicile(new DomicileEtablissement(date(2005, 1, 1), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Nyon.getNoOFS(), etb));
		{
			final ValidationResults results = validate(etb);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le domicile qui commence le 01.01.2005 chevauche le précédent", errors.get(0));
		}
	}
}
