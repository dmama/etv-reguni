package ch.vd.uniregctb.validation.rapport;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class RapportEntreTiersValidatorTest extends AbstractValidatorTest<RapportEntreTiers> {

	@Override
	protected String getValidatorBeanName() {
		return "defaultRapportEntreTiersValidator";
	}

	@Test
	public void testValidateRapportAnnule() {

		final RapportEntreTiers rapport = new RapportEntreTiers() {
			@Override
			public TypeRapportEntreTiers getType() {
				throw new NotImplementedException();
			}

			@Override
			public RapportEntreTiers duplicate() {
				throw new NotImplementedException();
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
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					// crée un ménage commun
					final PersonnePhysique jean = addNonHabitant("Jean", "Duchoux", date(1960, 1, 1), Sexe.MASCULIN);
					final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(idMenage, jean, null, date(1990, 1, 1), null);

					// ajoute une tutelle sur le ménage-commun (devrait être interdit !)
					final Tutelle tutelle = new Tutelle();
					tutelle.setDateDebut(date(2000, 1, 1));
					final PersonnePhysique huguette = addNonHabitant("Huguette", "Dupruneau", date(1960, 2, 3), Sexe.FEMININ);

					tiersService.addRapport(tutelle, ensemble.getMenage(), huguette);
					return null;
				}
			});
			fail();
		}
		catch (ValidationException e) {
			assertEquals("MenageCommun #" + idMenage + " - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Une représentation légale ne peut s'appliquer que sur une personne physique\n", e.getMessage());
		}
	}
}
