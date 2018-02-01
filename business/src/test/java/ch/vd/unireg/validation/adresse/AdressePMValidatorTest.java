package ch.vd.unireg.validation.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdressePM;
import ch.vd.unireg.type.TypeAdressePM;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdressePMValidatorTest extends AbstractValidatorTest<AdressePM> {

	@Override
	protected String getValidatorBeanName() {
		return "adressePMValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdresseAnnulee() {

		final AdressePM adresse = new AdressePM();

		// Adresse invalide (type nul) mais annulée => pas d'erreur
		{
			adresse.setType(null);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setType(TypeAdressePM.COURRIER);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDebut() {

		final AdressePM adresse = new AdressePM();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setType(TypeAdressePM.COURRIER);

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