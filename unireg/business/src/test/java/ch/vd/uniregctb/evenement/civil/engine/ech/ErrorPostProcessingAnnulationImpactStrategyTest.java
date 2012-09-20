package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfoComparator;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class ErrorPostProcessingAnnulationImpactStrategyTest extends BusinessTest {

	private ErrorPostProcessingAnnulationImpactStrategy strategy;
	private EvenementCivilEchDAO dao;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		dao = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		strategy = new ErrorPostProcessingAnnulationImpactStrategy(dao);
	}

	private EvenementCivilEch buildEvent(TypeEvenementCivilEch type, ActionEvenementCivilEch action, long noIndividu, long idEvt, @Nullable Long idEvtRef, RegDate date, EtatEvenementCivil etat) {
		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(idEvt);
		evt.setType(type);
		evt.setAction(action);
		evt.setEtat(etat);
		evt.setDateEvenement(date);
		evt.setNumeroIndividu(noIndividu);
		evt.setRefMessageId(idEvtRef);
		return hibernateTemplate.merge(evt);
	}

	@Test
	public void testNeToucheARienSiPasAnnulation() throws Exception {

		final long noIndividu = 4256673246L;
		final long idEvtRef = 22L;
		final long idEvtCorrection = 23321L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur mais sans annulation
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtRef, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvtRef, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(corr));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(1, remaining.size());       // donc a priori rien n'a été traité par la stratégie (n'oublions pas que l'événement initial n'a pas été transmis à la stratégie)
		Assert.assertEquals("Evénement de correction", infos.get(1), remaining.get(0));

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(info.getId() == idEvtRef ? EtatEvenementCivil.EN_ERREUR : EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne fait effectivement rien non plus
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(info.getId() == idEvtRef ? EtatEvenementCivil.EN_ERREUR : EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testNeToucheARienSiPasAnnulationMaisToutEnAttente() throws Exception {

		final long noIndividu = 4256673246L;
		final long idEvtRef = 22L;
		final long idEvtCorrection = 23321L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur mais sans annulation
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch arrivee = buildEvent(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtRef, null, date.addYears(-1), EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtRef, null, date, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvtRef, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(arrivee), new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(corr));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(2, remaining.size());       // donc a priori rien n'a été traité par la stratégie (n'oublions pas que l'événement en erreur n'a pas été transmis à la stratégie)
		Assert.assertEquals("Evénement initial", infos.get(1), remaining.get(0));
		Assert.assertEquals("Evénement de correction", infos.get(2), remaining.get(1));

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne fait effectivement rien non plus
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testNeToucheARienSiAnnulationMaisOriginalTraite() throws Exception {

		final long noIndividu = 4256673246L;
		final long idEvtRef = 22L;
		final long idEvtCorrection = 23321L;
		final long idEvtAnnulation = 1233423L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur mais sans annulation
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtRef, null, date, EtatEvenementCivil.TRAITE);
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvtRef, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvtRef, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(corr), new EvenementCivilEchBasicInfo(annulation));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(1, remaining.size());       // donc a priori rien n'a été traité par la stratégie (n'oublions pas que l'événement en erreur n'a pas été transmis à la stratégie)
		Assert.assertEquals("Evénement d'annulation", infos.get(1), remaining.get(0));

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(info.getId() == idEvtCorrection ? EtatEvenementCivil.EN_ERREUR : EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne fait effectivement rien non plus
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(info.getId() == idEvtCorrection ? EtatEvenementCivil.EN_ERREUR : EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testCasSimpleAnnulationSurEvenementEnErreur() throws Exception {

		final long noIndividu = 2784262L;
		final long idEvt = 4367L;
		final long idEvtAnnulation = 436724L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur avec annulation
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvt, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(annulation));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(0, remaining.size());       // donc a priori tout a été traité par la stratégie

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testAnnulationDeCorrectionSurEvenementEnErreur() throws Exception {

		final long noIndividu = 2784262L;
		final long idEvt = 4367L;
		final long idEvtCorrection = 2346L;
		final long idEvtAnnulation = 436724L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur avec annulation de la correction (-> seule celle-ci doit être marquée comme redondante, pas l'événement initial)
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvt, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvtCorrection, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(corr), new EvenementCivilEchBasicInfo(annulation));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(0, remaining.size());       // donc a priori tout a été traité par la stratégie

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					if (evt.getId() == idEvt) {
						Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
						Assert.assertNull(evt.getCommentaireTraitement());
						Assert.assertNull(evt.getDateTraitement());
					}
					else {
						Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
						Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
						Assert.assertNotNull(evt.getDateTraitement());
					}
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					if (evt.getId() == idEvt) {
						Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
						Assert.assertNull(evt.getCommentaireTraitement());
						Assert.assertNull(evt.getDateTraitement());
					}
					else {
						Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
						Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
						Assert.assertNotNull(evt.getDateTraitement());
					}
				}
				return null;
			}
		});
	}

	@Test
	public void testAnnulationEtCorrectionSurEvenementEnErreur() throws Exception {

		final long noIndividu = 2784262L;
		final long idEvt = 4367L;
		final long idEvtCorrection = 2346L;
		final long idEvtAnnulation = 436724L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur avec annulation et la correction (tout doit être marqué comme redondant)
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvt, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(corr), new EvenementCivilEchBasicInfo(annulation));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(0, remaining.size());       // donc a priori tout a été traité par la stratégie

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testAnnulationEtCorrectionSurEvenementEnErreurAvecChangementDeDateDansCorrection() throws Exception {

		final long noIndividu = 2784262L;
		final long idEvt = 4367L;
		final long idEvtCorrection = 2346L;
		final long idEvtAnnulation = 436724L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur avec annulation et la correction (tout doit être marqué comme redondant)
		// la date du divorce est justement l'objet de la correction, ce qui fait que l'ordre des événements dans la queue n'est plus le même)
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch corr = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrection, idEvt, date.getOneDayBefore(), EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvt, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				return Arrays.asList(new EvenementCivilEchBasicInfo(corr), new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(annulation));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(0, remaining.size());       // donc a priori tout a été traité par la stratégie

		// vérifions quand-même l'état en base !
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (EvenementCivilEchBasicInfo info : infos) {
					final EvenementCivilEch evt = dao.get(info.getId());
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
					Assert.assertNotNull(evt.getDateTraitement());
				}
				return null;
			}
		});
	}

	@Test
	public void testPlusieursAnnulationsEtAutres() throws Exception {

		final long noIndividu = 23784623L;
		final RegDate dateArrivee = date(2005, 3, 12);
		final RegDate dateDemenagement = dateArrivee.addMonths(6);
		final RegDate dateChangementNom = dateDemenagement.addMonths(1);
		final RegDate dateMariage = dateDemenagement.addMonths(10);
		final RegDate dateDeces = dateDemenagement.addMonths(3);
		final RegDate dateDepart = dateMariage.addDays(-15);

		long idEvenementCursor = 0L;
		final long idEvtArrivee = ++idEvenementCursor;
		final long idEvtDemenagement = ++idEvenementCursor;
		final long idEvtChangementNom = ++idEvenementCursor;
		final long idEvtAnnulationDemenagement = ++idEvenementCursor;
		final long idEvtMariage = ++idEvenementCursor;
		final long idEvtAnnulationMariage = ++idEvenementCursor;
		final long idEvtDepart = ++idEvenementCursor;
		final long idEvtCorrectionDepart = ++idEvenementCursor;
		final long idEvtDeces = ++idEvenementCursor;
		final long idEvtCorrectionDeces = ++idEvenementCursor;
		final long idEvtAnnulationDeces = ++idEvenementCursor;

		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				buildEvent(TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtArrivee, null, dateArrivee, EtatEvenementCivil.TRAITE);
				buildEvent(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtDemenagement, null, dateDemenagement, EtatEvenementCivil.A_VERIFIER);
				buildEvent(TypeEvenementCivilEch.CHGT_NOM, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtChangementNom, null, dateChangementNom, EtatEvenementCivil.FORCE);

				// cette annulation ne doit pas être marquée comme redondante
				final EvenementCivilEch annulDemenagement = buildEvent(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulationDemenagement, idEvtDemenagement, dateDemenagement, EtatEvenementCivil.EN_ERREUR);

				// le mariage et son annulation doivent être marqués comme redondants
				final EvenementCivilEch mariage = buildEvent(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtMariage, null, dateMariage, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch annulationMariage = buildEvent(TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulationMariage, idEvtMariage, dateMariage, EtatEvenementCivil.EN_ATTENTE);

				// le décès, sa correction et son annulation doivent être marqués comme redondant
				final EvenementCivilEch deces = buildEvent(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtDeces, null, dateDeces, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch correctionDeces = buildEvent(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrectionDeces, idEvtDeces, dateDeces.addDays(12), EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch annulationDeces = buildEvent(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulationDeces, idEvtDeces, dateDeces, EtatEvenementCivil.EN_ATTENTE);

				// rien ne doit être marqué comme redondant ici
				final EvenementCivilEch depart = buildEvent(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtDepart, null, dateMariage, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch correctionDepart = buildEvent(TypeEvenementCivilEch.DEPART, ActionEvenementCivilEch.CORRECTION, noIndividu, idEvtCorrectionDepart, idEvtDepart, dateDepart, EtatEvenementCivil.EN_ATTENTE);

				final List<EvenementCivilEchBasicInfo> infos = new ArrayList<EvenementCivilEchBasicInfo>();
				infos.add(new EvenementCivilEchBasicInfo(annulDemenagement));
				infos.add(new EvenementCivilEchBasicInfo(mariage));
				infos.add(new EvenementCivilEchBasicInfo(annulationMariage));
				infos.add(new EvenementCivilEchBasicInfo(deces));
				infos.add(new EvenementCivilEchBasicInfo(correctionDeces));
				infos.add(new EvenementCivilEchBasicInfo(annulationDeces));
				infos.add(new EvenementCivilEchBasicInfo(depart));
				infos.add(new EvenementCivilEchBasicInfo(correctionDepart));
				Collections.sort(infos, new EvenementCivilEchBasicInfoComparator());
				return infos;
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(2, remaining.size());       // donc a priori un traitement a bien eu lieu
		Assert.assertEquals(TypeEvenementCivilEch.DEPART, remaining.get(0).getType());
		Assert.assertEquals(TypeEvenementCivilEch.DEPART, remaining.get(1).getType());

		// vérifions quand-même l'état en base !
		// nous allons faire deux fois la même vérification... factorisons...
		final Runnable check = new Runnable() {
			@Override
			public void run() {
				try {
					doInNewTransactionAndSession(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {

							// arrivée -> traité car rien n'a changé
							{
								final EvenementCivilEch evt = dao.get(idEvtArrivee);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// démémagement -> toujours à vérifier
							{
								final EvenementCivilEch evt = dao.get(idEvtDemenagement);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// changement de nom -> toujours forcé
							{
								final EvenementCivilEch evt = dao.get(idEvtChangementNom);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.FORCE, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// annulation de déménagement -> toujours en erreur car l'événement initial était traité
							{
								final EvenementCivilEch evt = dao.get(idEvtAnnulationDemenagement);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// mariage -> redondant car l'annulation suit
							{
								final EvenementCivilEch evt = dao.get(idEvtMariage);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
								Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
								Assert.assertNotNull(evt.getDateTraitement());
							}
							// annulation de mariage -> redondant car le mariage n'était pas encore traité
							{
								final EvenementCivilEch evt = dao.get(idEvtAnnulationMariage);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
								Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
								Assert.assertNotNull(evt.getDateTraitement());
							}
							// départ -> toujours en attente
							{
								final EvenementCivilEch evt = dao.get(idEvtDepart);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// correction de départ -> toujours en attente
							{
								final EvenementCivilEch evt = dao.get(idEvtCorrectionDepart);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
								Assert.assertNull(evt.getCommentaireTraitement());
								Assert.assertNull(evt.getDateTraitement());
							}
							// décès -> redondant car une annulation est intervenue
							{
								final EvenementCivilEch evt = dao.get(idEvtDeces);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
								Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
								Assert.assertNotNull(evt.getDateTraitement());
							}
							// correction de décès -> redondant car une annulation de décès est intervenue
							{
								final EvenementCivilEch evt = dao.get(idEvtCorrectionDeces);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
								Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
								Assert.assertNotNull(evt.getDateTraitement());
							}
							// annulation de décès -> redondant car le décès n'était pas encore traité
							{
								final EvenementCivilEch evt = dao.get(idEvtAnnulationDeces);
								Assert.assertNotNull(evt);
								Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
								Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente", evt.getCommentaireTraitement());
								Assert.assertNotNull(evt.getDateTraitement());
							}
							return null;
						}
					});
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		// première vérification après la phase dite de collecte
		check.run();

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base
		check.run();
	}

	@Test
	public void testEffacementErreursAvecMarquageRedondant() throws Exception {

		final long noIndividu = 2784262L;
		final long idEvt = 4367L;
		final long idEvtAnnulation = 436724L;
		final long idEvtDeces = 45L;
		final RegDate date = RegDate.get();

		// création d'événements liés en erreur avec annulation
		final List<EvenementCivilEchBasicInfo> infos = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				final EvenementCivilEch init = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvt, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEchErreur erreurInit = new EvenementCivilEchErreur();
				erreurInit.setMessage("Erreur divorce");
				erreurInit.setType(TypeEvenementErreur.ERROR);
				init.setErreurs(new HashSet<EvenementCivilEchErreur>(Arrays.asList(erreurInit)));

				final EvenementCivilEch annulation = buildEvent(TypeEvenementCivilEch.DIVORCE, ActionEvenementCivilEch.ANNULATION, noIndividu, idEvtAnnulation, idEvt, date, EtatEvenementCivil.EN_ATTENTE);
				final EvenementCivilEch deces = buildEvent(TypeEvenementCivilEch.DECES, ActionEvenementCivilEch.PREMIERE_LIVRAISON, noIndividu, idEvtDeces, null, date, EtatEvenementCivil.EN_ERREUR);
				final EvenementCivilEchErreur erreurDeces = new EvenementCivilEchErreur();
				erreurDeces.setMessage("Erreur décès");
				erreurDeces.setType(TypeEvenementErreur.ERROR);
				deces.setErreurs(new HashSet<EvenementCivilEchErreur>(Arrays.asList(erreurDeces)));

				return Arrays.asList(new EvenementCivilEchBasicInfo(init), new EvenementCivilEchBasicInfo(annulation), new EvenementCivilEchBasicInfo(deces));
			}
		});

		// lancement de la phase de collecte
		Assert.assertTrue(strategy.needsTransactionOnCollectPhase());
		final DataHolder<Object> cdh = new DataHolder<Object>();
		final List<EvenementCivilEchBasicInfo> remaining = doInNewTransactionAndSession(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
			@Override
			public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
				return strategy.doCollectPhase(infos.subList(1, infos.size()), cdh);    // on ne passe pas l'événement initial en erreur à la stratégie c'est une stratégie de post-traitement de cette erreur
			}
		});

		// résultats ?
		Assert.assertNotNull(remaining);
		Assert.assertEquals(1, remaining.size());       // donc a priori un traitement a eu lieu
		Assert.assertEquals("Evénement de décès", infos.get(2), remaining.get(0));

		// vérifions quand-même l'état en base (on ne vérfie que les erreurs ici, le reste est vérifié dans un autre test)!
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// événement mis redondant -> les erreurs doivent avoir été enlevées
				{
					final EvenementCivilEch evt = dao.get(idEvt);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertNotNull(evt.getErreurs());
					Assert.assertEquals(0, evt.getErreurs().size());
				}
				// événement toujours en erreur -> on a touché à rien
				{
					final EvenementCivilEch evt = dao.get(idEvtDeces);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
					Assert.assertNotNull(evt.getErreurs());
					Assert.assertEquals(1, evt.getErreurs().size());
					Assert.assertEquals("Erreur décès", evt.getErreurs().iterator().next().getMessage());
				}
				return null;
			}
		});

		// reste à vérifier que la phase de finalisation ne casse rien
		Assert.assertFalse(strategy.needsTransactionOnFinalizePhase());
		strategy.doFinalizePhase(cdh.get());

		// nouvelle vérification de l'état en base (on ne vérfie que les erreurs ici, le reste est vérifié dans un autre test)!
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// événement mis redondant -> les erreurs doivent avoir été enlevées
				{
					final EvenementCivilEch evt = dao.get(idEvt);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertNotNull(evt.getErreurs());
					Assert.assertEquals(0, evt.getErreurs().size());
				}
				// événement toujours en erreur -> on a touché à rien
				{
					final EvenementCivilEch evt = dao.get(idEvtDeces);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
					Assert.assertNotNull(evt.getErreurs());
					Assert.assertEquals(1, evt.getErreurs().size());
					Assert.assertEquals("Erreur décès", evt.getErreurs().iterator().next().getMessage());
				}
				return null;
			}
		});
	}
}
