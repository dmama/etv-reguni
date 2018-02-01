package ch.vd.unireg.evenement.registrefoncier;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalRapprochementTiersRF;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;

public class RapprochementProprietaireHandlerTest extends BusinessTest {

	private RapprochementProprietaireHandlerImpl handler;
	private RapprochementRFDAO rapprochementRFDAO;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		rapprochementRFDAO = getBean(RapprochementRFDAO.class, "rapprochementRFDAO");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		handler = new RapprochementProprietaireHandlerImpl();
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setRapprochementRFDAO(rapprochementRFDAO);
		handler.setEvenementFiscalService(evenementFiscalService);
	}

	@Test
	public void testAddRapprochementEternel() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
			final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
			final Ids ids1 = new Ids();
			ids1.idContribuable = pp.getNumero();
			ids1.idTiersRF = rf.getId();
			return ids1;
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuable, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(status -> {
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
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.idContribuable), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique precedent = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
			final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);
			final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);

			// un rapprochement existant (+ un autre annulé pour faire bonne figure et vérifier en même temps que ceux-là ne sont pas pris en compte)
			addRapprochementRF(null, null, TypeRapprochementRF.AUTO, precedent, rf, true);
			addRapprochementRF(null, dateFinPrecedentRapprochement, TypeRapprochementRF.AUTO_MULTIPLE, precedent, rf, false);

			final Ids ids1 = new Ids();
			ids1.idContribuablePrecedent = precedent.getNumero();
			ids1.idContribuableNouveau = nouveau.getNumero();
			ids1.idTiersRF = rf.getId();
			return ids1;
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuableNouveau, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(status -> {
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
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertEquals(dateFinPrecedentRapprochement.getOneDayAfter(), event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.idContribuableNouveau), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique precedent = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
			final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);
			final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);

			// un rapprochement existant (+ un autre annulé pour faire bonne figure et vérifier en même temps que ceux-là ne sont pas pris en compte)
			addRapprochementRF(null, null, TypeRapprochementRF.AUTO, precedent, rf, true);
			addRapprochementRF(dateDebutPrecedentRapprochement, dateFinPrecedentRapprochement, TypeRapprochementRF.AUTO_MULTIPLE, precedent, rf, false);

			final Ids ids1 = new Ids();
			ids1.idContribuablePrecedent = precedent.getNumero();
			ids1.idContribuableNouveau = nouveau.getNumero();
			ids1.idTiersRF = rf.getId();
			return ids1;
		});

		// réception du message de rapprochement
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws EsbBusinessException {
				handler.addRapprochement(ids.idContribuableNouveau, ids.idTiersRF);
			}
		});

		// vérification des données en base
		doInNewTransactionAndSession(status -> {
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
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			// le rapprochement [null -> dateDebutPrecedentRapprochement]
			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.idContribuableNouveau), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());

			// le rapprochement [dateFinPrecedentRapprochement -> null]
			final EvenementFiscalRapprochementTiersRF event1 = (EvenementFiscalRapprochementTiersRF) events.get(1);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event1.getType());
			Assert.assertEquals(dateFinPrecedentRapprochement.getOneDayAfter(), event1.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.idContribuableNouveau), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}

	@Test
	public void testAddRapprochementContribuableInconnu() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
//				final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
			final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
			final Ids ids1 = new Ids();
			ids1.idContribuable = 453543;        // numéro de tiers bidon
			ids1.idTiersRF = rf.getId();
			return ids1;
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
		doInNewTransactionAndSession(status -> {
			final List<RapprochementRF> all = rapprochementRFDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(0, all.size());
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
		});
	}

	@Test
	public void testAddRapprochementTiersRFInconnu() throws Exception {

		final class Ids {
			long idContribuable;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
//				final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
			final Ids ids1 = new Ids();
			ids1.idContribuable = pp.getNumero();
			ids1.idTiersRF = 54845165L;
			return ids1;
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
		doInNewTransactionAndSession(status -> {
			final List<RapprochementRF> all = rapprochementRFDAO.getAll();
			Assert.assertNotNull(all);
			Assert.assertEquals(0, all.size());
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique existant = addNonHabitant("Patrick", "Duschmol", date(1964, 2, 1), Sexe.MASCULIN);
			final TiersRF rf = addPersonnePhysiqueRF("Patrick", "Duschmolle", date(1964, 2, 2), "547385965363876763", 853634L, null);
			final PersonnePhysique nouveau = addNonHabitant("Patrock", "Duschmolle", date(1964, 2, 2), Sexe.MASCULIN);

			// rapprochement déjà complet sur le tiers RF
			addRapprochementRF(null, null, TypeRapprochementRF.AUTO, existant, rf, false);

			final Ids ids1 = new Ids();
			ids1.idContribuableExistant = existant.getNumero();
			ids1.idContribuableNouveau = nouveau.getNumero();
			ids1.idTiersRF = rf.getId();
			return ids1;
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
		doInNewTransactionAndSession(status -> {
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
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
		});
	}

}
