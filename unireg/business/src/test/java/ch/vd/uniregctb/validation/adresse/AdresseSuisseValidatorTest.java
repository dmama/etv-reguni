package ch.vd.uniregctb.validation.adresse;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AdresseSuisseValidatorTest extends AbstractValidatorTest<AdresseSuisse> {

	@Override
	protected String getValidatorBeanName() {
		return "adresseSuisseValidator";
	}

	@Test
	public void testValidateAdresseAnnulee() {

		final AdresseSuisse adresse = new AdresseSuisse();

		// Adresse invalide (numéro rue et ordre poste nuls) mais annulée => pas d'erreur
		{
			adresse.setNumeroRue(null);
			adresse.setNumeroOrdrePoste(null);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			adresse.setNumeroRue(1234);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	public void testValidateDateDebut() {

		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setNumeroRue(MockRue.Bussigny.RueDeLIndustrie.getNoRue());

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