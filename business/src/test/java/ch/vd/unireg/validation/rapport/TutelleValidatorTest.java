package ch.vd.unireg.validation.rapport;

import java.util.Collections;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.Tutelle;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

@SuppressWarnings({"JavaDoc"})
public class TutelleValidatorTest extends AbstractValidatorTest<RepresentationLegale> {

	@Override
	protected String getValidatorBeanName() {
		return "tutelleValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDatesTutelle() throws Exception {
		final PersonnePhysique pupille = addNonHabitant("Dominique", "Ruette", date(1967, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);

		// Dates ok
		final Tutelle tutelle = new Tutelle();
		tutelle.setSujet(pupille);
		tutelle.setObjet(tuteur);
		tutelle.setDateDebut(date(2000, 1, 1));
		assertFalse(validate(tutelle).hasErrors());

		// Dates ko
		tutelle.setDateDebut(date(2000, 1, 1));
		tutelle.setDateFin(date(1996, 12, 31));
		assertValidation(Collections.singletonList(String.format("Le rapport-entre-tiers %s entre le tiers pupille %s et le tiers tuteur %s possède une date de début qui est après la date de fin: début = 01.01.2000, fin = 31.12.1996",
		                                                         tutelle, FormatNumeroHelper.numeroCTBToDisplay(pupille.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(tuteur.getNumero()))),
		                 null, validate(tutelle));
	}

	/**
	 * [SIFISC-719] Il doit être possible d'établir une tutelle sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTutelleSurPersonnePhysique() throws Exception {
		final PersonnePhysique pupille = addNonHabitant("Dominique", "Ruette", date(1967, 1, 1), Sexe.MASCULIN);
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Tutelle tutelle = new Tutelle(date(2000, 1, 1), null, pupille, tuteur, null);
		assertFalse(validate(tutelle).hasErrors());
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une tutelle sur un ménage commun
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTutelleSurMenage() throws Exception {
		final MenageCommun pupille = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Tutelle tutelle = new Tutelle();
		tutelle.setSujet(pupille);
		tutelle.setObjet(tuteur);
		tutelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Collections.singletonList("Une représentation légale ne peut s'appliquer que sur une personne physique"), null, validate(tutelle));
	}

	/**
	 * [SIFISC-719] Il ne doit pas être possible d'établir une tutelle depuis un ménage commun sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTutelleDepuisMenage() throws Exception {
		final MenageCommun tuteur = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Tutelle tutelle = new Tutelle();
		tutelle.setSujet(pupille);
		tutelle.setObjet(tuteur);
		tutelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Collections.singletonList("Un tuteur ne peut être qu'une personne physique ou une collectivité administrative"), null, validate(tutelle));
	}

	/**
	 * [SIFISC-719] Il DOIT être possible d'établir une tutelle depuis une collectivité administrative (exception pour l'office du tuteur général) sur une personne physique
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTutelleDepuisCollectiviteAdministrative() throws Exception {
		final CollectiviteAdministrative tuteur = (CollectiviteAdministrative) tiersDAO.save(new CollectiviteAdministrative());
		final PersonnePhysique pupille = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Tutelle tutelle = new Tutelle();
		tutelle.setSujet(pupille);
		tutelle.setObjet(tuteur);
		tutelle.setDateDebut(date(2000, 1, 1));
		assertFalse(validate(tutelle).hasErrors());
	}
}
