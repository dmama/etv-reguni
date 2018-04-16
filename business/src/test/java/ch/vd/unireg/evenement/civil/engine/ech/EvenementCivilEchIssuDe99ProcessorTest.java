package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchSourceHelper;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockOrigine;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchIssuDe99ProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Override
	public void onSetUp() throws Exception {
		setWantIndexationTiers(true);
		super.onSetUp();
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
				individu.setPrenomUsuel("Adriana");
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
				Assert.assertEquals("Evénement civil issu d'un eCH-0099 de commune. Événement traité sans modification Unireg.", evt.getCommentaireTraitement());
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
				individu.setPrenomUsuel("Adriana");
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
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				Assert.assertNotNull("l'origine du non-habitant devrait être Orbe (VD)", pp.getOrigine());
				Assert.assertEquals("l'origine du non-habitant devrait être Orbe (VD)", "Orbe", pp.getOrigine().getLibelle());
				Assert.assertEquals("l'origine du non-habitant devrait être Orbe (VD)", ServiceInfrastructureRaw.SIGLE_CANTON_VD, pp.getOrigine().getSigleCanton());
				return pp.getNumero();
			}
		});

		// maintenant, on change l'origine
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockOrigine nlleOrigine = new MockOrigine(MockCommune.Pully.getNomOfficiel(), ServiceInfrastructureRaw.SIGLE_CANTON_VD);
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
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNotNull(pp.getOrigine());
				Assert.assertEquals("Pully", pp.getOrigine().getLibelle());
				Assert.assertEquals(ServiceInfrastructureRaw.SIGLE_CANTON_VD, pp.getOrigine().getSigleCanton());

				return null;
			}
		});

	}

	@Test
	public void testNoIndividuAbsentSurEch99PremierDeGrappeDeTraitement() throws Exception {

		final long noIndividu = 2723567L;
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
				return pp.getNumero();
			}
		});

		final long evtId;
		AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
		try {
			// et on envoie l'événement issu de 99
			evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final long evtIdTraite = 14532L;
					{
						final EvenementCivilEch evt = new EvenementCivilEch();
						evt.setId(evtIdTraite);
						evt.setAction(ActionEvenementCivilEch.CORRECTION);
						evt.setDateEvenement(RegDate.get());
						evt.setEtat(EtatEvenementCivil.TRAITE);
						evt.setNumeroIndividu(noIndividu);
						evt.setType(TypeEvenementCivilEch.TESTING);
						hibernateTemplate.merge(evt);
					}

					{
						final EvenementCivilEch evt = new EvenementCivilEch();
						evt.setId(4545121L);
						evt.setAction(ActionEvenementCivilEch.CORRECTION);
						evt.setDateEvenement(RegDate.get());
						evt.setEtat(EtatEvenementCivil.A_TRAITER);
						evt.setNumeroIndividu(null);
						evt.setType(TypeEvenementCivilEch.TESTING);
						evt.setRefMessageId(evtIdTraite);
						return hibernateTemplate.merge(evt).getId();
					}
				}
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du bon traitement de l'événement encore à traiter
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());      // événement traité
				Assert.assertEquals((Long) noIndividu, evt.getNumeroIndividu());    // numéro individu récupéré par la grappe
				return null;
			}
		});
	}
}
