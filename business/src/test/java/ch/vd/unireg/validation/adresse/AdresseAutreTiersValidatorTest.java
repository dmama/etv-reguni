package ch.vd.unireg.validation.adresse;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseAutreTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdresseAutreTiersValidatorTest extends AbstractValidatorTest<AdresseAutreTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "adresseAutreTiersValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDateDebut() {

		final AdresseAutreTiers adresse = new AdresseAutreTiers();
		final PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNumero(2L);
		adresse.setTiers(tiers);
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setAutreTiersId(1L);
		adresse.setType(TypeAdresseTiers.COURRIER);

		// Date de début nulle
		{
			final ValidationResults results = validate(adresse);
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("L'adresse AdresseTiers{id=null, dateDebut=, dateFin=, usage=COURRIER, tiers=PersonnePhysique n°2} possède une date de début nulle", errors.get(0));
		}

		// Date de début renseignée
		{
			adresse.setDateDebut(RegDate.get(2000, 1, 1));
			assertFalse(validate(adresse).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateAutreTiersId() {

		final AdresseAutreTiers adresse = new AdresseAutreTiers();
		final PersonnePhysique tiers = new PersonnePhysique(false);
		tiers.setNumero(1L);
		adresse.setTiers(tiers);
		adresse.setDateDebut(RegDate.get(2000, 1, 1));
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setAutreTiersId(1L);
		adresse.setType(TypeAdresseTiers.COURRIER);

		// Tiers id et autre iters id identiques
		{
			final ValidationResults results = validate(adresse);
			assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le tiers cible doit être différent du tiers courant sur une adresse 'autre tiers' [AdresseTiers{id=null, dateDebut=01.01.2000, dateFin=, usage=COURRIER, tiers=PersonnePhysique n°1}]", errors.get(0));
		}

		// Tiers id et autre iters id différents
		{
			adresse.setAutreTiersId(2L);
			assertFalse(validate(adresse).hasErrors());
		}
	}
}