package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchSourceHelper;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementErreur;
import ch.vd.uniregctb.type.TypePermis;

@SuppressWarnings("JavaDoc")
public class EvenementCivilEchGrappeProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testAnnulationTotale() throws Exception {

		final long noIndividu = 2367326L;
		final RegDate dateNaissance = date(1980, 12, 25);
		final RegDate dateArrivee = date(1998, 12, 25);
		final RegDate dateMariage = dateArrivee.addMonths(3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Sorel", "Julien", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// réception des événements civils de mariage, de correction de mariage et d'annulation
		final long noEvtMariage = 23673256L;
		final long noEvtAnnulation = 26726222324L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtMariage);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateMariage);
					evt.setEtat(EtatEvenementCivil.EN_ERREUR);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);

					final EvenementCivilEchErreur erreur = new EvenementCivilEchErreur();
					erreur.setMessage("Toto");
					erreur.setType(TypeEvenementErreur.ERROR);
					evt.setErreurs(new HashSet<>(Collections.singletonList(erreur)));

					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtAnnulation);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.ANNULATION);
					evt.setDateEvenement(dateMariage);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtMariage);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});

		// traitement des événements de l'individu
		traiterEvenements(noIndividu);

		// vérification que les événements ont bien été marqués comme redondant sans traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtMariage);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente.", evt.getCommentaireTraitement());
					Assert.assertEquals(0, evt.getErreurs().size());
				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtAnnulation);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.REDONDANT, evt.getEtat());
					Assert.assertEquals("Groupe d'événements annulés alors qu'ils étaient encore en attente.", evt.getCommentaireTraitement());
					Assert.assertEquals(0, evt.getErreurs().size());
				}

				// et fiscalement, rien ne doit avoir changé
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testAnnulationNonTotale() throws Exception {

		final long noIndividu = 2367326L;
		final RegDate dateNaissance = date(1980, 12, 25);
		final RegDate dateArrivee = date(1998, 12, 25);
		final RegDate dateMariage = dateArrivee.addMonths(3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Sorel", "Julien", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// réception des événements civils de mariage, de correction de mariage et d'annulation
		final long noEvtMariage = 23673256L;
		final long noEvtCorrection = 32536263L;
		final long noEvtCorrectionSurCorrection = 451454512L;
		final long noEvtAnnulation = 26726222324L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtMariage);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateMariage);
					evt.setEtat(EtatEvenementCivil.EN_ERREUR);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);

					final EvenementCivilEchErreur erreur = new EvenementCivilEchErreur();
					erreur.setMessage("Toto");
					erreur.setType(TypeEvenementErreur.ERROR);
					evt.setErreurs(new HashSet<>(Collections.singletonList(erreur)));

					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtCorrection);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(dateMariage.addDays(-10));
					evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtMariage);
					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtCorrectionSurCorrection);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(dateMariage.addDays(-10));
					evt.setEtat(EtatEvenementCivil.TRAITE);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtCorrection);
					evt.setCommentaireTraitement("Tralala");
					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtAnnulation);
					evt.setType(TypeEvenementCivilEch.MARIAGE);
					evt.setAction(ActionEvenementCivilEch.ANNULATION);
					evt.setDateEvenement(dateMariage);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtCorrectionSurCorrection);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});

		// traitement des événements de l'individu
		traiterEvenements(noIndividu);

		// vérification que les événements ont bien été marqués comme redondant sans traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtMariage);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
					Assert.assertEquals(1, evt.getErreurs().size());

					final EvenementCivilEchErreur erreur = evt.getErreurs().iterator().next();
					Assert.assertNotNull(erreur);
					Assert.assertEquals("L'individu principal n'est ni marié ni pacsé dans le civil", erreur.getMessage());
				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtCorrection);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", evt.getCommentaireTraitement());
					Assert.assertEquals(0, evt.getErreurs().size());
				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtCorrectionSurCorrection);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
					Assert.assertEquals("Tralala", evt.getCommentaireTraitement());
					Assert.assertEquals(0, evt.getErreurs().size());
				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtAnnulation);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, evt.getEtat());
					Assert.assertNull(evt.getCommentaireTraitement());
					Assert.assertEquals(0, evt.getErreurs().size());
				}

				// et fiscalement, rien ne doit avoir changé
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());

				return null;
			}
		});
	}

	//SIFISC-15545
	@Test(timeout = 10000L)
	public void testOrdonnancement1Deces1AnnulationDeces1NouveauDeces() throws Exception {

		final long noIndividu = 2367326L;
		final RegDate dateNaissance = date(1980, 12, 25);
		final RegDate dateArrivee = date(1998, 12, 25);
		final RegDate dateDeces = date(2015, 3, 25);
		final RegDate dateSecondDeces = date(2015, 4, 25);
		final RegDate dateAnnulationPremierDeces = date(2015, 4, 25);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Sorel", "Julien", Sexe.MASCULIN);
				individu.setDateDeces(dateDeces);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminDesClos, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// réception des événements civils de mariage, de correction de mariage et d'annulation
		final long noEvtPremierDeces = 23673256L;
		final long noEvtAnnulationPremierDeces = 32536200L;
		final long noEvtSecondDeces = 32536263L;
				doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtPremierDeces);
					evt.setType(TypeEvenementCivilEch.DECES);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDeces);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);

					hibernateTemplate.merge(evt);
				}

				return null;
			}
		});

		// traitement des événements de l'individu
		traiterEvenements(noIndividu);
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtSecondDeces);
					evt.setType(TypeEvenementCivilEch.DECES);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDeces);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}

				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtAnnulationPremierDeces);
					evt.setType(TypeEvenementCivilEch.DECES);
					evt.setAction(ActionEvenementCivilEch.ANNULATION);
					evt.setDateEvenement(dateDeces);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});


		// traitement des événements de l'individu
		traiterEvenements(noIndividu);
		// vérification que les événements ont bien été marqués comme redondant sans traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtPremierDeces);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtSecondDeces);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				}
				{
					final EvenementCivilEch evt = evtCivilDAO.get(noEvtAnnulationPremierDeces);
					Assert.assertNotNull(evt);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				}

				// et fiscalement, rien ne doit avoir changé
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertEquals(dateDeces,pp.getDateDeces());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementGroupeCorrectif() throws Exception {

		final long noIndividu = 282364L;
		final RegDate dateNaissance = date(1990, 9, 4);
		final RegDate dateDepartOriginelle = date(2012, 5, 23);
		final RegDate dateDepartCorrecte = date(2012, 6, 3);
		final RegDate dateArrivee = dateDepartCorrecte.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Bovary", "Emma", Sexe.FEMININ);
				addNationalite(individu, MockPays.France, dateNaissance, null);
				addPermis(individu, TypePermis.ETABLISSEMENT, dateNaissance, null, false);

				final MockAdresse avant = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, dateDepartCorrecte);
				avant.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(), null));

				final MockAdresse apres = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.CheminTraverse, null, dateDepartCorrecte.addDays(1), null);
				apres.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Echallens.getNoOFS(), null));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});

		final long noEvtDepartOriginel = 8181513L;
		final long noEvtCorrectionDepart = 4215152L;
		final long noEvtArrivee = 454121L;

		// création des événements civils (sans correction pour le moment)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtDepartOriginel);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDepartOriginelle);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtArrivee);
					evt.setType(TypeEvenementCivilEch.ARRIVEE);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateArrivee);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});

		// on traite les événements civils -> le départ doit partir en erreur (pas d'adresse qui se termine à la bonne date) et l'arrivée doit passer en attente
		traiterEvenements(noIndividu);

		// vérification de la première étape
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtDepartOriginel);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, ech.getEtat());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(1, erreurs.size());

					final EvenementCivilEchErreur erreur = erreurs.iterator().next();
					Assert.assertNotNull(erreur);
					Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.", erreur.getMessage());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtArrivee);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, ech.getEtat());
				}
				return null;
			}
		});

		// arrivée d'un nouvel événement civil (la correction du départ)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtCorrectionDepart);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(dateDepartCorrecte);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtDepartOriginel);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});

		// relance des événements civils
		traiterEvenements(noIndividu);

		// maintenant, tout doit avoir été traité, reste à le vérifier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtDepartOriginel);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertEquals("Ignoré car départ vaudois : la nouvelle commune de résidence Aubonne est toujours dans le canton. Evénement et correction(s) pris en compte ensemble.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtCorrectionDepart);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtArrivee);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertNull(ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}

				// et on vérifie aussi que les fors de la personne physique ont été modifiés correctement
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());

				return null;
			}
		});
	}

	/**
	 * Le but ici est de tester le cas sorti de l'immagination de R. Carbo :
	 * <ul>
	 *     <li>arrivée en erreur au 1.1.2011</li>
	 *     <li>correction de cette arrivée (en attente) au 1.1.2012</li>
	 *     <li>correction encore (forcée par erreur) au 1.5.2011</li>
	 * </ul>
	 * <p/> --> une fois le traitement en grappes activé, quelle est la date du for créé ? Le 1.5.2011 bien-sûr, même si elle vient d'un événement dans un état final
	 */
	@Test(timeout = 10000L)
	public void testDateTraitementSiDernierElementDansGrappeEstForce() throws Exception {

		final long noIndividu = 2674325324L;
		final RegDate dateArriveeInitiale = date(2011, 1, 1);
		final RegDate datePremiereCorrection = date(2012, 1, 1);
		final RegDate dateCorrectionForcee = date(2011, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1970, 5, 3), "Piccolina", "Antonia", Sexe.FEMININ);
				addNationalite(ind, MockPays.Suisse, null, null);

				final MockAdresse adr = addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateCorrectionForcee, null);
				adr.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Danemark.getNoOFS(), null));
			}
		});

		final long idArriveeInitiale = 23725L;
		final long idPremiereCorrection = 26253343L;
		final long idCorrectionForcee = 423637252L;

		// mise en place fiscale -> les événements civils
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idArriveeInitiale);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateArriveeInitiale);
					ech.setEtat(EtatEvenementCivil.EN_ERREUR);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(null);
					hibernateTemplate.merge(ech);
				}
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idPremiereCorrection);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(datePremiereCorrection);
					ech.setEtat(EtatEvenementCivil.EN_ATTENTE);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idArriveeInitiale);
					hibernateTemplate.merge(ech);
				}
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idCorrectionForcee);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateCorrectionForcee);
					ech.setEtat(EtatEvenementCivil.FORCE);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idPremiereCorrection);
					hibernateTemplate.merge(ech);
				}
				return null;
			}
		});

		// traitement des événements civils
		traiterEvenements(noIndividu);

		// vérification du for créé pour le nouveau contribuable
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// d'abord on vérifie l'état des événements civils (TRAITE, sauf pour le FORCE qui le reste)
				{
					final EvenementCivilEch ech = evtCivilDAO.get(idArriveeInitiale);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertEquals("Evénement et correction(s) pris en compte ensemble.", ech.getCommentaireTraitement());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(idPremiereCorrection);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", ech.getCommentaireTraitement());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(idCorrectionForcee);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.FORCE, ech.getEtat());
					Assert.assertNull(ech.getCommentaireTraitement());
				}

				// le contribuable ?
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateCorrectionForcee, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bussigny.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementAVerifier() throws Exception {

		final long noIndividu = 367325L;
		final RegDate dateNaissance = date(1989, 6, 20);
		final RegDate dateArrivee = date(2008, 3, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Piccolino", "Antonio", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, null, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
			}
		});

		// création de l'évément fiscal corrigé de séparation
		final long idEvtAnnonce = 43843672L;
		final long idEvtCorrection = 44582645L;
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idEvtAnnonce);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(null);
					hibernateTemplate.merge(ech);
				}
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idEvtCorrection);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idEvtAnnonce);
					hibernateTemplate.merge(ech);
				}
				return null;
			}
		});

		// traitement de l'événement civil d'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état des événements civils après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = evtCivilDAO.get(idEvtAnnonce);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.A_VERIFIER, ech.getEtat());
					Assert.assertEquals("Evénement et correction(s) pris en compte ensemble.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(1, erreurs.size());

					final EvenementCivilEchErreur erreur = erreurs.iterator().next();
					Assert.assertNotNull(erreur);
					Assert.assertEquals(TypeEvenementErreur.WARNING, erreur.getType());
					Assert.assertEquals("Ancienne adresse avant l'arrivée inconnue : veuillez indiquer le motif d'ouverture du for principal.", erreur.getMessage());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(idEvtCorrection);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.A_VERIFIER, ech.getEtat());
					Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}
				return null;
			}
		});

		// vérification de la création de la personne physique arrivée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
				Assert.assertEquals(dateArrivee, ffp.getDateDebut());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertNull(ffp.getDateFin());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementEnErreur() throws Exception {

		final long noIndividu = 282364L;
		final RegDate dateNaissance = date(1990, 9, 4);
		final RegDate dateDepartOriginelle = date(2012, 5, 23);
		final RegDate dateDepartCorrecte = date(2012, 6, 3);
		final RegDate dateArrivee = dateDepartCorrecte.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Piccolino", "Antonio", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, null, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
			}
		});

		final long noEvtDepartOriginel = 8181513L;
		final long noEvtCorrectionDepart = 4215152L;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtDepartOriginel);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDepartOriginelle);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtCorrectionDepart);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(dateDepartOriginelle);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtDepartOriginel);
					hibernateTemplate.merge(evt);
				}
				return null;
			}
		});

		// traitement de l'événement civil d'arrivée
		traiterEvenements(noIndividu);

		// vérification de l'état des événements civils après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtDepartOriginel);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, ech.getEtat());
					Assert.assertEquals("Evénement et correction(s) pris en compte ensemble.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(1, erreurs.size());

					final EvenementCivilEchErreur erreur = erreurs.iterator().next();
					Assert.assertNotNull(erreur);
					Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
					Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.", erreur.getMessage());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtCorrectionDepart);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, ech.getEtat());
					Assert.assertEquals("Evénement directement pris en compte dans le traitement de l'événement référencé.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementEch99DerriereErreur() throws Exception {

		final long noIndividu = 282364L;
		final RegDate dateNaissance = date(1990, 9, 4);
		final RegDate dateDepartOriginelle = date(2012, 5, 23);
		final RegDate dateDepartCorrecte = date(2012, 6, 3);
		final RegDate dateArrivee = dateDepartCorrecte.addDays(1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Piccolino", "Antonio", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, null, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateArrivee, null);
			}
		});

		final long noEvtDepartOriginel = 8181513L;
		final long noEvtCorrectionDepart = 4215152L;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtDepartOriginel);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					evt.setDateEvenement(dateDepartOriginelle);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(null);
					hibernateTemplate.merge(evt);
				}

				AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
				try {
					final EvenementCivilEch evt = new EvenementCivilEch();
					evt.setId(noEvtCorrectionDepart);
					evt.setType(TypeEvenementCivilEch.DEPART);
					evt.setAction(ActionEvenementCivilEch.CORRECTION);
					evt.setDateEvenement(dateDepartOriginelle);
					evt.setEtat(EtatEvenementCivil.A_TRAITER);
					evt.setNumeroIndividu(noIndividu);
					evt.setRefMessageId(noEvtDepartOriginel);
					hibernateTemplate.merge(evt);
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}
				return null;
			}
		});

		// traitement de l'événement civil d'arrivée et de sa correction issue de eCH-99
		traiterEvenements(noIndividu);

		// vérification de l'état des événements civils après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtDepartOriginel);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, ech.getEtat());
					Assert.assertEquals("Evénement et correction(s) pris en compte ensemble.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(1, erreurs.size());

					final EvenementCivilEchErreur erreur = erreurs.iterator().next();
					Assert.assertNotNull(erreur);
					Assert.assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
					Assert.assertEquals("Aucune adresse principale ou secondaire ne se termine à la date de l'événement.", erreur.getMessage());
				}
				{
					final EvenementCivilEch ech = evtCivilDAO.get(noEvtCorrectionDepart);
					Assert.assertNotNull(ech);
					Assert.assertEquals(EtatEvenementCivil.TRAITE, ech.getEtat());
					Assert.assertEquals("Evénement civil issu d'un eCH-0099 de commune. Événement traité sans modification Unireg.", ech.getCommentaireTraitement());

					final Set<EvenementCivilEchErreur> erreurs = ech.getErreurs();
					Assert.assertNotNull(erreurs);
					Assert.assertEquals(0, erreurs.size());
				}
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementDerriereErreur() throws Exception {

		// arrivée en erreur (ancien habitant qui revient, donc toujours non-habitant)
		// correction d'arrivée qui change le nom (-> pas d'impact fiscal)
		// comme la correction d'arrivée n'est pas un événement "principalement indexateur", il doit passer en attente sans reprise de données du civil

		final long noIndividu = 42784723L;
		final RegDate dateDebutAdresse = date(2012, 5, 30);
		final RegDate dateArrivee = dateDebutAdresse.addDays(2);        // afin de mettre l'arrivée en erreur
		final String ancienNom = "Tartempion";
		final String nouveauNom = "Tourtenpièce";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, ancienNom, "Gudule", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateDebutAdresse, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEPART_HS, MockPays.Allemagne);
				return pp.getNumero();
			}
		});

		// changement de nom
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNom(nouveauNom);
			}
		});

		final class Ids {
			long idAnnonce;
			long idCorrection;
		}

		// événement civil d'arrivée
		final Ids evtIds = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(ancienNom, pp.getNom());

				final Ids ids = new Ids();
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(4378423L);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ids.idAnnonce = hibernateTemplate.merge(ech).getId();
				}
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(4524734323L);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(ids.idAnnonce);
					ids.idCorrection= hibernateTemplate.merge(ech).getId();
				}

				return ids;
			}
		});

		// traitement des événements civils de l'individu
		traiterEvenements(noIndividu);

		// vérification de l'état après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(ancienNom, pp.getNom());

				final EvenementCivilEch annonce = evtCivilDAO.get(evtIds.idAnnonce);
				Assert.assertNotNull(annonce);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, annonce.getEtat());

				final EvenementCivilEch correction = evtCivilDAO.get(evtIds.idCorrection);
				Assert.assertNotNull(correction);
				Assert.assertEquals(EtatEvenementCivil.EN_ATTENTE, correction.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testTraitementIndexation99DerriereErreur() throws Exception {

		// arrivée en erreur (ancien habitant qui revient, donc toujours non-habitant)
		// correction d'arrivée eCH99 qui change le nom (-> pas d'impact fiscal)
		// mais on s'attend à ce que le nom soit également modifié dans la base Unireg (car non-habitant)

		final long noIndividu = 42784723L;
		final RegDate dateDebutAdresse = date(2012, 5, 30);
		final RegDate dateArrivee = dateDebutAdresse.addDays(2);        // afin de mettre l'arrivée en erreur
		final String ancienNom = "Tartempion";
		final String nouveauNom = "Tourtenpièce";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, ancienNom, "Gudule", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateDebutAdresse, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEPART_HS, MockPays.Allemagne);
				return pp.getNumero();
			}
		});

		// changement de nom
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNom(nouveauNom);
			}
		});

		final class Ids {
			long idAnnonce;
			long idCorrection;
		}

		// événement civil d'arrivée
		final Ids evtIds = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(ancienNom, pp.getNom());

				final Ids ids = new Ids();
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(4378423L);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ids.idAnnonce = hibernateTemplate.merge(ech).getId();
				}

				AuthenticationHelper.pushPrincipal(EvenementCivilEchSourceHelper.getVisaForEch99());
				try {
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(4524734323L);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(ids.idAnnonce);
					ids.idCorrection= hibernateTemplate.merge(ech).getId();
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}

				return ids;
			}
		});

		// traitement des événements civils de l'individu
		traiterEvenements(noIndividu);

		// vérification de l'état après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(nouveauNom, pp.getNom());

				final EvenementCivilEch annonce = evtCivilDAO.get(evtIds.idAnnonce);
				Assert.assertNotNull(annonce);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, annonce.getEtat());

				final EvenementCivilEch correction = evtCivilDAO.get(evtIds.idCorrection);
				Assert.assertNotNull(correction);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, correction.getEtat());
				return null;
			}
		});
	}

	@Test(timeout = 10000L)
	public void testPassageEnAttenteDesReferrersDesEvenementsMisEnAttente() throws Exception {

		final long noIndividu = 42784723L;
		final RegDate dateArrivee = date(2012, 5, 30);
		final RegDate dateDepart = dateArrivee.addMonths(5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Tatempion", "Gudule", Sexe.FEMININ);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEPART_HS, MockPays.Allemagne);
				return pp.getNumero();
			}
		});

		final long idAnnonceArrivee = 367432436L;
		final long idCorrectionArriveeDejaTraitee = 437846734L;
		final long idCorrectionArrivee = 247224362L;
		final long idAnnonceDepart = 56785437L;
		final long idCorrectionDepartDejaTraitee = 467653453L;
		final long idCorrectionDepart = 4567432L;

		// événements civils
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// arivée à mettre en erreur
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idAnnonceArrivee);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.EN_ERREUR);
					ech.setNumeroIndividu(noIndividu);
					hibernateTemplate.merge(ech);
				}
				// correction de l'arrivée déjà traitée
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idCorrectionArriveeDejaTraitee);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.EN_ERREUR);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idAnnonceArrivee);
					hibernateTemplate.merge(ech);
				}
				// correction de l'arrivée à mettre en attente
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idCorrectionArrivee);
					ech.setType(TypeEvenementCivilEch.ARRIVEE);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateArrivee);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idCorrectionArriveeDejaTraitee);
					hibernateTemplate.merge(ech);
				}

				// départ en erreur
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idAnnonceDepart);
					ech.setType(TypeEvenementCivilEch.DEPART);
					ech.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
					ech.setDateEvenement(dateDepart);
					ech.setEtat(EtatEvenementCivil.EN_ERREUR);
					ech.setNumeroIndividu(noIndividu);
					hibernateTemplate.merge(ech);
				}
				// correction de départ déjà traitée
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idCorrectionDepartDejaTraitee);
					ech.setType(TypeEvenementCivilEch.DEPART);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateDepart);
					ech.setEtat(EtatEvenementCivil.EN_ERREUR);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idAnnonceDepart);
					hibernateTemplate.merge(ech);
				}
				// correction de départ à mettre en attente
				{
					final EvenementCivilEch ech = new EvenementCivilEch();
					ech.setId(idCorrectionDepart);
					ech.setType(TypeEvenementCivilEch.DEPART);
					ech.setAction(ActionEvenementCivilEch.CORRECTION);
					ech.setDateEvenement(dateDepart);
					ech.setEtat(EtatEvenementCivil.A_TRAITER);
					ech.setNumeroIndividu(noIndividu);
					ech.setRefMessageId(idCorrectionDepartDejaTraitee);
					hibernateTemplate.merge(ech);
				}

				return null;
			}
		});

		// traitement des événements civils de l'individu
		traiterEvenements(noIndividu);

		final class Checker {
			void check(long idEvt, EtatEvenementCivil etatAttendu) {
				final EvenementCivilEch ech = evtCivilDAO.get(idEvt);
				Assert.assertNotNull(ech);
				Assert.assertEquals(etatAttendu, ech.getEtat());
			}
		}

		// vérification de l'état après traitement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Checker checker = new Checker();
				checker.check(idAnnonceArrivee, EtatEvenementCivil.EN_ERREUR);
				checker.check(idCorrectionArriveeDejaTraitee, EtatEvenementCivil.EN_ERREUR);
				checker.check(idCorrectionArrivee, EtatEvenementCivil.EN_ATTENTE);
				checker.check(idAnnonceDepart, EtatEvenementCivil.EN_ERREUR);
				checker.check(idCorrectionDepartDejaTraitee, EtatEvenementCivil.EN_ERREUR);
				checker.check(idCorrectionDepart, EtatEvenementCivil.EN_ATTENTE);
				return null;
			}
		});
	}
}
