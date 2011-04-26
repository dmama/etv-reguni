package ch.vd.uniregctb.validation.rapport;

import java.util.Arrays;

import org.junit.Test;

import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertFalse;

@SuppressWarnings({"JavaDoc"})
public class RepresentationLegaleValidatorTest extends AbstractValidatorTest<RepresentationLegale> {

	@Override
	protected String getValidatorBeanName() {
		return "representationLegaleValidator";
	}

	@Test
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
		assertValidation(Arrays.asList(String.format("Le rapport-entre-tiers %s possède une date de début qui est après la date de fin: début = 01.01.2000 fin = 31.12.1996", tutelle)), null,
				validate(tutelle));
	}

	/**
	 * [SIFISC-719] Il doit être possible d'établir une tutelle sur une personne physique
	 */
	@Test
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
	public void testValidateTutelleSurMenage() throws Exception {
		final MenageCommun pupille = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final Tutelle tutelle = new Tutelle();
		tutelle.setSujet(pupille);
		tutelle.setObjet(tuteur);
		tutelle.setDateDebut(date(2000, 1, 1));
		assertValidation(Arrays.asList("Une représentation légale ne peut s'appliquer que sur une personne physique"), null, validate(tutelle));
	}

	/**
	 * [SIFISC-719] Il doit être possible d'établir une curatelle sur une personne physique
	 */
	@Test
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
	 * [SIFISC-719] Il doit être possible d'établir une conseil légal sur une personne physique
	 */
	@Test
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
	public void testValidateConseilLegalSurMenage() throws Exception {
		final MenageCommun pupille = (MenageCommun) tiersDAO.save(new MenageCommun());
		final PersonnePhysique tuteur = addNonHabitant("Michèle", "Talbot", date(1972, 3, 24), Sexe.FEMININ);
		hibernateTemplate.flush();

		final ConseilLegal conseilLegal = new ConseilLegal();
		conseilLegal.setSujet(pupille);
		conseilLegal.setObjet(tuteur);
		conseilLegal.setDateDebut(date(2000, 1, 1));
		assertValidation(Arrays.asList("Une représentation légale ne peut s'appliquer que sur une personne physique"), null, validate(conseilLegal));
	}
}
