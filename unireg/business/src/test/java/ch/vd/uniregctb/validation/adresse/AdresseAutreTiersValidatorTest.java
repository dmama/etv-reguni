package ch.vd.uniregctb.validation.adresse;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AdresseAutreTiersValidatorTest extends AbstractValidatorTest<AdresseAutreTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "adresseAutreTiersValidator";
	}

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseAutreTiers adresse = new AdresseAutreTiers();

		// Adresse invalide (type nul) mais annulée => pas d'erreur
		{
			adresse.setType(null);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setType(TypeAdresseTiers.COURRIER);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	public void testValidateDateDebut() {

		final AdresseAutreTiers adresse = new AdresseAutreTiers();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setAutreTiersId(1L);
		adresse.setType(TypeAdresseTiers.COURRIER);

		// Date de début nulle
		{
			final ValidationResults results = validate(adresse);
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'adresse AdresseTiers{id=null, dateDebut=, dateFin=, usage=COURRIER, tiers=null} possède une date de début nulle", errors.get(0));
		}

		// Date de début renseignée
		{
			adresse.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(validate(adresse).hasErrors());
		}
	}
}