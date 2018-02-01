package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class CorrectionRelationEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	public CorrectionRelationEchProcessorTest() {
		setWantSynchroParentes(true);
	}

	private void assertParentes(long idEnfant, RegDate dateValidite, long... idsParents) {
		final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(idEnfant);
		final Set<Long> expectedIdsSet = new HashSet<>(idsParents.length);
		for (int i = 0 ; i < idsParents.length ; ++ i) {
			expectedIdsSet.add(idsParents[i]);
		}

		final List<PersonnePhysique> parents = tiersService.getParents(enfant, dateValidite);
		for (PersonnePhysique parent : parents) {
			Assert.assertTrue(Long.toString(parent.getNumero()) + " en trop !", expectedIdsSet.contains(parent.getNumero()));
			expectedIdsSet.remove(parent.getNumero());
		}

		Assert.assertTrue(Arrays.toString(expectedIdsSet.toArray(new Long[expectedIdsSet.size()])) + " manquant(s) !", expectedIdsSet.isEmpty());
	}

	@Test(timeout = 10000)
	public void testAucuneModificationConjointRemplacementEnfant() throws Exception {

		final long noLui = 326232356L;
		final long noElle = 312642357L;
		final long noEnfant1 = 442237823L;
		final long noEnfant2 = 35728373L;
		final long noEnfant3 = 42728527L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noElle, date(1980, 7, 13), "Bouille", "Meredith", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);

				final MockIndividu enfant1 = addIndividu(noEnfant1, date(2005, 10, 3), "Bouille", "Gérard", Sexe.MASCULIN);
				final MockIndividu enfant2 = addIndividu(noEnfant2, date(2007, 1, 14), "Bouille", "Albertine", Sexe.FEMININ);
				final MockIndividu enfant3 = addIndividu(noEnfant3, date(2007, 2, 1), "Bouille", "Francine", Sexe.FEMININ);
				addLiensFiliation(enfant1, lui, elle, date(2005, 10, 3), null);
				addLiensFiliation(enfant2, lui, elle, date(2007, 1, 14), null);
			}
		});

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
			long idEnfant1;
			long idEnfant2;
			long idEnfant3;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);

				final PersonnePhysique enfant1 = addHabitant(noEnfant1);
				final PersonnePhysique enfant2 = addHabitant(noEnfant2);
				final PersonnePhysique enfant3 = addHabitant(noEnfant3);

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				ids.idEnfant1 = enfant1.getNumero();
				ids.idEnfant2 = enfant2.getNumero();
				ids.idEnfant3 = enfant3.getNumero();
				return ids;
			}
		});

		// vérification de la présence des parentés attendues
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertParentes(ids.idEnfant1, null, ids.idLui, ids.idElle);
				assertParentes(ids.idEnfant2, null, ids.idLui, ids.idElle);
				assertParentes(ids.idEnfant3, null);
			}
		});

		// modification de la relation de filiation descendante : c'est "enfant3", la fille de "lui", pas "enfant2" !
		doModificationIndividu(noEnfant2, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.getParents().removeIf(rel -> rel.getNumeroAutreIndividu() == noLui);
			}
		});
		doModificationIndividu(noEnfant3, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				MockServiceCivil.addLienVersParent(individu, serviceCivil.getIndividu(noLui, null), date(2007, 2, 1), null);
			}
		});

		// création d'un événement civil (les corrections de relations de filiation sont envoyées sur les enfants)
		final long evtIdEnfant2 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noEnfant2);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noEnfant2);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdEnfant2);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				assertParentes(ids.idEnfant1, null, ids.idLui, ids.idElle);
				assertParentes(ids.idEnfant2, null, ids.idElle);
				assertParentes(ids.idEnfant3, null);
			}
		});

		// création d'un événement civil (les corrections de relations de filiation sont envoyées sur les enfants)
		final long evtIdEnfant3 = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782457L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noEnfant3);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noEnfant3);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdEnfant3);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				assertParentes(ids.idEnfant1, null, ids.idLui, ids.idElle);
				assertParentes(ids.idEnfant2, null, ids.idElle);
				assertParentes(ids.idEnfant3, null, ids.idLui);
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationConjoint() throws Exception {

		final long noLui = 326232356L;
		final long noElle = 312642357L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noElle, date(1980, 7, 13), "Bouille", "Meredith", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				return ids;
			}
		});

		// modification des liens d'appartenance ménage (-> on annule le lien vers Madame)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				Assert.assertNotNull(mc);

				for (RapportEntreTiers ret : mc.getRapportsObjet()) {
					if (!ret.isAnnule() && ret.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE && ret.getSujetId().equals(ids.idElle)) {
						ret.setAnnule(true);
					}
				}
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles.", erreur.getMessage());
			}
		});
	}

	/**
	 * [SIFISC-13763] Les relations civiles ne tiennent pas compte des séparations, mais les relations fiscales si, donc on vérifie que cela fonctionne tout de même
	 */
	@Test(timeout = 10000)
	public void testAucuneModificationSurConjointsSepares() throws Exception {

		final long noLui = 326232356L;
		final long noElle = 312642357L;
		final RegDate dateMariage = date(2004, 12, 6);
		final RegDate dateSeparation = date(2011, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noElle, date(1980, 7, 13), "Bouille", "Meredith", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
				separeIndividus(lui, elle, dateSeparation);
			}
		});

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, dateSeparation.getOneDayBefore());

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				return ids;
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			}
		});
	}

	/**
	 * [SIFISC-13761] marié seul civil face à célibataire fiscal
	 */
	@Test(timeout = 10000)
	public void testMarieSeulCivilEtCelibataireFiscal() throws Exception {

		final long noLui = 326232356L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				marieIndividu(lui, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				return lui.getNumero();
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles.", erreur.getMessage());
			}
		});

	}

	/**
	 * [SIFISC-13764] marié seul fiscal face à célibataire civil
	 */
	@Test(timeout = 10000)
	public void testMarieSeulFiscalEtCelibataireCivil() throws Exception {

		final long noLui = 326232356L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, null, dateMariage, null);
				return lui.getNumero();
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles.", erreur.getMessage());
			}
		});

	}

	/**
	 * SIFISC-13761 : cas d'un marié seul civil & fiscal
	 */
	@Test
	public void testMarieSeulCivilEtFiscal() throws Exception {
		final long noLui = 326232356L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				marieIndividu(lui, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, null, dateMariage, null);
				return lui.getNumero();
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			}
		});
	}

	/**
	 * SIFISC-13761 : cas d'un marié seul civil mais pas fiscal
	 */
	@Test
	public void testMarieSeulCivilMaisPasFiscal() throws Exception {
		final long noLui = 326232356L;
		final RegDate dateMariage = date(2004, 12, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noLui, date(1980, 10, 25), "Bouille", "Simon", Sexe.MASCULIN);
				marieIndividu(lui, dateMariage);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addNonHabitant("Agathe", "Bouille", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				return lui.getNumero();
			}
		});

		// envoi d'un événement de correction de relation sur Monsieur
		final long evtIdLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782458L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(RegDate.get());
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.CORR_RELATIONS);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noLui);

		// vérification du traitement en erreur
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtIdLui);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals("L'historique des conjoints fiscaux n'est pas réconciliable de manière univoque avec les données civiles.", erreur.getMessage());
			}
		});
	}
}
