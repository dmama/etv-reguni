package ch.vd.unireg.validation.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdresseSuisseValidatorTest extends AbstractValidatorTest<AdresseSuisse> {

	@Override
	protected String getValidatorBeanName() {
		return "adresseSuisseValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
			adresse.setNumeroOrdrePoste(152);
			adresse.setAnnule(true);
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDebut() {

		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setNumeroRue(MockRue.Bussigny.RueDeLIndustrie.getNoRue());
		adresse.setNumeroOrdrePoste(MockRue.Bussigny.RueDeLIndustrie.getNoLocalite());

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

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateNpaCasePostaleSansNumero() throws Exception {

		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setDateDebut(date(2000, 1, 1));
		adresse.setNumeroRue(MockRue.Bussigny.RueDeLIndustrie.getNoRue());
		adresse.setNumeroOrdrePoste(MockRue.Bussigny.RueDeLIndustrie.getNoLocalite());
		adresse.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
		adresse.setNpaCasePostale(1040);

		// sans numéro de case postale
		{
			assertFalse(validate(adresse).hasErrors());
		}

		// avec un numéro de case postale -> même combat
		{
			adresse.setNumeroCasePostale(12);
			assertFalse(validate(adresse).hasErrors());
		}
	}
}