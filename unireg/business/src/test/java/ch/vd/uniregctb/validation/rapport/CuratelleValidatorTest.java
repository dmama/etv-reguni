package ch.vd.uniregctb.validation.rapport;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

@SuppressWarnings({"JavaDoc"})
public class CuratelleValidatorTest extends AbstractValidatorTest<RepresentationLegale> {

	@Override
	protected String getValidatorBeanName() {
		return "curatelleValidator";
	}

	/**
	 * [SIFISC-719] Il doit être possible d'établir une curatelle sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCuratelleSurPersonnePhysique() throws Exception {
		final PersonnePhysique pupille = addNonHabitant("Dominique", "Ruette", date(1967, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Curatelle curatelle = new Curatelle(date(2000, 1, 1), null, pupille, tuteur, null);
		assertFalse(validate(curatelle).hasErrors());
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une curatelle sur un ménage commun
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCuratelleSurMenage() throws Exception {
		final MenageCommun pupille = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Curatelle curatelle = new Curatelle();
		curatelle.setSujet(pupille);
		curatelle.setObjet(tuteur);
		curatelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Arrays.asList("Une représentation légale ne peut s'appliquer que sur une personne physique"), null, validate(curatelle));
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une curatelle depuis un ménage commun sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCuratelleDepuisMenage() throws Exception {
		final MenageCommun tuteur = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Curatelle curatelle = new Curatelle();
		curatelle.setSujet(pupille);
		curatelle.setObjet(tuteur);
		curatelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Arrays.asList("Un curateur ne peut être qu'une personne physique"), null, validate(curatelle));
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une curatelle depuis une collectivité administrative sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCuratelleDepuisCollectiviteAdministrative() throws Exception {
		final CollectiviteAdministrative tuteur = (CollectiviteAdministrative) tiersDAO.save(new CollectiviteAdministrative());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Curatelle curatelle = new Curatelle();
		curatelle.setSujet(pupille);
		curatelle.setObjet(tuteur);
		curatelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Arrays.asList("Un curateur ne peut être qu'une personne physique"), null, validate(curatelle));
	}
}
