package ch.vd.unireg.validation.tiers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DomicileEtablissementValidatorTest extends AbstractValidatorTest<DomicileEtablissement> {

	@Override
	protected String getValidatorBeanName() {
		return "domicileEtablissementValidator";
	}

	@Test
	public void testValidateDateDebut() {

		final DomicileEtablissement domicile = new DomicileEtablissement();
		domicile.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		domicile.setNumeroOfsAutoriteFiscale(MockCommune.Vevey.getNoOFS());

		// Date de début nulle
		{
			final ValidationResults results = validate(domicile);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le domicile d'établissement DomicileEtablissement (? - ?) possède une date de début nulle", errors.get(0));
		}

		// Date de début renseignée
		{
			domicile.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(validate(domicile).hasErrors());
		}
	}
}
