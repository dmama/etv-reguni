package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class ArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	private MetierService metierService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		metierService = getBean(MetierService.class, "metierService");
	}

	@Override
	protected void truncateDatabase() throws Exception {
		/**
		 * Même si en fait on ne veut pas d'indexation, il est important, dans les tests d'arrivée, que l'indexeur soit
		 * vide avant de démarrer le test (puisqu'on recherche dans les non-habitants quelqu'un qui pourrait convenir...)
		 */
		final boolean wantIndexation = this.wantIndexation;
		setWantIndexation(true);
		try {
			super.truncateDatabase();
		}
		finally {
			setWantIndexation(wantIndexation);
		}
	}

	@Test(timeout = 10000L)
	public void testArriveeCelibataire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Espagne.getNoOFS(), null));

				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-5466] Vérifie qu'un événement d'arrivée redondant pour un contribuable célibataire est bien traité comme redondant.
	 */
	@Test(timeout = 10000L)
	public void testArriveeCelibataireAvecRedondance() throws Exception {

		final long noIndividu = 1057790L;
		final RegDate dateArrivee = date(2011, 10, 1);

		// la personne civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1987, 2, 4);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Jolias", "Virginie", false);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Zurich.getNoOFS(), null));

				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
			}
		});

		// Cette personne est déjà enregistrée dans la base
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return null;
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// on s'assure que l'événement est détecté comme redondant
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveesCoupleAvecRedondance() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2011, 10, 31);
		final RegDate dateMariage = date(2001, 10, 1);
		
		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);
				final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				addForPrincipal(etc.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
				return null;
			}
		});


		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});
		
		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);
		
		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getConjoint());
				assertEquals(pp.getId(), couple.getPrincipal().getNumero());
				
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				
				return null;
			}
		});
		
		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getPrincipal());
				assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveesCoupleAvecRedondancePosterieure() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(1);
		final RegDate dateMariage = date(2001, 10, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getConjoint());
				assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArriveeLui, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getPrincipal());
				assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArriveeLui, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveesAnterieurCouple() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(-1);        // arrivée décalée, avant le premier (si c'était après, ce serait traité)
		final RegDate dateMariage = date(2001, 10, 1);


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getConjoint());
				assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArriveeLui, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getPrincipal());
				assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				final RegDate dateDebutFor = ffp.getDateDebut();

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
				assertEquals(message, erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testRetourCoupleApresDepart() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateDepart = date(2010,5,7);
		final RegDate dateArriveeLui = date(2011, 10, 31);
		final RegDate dateArriveeElle = dateArriveeLui.addMonths(-1);        // arrivée décalée, avant le premier
		final RegDate dateMariage = date(2001, 10, 1);



		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrAvantDepartLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateDepart);
				adrAvantDepartLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrFranceLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeLui.getOneDayBefore());
				adrFranceLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				adrFranceLui.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));


				final MockAdresse adrAvantDepartElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateDepart);
				adrAvantDepartElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrFranceElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeElle.addMonths(-1));
				adrFranceElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				adrFranceElle.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique ppLui = addHabitant(noLui);
				PersonnePhysique ppElle = addHabitant(noElle);
				MenageCommun menage = metierService.marie(dateMariage, ppLui, ppElle, null, EtatCivil.MARIE, null);
				tiersService.closeAllForsFiscaux(menage, dateDepart, MotifFor.DEPART_HS);
				addForPrincipal(menage, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.France);
				return null;
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noLui);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getConjoint());
				assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArriveeLui, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNotNull(couple.getPrincipal());
				assertEquals(pp.getId(), couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				final RegDate dateDebutFor = ffp.getDateDebut();

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
				assertEquals(message, erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveeNumeroOfsCommuneInconnue() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, null, null));

				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());


				return null;
			}
		});

	}

	@Test(timeout = 10000L)
	public void testArriveeProvenanceAbsente() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateArrivee = date(2011, 10, 31);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1956, 4, 23);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				addNationalite(ind, MockPays.Espagne, dateNaissance, null);
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.A_VERIFIER, evt.getEtat());

				final Set<EvenementCivilEchErreur> warnings = evt.getErreurs();
				assertNotNull(warnings);
				assertEquals(1, warnings.size());
				final EvenementCivilEchErreur warning = warnings.iterator().next();
				assertNotNull(warning);
				assertEquals(TypeEvenementErreur.WARNING, warning.getType());
				assertEquals("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal.", warning.getMessage());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-5286] Vérifie que l'arrivée dans une commune vaudoise avec une localisation précédente vaudoise (mais pas d'adresse correspondante) sur pour individu inconnu dans Unireg lève bien une erreur.
	 */
	@Test(timeout = 10000L)
	public void testHandleArriveeIndividuInconnuAvecLocalisationPrecedenteDansCommuneVaudoise() throws Exception {

		final Long noInd = 324543L;
		final RegDate dateArrivee = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noInd, date(1965, 3, 12), "Bolomey", "Brian", true);
				addNationalite(ind, MockPays.Suisse, date(1965, 3, 12), null);
				MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.CheminDeMornex, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Nyon.getNoOFS(), null));
			}
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noInd);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement
		traiterEvenements(noInd);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				assertNotNull(erreur);
				assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				assertEquals("L'individu est inconnu dans registre fiscal mais arrive depuis une commune vaudoise (incohérence entre les deux registres)",
						erreur.getMessage());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testArriveeHCAncienNonHabitantSIFISC6032() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);

			setWantIndexation(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Chollet", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);
					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
					addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
					addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
					return pp.getNumero();
				}
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateArrivee);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setType(TypeEvenementCivilEch.ARRIVEE);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evtId);
					assertNotNull(evt);
					assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
					final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
					assertNotNull(pp);
					assertEquals((Long) ppId, pp.getNumero());
					final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(pp, null);
					assertNotNull(etc);
					final ForFiscalPrincipal ffpMenage = etc.getMenage().getForFiscalPrincipalAt(null);
					assertNotNull(ffpMenage);
					assertEquals(MotifFor.ARRIVEE_HC, ffpMenage.getMotifOuverture());
					assertNull(ffpMenage.getDateFin());
					final ForFiscalPrincipal ffpPP = pp.getDernierForFiscalPrincipal();
					assertNotNull(ffpPP);
					assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpPP.getMotifFermeture());
					assertEquals(dateMariage.getOneDayBefore(), ffpPP.getDateFin());
					return null;
				}
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	/**
	 * [SIFISC-6109] Vérifie qu'un événement d'arrivée pour un seul membre d'un couple de non-habitants (ancien habitant donc connu du civil) ne passe pas les 2 en habitant
	 */
	@Test (timeout = 10000L)
	public void testArriveeUnSeulMembreDuCoupleDepuisHS_SIFISC_6109() throws Exception {

		try {
			final long noIndividuMonsieur = 1057790L;
			final long noIndividuMadame = 1057791L;
			final RegDate dateDepartCouple = date(2010, 10, 1);
			final RegDate dateRetourMoniseurSeul = date(2011, 10, 1);
			final RegDate dateRetourMadameSeule = dateRetourMoniseurSeul.addMonths(1);
			final RegDate dateNaissanceMonsieur = date(1987, 2, 4);
			final RegDate dateNaissanceMadame = date(1990, 3, 5);
			final RegDate dateMariage = date(2009, 7, 8);

			setWantIndexation(true);

			// Le civil
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {

					final MockIndividu monsieur = addIndividu(noIndividuMonsieur, dateNaissanceMonsieur, "Jolias", "Virgil", true);
					final MockAdresse adresseMonsieur = addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateRetourMoniseurSeul, null);
					adresseMonsieur.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
					addNationalite(monsieur, MockPays.Suisse, dateNaissanceMonsieur, null);
					final MockIndividu madame = addIndividu(noIndividuMadame, dateNaissanceMadame, "Jolias", "Virginie", true);
					final MockAdresse adresseMadame = addAdresse(madame, TypeAdresseCivil.PRINCIPALE, null, "rue de l'église, 4", "01250", MockPays.France, dateDepartCouple, dateRetourMadameSeule.getOneDayBefore());
					final MockAdresse adresse2Madame = addAdresse(madame, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, dateRetourMadameSeule, null);
					adresseMadame.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
					adresse2Madame.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
					addNationalite(madame, MockPays.Suisse, dateNaissanceMonsieur, null);
					marieIndividus(monsieur, madame, dateMariage);
				}
			});

			// Le fiscal
			final long ids[] = doInNewTransactionAndSession(new TxCallback<long[]>() {
				@Override
				public long[] execute(TransactionStatus status) throws Exception {
					final PersonnePhysique ppMadame = addNonHabitant("Jolias", "Virginie", dateNaissanceMadame, Sexe.FEMININ);
					final PersonnePhysique ppMonsieur = addNonHabitant("Jolias", "Virgil", dateNaissanceMonsieur, Sexe.MASCULIN);
					// ancien habitant: ils ont un numéro d'individu
					ppMadame.setNumeroIndividu(noIndividuMadame);
					ppMonsieur.setNumeroIndividu(noIndividuMonsieur);
					final EnsembleTiersCouple etc = addEnsembleTiersCouple(ppMonsieur, ppMadame, dateMariage, null);
					addForPrincipal(etc.getMenage(), dateMariage,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepartCouple.getOneDayBefore(), MotifFor.DEPART_HS, MockPays.France);
					addForPrincipal(etc.getMenage(), date(2010,12,1),MotifFor.ACHAT_IMMOBILIER, MockPays.France);
					return new long[] {ppMonsieur.getNumero(), ppMadame.getNumero(), etc.getMenage().getNumero()};
				}
			});

			final long idMonsieur = ids[0];
			final long idMadame = ids[1];

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14532L);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateRetourMoniseurSeul);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividuMonsieur);
					evt.setType(TypeEvenementCivilEch.ARRIVEE);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traitement de l'événement
			traiterEvenements(noIndividuMonsieur);

			// on s'assure que Madame n'est pas habitante ( et que monsieur l'est au passage..)
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evtId);
					assertNotNull(evt);
					assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

					final PersonnePhysique monsieur = (PersonnePhysique) tiersService.getTiers(idMonsieur);
					assertNotNull(monsieur);
					assertTrue("Monsieur doit être habitant", monsieur.isHabitantVD());

					final PersonnePhysique madame = (PersonnePhysique) tiersService.getTiers(idMadame);
					assertNotNull(madame);
					assertFalse("Madame ne doit être habitante", madame.isHabitantVD());


					return null;
				}
			});


			// événement d'arrivée
			final long evt2Id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
				@Override
				public Long doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(14533L);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateRetourMadameSeule);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividuMadame);
					evt.setType(TypeEvenementCivilEch.ARRIVEE);
					return hibernateTemplate.merge(evt).getId();
				}
			});

			// traitement de l'événement
			traiterEvenements(noIndividuMadame);

			// Madame arrive finallement 1 mois apres monsieur
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(evt2Id);
					assertNotNull(evt);
					assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

					final PersonnePhysique madame = (PersonnePhysique) tiersService.getTiers(idMadame);
					assertNotNull(madame);
					assertTrue("Madame doit être habitante", madame.isHabitantVD());
					return null;
				}
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	@Test(timeout = 10000L)
	public void testArriveesAnterieurCoupleAMettreEnErreur() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArriveeLui = date(2012, 3, 12);
		final RegDate dateArriveeElle = date(2012,6,15);        // arrivée décalée, avant le premier (si c'était après, ce serait traité)
		final RegDate dateMariage = date(2005, 8, 2);


		// mise en place civile Madame
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividu(elle, dateMariage);

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		globalTiersIndexer.sync();
		// création de l'événement civil pour l'arrivée de Madame
		final long evtElle = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeElle);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noElle);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de madame
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
				assertNotNull(pp);

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNotNull(couple.getMenage());
				assertNull(couple.getConjoint());
				assertEquals(pp.getId(), couple.getPrincipal().getNumero());

				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArriveeElle, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());

				return null;
			}
		});



		// mise en place civile Monsieur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(elle, MockPays.France, naissanceElle, null);

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				addNationalite(lui, MockPays.France, naissanceLui, null);

				marieIndividus(lui,elle, dateMariage);
				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});


		globalTiersIndexer.sync();
		// événement civil de l'arrivée de monsieur

		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(321674L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArriveeLui);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				String message = String.format("Le conjoint de l'individu (n° %s) correspond à un(e) marié(e) seul",noLui);
				assertEquals(message, erreur.getMessage());
				return null;
			}
		});
	}

	/**
	 * SIFISC-6926
	 */
	@Test(timeout = 10000L)
	public void testArriveePersonneMarieConnueHorsSuisseCelibataire() throws Exception {

		final long noIndividu = 32673256L;
		final RegDate debutHS = date(2009, 9, 1);
		final RegDate dateMariage = date(2011, 6, 6);
		final RegDate dateArrivee = date(2012, 5, 5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1980, 10, 25);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Tine", "Albert", Sexe.MASCULIN);
				marieIndividu(ind, dateMariage);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Rue de la tour Eiffel", "23", 75007, null, "Paris", MockPays.France, debutHS, dateArrivee.getOneDayBefore());
				final MockAdresse adresseVD = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresseVD.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addNationalite(ind, MockPays.France, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, debutHS, MotifFor.DEPART_HS, MockPays.France);
				return pp.getNumero();
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				assertNull(evt.getCommentaireTraitement());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				final ForFiscalPrincipal ffpPP = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffpPP);
				assertEquals(debutHS, ffpPP.getDateDebut());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, ffpPP.getTypeAutoriteFiscale());
				assertEquals(dateMariage.getOneDayBefore(), ffpPP.getDateFin());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNull(couple.getConjoint());

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);
				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateArrivee, ffpMc.getDateDebut());
				assertNull(ffpMc.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpMc.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffpMc.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});
	}

	//SIFISC-6065
	//Test la detection de personne marie appartenant à un menage commun à la date d el'évènement mais qui ont des rapports entre tiers incohérents avec les dates du civil
	@Test (timeout = 10000L)
	public void testArriveeCoupleAvecIncoherenceEtatCivilRapportMenage() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2010, 7, 31);
		final RegDate dateMariage = date(2001, 10, 1);
		final RegDate dateDebutRapport = date(2010, 9, 8);
		class Ids {
			Long monsieur;
			Long madame;
		}
		final Ids ids = new Ids();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.France, naissanceLui, null);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD,MockCommune.Bussigny.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noLui);
				final PersonnePhysique elle = addHabitant(noElle);

				final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateDebutRapport, null);
				addForPrincipal(etc.getMenage(), dateDebutRapport, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
				ids.monsieur = lui.getNumero();
				ids.madame = elle.getNumero();
				return null;
			}
		});


		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification du traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtLui);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				assertNotNull(erreurs);
				assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				assertNotNull(erreur);
				assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				final String messageErreurAttendu = String.format("L'arrivant(e) [%s] a un état civil marié ou pacsé à la date de l'évènement ainsi qu'un ménage commun. " +
						"Cependant, aucun lien d'appartenance ménage n'a été trouvé pour cette date: [%s]. Vérifier si il n'y a pas une incohérence entre les dates civiles et fiscales",
						FormatNumeroHelper.numeroCTBToDisplay(ids.monsieur),
						RegDateHelper.dateToDashString(dateArrivee));
				assertEquals(messageErreurAttendu,
						erreur.getMessage());
				return null;
			}
		});
	}

	/**
	 * SIFISC-7276
	 *
	 * Test non-regression dans le cas d'une personne revenant sur le canton comme  marié alors qu'elle est partie célibataire
	 * On ferme le for meme si le flag habitant est renseigné à true (ce qui est en soit incoherent puisque la personne arrive
	 * depuis Hors-Suisse...)
	 *
	 */
	@Test(timeout = 10000L)
	public void testArriveePersonneMarieConnueHorsSuisseCelibataireAvecFlagHabitant() throws Exception {

		final long noIndividu = 32673256L;
		final RegDate debutHS = date(2009, 9, 1);
		final RegDate dateMariage = date(2011, 6, 6);
		final RegDate dateArrivee = date(2012, 5, 5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1980, 10, 25);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Tine", "Albert", Sexe.MASCULIN);
				marieIndividu(ind, dateMariage);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, "Rue de la tour Eiffel", "23", 75007, null, "Paris", MockPays.France, debutHS, dateArrivee.getOneDayBefore());
				final MockAdresse adresseVD = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresseVD.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addNationalite(ind, MockPays.France, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, debutHS, MotifFor.DEPART_HS, MockPays.France);
				pp.setHabitant(true);
				return pp.getNumero();
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				assertNull(evt.getCommentaireTraitement());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				final ForFiscalPrincipal ffpPP = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffpPP);
				assertEquals(debutHS, ffpPP.getDateDebut());
				assertEquals(TypeAutoriteFiscale.PAYS_HS, ffpPP.getTypeAutoriteFiscale());
				assertEquals(dateMariage.getOneDayBefore(), ffpPP.getDateFin());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);
				assertNull(couple.getConjoint());

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);
				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateArrivee, ffpMc.getDateDebut());
				assertNull(ffpMc.getDateFin());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpMc.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffpMc.getNumeroOfsAutoriteFiscale());
				return null;
			}
		});
	}

	@Test
	public void testArriveeRetraiteSourcier() throws Exception {
		final long noIndividu = 138946274L;
		final RegDate dateNaissance = RegDate.get().addYears(-70);      // à changer quand l'âge de la retraite atteindra 70 ans...
		final RegDate dateArrivee = RegDate.get().addDays(-20);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Boyington", "Gregory", Sexe.MASCULIN);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(ind, MockPays.EtatsUnis, dateNaissance, null);

				final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.EtatsUnis.getNoOFS(), null));
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3273426L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());

				return null;
			}
		});
	}

	@Test
	public void testArriveeCoupleSourcierDontUnRetraite() throws Exception {
		final long noIndividuLui = 138946274L;
		final long noIndividuElle = 138946275L;
		final RegDate dateNaissanceLui = RegDate.get().addYears(-70);      // à changer quand l'âge de la retraite atteindra 70 ans...
		final RegDate dateNaissanceElle = RegDate.get().addYears(-50);
		final RegDate dateArrivee = RegDate.get().addDays(-20);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissanceLui, "Boyington", "Gregory", Sexe.MASCULIN);
				addPermis(lui, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(lui, MockPays.EtatsUnis, dateNaissanceLui, null);
				final MockAdresse adresse = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.EtatsUnis.getNoOFS(), null));

				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissanceElle, "Boyington", "Pamela", Sexe.FEMININ);
				addPermis(elle, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(elle, MockPays.EtatsUnis, dateNaissanceElle, null);

				marieIndividus(lui, elle, dateArrivee.addYears(-20));
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3273426L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuLui);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, null);
				assertNotNull(couple);
				assertNotNull(couple.getPrincipal());
				assertNotNull(couple.getConjoint());
				assertNotNull(couple.getMenage());

				final ForFiscalPrincipal ffp = couple.getMenage().getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());

				return null;
			}
		});
	}

	@Test
	public void testArriveeCoupleSourcierAucunRetraite() throws Exception {
		final long noIndividuLui = 138946274L;
		final long noIndividuElle = 138946275L;
		final RegDate dateNaissance = RegDate.get().addYears(-50);
		final RegDate dateArrivee = RegDate.get().addDays(-20);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Boyington", "Gregory", Sexe.MASCULIN);
				addPermis(lui, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(lui, MockPays.EtatsUnis, dateNaissance, null);
				final MockAdresse adresse = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDesBergieres, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.EtatsUnis.getNoOFS(), null));

				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissance, "Boyington", "Pamela", Sexe.FEMININ);
				addPermis(elle, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(elle, MockPays.EtatsUnis, dateNaissance, null);

				marieIndividus(lui, elle, dateArrivee.addYears(-20));
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3273426L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuLui);
				assertNotNull(pp);
				assertTrue(pp.isHabitantVD());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, null);
				assertNotNull(couple);
				assertNotNull(couple.getPrincipal());
				assertNotNull(couple.getConjoint());
				assertNotNull(couple.getMenage());

				final ForFiscalPrincipal ffp = couple.getMenage().getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertNull(ffp.getDateFin());
				assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());

				return null;
			}
		});
	}

	/**
	 * SIFISC-9180 : le motif de rattachement "DIPLOMATE_ETRANGER" était conservé sur le for de domicile vaudois après l'arrivée
	 */
	@Test
	public void testArriveeDiplomateEtranger() throws Exception {

		final long noIndividuLui = 5738964L;
		final long noIndividuElle = 4378562L;
		final RegDate dateMariage = date(1999, 9, 21);
		final RegDate dateAchat = date(2008, 2, 5);
		final RegDate dateArrivee = date(2013, 5, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, date(1971, 11, 7), "Tabernacle", "John", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noIndividuElle, date(1972, 11, 1), "Tabernacle", "Vanessa", Sexe.FEMININ);
				addNationalite(lui, MockPays.EtatsUnis, date(1971, 11, 7), null);
				addNationalite(elle, MockPays.EtatsUnis, date(1972, 11, 1), null);
				addPermis(lui, TypePermis.PAS_ATTRIBUE, dateArrivee, null, false);
				addPermis(elle, TypePermis.PAS_ATTRIBUE, dateArrivee, null, false);
				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adresse = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
				adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
			}
		});

		// mise en place fiscale : ils possédaient un immeuble avant d'arriver
		final long ppMenage = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, dateAchat, null, null, null, MockPays.EtatsUnis.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DIPLOMATE_ETRANGER);
				addForSecondaire(mc, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				return mc.getNumero();
			}
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3273426L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuLui);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du for principal vaudois suite à l'arrivée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch ech = evtCivilDAO.get(evtArrivee);
				assertNotNull(ech);
				assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ppMenage);
				assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateArrivee, ffp.getDateDebut());
				assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture());
				assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				return null;
			}
		});
	}

	/**
	 * Une petite famille est arrivée sur le canton, et les événements sont traités dans l'ordre : maman, bébé puis papa.<p/>
	 * Nous nous plaçons au moment du traitement de l'arrivée de bébé... (maman et papa ne sont pas mariés, donc seule
	 * maman existe dans le registre fiscal à ce moment).
	 */
	@Test
	public void testCalculParentesSiArriveeEnfantAvantParent() throws Exception {

		final long indPapa = 4367436L;
		final long indMaman = 347357L;
		final long indBebe = 4378243526L;
		final RegDate dateNaissanceBebe = date(2007, 1, 5);
		final RegDate dateArrivee = RegDate.get().addYears(-2);     // comme ça l'enfant reste mineur et on ne lui crée pas de for

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(indPapa, null, "Chollet", "Ignacio", Sexe.MASCULIN);
				final MockIndividu maman = addIndividu(indMaman, null, "Chollet", "Mireille", Sexe.FEMININ);
				final MockIndividu bebe = addIndividu(indBebe, dateNaissanceBebe, "Chollet", "Sigourney", Sexe.FEMININ);
				addLiensFiliation(bebe, papa, maman, dateNaissanceBebe, null);

				addAdresse(papa, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
				addAdresse(maman, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);

				final MockAdresse adresseBebe = addAdresse(bebe, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
				adresseBebe.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
				addNationalite(bebe, MockPays.France, dateNaissanceBebe, null);
				addPermis(bebe, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale (sans calcul des parentés, siouplait...)
		final long idMaman = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique maman = addHabitant(indMaman);
				addForPrincipal(maman, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				return maman.getNumero();
			}
		});

		// création de l'événement d'arrivée du bébé
		final long evtArrivee = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(3273426L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(indBebe);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement de l'événement civil
		traiterEvenements(indBebe);

		// vérification de l'état des flags parenteDirty sur les tiers existants
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique maman = tiersService.getPersonnePhysiqueByNumeroIndividu(indMaman);
				assertNotNull(maman);
				assertEquals((Long) idMaman, maman.getNumero());

				final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
				assertNotNull(bebe);

				// les relations depuis maman
				{
					final Set<RapportEntreTiers> relEnfants = maman.getRapportsObjet();
					assertNotNull(relEnfants);
					assertEquals(1, relEnfants.size());

					final RapportEntreTiers relEnfant = relEnfants.iterator().next();
					assertNotNull(relEnfant);
					assertEquals(TypeRapportEntreTiers.PARENTE, relEnfant.getType());
					assertEquals((Long) idMaman, relEnfant.getObjetId());
					assertEquals(bebe.getNumero(), relEnfant.getSujetId());
					assertEquals(dateNaissanceBebe, relEnfant.getDateDebut());
					assertNull(relEnfant.getDateFin());
					assertFalse(relEnfant.isAnnule());

					assertFalse(maman.isParenteDirty());
				}

				// les relations depuis bébé
				{
					final Set<RapportEntreTiers> relParents = bebe.getRapportsSujet();
					assertNotNull(relParents);
					assertEquals(1, relParents.size());

					final RapportEntreTiers relParent = relParents.iterator().next();
					assertNotNull(relParent);
					assertEquals(TypeRapportEntreTiers.PARENTE, relParent.getType());
					assertEquals((Long) idMaman, relParent.getObjetId());
					assertEquals(bebe.getNumero(), relParent.getSujetId());
					assertEquals(dateNaissanceBebe, relParent.getDateDebut());
					assertNull(relParent.getDateFin());
					assertFalse(relParent.isAnnule());

					assertTrue(bebe.isParenteDirty());      // il manque la relation vers papa qui n'existe pas encore au fiscal
				}

				return null;
			}
		});

		// arrivée de papa (en raccourci -> on crée directement le gars en base)
		// le recalcul des parentés est activé mais ne devrait rien changer (car la liaison entre bébé et papa est inconnue fiscalement pour le moment)
		final long idPapa = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique papa = addHabitant(indPapa);
				addForPrincipal(papa, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				return papa.getNumero();
			}
		});

		// vérification des relations depuis bébé (-> rien n'a changé)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
				assertNotNull(bebe);

				// les relations depuis bébé
				{
					final Set<RapportEntreTiers> relParents = bebe.getRapportsSujet();
					assertNotNull(relParents);
					assertEquals(1, relParents.size());

					final RapportEntreTiers relParent = relParents.iterator().next();
					assertNotNull(relParent);
					assertEquals(TypeRapportEntreTiers.PARENTE, relParent.getType());
					assertEquals((Long) idMaman, relParent.getObjetId());
					assertEquals(bebe.getNumero(), relParent.getSujetId());
					assertEquals(dateNaissanceBebe, relParent.getDateDebut());
					assertNull(relParent.getDateFin());
					assertFalse(relParent.isAnnule());

					assertTrue(bebe.isParenteDirty());      // il manque la relation vers papa qui n'existe pas encore au fiscal
				}

				return null;
			}
		});

		// refresh des données de parenté du bébé (ici, on devrait créer la relation de parenté vers papa)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
				assertNotNull(bebe);
				tiersService.refreshParentesSurPersonnePhysique(bebe, false);
				return null;
			}
		});

		// vérification des relations depuis bébé (-> papa est arrivé)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
				assertNotNull(bebe);

				final Set<RapportEntreTiers> relParents = bebe.getRapportsSujet();
				assertNotNull(relParents);
				assertEquals(2, relParents.size());

				final List<RapportEntreTiers> sortedRelParents = new ArrayList<>(relParents);
				Collections.sort(sortedRelParents, new Comparator<RapportEntreTiers>() {
					@Override
					public int compare(RapportEntreTiers o1, RapportEntreTiers o2) {
						return Long.compare(o1.getObjetId(), o2.getObjetId());
					}
				});

				{
					final RapportEntreTiers relParent = sortedRelParents.get(0);
					assertNotNull(relParent);
					assertEquals(TypeRapportEntreTiers.PARENTE, relParent.getType());
					assertEquals((Long) idMaman, relParent.getObjetId());
					assertEquals(bebe.getNumero(), relParent.getSujetId());
					assertEquals(dateNaissanceBebe, relParent.getDateDebut());
					assertNull(relParent.getDateFin());
					assertFalse(relParent.isAnnule());
				}
				{
					final RapportEntreTiers relParent = sortedRelParents.get(1);
					assertNotNull(relParent);
					assertEquals(TypeRapportEntreTiers.PARENTE, relParent.getType());
					assertEquals((Long) idPapa, relParent.getObjetId());
					assertEquals(bebe.getNumero(), relParent.getSujetId());
					assertEquals(dateNaissanceBebe, relParent.getDateDebut());
					assertNull(relParent.getDateFin());
					assertFalse(relParent.isAnnule());
				}

				assertFalse(bebe.isParenteDirty());
				return null;
			}
		});
	}
}
