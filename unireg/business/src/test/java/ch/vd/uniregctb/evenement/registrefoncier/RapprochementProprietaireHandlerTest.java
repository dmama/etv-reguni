package ch.vd.uniregctb.evenement.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementProprietaireHandlerTest extends BusinessTest {

	private RapprochementProprietaireHandlerImpl handler;
	private RapprochementRFDAO rapprochementRFDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		rapprochementRFDAO = getBean(RapprochementRFDAO.class, "rapprochementRFDAO");
		handler = new RapprochementProprietaireHandlerImpl();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setRapprochementRFDAO(rapprochementRFDAO);
	}

	@Test
	public void testAddRapprochementEternel() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
				final Ids ids = new Ids();
				ids.idContribuable = pp.getNumero();
				ids.idTiersRF = rf.getId();
				return ids;
			}
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuable, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());

				final RapprochementRF rapprochement = all.get(0);
				Assert.assertNotNull(rapprochement);
				Assert.assertFalse(rapprochement.isAnnule());
				Assert.assertNull(rapprochement.getDateDebut());
				Assert.assertNull(rapprochement.getDateFin());
				Assert.assertEquals((Long) ids.idContribuable, rapprochement.getContribuable().getNumero());
				Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
				Assert.assertEquals(TypeRapprochementRF.MANUEL, rapprochement.getTypeRapprochement());
			}
		});
	}

	@Test
	public void testAddRapprochementNonEternel() throws Exception {

		final RegDate dateFinPrecedentRapprochement = date(2000, 12, 31);

		final class Ids {
			long idContribuablePrecedent;
			long idContribuableNouveau;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique precedent = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
				final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);
				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);

				// un rapprochement existant (+ un autre annulé pour faire bonne figure et vérifier en même temps que ceux-là ne sont pas pris en compte)
				addRapprochementRF(null, null, TypeRapprochementRF.AUTO, precedent, rf, true);
				addRapprochementRF(null, dateFinPrecedentRapprochement, TypeRapprochementRF.AUTO_MULTIPLE, precedent, rf, false);

				final Ids ids = new Ids();
				ids.idContribuablePrecedent = precedent.getNumero();
				ids.idContribuableNouveau = nouveau.getNumero();
				ids.idTiersRF = rf.getId();
				return ids;
			}
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuableNouveau, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(3, all.size());

				// triés par ID
				final List<RapprochementRF> tries = all.stream()
						.sorted(Comparator.comparing(RapprochementRF::getId))
						.collect(Collectors.toList());

				{
					final RapprochementRF rapprochement = tries.get(0);
					Assert.assertNotNull(rapprochement);
					Assert.assertTrue(rapprochement.isAnnule());
					Assert.assertNull(rapprochement.getDateDebut());
					Assert.assertNull(rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuablePrecedent, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.getTypeRapprochement());
				}
				{
					final RapprochementRF rapprochement = tries.get(1);
					Assert.assertNotNull(rapprochement);
					Assert.assertFalse(rapprochement.isAnnule());
					Assert.assertNull(rapprochement.getDateDebut());
					Assert.assertEquals(dateFinPrecedentRapprochement, rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuablePrecedent, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.AUTO_MULTIPLE, rapprochement.getTypeRapprochement());
				}
				{
					final RapprochementRF rapprochement = tries.get(2);
					Assert.assertNotNull(rapprochement);
					Assert.assertFalse(rapprochement.isAnnule());
					Assert.assertEquals(dateFinPrecedentRapprochement.getOneDayAfter(), rapprochement.getDateDebut());
					Assert.assertNull(rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuableNouveau, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.MANUEL, rapprochement.getTypeRapprochement());
				}
			}
		});
	}

	@Test
	public void testAddRapprochementMultiPeriode() throws Exception {

		final RegDate dateDebutPrecedentRapprochement = date(1999, 1, 1);
		final RegDate dateFinPrecedentRapprochement = date(2000, 12, 31);

		final class Ids {
			long idContribuablePrecedent;
			long idContribuableNouveau;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique precedent = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
				final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);
				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);

				// un rapprochement existant (+ un autre annulé pour faire bonne figure et vérifier en même temps que ceux-là ne sont pas pris en compte)
				addRapprochementRF(null, null, TypeRapprochementRF.AUTO, precedent, rf, true);
				addRapprochementRF(dateDebutPrecedentRapprochement, dateFinPrecedentRapprochement, TypeRapprochementRF.AUTO_MULTIPLE, precedent, rf, false);

				final Ids ids = new Ids();
				ids.idContribuablePrecedent = precedent.getNumero();
				ids.idContribuableNouveau = nouveau.getNumero();
				ids.idTiersRF = rf.getId();
				return ids;
			}
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuableNouveau, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(4, all.size());

				// triés par ID
				final List<RapprochementRF> tries = all.stream()
						.sorted(Comparator.comparing(RapprochementRF::getId))
						.collect(Collectors.toList());

				{
					final RapprochementRF rapprochement = tries.get(0);
					Assert.assertNotNull(rapprochement);
					Assert.assertTrue(rapprochement.isAnnule());
					Assert.assertNull(rapprochement.getDateDebut());
					Assert.assertNull(rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuablePrecedent, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.getTypeRapprochement());
				}
				{
					final RapprochementRF rapprochement = tries.get(1);
					Assert.assertNotNull(rapprochement);
					Assert.assertFalse(rapprochement.isAnnule());
					Assert.assertEquals(dateDebutPrecedentRapprochement, rapprochement.getDateDebut());
					Assert.assertEquals(dateFinPrecedentRapprochement, rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuablePrecedent, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.AUTO_MULTIPLE, rapprochement.getTypeRapprochement());
				}
				{
					final RapprochementRF rapprochement = tries.get(2);
					Assert.assertNotNull(rapprochement);
					Assert.assertFalse(rapprochement.isAnnule());
					Assert.assertNull(null, rapprochement.getDateDebut());
					Assert.assertEquals(dateDebutPrecedentRapprochement.getOneDayBefore(), rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuableNouveau, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.MANUEL, rapprochement.getTypeRapprochement());
				}
				{
					final RapprochementRF rapprochement = tries.get(3);
					Assert.assertNotNull(rapprochement);
					Assert.assertFalse(rapprochement.isAnnule());
					Assert.assertEquals(dateFinPrecedentRapprochement.getOneDayAfter(), rapprochement.getDateDebut());
					Assert.assertNull(rapprochement.getDateFin());
					Assert.assertEquals((Long) ids.idContribuableNouveau, rapprochement.getContribuable().getNumero());
					Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
					Assert.assertEquals(TypeRapprochementRF.MANUEL, rapprochement.getTypeRapprochement());
				}
			}
		});
	}

	@Test
	public void testAddRapprochementContribuableInconnu() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
//				final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
				final Ids ids = new Ids();
				ids.idContribuable = 453543;        // numéro de tiers bidon
				ids.idTiersRF = rf.getId();
				return ids;
			}
		});

		// réception du message de rapprochement
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws EsbBusinessException {
					handler.addRapprochement(ids.idContribuable, ids.idTiersRF);
				}
			});
			Assert.fail("Aurait dû sauter car le contribuable est inconnu...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, e.getCode());
			Assert.assertEquals("Pas de contribuable connu avec le numéro annoncé.", e.getMessage());
		}

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());
			}
		});
	}

	@Test
	public void testAddRapprochementTiersRFInconnu() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
//				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
				final Ids ids = new Ids();
				ids.idContribuable = pp.getNumero();
				ids.idTiersRF = 54845165L;
				return ids;
			}
		});

		// réception du message de rapprochement
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws EsbBusinessException {
					handler.addRapprochement(ids.idContribuable, ids.idTiersRF);
				}
			});
			Assert.fail("Aurait dû sauter car le tiers RF est inconnu...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, e.getCode());
			Assert.assertEquals("Pas de tiers RF connu avec l'identifiant donné.", e.getMessage());
		}

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(0, all.size());
			}
		});
	}

	@Test
	public void testAddRapprochementTiersRFDejaCompletementRapproche() throws Exception {

		final class Ids {
			long idContribuableExistant;
			long idContribuableNouveau;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique existant = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
				final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);

				// rapprochement déjà complet sur le tiers RF
				addRapprochementRF(null, null, TypeRapprochementRF.AUTO, existant, rf, false);

				final Ids ids = new Ids();
				ids.idContribuableExistant = existant.getNumero();
				ids.idContribuableNouveau = nouveau.getNumero();
				ids.idTiersRF = rf.getId();
				return ids;
			}
		});

		// réception du message de rapprochement
		try {
			doInNewTransactionAndSession(new TxCallbackWithoutResult() {
				@Override
				public void execute(TransactionStatus status) throws EsbBusinessException {
					handler.addRapprochement(ids.idContribuableExistant, ids.idTiersRF);
				}
			});
			Assert.fail("Aurait dû sauter car le tiers RF est déjà complètement couvert...");
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals(EsbBusinessCode.IDENTIFICATION_DONNEES_INVALIDES, e.getCode());
			Assert.assertEquals("Le tiers RF indiqué n'a plus de période disponible pour un nouveau rapprochement.", e.getMessage());
		}

		// vérification des données en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<RapprochementRF> all = rapprochementRFDAO.getAll();
				Assert.assertNotNull(all);
				Assert.assertEquals(1, all.size());

				final RapprochementRF rapprochement = all.get(0);
				Assert.assertNotNull(rapprochement);
				Assert.assertFalse(rapprochement.isAnnule());
				Assert.assertNull(rapprochement.getDateDebut());
				Assert.assertNull(rapprochement.getDateFin());
				Assert.assertEquals((Long) ids.idContribuableExistant, rapprochement.getContribuable().getNumero());
				Assert.assertEquals((Long) ids.idTiersRF, rapprochement.getTiersRF().getId());
				Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.getTypeRapprochement());
			}
		});
	}

}
