package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementErreur;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ArriveeEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	private MetierService metierService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		metierService = getBean(MetierService.class, "metierService");
	}

	@Override
	protected void truncateDatabase() throws Exception {
		// Même si en fait on ne veut pas d'indexation, il est important, dans les tests d'arrivée, que l'indexeur soit
		// vide avant de démarrer le test (puisqu'on recherche dans les non-habitants quelqu'un qui pourrait convenir...)
		final boolean wantIndexation = this.wantIndexationTiers;
		setWantIndexationTiers(true);
		try {
			super.truncateDatabase();
		}
		finally {
			setWantIndexationTiers(wantIndexation);
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
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// on s'assure que l'événement est détecté comme redondant
		doInNewTransactionAndSession(status -> {
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			final PersonnePhysique elle = addHabitant(noElle);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			addForPrincipal(etc.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			return null;
		});


		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});
		
		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);
		
		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});
		
		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	/**
	 * [SIFISC-28216] Ce test vérifie que l'arrivée à la même date valeur de deux personnes mariées mais avec :
	 * <ul>
	 *     <li>des permis différent (Monsieur = permis B, Madame = permis C)</li>
	 *     <li>des traitements des arrivées différées (arrivée de Madame traitée plusieurs jours après celle de Monsieur) </li>
	 * </ul>
	 * .... calcule bien le mode d'imposition comme 'ordinaire' sur le ménage commun.
	 */
	@Test//(timeout = 10000L)
	public void testArriveesCoupleAvecMonsieurPermisBMadamePersmisC() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2017, 7, 20);
		final RegDate dateMariage = date(2016, 6, 10);

		// mise en place civile (seule l'arrivée de Monsieur est disponible à cet instant)
		final MockServiceCivil mockServiceCivil = new MockServiceCivil() {
			@Override
			protected void init() {

				final RegDate naissanceLui = date(1970, 1, 1);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				addNationalite(lui, MockPays.Danemark, naissanceLui, null);
				addPermis(lui, TypePermis.SEJOUR, dateArrivee, null, false);

				final RegDate naissanceElle = date(1970, 1, 1);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(elle, MockPays.France, naissanceElle, null);
				// Madame est toujours à Genève et toujours sans permis connu)

				marieIndividus(lui, elle, dateMariage);

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCanton.Geneve.getNoOFS(), null));
			}
		};
		serviceCivil.setUp(mockServiceCivil);

		// le ménage existait avant leurs arrivées en raison d'un immeuble à Lausanne
		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			final PersonnePhysique elle = addHabitant(noElle);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			addForPrincipal(etc.getMenage(), date(2017, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Geneve);
			addForSecondaire(etc.getMenage(), date(2017, 6, 30), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			return null;
		});


		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// le ménage commun doit maintenant avoir un for principal à Lausanne avec le mode d'imposition Mixte (= Monsieur permis B + immeuble)
		doInNewTransactionAndSession(status -> {
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
			final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture());
			assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());

			return null;
		});

		// saisie différée du permis C et de l'arrivée de madame dans le service civil
		{
			final MockIndividu elle = mockServiceCivil.getIndividu(noElle);
			assertNotNull(elle);
			mockServiceCivil.addPermis(elle, TypePermis.ETABLISSEMENT, dateArrivee, null, false);

			final MockAdresse adrElle = mockServiceCivil.addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
			adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCanton.Geneve.getNoOFS(), null));
		}

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de madame
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement : le mode d'imposition du for du ménage doit corrigé en ordinaire
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
			assertNotNull(pp);

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
			assertNotNull(couple);
			assertNotNull(couple.getMenage());
			assertNotNull(couple.getPrincipal());
			assertEquals(pp.getId(), couple.getConjoint().getNumero());

			final MenageCommun mc = couple.getMenage();
			final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture());
			assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());

			return null;
		});
	}


	//SIFISC-11697
	@Test(timeout = 10000L)
	public void testArriveeMadameSurCoupleVaudoisCommuneDifferente() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2011, 10, 31);
		final RegDate dateArriveeCantonMonsieur = date(2009, 10, 31);
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArriveeCantonMonsieur, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			final PersonnePhysique elle = addHabitant(noElle);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			addForPrincipal(etc.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			return null;
		});



		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
			assertEquals(1,mc.getForsFiscaux().size());
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			return null;
		});
	}

	//SIFISC-11697
	@Test(timeout = 10000L)
	public void testArriveeMadameSurCoupleVaudoisMemeCommune() throws Exception {

		final long noLui = 246L;
		final long noElle = 3342L;
		final RegDate dateArrivee = date(2011, 10, 31);
		final RegDate dateArriveeCantonMonsieur = date(2009, 10, 31);
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArriveeCantonMonsieur, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			final PersonnePhysique elle = addHabitant(noElle);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			addForPrincipal(etc.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny, ModeImposition.SOURCE);
			return null;
		});



		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtElle);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noElle);
			assertNotNull(pp);

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
			assertNotNull(couple);
			assertNotNull(couple.getMenage());
			assertNotNull(couple.getPrincipal());
			assertEquals(pp.getId(), couple.getConjoint().getNumero());

			final MenageCommun mc = couple.getMenage();
			assertEquals(1,mc.getForsFiscaux().size());
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(MockCommune.Bussigny.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale().intValue());
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
			return null;
		});
	}

	/**
	 * [SIFISC-26927] Ce test vérifie que les motifs de fermeture/ouverture des fors fiscaux sont calculés correctement dans le cas suivant :
	 * <ol>
	 * <li>un ménage est composé d'un habitant et d'une non-habitante</li>
	 * <li>l'habitant déménage dans le canton à une date X => les fors fiscaux du ménage sont mis-à-jour en conséquences (OK)</li>
	 * <li>la non-habitante arrive dans le canton à une date Y <b>antérieure</b> à la date X de déménagement du couple,
	 * mais le traitement de l'arrivée se fait après le déménagement => l'événement d'arrivée part en erreur (OK)</li>
	 * <li>un opérateur annule le déménagement du ménage pour permettre le traitement de l'événement d'arrivée (OK)</li>
	 * <li>le traitement de l'événement d'arrivée va redéménager le ménage à la date X sur la commune du principal (OK),
	 * <b>mais doit utiliser les motifs de fermeture/ouverture des fors fiscaux "Déménagement"</b> et non pas "Arrive HS/HC" dans ce cas-là (KO avant le SIFISC-26927).</li>
	 * </ol>
	 */
	@Test//(timeout = 10000L)
	public void testArriveeMadameSurCoupleVaudoisAyantDemenageEntretemps() throws Exception {

		// cas du ménage n°115.666.96
		final long noMonsieur = 322847L;
		final long noMadame = 2450547L;
		final RegDate naissanceMonsieur = date(1970, 1, 1);
		final RegDate naissanceMadame = date(1970, 1, 1);

		final RegDate dateMariage = date(2017, 4, 3);
		final RegDate dateDemenagement = date(2017, 10, 1); // date déménagement de monsieur
		final RegDate dateArrivee = date(2017, 9, 5);       // date d'arrivée de madame

		setWantIndexationTiers(true);

		// adresses de monsieur : Bussigny puis Moudon le 01.10.2017
		// adresses de madame : France puis Moudon le 05.09.2017
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noMonsieur, naissanceMonsieur, "Tartempion", "François", true);
				final MockIndividu elle = addIndividu(noMadame, naissanceMadame, "Tartempion", "Françoise", false);
				addNationalite(lui, MockPays.Suisse, naissanceMonsieur, null);
				addNationalite(elle, MockPays.France, naissanceMadame, null);
				marieIndividus(lui, elle, dateMariage);

				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, naissanceMonsieur, dateDemenagement.getOneDayBefore());
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateDemenagement, null);
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		class Ids {
			Long monsieur;
			Long madame;
			Long menage;
		}
		final Ids ids = new Ids();

		// situation fiscal du ménage :
		//  - un for fiscal sur Bussigny, puis
		//  - un for fiscal sur Moudon
		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique monsieur = addHabitant(noMonsieur);
			final PersonnePhysique madame = addNonHabitant("Françoise", "Tartempion", naissanceMadame, Sexe.FEMININ);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
			final MenageCommun menage = etc.getMenage();
			addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny);
			addForPrincipal(menage, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Moudon);
			ids.monsieur = monsieur.getNumero();
			ids.madame = madame.getNumero();
			ids.menage = menage.getNumero();
			return null;
		});

		globalTiersIndexer.sync();

		// événement civil de l'arrivée de madame
		final long evtMadame = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de madame
		traiterEvenements(noMadame);

		// l'événement est en erreur parce que madame arrive avant le déménagement du ménage mais que l'arrivée est traitée après le déménagement.
		doInNewTransactionAndSession(status -> {

			// l'événement doit être en erreur
			final EvenementCivilEch evt = evtCivilDAO.get(evtMadame);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertEquals(1, erreurs.size());
			assertEquals("Il y a eu d'autres changements déjà pris en compte après l'arrivée", erreurs.iterator().next().getMessage());

			// rien ne doit avoir changé sur le ménage
			final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.menage);
			final List<ForFiscal> forsFiscaux = mc.getForsFiscauxSorted();
			assertEquals(2, forsFiscaux.size());
			assertForPrincipal(dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                   dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD,
			                   MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) forsFiscaux.get(0));
			assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD,
			                   MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, (ForFiscalPrincipalPP) forsFiscaux.get(1));
			return null;
		});

		// on annule le déménagement du ménage commun (dans la réalité, c'est un opérateur que le fait manuellement)
		doInNewTransaction(status -> {
			final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.menage);
			final List<ForFiscal> forsFiscaux = mc.getForsFiscauxSorted();

			// on annule le for sur Moudon
			forsFiscaux.get(1).setAnnule(true);

			// on réouvre le for sur Bussigny
			final ForFiscalPrincipal forBussigny = (ForFiscalPrincipal) forsFiscaux.get(0);
			forBussigny.setDateFin(null);
			forBussigny.setMotifFermeture(null);

			return null;
		});

		// on relance le traitement de l'arrivée de madame
		traiterEvenements(noMadame);

		// l'événement doit maintenant être traité
		doInNewTransactionAndSession(status -> {

			final EvenementCivilEch evt = evtCivilDAO.get(evtMadame);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			// le ménage commun doit être maintenant correctement déménagé sur Moudon (avec surtout les motifs DEMENAGEMENT_VD)
			final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.menage);
			final List<ForFiscalPrincipalPP> forsFiscaux = mc.getForsFiscauxPrincipauxActifsSorted();
			assertEquals(2, forsFiscaux.size());
			assertForPrincipal(dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                   dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD,
			                   MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, forsFiscaux.get(0));
			assertForPrincipal(dateDemenagement, MotifFor.DEMENAGEMENT_VD,
			                   MockCommune.Moudon, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, forsFiscaux.get(1));
			return null;
		});
	}

	/**
	 * [SIFISC-17204] Ce test vérifie que le déménagement dans une autre une autre commune d'un individu séparé alors qu'il est en ménage-commun lève bien une erreur (ce cas doit être traité manuellement).
	 */
	@Test(timeout = 10000L)
	public void testArriveeVDMadameSepareeSurCoupleMarieSeul() throws Exception {

		final long noMadame = 123456L;
		final RegDate naissanceMadame = date(1972, 9, 4);

		final RegDate dateMariage = date(2001, 10, 12);
		final RegDate dateSeparation = date(2008, 6, 1);
		final RegDate dateArrivee = RegDate.get(2010, 1, 1);
		final RegDate dateDemenagement = date(2015, 5, 27);

		setWantIndexationTiers(true);

		// adresses de madame : Lausanne puis Renens le 27.05.2015
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu elle = addIndividu(noMadame, naissanceMadame, "Tartempion", "Françoise", false);
				addNationalite(elle, MockPays.France, naissanceMadame, null);
				marieIndividu(elle, dateMariage);
				separeIndividu(elle, dateSeparation);

				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, dateArrivee, dateDemenagement.getOneDayBefore());
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, dateDemenagement, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
			}
		});

		class Ids {
			Long madame;
			Long menage;
		}
		final Ids ids = new Ids();

		// situation fiscal du ménage :
		//  - un for fiscal sur Lausanne
		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique madame = addHabitant(noMadame);
			final EnsembleTiersCouple etc = addEnsembleTiersCouple(madame, null, dateMariage, null);
			final MenageCommun menage = etc.getMenage();
			addForPrincipal(menage, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			ids.madame = madame.getNumero();
			ids.menage = menage.getNumero();
			return null;
		});

		globalTiersIndexer.sync();

		// événement civil de l'arrivée de madame
		final long evtMadame = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMadame);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de madame
		traiterEvenements(noMadame);

		// l'événement devrait être en erreur parce que la situation civile (séparé) ne correspond pas à la situation fiscale (mariée)
		doInNewTransactionAndSession(status -> {

			// l'événement doit être en erreur
			final EvenementCivilEch evt = evtCivilDAO.get(evtMadame);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertEquals(1, erreurs.size());
			assertEquals("La personne arrivante (n°" + FormatNumeroHelper.numeroCTBToDisplay(ids.madame) +
					             ") est seule au civil (SEPARE) mais appartient à un ménage-commun au fiscal (n°" + FormatNumeroHelper.numeroCTBToDisplay(ids.menage) +
					             "). Veuillez traiter l'événement manuellement.", erreurs.iterator().next().getMessage());

			return null;
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeLui);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeElle);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeLui);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeElle);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
			assertEquals(message, erreur.getMessage());
			return null;
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

				final MockAdresse adrAvantDepartLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, dateDepart);
				adrAvantDepartLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrFranceLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeLui.getOneDayBefore());
				adrFranceLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				adrFranceLui.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));


				final MockAdresse adrAvantDepartElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, dateDepart);
				adrAvantDepartElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final MockAdresse adrFranceElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE,null, null,null,null,null,MockPays.France, dateDepart.getOneDayAfter(), dateArriveeElle.addMonths(-1));
				adrFranceElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				adrFranceElle.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeElle, null);
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
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeLui);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});

		// événement civil de l'arrivée de madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeElle);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun", RegDateHelper.dateToDashString(dateArriveeElle),noElle);
			assertEquals(message, erreur.getMessage());
			return null;
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
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());


			return null;
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
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noInd);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noInd);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		});
	}

	@Test(timeout = 10000L)
	public void testArriveeHCAncienNonHabitantSIFISC6032() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);

			setWantIndexationTiers(true);

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
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
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
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	/**
	 * [SIFISC-28817] Ce test vérifie que Unireg ne crashe pas sur le traitement d'un événement d'arrivée avec les conditions suivantes :
	 * <ul>
	 * <li>un couple de non-habitants connus d'Unireg</li>
	 * <li>le principal est un ancien habitant, il possède donc un numéro d'individu</li>
	 * <li>le conjoint est un non-habitant, il ne possède pas de numéro d'individu</li>
	 * <li>le principal n'est pas suisse</li>
	 * <li>le principal arrive depuis HC/HS dans le canton de Vaud</li>
	 * </ul>
	 */
	@Test//(timeout = 10000L)
	public void testArriveeHCSurCoupleNonHabitants() throws Exception {

		try {
			final long noIndividuIgnacio = 126673246L;
			final long noIndividuUrsule = 126672111L;
			final RegDate dateNaissanceIgnacio = date(1956, 4, 23);
			final RegDate dateNaissanceUrsule = date(1956, 11, 7);
			final RegDate dateAchat = date(2010, 1, 15);
			final RegDate dateMariage = date(2000, 3, 2);
			final RegDate dateArrivee = date(2011, 10, 31);

			setWantIndexationTiers(true);

			// Les deux membres arrivent sur Vaud
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ignacio = addIndividu(noIndividuIgnacio, dateNaissanceIgnacio, "Dubois", "Ignacio", true);
					{
						final MockAdresse adresse = addAdresse(ignacio, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
						adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
						addNationalite(ignacio, MockPays.France, dateNaissanceIgnacio, null);
					}

					final MockIndividu ursule = addIndividu(noIndividuUrsule, dateNaissanceUrsule, "Dubois", "Ursule", false);
					{
						final MockAdresse adresse = addAdresse(ursule, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
						adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
						addNationalite(ursule, MockPays.Suisse, dateNaissanceUrsule, null);
					}
					marieIndividus(ignacio, ursule, dateMariage);
				}
			});

			// Mise en place du fiscal : un couple de non-habitants dont le principal est connu au civil
			final long menageId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique ignacio = addNonHabitant("Ignacio", "Dubois", dateNaissanceIgnacio, Sexe.MASCULIN);
				ignacio.setNumeroIndividu(noIndividuIgnacio);   // <-- Ignacio est un ancien habitant
				final PersonnePhysique ursule = addNonHabitant("Ursule", "Dubois", dateNaissanceUrsule, Sexe.FEMININ);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(ignacio, ursule, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, dateAchat, MotifFor.ACHAT_IMMOBILIER, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addForSecondaire(menage, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Vaulion.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return menage.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée d'Ignacio (Ursule est encore non-habitante)
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuIgnacio);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividuIgnacio);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final MenageCommun menage = (MenageCommun) tiersService.getTiers(menageId);
				assertNotNull(menage);

				// le for fiscal du ménage a été déplacé à Echallens
				final ForFiscalPrincipal ffpMenage = menage.getForFiscalPrincipalAt(null);
				assertNotNull(ffpMenage);
				assertEquals(MotifFor.ARRIVEE_HC, ffpMenage.getMotifOuverture());
				assertEquals(Integer.valueOf(MockCommune.Echallens.getNoOFS()), ffpMenage.getNumeroOfsAutoriteFiscale());
				assertNull(ffpMenage.getDateFin());

				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	@Test
	public void testArriveeNonHabitantNAVS13() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);
			final String navs13 = "3218526549783";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, null, "", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				pp.setNumeroAssureSocial(navs13);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals((Long) ppId, pp.getNumero());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	//SIFISC_12951
	@Test
	public void testArriveeNonHabitantNAVS13_SansSexe_sans_DateNaisance() throws Exception {

		try {
			final long noIndividu = 695860;
			final RegDate dateNaissance = date(1982, 6, 14);
			final RegDate dateArrivee = date(2014, 7, 10);
			final String navs13 = "7565683992644";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bocktaels", "Jérémie Daniel Gabriel", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.France, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

				}
			});

			// Mise en place du fiscal
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Bocktaels", "Chollet", null, null);
				pp.setNumeroAssureSocial(navs13);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Zurich.GloriaStrasse);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals((Long) ppId, pp.getNumero());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	//SIFISC_12951
	@Test
	public void testArriveeNonHabitantNAVS13_SansSexe() throws Exception {

		try {
			final long noIndividu = 695860;
			final RegDate dateNaissance = date(1982, 6, 14);
			final RegDate dateArrivee = date(2014, 7, 10);
			final String navs13 = "7565683992644";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Bocktaels", "Jérémie Daniel Gabriel", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.France, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

				}
			});

			// Mise en place du fiscal
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Bocktaels", "Chollet", dateNaissance, null);
				pp.setNumeroAssureSocial(navs13);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Zurich.GloriaStrasse);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals((Long) ppId, pp.getNumero());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	@Test
	public void testArriveeNonHabitantNAVS13Doublon() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);
			final String navs13 = "3218526549783";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, null, "", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId1 = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				pp.setNumeroAssureSocial(navs13);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			// Mise en place du fiscal
			final long ppId2 = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Hortense", "Hipelnik", dateNaissance, Sexe.FEMININ);
				pp.setNumeroAssureSocial(navs13);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				List<EvenementCivilEchErreur> erreurs = new ArrayList<>(evt.getErreurs());
				assertEquals(1,erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.get(0);
				assertEquals("Plusieurs tiers non-habitants assujettis potentiels trouvés ("+FormatNumeroHelper.numeroCTBToDisplay(ppId1)+
						", "+FormatNumeroHelper.numeroCTBToDisplay(ppId2)+")",erreur.getMessage());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}


	@Test
	public void testArriveeNonHabitantSansNAVS13Doublon() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);
			final String navs13 = "3218526549783";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Chollet", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId1 = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			// Mise en place du fiscal
			final long ppId2 = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				List<EvenementCivilEchErreur> erreurs = new ArrayList<>(evt.getErreurs());
				assertEquals(1,erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.get(0);
				assertEquals("Plusieurs tiers non-habitants assujettis potentiels trouvés ("+FormatNumeroHelper.numeroCTBToDisplay(ppId1)+
						", "+FormatNumeroHelper.numeroCTBToDisplay(ppId2)+")",erreur.getMessage());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}



	@Test
	public void testArriveeNonHabitantNAVS13Different() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);
			final String navs13 = "3218526549783";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Chollet", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId1 = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				pp.setNumeroAssureSocial("9518526549783");
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
				List<EvenementCivilEchErreur> erreurs = new ArrayList<>(evt.getErreurs());
				assertEquals(1,erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.get(0);
				assertEquals("Le non-habitant trouvé ("+FormatNumeroHelper.numeroCTBToDisplay(ppId1)+") a un numero d'assure social qui ne correspond pas à celui de l'individu de l'évènement",erreur.getMessage());
				return null;
			});
		}
		finally {
			globalTiersIndexer.overwriteIndex();
		}
	}

	@Test
	public void testArriveeNonHabitantNAVS13Absent() throws Exception {

		try {
			final long noIndividu = 126673246L;
			final RegDate dateNaissance = date(1956, 4, 23);
			final RegDate dateMariage = date(2010, 1, 15);
			final RegDate dateArrivee = date(2011, 10, 31);
			final String navs13 = "3218526549783";
			setWantIndexationTiers(true);

			// le p'tit nouveau
			serviceCivil.setUp(new DefaultMockServiceCivil(false) {
				@Override
				protected void init() {
					final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Chollet", "Ignacio", true);
					final MockAdresse adresse = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateArrivee, null);
					adresse.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
					addNationalite(ind, MockPays.Suisse, dateNaissance, null);

					ind.setNouveauNoAVS(navs13);

					marieIndividu(ind, dateMariage);
				}
			});

			// Mise en place du fiscal
			final long ppId = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Ignacio", "Chollet", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(pp, dateArrivee.addYears(-5), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Geneve, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, dateArrivee.addYears(-5), null, MockRue.Geneve.AvenueGuiseppeMotta);
				return pp.getNumero();
			});

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateArrivee);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividu);

			// vérification du traitement
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evtId);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				assertNotNull(pp);
				assertEquals((Long) ppId, pp.getNumero());
				return null;
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

			setWantIndexationTiers(true);

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
			final long ids[] = doInNewTransactionAndSession(status -> {
				final PersonnePhysique ppMadame = addNonHabitant("Jolias", "Virginie", dateNaissanceMadame, Sexe.FEMININ);
				final PersonnePhysique ppMonsieur = addNonHabitant("Jolias", "Virgil", dateNaissanceMonsieur, Sexe.MASCULIN);
				// ancien habitant: ils ont un numéro d'individu
				ppMadame.setNumeroIndividu(noIndividuMadame);
				ppMonsieur.setNumeroIndividu(noIndividuMonsieur);
				final EnsembleTiersCouple etc = addEnsembleTiersCouple(ppMonsieur, ppMadame, dateMariage, null);
				addForPrincipal(etc.getMenage(), dateMariage,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepartCouple.getOneDayBefore(), MotifFor.DEPART_HS, MockPays.France);
				addForPrincipal(etc.getMenage(), date(2010,12,1),MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				return new long[] {ppMonsieur.getNumero(), ppMadame.getNumero(), etc.getMenage().getNumero()};
			});

			final long idMonsieur = ids[0];
			final long idMadame = ids[1];

			globalTiersIndexer.sync();

			// événement d'arrivée
			final long evtId = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14532L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateRetourMoniseurSeul);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuMonsieur);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividuMonsieur);

			// on s'assure que Madame n'est pas habitante ( et que monsieur l'est au passage..)
			doInNewTransactionAndSession(status -> {
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
			});


			// événement d'arrivée
			final long evt2Id = doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(14533L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateRetourMadameSeule);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividuMadame);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			});

			// traitement de l'événement
			traiterEvenements(noIndividuMadame);

			// Madame arrive finallement 1 mois apres monsieur
			doInNewTransactionAndSession(status -> {
				final EvenementCivilEch evt = evtCivilDAO.get(evt2Id);
				assertNotNull(evt);
				assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());

				final PersonnePhysique madame = (PersonnePhysique) tiersService.getTiers(idMadame);
				assertNotNull(madame);
				assertTrue("Madame doit être habitante", madame.isHabitantVD());
				return null;
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

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});

		globalTiersIndexer.sync();
		// création de l'événement civil pour l'arrivée de Madame
		final long evtElle = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeElle);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noElle);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de madame
		traiterEvenements(noElle);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		});



		// mise en place civile Monsieur
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final RegDate naissanceElle = date(1971, 6, 21);
				final MockIndividu elle = addIndividu(noElle, naissanceElle, "Tartempion", "Françoise", false);
				addNationalite(elle, MockPays.France, naissanceElle, null);

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeElle, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));

				final RegDate naissanceLui = date(1970, 3, 12);
				final MockIndividu lui = addIndividu(noLui, naissanceLui, "Tartempion", "François", true);
				addNationalite(lui, MockPays.France, naissanceLui, null);

				marieIndividus(lui,elle, dateMariage);
				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArriveeLui, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			}
		});


		globalTiersIndexer.sync();
		// événement civil de l'arrivée de monsieur

		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(321674L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArriveeLui);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification de l'état de traitement de l'événement
		doInNewTransactionAndSession(status -> {
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, debutHS, MotifFor.DEPART_HS, MockPays.France);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état de l'événement
		doInNewTransactionAndSession(status -> {
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

				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
				adrLui.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Bussigny.getNoOFS(), null));

				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateArrivee, null);
				adrElle.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD,MockCommune.Bussigny.getNoOFS(), null));
			}
		});

		doInNewTransactionAndSession((TransactionCallback<Long>) status -> {
			final PersonnePhysique lui = addHabitant(noLui);
			final PersonnePhysique elle = addHabitant(noElle);

			final EnsembleTiersCouple etc = addEnsembleTiersCouple(lui, elle, dateDebutRapport, null);
			addForPrincipal(etc.getMenage(), dateDebutRapport, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			ids.monsieur = lui.getNumero();
			ids.madame = elle.getNumero();
			return null;
		});


		// création de l'événement civil pour l'arrivée de monsieur
		final long evtLui = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée de monsieur
		traiterEvenements(noLui);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, debutHS, MotifFor.DEPART_HS, MockPays.France);
			pp.setHabitant(true);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état de l'événement
		doInNewTransactionAndSession(status -> {
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
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			assertNotNull(pp);
			assertTrue(pp.isHabitantVD());

			final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());

			return null;
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
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
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

			final ForFiscalPrincipalPP ffp = couple.getMenage().getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());

			return null;
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
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
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

			final ForFiscalPrincipalPP ffp = couple.getMenage().getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());

			return null;
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
		final long ppMenage = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, dateAchat, null, null, null, MockPays.EtatsUnis, MotifRattachement.DIPLOMATE_ETRANGER);
			addForSecondaire(mc, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

			return mc.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification du for principal vaudois suite à l'arrivée
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch ech = evtCivilDAO.get(evtArrivee);
			assertNotNull(ech);
			assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());

			final MenageCommun mc = (MenageCommun) tiersDAO.get(ppMenage);
			assertNotNull(mc);

			final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateArrivee, ffp.getDateDebut());
			assertEquals(MotifFor.ARRIVEE_HC, ffp.getMotifOuverture());
			assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
			return null;
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
		final long idMaman = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, false, status -> {
			final PersonnePhysique maman = addHabitant(indMaman);
			addForPrincipal(maman, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
			return maman.getNumero();
		});

		// création de l'événement d'arrivée du bébé
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(indBebe);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(indBebe);

		// vérification de l'état des flags parenteDirty sur les tiers existants
		doInNewTransactionAndSession(status -> {
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
		});

		// arrivée de papa (en raccourci -> on crée directement le gars en base)
		// le recalcul des parentés est activé mais ne devrait rien changer (car la liaison entre bébé et papa est inconnue fiscalement pour le moment)
		final long idPapa = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique papa = addHabitant(indPapa);
			addForPrincipal(papa, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
			return papa.getNumero();
		});

		// vérification des relations depuis bébé (-> rien n'a changé)
		doInNewTransactionAndSession(status -> {
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
		});

		// refresh des données de parenté du bébé (ici, on devrait créer la relation de parenté vers papa)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
			assertNotNull(bebe);
			tiersService.refreshParentesSurPersonnePhysique(bebe, false);
			return null;
		});

		// vérification des relations depuis bébé (-> papa est arrivé)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique bebe = tiersService.getPersonnePhysiqueByNumeroIndividu(indBebe);
			assertNotNull(bebe);

			final Set<RapportEntreTiers> relParents = bebe.getRapportsSujet();
			assertNotNull(relParents);
			assertEquals(2, relParents.size());

			final List<RapportEntreTiers> sortedRelParents = new ArrayList<>(relParents);
			sortedRelParents.sort(Comparator.comparingLong(RapportEntreTiers::getObjetId));

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
		});
	}

	@Test(timeout = 10000L)
	public void testArriveeCelibataireAvecDecisionAci() throws Exception {

		final long noIndividu = 1057790L;
		final RegDate dateArrivee = date(2011, 10, 1);

		class Ids {
			Long virginie;
		}
		final Ids ids = new Ids();

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
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addDecisionAci(pp, date(2008, 10, 1), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			ids.virginie = pp.getNumero();
			return null;
		});

		// événement d'arrivée
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// on s'assure que l'événement est détecté comme redondant
		doInNewTransactionAndSession(status -> {
			PersonnePhysique virginie = (PersonnePhysique)tiersDAO.get(ids.virginie);
			assertNotNull(virginie);
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
					FormatNumeroHelper.numeroCTBToDisplay(virginie.getNumero()));
			assertEquals(message, erreur.getMessage());
			return null;
		});
	}


	@Test
	public void testArriveeCoupleDecisionAciSurPrincipal() throws Exception {
		final long noIndividuLui = 138946274L;
		final long noIndividuElle = 138946275L;
		final RegDate dateNaissance = RegDate.get().addYears(-50);
		final RegDate dateArrivee = RegDate.get().addDays(-20);
		class Ids {
			Long monsieur;
			Long madame;
		}
		final Ids ids = new Ids();


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

		// Cette personne est déjà enregistrée dans la base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique ppLui = addHabitant(noIndividuLui);
			addHabitant(noIndividuElle);
			addDecisionAci(ppLui, date(2008, 10, 1), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			ids.monsieur = ppLui.getNumero();
			return null;
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
			final PersonnePhysique ppLui = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuLui);
			assertNotNull(ppLui);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
					FormatNumeroHelper.numeroCTBToDisplay(ppLui.getNumero()));
			assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test
	public void testArriveeCoupleDecisionAciSurConjoint() throws Exception {
		final long noIndividuLui = 138946274L;
		final long noIndividuElle = 138946275L;
		final RegDate dateNaissance = RegDate.get().addYears(-50);
		final RegDate dateArrivee = RegDate.get().addDays(-20);
		class Ids {
			Long monsieur;
			Long madame;
		}
		final Ids ids = new Ids();


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

		// Cette personne est déjà enregistrée dans la base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique ppLui = addHabitant(noIndividuLui);
			final PersonnePhysique ppElle = addHabitant(noIndividuElle);
			addDecisionAci(ppElle, date(2008, 10, 1), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			ids.monsieur = ppLui.getNumero();
			ids.madame = ppElle.getNumero();
			return null;
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividuLui);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividuLui);

		// vérification des résultats
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
			final PersonnePhysique ppLui = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuLui);
			final PersonnePhysique ppElle = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuElle);
			assertNotNull(ppLui);
			assertNotNull(ppElle);
			assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			assertNotNull(erreurs);
			assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) a un conjoint (%s) qui fait l'objet d'une décision ACI",
					FormatNumeroHelper.numeroCTBToDisplay(ppLui.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(ppElle.getNumero()));
			assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure : individu seul
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourCelibataire() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateArrivee = date(2016, 4, 6);

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				final MockAdresse adresseAvant = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateArrivee.getOneDayBefore());
				adresseAvant.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.RomainmotierEnvy.getNoOFS(), null));
				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateArrivee.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(pp, dateArrivee, MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertTrue(pp.isHabitantVD());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : pp.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(4, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(2);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(3);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (avant 2 ans) : individu seul
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourCelibataireMoinsDeDeuxAnsEcart() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateDepart = date(2015, 7, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il ne devrait pas y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isAfter(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertTrue(pp.isHabitantVD());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : pp.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(4, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(2);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(3);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (avant 2 ans) : individu seul
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourCelibataireMoinsDeDeuxAnsEcartMaisMauvaiseCommune() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateDepart = date(2015, 7, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il ne devrait pas y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isAfter(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.LIsle.getNoOFS(), null));       // L'Isle n'est pas Aubonne...
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				Assert.assertEquals("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de communes vaudoises différentes.", erreur.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertFalse(pp.isHabitantVD());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : pp.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(2, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (après 2 ans) : individu seul
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourCelibatairePlusDeDeuxAnsEcart() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateDepart = date(2014, 2, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il devrait y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isBefore(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				Assert.assertEquals("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de la date de départ, trop vieille.", erreur.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertFalse(pp.isHabitantVD());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : pp.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(2, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure : ménage commun
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourMenageCommun() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateMariage = date(2000, 7, 13);
		final RegDate dateArrivee = date(2016, 4, 6);

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				final MockAdresse adresseAvant = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateArrivee.getOneDayBefore());
				adresseAvant.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.RomainmotierEnvy.getNoOFS(), null));
				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateArrivee.getOneDayBefore(), MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(mc, dateArrivee, MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return mc.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);
				Assert.assertFalse(mc.isAnnule());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : mc.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(4, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(2);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(3);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (avant 2 ans) : menage commun
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourMenageCommunMoinsDeDeuxAnsEcart() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateMariage = date(2000, 7, 13);
		final RegDate dateDepart = date(2015, 7, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il ne devrait pas y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isAfter(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(mc, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return mc.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);
				Assert.assertFalse(mc.isAnnule());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : mc.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(4, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(2);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(3);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (avant 2 ans) : ménage commun
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourMenageCommunMoinsDeDeuxAnsEcartMaisMauvaiseCommune() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateMariage = date(2000, 7, 13);
		final RegDate dateDepart = date(2015, 7, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il ne devrait pas y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isAfter(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateArrivee.getOneDayBefore());

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.LIsle.getNoOFS(), null));       // L'Isle n'est pas Aubonne
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(mc, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return mc.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				Assert.assertEquals("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de communes vaudoises différentes.", erreur.getMessage());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);
				Assert.assertFalse(mc.isAnnule());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : mc.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(2, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-5451] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (après 2 ans) : ménage commun
	 */
	@Test
	public void testRattrapageDepartPaysInconnuAvecArriveeVaudoisePourMenageCommunPlusDeDeuxAnsEcart() throws Exception {

		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateMariage = date(2000, 7, 13);
		final RegDate dateDepart = date(2014, 2, 3);
		final RegDate dateArrivee = date(2016, 4, 6);
		Assert.assertTrue("Il devrait y avoir plus de 2 ans entre les deux dates", dateDepart.addYears(2).isBefore(dateArrivee));

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(mc, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			return mc.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				Assert.assertEquals("Tentative de rattrapage d'un départ pour pays inconnu avortée en raison de la date de départ, trop vieille.", erreur.getMessage());

				final MenageCommun mc = (MenageCommun) tiersDAO.get(mcId);
				Assert.assertNotNull(mc);
				Assert.assertFalse(mc.isAnnule());

				final List<ForFiscalPrincipalPP> ffps = new ArrayList<>();
				for (ForFiscal ff : mc.getForsFiscaux()) {
					if (ff instanceof ForFiscalPrincipalPP) {
						ffps.add((ForFiscalPrincipalPP) ff);
					}
				}
				ffps.sort(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()));
				Assert.assertEquals(2, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateMariage, ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	/**
	 * [SIFISC-22358] Rattrapage d'un départ HS vers pays inconnu avec une arrivée vaudoise ultérieure (dans les deux ans) en présence
	 * de for secondaire...
	 */
	@Test
	public void testRattrapagePaysInconnuAvecArriveeVaudoiseEnPresenceDeForSecondaire() throws Exception {
		final long noIndividu = 45115L;
		final RegDate dateNaissance = date(1965, 8, 25);
		final RegDate dateDepart = date(2014, 2, 3);
		final RegDate dateArrivee = date(2014, 2, 7);

		// mise en place civile (on connaissait la personne avant)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Rastapopoulos", "Magdalena", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateNaissance, dateDepart);

				final MockAdresse adresseApres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Romainmotier.CheminDuCochet, null, dateArrivee, null);
				adresseApres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addEtatCivil(individu, dateNaissance, TypeEtatCivil.CELIBATAIRE);
			}
		});

		// mise en place fiscale (on part du point où le départ pour pays inconnu a déjà été enregistré, car on avait reçu un départ sans destination...)
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Aubonne, ModeImposition.INDIGENT);
			addForPrincipal(pp, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.PaysInconnu, ModeImposition.ORDINAIRE);
			addForSecondaire(pp, dateDepart.addDays(-10), MotifFor.ACHAT_IMMOBILIER, MockCommune.ChateauDoex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// événement civil d'arrivée
		final long evtArrivee = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(3273426L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateArrivee);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.ARRIVEE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(evtArrivee);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertTrue(pp.isHabitantVD());

				// les fors principaux
				final List<ForFiscalPrincipalPP> ffps = pp.getForsFiscaux().stream()
						.filter(ForFiscalPrincipalPP.class::isInstance)
						.map(ForFiscalPrincipalPP.class::cast)
						.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()))
						.collect(Collectors.toList());
				Assert.assertEquals(4, ffps.size());
				{
					final ForFiscalPrincipalPP ffp = ffps.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateArrivee.getOneDayBefore(), ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(1);
					Assert.assertNotNull(ffp);
					Assert.assertFalse(ffp.isAnnule());
					Assert.assertEquals(dateArrivee, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(2);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(dateDepart, ffp.getDateFin());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.INDIGENT, ffp.getModeImposition());
				}
				{
					final ForFiscalPrincipalPP ffp = ffps.get(3);
					Assert.assertNotNull(ffp);
					Assert.assertTrue(ffp.isAnnule());
					Assert.assertEquals(dateDepart.getOneDayAfter(), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.PaysInconnu.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}

				// le for secondaire ne doit pas avoir bougé
				final List<ForFiscalSecondaire> ffss = pp.getForsFiscaux().stream()
						.filter(ForFiscalSecondaire.class::isInstance)
						.map(ForFiscalSecondaire.class::cast)
						.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(new DateRangeComparator<>()))
						.collect(Collectors.toList());
				Assert.assertEquals(1, ffss.size());
				final ForFiscalSecondaire ffs = ffss.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateDepart.addDays(-10), ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.ChateauDoex.getNoOFS(), ffs.getNumeroOfsAutoriteFiscale());
			}
		});
	}
}
