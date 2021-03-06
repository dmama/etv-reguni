package ch.vd.unireg.validation.rapport;

import java.util.Collections;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ConseilLegal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

@SuppressWarnings({"JavaDoc"})
public class ConseilLegalValidatorTest extends AbstractValidatorTest<RepresentationLegale> {

	@Override
	protected String getValidatorBeanName() {
		return "conseilLegalValidator";
	}

	/**
	 * [SIFISC-719] Il doit être possible d'établir une conseil légal sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateConseilLegalSurPersonnePhysique() throws Exception {
		final PersonnePhysique pupille = addNonHabitant("Dominique", "Ruette", date(1967, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final ConseilLegal conseilLegal = new ConseilLegal(date(2000, 1, 1), null, pupille, tuteur, null);
		assertFalse(validate(conseilLegal).hasErrors());
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une conseil légal sur un ménage commun
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateConseilLegalSurMenage() throws Exception {
		final MenageCommun pupille = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final ConseilLegal conseilLegal = new ConseilLegal();
		conseilLegal.setSujet(pupille);
		conseilLegal.setObjet(tuteur);
		conseilLegal.setDateDebut(date(2000, 1, 1));
		assertValidation(Collections.singletonList("Une représentation légale ne peut s'appliquer que sur une personne physique"), null, validate(conseilLegal));
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir un conseil légal depuis un ménage commun sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateConseilLegalDepuisMenage() throws Exception {
		final MenageCommun tuteur = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final ConseilLegal curatelle = new ConseilLegal();
		curatelle.setSujet(pupille);
		curatelle.setObjet(tuteur);
		curatelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Collections.singletonList("Un conseiller légal ne peut être qu'une personne physique ou une collectivité administrative"), null, validate(curatelle));
	}

	/**
	 * [SIFISC-2483] Il doit être possible d'établir un conseil légal depuis une collectivité administrative sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateConseilLegalDepuisCollectiviteAdministrative() throws Exception {
		final CollectiviteAdministrative tuteur = (CollectiviteAdministrative) tiersDAO.save(new CollectiviteAdministrative());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final ConseilLegal conseilLegal = new ConseilLegal(date(2000, 1, 1), null, pupille, tuteur, null);
		assertFalse(validate(conseilLegal).hasErrors());
	}
}
