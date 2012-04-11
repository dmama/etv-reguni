package ch.vd.uniregctb.validation.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AdresseEtrangereValidatorTest extends AbstractValidatorTest<AdresseEtrangere> {

	@Override
	protected String getValidatorBeanName() {
		return "adresseEtrangereValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAdresseAnnulee() {

		final AdresseEtrangere adresse = new AdresseEtrangere();

		// Adresse invalide (numéro ofs pays nul) mais annulée => pas d'erreur
		{
			adresse.setNumeroOfsPays(null);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setNumeroOfsPays(4321);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDebut() {

		final AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setNumeroOfsPays(MockPays.Allemagne.getNoOFS());

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