package ch.vd.unireg.validation.rapport;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.shared.validation.ValidationException;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.Tutelle;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class RapportEntreTiersValidatorTest extends AbstractValidatorTest<RapportEntreTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "defaultRapportEntreTiersValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateRapportAnnule() {

		final RapportEntreTiers rapport = new RapportEntreTiers() {
			@Override
			public TypeRapportEntreTiers getType() {
				throw new NotImplementedException("");
			}

			@Override
			public RapportEntreTiers duplicate() {
				throw new NotImplementedException("");
			}

			@Override
			public String getDescriptionTypeObjet() {
				return null;
			}

			@Override
			public String getDescriptionTypeSujet() {
				return null;
			}
		};

		// Adresse invalide (date début nul) mais annulée => pas d'erreur
		{
			rapport.setDateDebut(null);
			rapport.setAnnule(true);
			assertFalse(validate(rapport).hasErrors());
		}

		// Adresse valide et annulée => pas d'erreur
		{
			rapport.setDateDebut(date(2000, 1, 1));
			rapport.setAnnule(true);
			assertFalse(validate(rapport).hasErrors());
		}
	}

	/**
	 * [SIFISC-719] Vérifie que l'infrastructure de validation des rapports-entre-tiers est bien en place.
	 */
	@Test
	public void testInfrastructureValidationRapports() throws Exception {

		final long idMenage = 10851795;
		try {
			doInNewTransactionAndSession(status -> {
				// crée un ménage commun
				final PersonnePhysique jean = addNonHabitant("Jean", "Duchoux", date(1960, 1, 1), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(idMenage, jean, null, date(1990, 1, 1), null);

				// ajoute une tutelle sur le ménage-commun (devrait être interdit !)
				final Tutelle tutelle = new Tutelle();
				tutelle.setDateDebut(date(2000, 1, 1));
				final PersonnePhysique huguette = addNonHabitant("Huguette", "Dupruneau", date(1960, 2, 3), Sexe.FEMININ);

				tiersService.addRapport(tutelle, ensemble.getMenage(), huguette);
				return null;
			});
			fail();
		}
		catch (ValidationException e) {
			assertEquals("MenageCommun #" + idMenage + " - 1 erreur(s) - 0 avertissement(s):\n" +
					" [E] Une représentation légale ne peut s'appliquer que sur une personne physique\n", e.getMessage());
		}
	}

	@Test
	public void testValidationRapportAssujettissement() throws Exception {

		final long idMenage = 10851795;
		try {
			doInNewTransactionAndSession(status -> {
				// crée un ménage commun
				final PersonnePhysique jean = addNonHabitant("Jean", "Duchoux", date(1960, 1, 1), Sexe.MASCULIN);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(idMenage, jean, null, date(1990, 1, 1), null);

				// ajoute un assujettissement par substition (devrait être interdit !)
				final AssujettissementParSubstitution assujettissement = new AssujettissementParSubstitution();
				assujettissement.setDateDebut(date(2000, 1, 1));
				final PersonnePhysique huguette = addNonHabitant("Huguette", "Dupruneau", date(1960, 2, 3), Sexe.FEMININ);

				tiersService.addRapport(assujettissement, huguette, ensemble.getMenage());
				return null;
			});
			fail();
		}
		catch (ValidationException e) {
			assertEquals("MenageCommun #" + idMenage + " - 1 erreur(s) - 0 avertissement(s):\n" +
					" [E] Le tiers dont l'assujetissement se substitue n'est pas une personne physique\n", e.getMessage());
		}
	}

	@Test
	public void testRapportSurMemeTiers() throws Exception {
		try {
			doInNewTransactionAndSession(status -> {
				final MenageCommun mc = hibernateTemplate.merge(new MenageCommun());
				final RapportEntreTiers ret = new AppartenanceMenage();
				ret.setDateDebut(date(2000, 1, 1));
				ret.setObjetId(mc.getNumero());
				ret.setSujetId(mc.getNumero());
				hibernateTemplate.merge(ret);
				return null;
			});
			fail();
		}
		catch (ValidationException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("est présent aux deux extrêmités du rapport entre tiers"));
		}
	}
}
