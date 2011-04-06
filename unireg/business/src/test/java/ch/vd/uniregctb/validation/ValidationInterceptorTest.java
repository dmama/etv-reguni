package ch.vd.uniregctb.validation;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.AbstractSpringTest;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class ValidationInterceptorTest extends BusinessTest {

	private ValidationInterceptor validationInterceptor;
	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Vérifie que la validation sur une entité valide ne lève pas d'exception.
	 */
	@Test
	@NotTransactional
	public void testValidationEntiteValide() throws Exception {

		doInNewTransaction(new TransactionCallback() {
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
	@NotTransactional
	public void testValidationEntiteInvalide() throws Exception {

		try {
			doInNewTransaction(new TransactionCallback() {
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
	@NotTransactional
	public void testValidationSousEntiteValideEtParentValide() throws Exception {

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique jean = addNonHabitant("Jean", "Dupneu", date(2003, 2, 2), Sexe.MASCULIN);
				AbstractSpringTest.assertEmpty(validationService.validate(jean).getErrors());

				final ForFiscalPrincipal ffp = addForPrincipal(jean, date(2004, 3, 3), MotifFor.ARRIVEE_HC, null, null, MockCommune.Aigle.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
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
	@NotTransactional
	public void testValidationSousEntiteValideEtParentInvalide() throws Exception {

		final Long jeanId = addInvalidePP();

		try {
			doInNewTransaction(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					// Jean est invalide mais non-modifié dans la session courante, il ne sera donc pas validé automatiquement pour lui-même
					final PersonnePhysique jean = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, jeanId);
					final ValidationResults results = validationService.validate(jean);
					Assert.assertEquals(1, results.errorsCount());
					Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));

					final ForFiscalPrincipal ffp = addForPrincipal(jean, date(2004, 3, 3), MotifFor.ARRIVEE_HC, null, null, MockCommune.Bussigny.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
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
	@NotTransactional
	public void testValidationJoinEntiteValideEtParentsValides() throws Exception {

		doInNewTransaction(new TransactionCallback() {
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
	@NotTransactional
	public void testValidationJoinEntiteValideEtParentsInvalides() throws Exception {

		final Long jeanId = addInvalidePP();

		try {
			doInNewTransaction(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {

					// Jean est invalide mais non-modifié dans la session courante, il ne sera donc pas validé automatiquement pour lui-même
					final PersonnePhysique jean = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, jeanId);
					final ValidationResults results = validationService.validate(jean);
					Assert.assertEquals(1, results.errorsCount());
					Assert.assertEquals("Le nom est un attribut obligatoire pour un non-habitant", results.getErrors().get(0));

					MenageCommun menage = new MenageCommun();
					menage = (MenageCommun) hibernateTemplate.merge(menage);

					AppartenanceMenage rapport = new AppartenanceMenage(date(2011, 3, 12), null, jean, menage);
					rapport = (AppartenanceMenage) hibernateTemplate.merge(rapport);
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

		try {
			validationInterceptor.setEnabled(false); // on désactive temporairement l'interception pour permettre de sauver un tiers invalide

			return (Long) doInNewTransaction(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
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
		finally {
			validationInterceptor.setEnabled(true);
		}
	}

	@Test
	public void testValidationOnSave() throws Exception {

		// On teste que si le nom est NULL, on a une erreur de validation
		try {
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					PersonnePhysique nh = new PersonnePhysique(false);
					hibernateTemplate.save(nh);
					fail();
					return null;
				}
			});
		}
		catch (ValidationException e) {
			assertEquals(1, e.getErrors().size());
		}

		// On teste que si le nom est PAS null, on n'a pas d'erreurs de validation
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("toto");
			hibernateTemplate.save(nh);
		}
	}
}
