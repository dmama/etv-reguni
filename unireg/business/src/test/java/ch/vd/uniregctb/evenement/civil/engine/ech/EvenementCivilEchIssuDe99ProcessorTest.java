package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockOrigine;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchSourceHelper;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchIssuDe99ProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Override
	protected void runOnSetUp() throws Exception {
		setWantIndexation(true);
		super.runOnSetUp();
	}

	@Test(timeout = 10000L)
	public void testHabitant() throws Exception {

		final long noIndividu = 3467843L;
		final RegDate dateNaissance = date(1989, 10, 3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bouliokova", "Tatiana", Sexe.FEMININ);
				addNationalite(ind, MockPays.Russie, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 11, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		// indexation
		globalTiersIndexer.sync();

		// vérification de l'indexation
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Tatiana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(1, data.size());
			Assert.assertEquals((Long) ppId, data.get(0).getNumero());
			Assert.assertEquals("Tatiana Bouliokova", data.get(0).getNom1());
		}

		// maintenant, on change le prénom
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPrenom("Adriana");
			}
		});

		final long evtId;
		AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
		try {
			// et on envoie l'événement issu de 99
			evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(RegDate.get());
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setType(TypeEvenementCivilEch.TESTING);
					return hibernateTemplate.merge(evt).getId();
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		globalTiersIndexer.sync();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement civil issu d'un eCH-0099 de commune.", evt.getCommentaireTraitement());
				return null;
			}
		});

		// vérification de l'indexation -> plus rien avec l'ancien prénom...
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Tatiana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(0, data.size());
		}
		// ... mais un résultat avec le nouveau prénom
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Adriana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(1, data.size());
			Assert.assertEquals((Long) ppId, data.get(0).getNumero());
			Assert.assertEquals("Adriana Bouliokova", data.get(0).getNom1());
		}
	}

	@Test(timeout = 10000L)
	public void testNonHabitant() throws Exception {

		final long noIndividu = 3467843L;
		final RegDate dateNaissance = date(1989, 10, 3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bouliokova", "Tatiana", Sexe.FEMININ);
				addNationalite(ind, MockPays.Russie, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(2000, 11, 1), MotifFor.ARRIVEE_HS, date(2011, 12, 31), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2012, 1, 1), MotifFor.DEPART_HS, MockPays.Russie);
				return pp.getNumero();
			}
		});

		// indexation
		globalTiersIndexer.sync();

		// vérification de l'indexation
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Tatiana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(1, data.size());
			Assert.assertEquals((Long) ppId, data.get(0).getNumero());
			Assert.assertEquals("Tatiana Bouliokova", data.get(0).getNom1());
		}

		// maintenant, on change le prénom
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setPrenom("Adriana");
			}
		});

		final long evtId;
		AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
		try {
			// et on envoie l'événement issu de 99
			evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(RegDate.get());
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setType(TypeEvenementCivilEch.TESTING);
					return hibernateTemplate.merge(evt).getId();
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		globalTiersIndexer.sync();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement civil issu d'un eCH-0099 de commune.", evt.getCommentaireTraitement());
				return null;
			}
		});

		// vérification de l'indexation -> plus rien avec l'ancien prénom...
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Tatiana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(0, data.size());
		}
		// ... mais un résultat avec le nouveau prénom
		{
			final TiersCriteria criterion = new TiersCriteria();
			criterion.setNomRaison("Adriana");
			final List<TiersIndexedData> data = globalTiersSearcher.search(criterion);
			Assert.assertNotNull(data);
			Assert.assertEquals(1, data.size());
			Assert.assertEquals((Long) ppId, data.get(0).getNumero());
			Assert.assertEquals("Adriana Bouliokova", data.get(0).getNom1());
		}
	}

	@Test(timeout = 10000L)
	public void testNonHabitantChangementOrigine() throws Exception {

		final long noIndividu = 3467843L;
		final RegDate dateNaissance = date(1989, 10, 3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bouliokova", "Tatiana", Sexe.FEMININ);
				addNationalite(ind, MockPays.Russie, dateNaissance, null);
				addOrigine(ind, MockCommune.Orbe);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				tiersService.changeHabitantenNH(pp);
				Assert.assertEquals("l'origine du non-habitant devrait être Orbe", "Orbe", pp.getLibelleCommuneOrigine());
				return pp.getNumero();
			}
		});

		// maintenant, on change le prénom et l'origine
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockOrigine nlleOrigine = new MockOrigine();
				nlleOrigine.setNomLieu("Saint-Petersbourg (anciennement Lenigrad, anciennement Petrograd et encore avant Saint-Petersbourg)");
				individu.setOrigines(Collections.<Origine>singletonList(nlleOrigine));
			}
		});

		final long evtId;
		AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
		try {
			// et on envoie l'événement issu de 99
			evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(RegDate.get());
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setType(TypeEvenementCivilEch.TESTING);
					return hibernateTemplate.merge(evt).getId();
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		// traitement de l'événement civil
		traiterEvenements(noIndividu);


		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Evénement civil issu d'un eCH-0099 de commune.", evt.getCommentaireTraitement());

				// Verification que l'origine est bien été reprise du civile
				PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				assertContains("Saint-Petersbourg", pp.getLibelleCommuneOrigine());

				return null;
			}
		});

	}

}
