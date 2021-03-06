package ch.vd.unireg.validation.rapport;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class HeritageValidatorTest extends AbstractValidatorTest<Heritage> {

	@Override
	protected String getValidatorBeanName() {
		return "heritageValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHeritageComplet() throws Exception {
		final RegDate dateDeces = date(2016, 7, 3);
		final PersonnePhysique defunt = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
		defunt.setDateDeces(dateDeces);
		final PersonnePhysique heritier = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
		final Heritage heritage = addHeritage(heritier, defunt, dateDeces.getOneDayAfter(), null, null);
		final ValidationResults vr = validate(heritage);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.getErrors().size());
		Assert.assertEquals(0, vr.getWarnings().size());
	}

	@Test
	public void testMauvaiseClasseDefunt() throws Exception {

		final class Ids {
			long idHeritage;
			long idHeritier;
			long idDefunt;
		}

		// mise en place bancale
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique defunt = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(defunt, null, date(2000, 1, 1), null);
			final MenageCommun menageCommun = couple.getMenage();
			final PersonnePhysique heritier = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
			final Heritage heritage = addHeritage(heritier, defunt, date(2016, 5, 3), null, null);

			// bricolage pour que le défunt du rapport entre tiers soit un ménage
			heritage.setObjet(menageCommun);
			defunt.getRapportsObjet().remove(heritage);
			menageCommun.addRapportObjet(heritage);

			final Ids res = new Ids();
			res.idHeritage = heritage.getId();
			res.idHeritier = heritier.getNumero();
			res.idDefunt = menageCommun.getNumero();
			return res;
		});

		// validation manuelle
		doInNewTransactionAndSession(status -> {
			final Heritage heritage = hibernateTemplate.get(Heritage.class, ids.idHeritage);
			Assert.assertNotNull(heritage);

			final ValidationResults vr = validate(heritage);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.getErrors().size());
			Assert.assertEquals(0, vr.getWarnings().size());

			{
				final String erreur = vr.getErrors().get(0);
				Assert.assertEquals(String.format("Le tiers défunt %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(ids.idDefunt)), erreur);
			}
			return null;
		});
	}

	@Test
	public void testMauvaiseClasseHeritier() throws Exception {

		final class Ids {
			long idHeritage;
			long idHeritier;
			long idDefunt;
		}

		// mise en place bancale
		final Ids ids = doInNewTransactionAndSessionWithoutValidation(status -> {
			final RegDate dateDeces = date(2016, 5, 2);
			final PersonnePhysique defunt = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
			defunt.setDateDeces(dateDeces);
			final PersonnePhysique heritier = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(heritier, null, date(2000, 1, 1), null);
			final MenageCommun menageCommun = couple.getMenage();
			final Heritage heritage = addHeritage(heritier, defunt, dateDeces.getOneDayAfter(), null, null);

			// bricolage pour que l'héritier du rapport entre tiers soit un ménage
			heritage.setSujet(menageCommun);
			heritier.getRapportsSujet().remove(heritage);
			menageCommun.addRapportSujet(heritage);

			final Ids res = new Ids();
			res.idHeritage = heritage.getId();
			res.idHeritier = menageCommun.getNumero();
			res.idDefunt = defunt.getNumero();
			return res;
		});

		// validation manuelle
		doInNewTransactionAndSession(status -> {
			final Heritage heritage = hibernateTemplate.get(Heritage.class, ids.idHeritage);
			Assert.assertNotNull(heritage);

			final ValidationResults vr = validate(heritage);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.getErrors().size());
			Assert.assertEquals(0, vr.getWarnings().size());

			{
				final String erreur = vr.getErrors().get(0);
				Assert.assertEquals(String.format("Le tiers héritier %s n'est pas une personne physique", FormatNumeroHelper.numeroCTBToDisplay(ids.idHeritier)), erreur);
			}
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHeritagePourDefuntNonDecede() throws Exception {
		final PersonnePhysique defunt = addNonHabitant("Papa", "Barbapapa", null, Sexe.MASCULIN);
		final PersonnePhysique heritier = addNonHabitant("Barbidur", "Barbapapa", null, Sexe.MASCULIN);
		final Heritage heritage = addHeritage(heritier, defunt, date(2017, 3, 2), null, null);
		final ValidationResults vr = validate(heritage);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.getErrors().size());
		Assert.assertEquals(1, vr.getWarnings().size());
		{
			final String warning = vr.getWarnings().get(0);
			Assert.assertEquals(String.format("Héritage (02.03.2017 - ?) entre le tiers héritier %s et le tiers défunt(e) %s alors que le 'défunt' n'est pas décédé",
			                                  FormatNumeroHelper.numeroCTBToDisplay(heritier.getNumero()),
			                                  FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero())),
			                    warning);
		}
	}
}
