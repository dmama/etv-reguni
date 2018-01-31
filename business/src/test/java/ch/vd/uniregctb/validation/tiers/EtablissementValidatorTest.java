package ch.vd.uniregctb.validation.tiers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;

public class EtablissementValidatorTest extends AbstractValidatorTest<Etablissement> {

	@Override
	protected String getValidatorBeanName() {
		return "etablissementValidator";
	}

	@Test
	public void testChevauchementDomiciles() throws Exception {

		final Etablissement etb = new Etablissement();
		etb.addDomicile(new DomicileEtablissement(date(2000, 1, 1), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), etb));

		// ici, pas d'erreur
		{
			final ValidationResults results = validate(etb);
			Assert.assertFalse(results.hasErrors());
		}

		// rajoutons un autre établissement qui chevauche, et rien ne va plus
		etb.addDomicile(new DomicileEtablissement(date(2005, 1, 1), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Nyon.getNoOFS(), etb));
		{
			final ValidationResults results = validate(etb);
			Assert.assertTrue(results.hasErrors());
			final List<String> errors = results.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le domicile qui commence le 01.01.2005 chevauche le précédent", errors.get(0));
		}
	}

	@Test
	public void testLiensActiviteEconomique() throws Exception {

		class Ids {
			long idPP;
			long idEtb;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alphonse", "Duchemin", null, Sexe.MASCULIN);
				final Etablissement etb = addEtablissement(null);
				final Ids ids = new Ids();
				ids.idPP = pp.getNumero();
				ids.idEtb = etb.getNumero();
				return ids;
			}
		});

		// ici, pas d'erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtb);
				final ValidationResults results = validate(etb);
				Assert.assertFalse(results.hasErrors());
			}
		});

		// rajoutons un premier lien -> toujours pas d'erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtb);
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPP);
				addActiviteEconomique(pp, etb, date(2000, 1, 1), date(2000, 12, 31), true);

				final ValidationResults results = validate(etb);
				Assert.assertFalse(results.toString(), results.hasErrors());
			}
		});

		// rajoutons un autre lien, sans chevauchement -> toujours pas d'erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtb);
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPP);
				addActiviteEconomique(pp, etb, date(2002, 1, 1), null, true);

				final ValidationResults results = validate(etb);
				Assert.assertFalse(results.hasErrors());
			}
		});

		// rajoutons un autre lien, avec chevauchement cette fois -> erreur
		doInNewTransactionAndSessionWithoutValidation(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Etablissement etb = (Etablissement) tiersDAO.get(ids.idEtb);
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPP);
				addActiviteEconomique(pp, etb, date(2000, 10, 1), date(2002, 2, 28), true);

				final ValidationResults results = validate(etb);
				Assert.assertNotNull(results);
				Assert.assertEquals(2, results.errorsCount());

				final List<String> errors = results.getErrors();
				Assert.assertEquals("Le lien d'activité économique qui commence le 01.10.2000 chevauche le précédent ([01.01.2000 ; 31.12.2000])", errors.get(0));
				Assert.assertEquals("Le lien d'activité économique qui commence le 01.01.2002 chevauche le précédent ([01.10.2000 ; 28.02.2002])", errors.get(1));
			}
		});
	}
}
