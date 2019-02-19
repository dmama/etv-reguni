package ch.vd.unireg.validation;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.shared.validation.ValidationException;
import ch.vd.shared.validation.ValidationMessage;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.AbstractSpringTest;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class ValidationInterceptorTest extends BusinessTest {

	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Vérifie que la validation sur une entité valide ne lève pas d'exception.
	 */
	@Test
	public void testValidationEntiteValide() throws Exception {

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(2003, 2, 2), Sexe.MASCULIN);
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());
				return null;
			}
		});
	}

	/**
	 * Vérifie que la validation sur une entité invalide lève bien une exception.
	 */
	@Test
	public void testValidationEntiteInvalide() throws Exception {

		try {
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(2003, 2, 2), Sexe.MASCULIN);
					jean.setNom(null); // le nom est obligatoire

					final ValidationResults results = validationService.validate(jean);
					Assert.assertEquals(1, results.errorsCount());
					Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));
					return null;
				}
			});
			Assert.fail("Une exception de validation aurait dû être levée.");
		}
		catch (ValidationException e) {
			final List<ValidationMessage> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", errors.get(0).getMessage());
		}
	}

	/**
	 * Vérifie que la validation sur une sous-entité valide et qui référence une entité parente elle-même valide ne lève pas d'exception.
	 */
	@Test
	public void testValidationSousEntiteValideEtParentValide() throws Exception {

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(2003, 2, 2), Sexe.MASCULIN);
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());

				final ForFiscalPrincipal ffp = addForPrincipal(jean, date(2004, 3, 3), MotifFor.ARRIVEE_HC, null, null, MockCommune.Aigle, MotifRattachement.DOMICILE);
				AbstractSpringTest.assertEmpty(validationService.validate(ffp).getErrors());
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());
				return null;
			}
		});
	}

	/**
	 * Vérifie que la validation sur une sous-entité valide et qui référence une entité parente invalide lève bien une exception.
	 */
	@Test
	public void testValidationSousEntiteValideEtParentInvalide() throws Exception {

		final Long jeanId = addInvalidePP();

		try {
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {

					// Jean est invalide mais non-modifié dans la session courante, il ne sera donc pas validé automatiquement pour lui-même
					final PersonnePhysique jean = hibernateTemplate.get(PersonnePhysique.class, jeanId);
					final ValidationResults results = validationService.validate(jean);
					Assert.assertEquals(1, results.errorsCount());
					Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));

					final ForFiscalPrincipal ffp = addForPrincipal(jean, date(2004, 3, 3), MotifFor.ARRIVEE_HC, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE);
					AbstractSpringTest.assertEmpty(validationService.validate(ffp).getErrors()); // la sous-entité elle-même est valide
					return null;
				}
			});
			Assert.fail("Une exception de validation aurait dû être levée.");
		}
		catch (ValidationException e) {
			final List<ValidationMessage> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", errors.get(0).getMessage());
		}
	}

	/**
	 * Vérifie que la validation sur une join-entité valide et qui référence deux entités elles-mêmes valides ne lève pas d'exception.
	 */
	@Test
	public void testValidationJoinEntiteValideEtParentsValides() throws Exception {

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(1985, 2, 2), Sexe.MASCULIN);
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());

				final PersonnePhysique olga = addNonHabitant("Olga", "Dupneu", date(1992, 2, 2), Sexe.FEMININ);
				AbstractSpringTest.assertEmpty(validationService.validate(olga).getErrors());

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(jean, olga, date(2011, 3, 12), null);
				AbstractSpringTest.assertEmpty(validationService.validate(ensemble.getMenage()).getErrors());
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());
				AbstractSpringTest.assertEmpty(validationService.validate(olga).getErrors());
				return null;
			}
		});
	}

	/**
	 * Vérifie que la validation sur une join-entité valide et qui référence au moins une entité parente invalide lève bien une exception.
	 */
	@Test
	public void testValidationJoinEntiteValideEtParentsInvalides() throws Exception {

		final Long jeanId = addInvalidePP();

		try {
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {

					// Jean est invalide mais non-modifié dans la session courante, il ne sera donc pas validé automatiquement pour lui-même
					final PersonnePhysique jean = hibernateTemplate.get(PersonnePhysique.class, jeanId);
					final ValidationResults results = validationService.validate(jean);
					Assert.assertEquals(1, results.errorsCount());
					Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));

					MenageCommun menage = new MenageCommun();
					menage = hibernateTemplate.merge(menage);

					AppartenanceMenage rapport = new AppartenanceMenage(date(2011, 3, 12), null, jean, menage);
					rapport = hibernateTemplate.merge(rapport);
					AbstractSpringTest.assertEmpty(validationService.validate(rapport).getErrors()); // la join-entité elle-même est valide
					
					return null;
				}
			});
			Assert.fail("Une exception de validation aurait dû être levée.");
		}
		catch (ValidationException e) {
			final List<ValidationMessage> errors = e.getErrors();
			Assert.assertEquals(1, errors.size());
			Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", errors.get(0).getMessage());
		}
	}

	/**
	 * Créé une personne physique qui ne valide pas et retourne son id.
	 *
	 * @return l'id de la personne physique créée.
	 */
	private Long addInvalidePP() throws Exception {

		// on désactive temporairement l'interception pour permettre de sauver un tiers invalide
		return doInNewTransactionAndSessionWithoutValidation(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(2003, 2, 2), Sexe.MASCULIN);
				jean.setNom(null); // le nom est obligatoire

				// On s'assure que le tiers est bien invalide
				final ValidationResults results = validationService.validate(jean);
				Assert.assertEquals(1, results.errorsCount());
				Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));

				return jean.getId();
			}
		});
	}

	@Test
	public void testValidationOnSave() throws Exception {

		// On teste que si le nom est NULL, on a une erreur de validation
		try {
			doInNewTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					PersonnePhysique nh = new PersonnePhysique(false);
					hibernateTemplate.merge(nh);
					return null;
				}
			});
			fail();
		}
		catch (ValidationException e) {
			assertEquals(1, e.getErrors().size());
		}

		// On teste que si le nom est PAS null, on n'a pas d'erreurs de validation
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("toto");
				hibernateTemplate.merge(nh);
				return null;
			}
		});
	}
}
