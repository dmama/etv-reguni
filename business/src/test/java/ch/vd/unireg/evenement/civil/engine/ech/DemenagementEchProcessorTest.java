package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;

import static org.junit.Assert.assertEquals;

public class DemenagementEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testDemenagementCelibataire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite, MotifFor.MAJORITE, MockCommune.Cossonay);
				addAdresseSuisse(osvalde, TypeAdresseTiers.DOMICILE, dateMajorite, null, MockRue.CossonayVille.AvenueDuFuniculaire);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateMajorite, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
			return null;
		});
	}

	@Test
	public void testDemenagementCelibataireAvecDecision() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);

				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite, MotifFor.MAJORITE, MockCommune.Cossonay);
				addDecisionAci(osvalde, dateDemenagement.addMonths(6), null, MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
				addAdresseSuisse(osvalde, TypeAdresseTiers.DOMICILE, dateMajorite, null, MockRue.CossonayVille.AvenueDuFuniculaire);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique osvalde = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(osvalde);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(osvalde.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test
	public void testDemenagementCoupleAvecDecision() throws Exception {

		final long noMadame = 46215611L;
		final long noMonsieur = 78215611L;
		final RegDate dateNaissance = date(1956, 4, 23);
		final RegDate dateMariage = date(2008, 10, 19);
		final RegDate dateDemenagement = date(2008, 5, 6);

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu monsieur = addIndividu(noMonsieur, date(1923, 2, 12), "Crispus", "Santacorpus", true);
				addNationalite(monsieur, MockPays.Suisse, date(1923, 2, 12), null);
				final MockIndividu madame = addIndividu(noMadame, date(1974, 8, 1), "Lisette", "Bouton", false);
				addNationalite(madame, MockPays.France, date(1974, 8, 1), null);
				marieIndividus(monsieur, madame, dateMariage);
				final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
				final MockAdresse adresseAvant = addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(monsieur, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique monsieur = addHabitant(noMonsieur);
				final PersonnePhysique madame = addHabitant(noMadame);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(monsieur, madame, dateMariage, null);
				addForPrincipal(ensemble.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens);
				addDecisionAci(ensemble.getMenage(), date(2012, 5, 1), null, MockCommune.Vevey.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
				return null;
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long separationId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(454563456L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});


		// événement demenagement
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noMonsieur);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noMonsieur);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique monsieur = tiersService.getPersonnePhysiqueByNumeroIndividu(noMonsieur);
			Assert.assertNotNull(monsieur);
			final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(monsieur, dateMariage.getOneDayAfter());
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(monsieur.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(etc.getMenage().getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementSecondaire() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				final MockAdresse adressePrincipal = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMajorite, null);
				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, veilleDemenagement);
				final MockAdresse adresseApres = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateDemenagement, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite, MotifFor.MAJORITE, MockPays.Espagne);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testDemenagementSecondaireSansChangementAdresse() throws Exception {

		final long noIndividu = 126673246L;
		final RegDate dateDemenagement = date(2011, 10, 31);
		final RegDate veilleDemenagement = dateDemenagement.getOneDayBefore();
		final RegDate dateMajorite = date(1974, 4, 23);
		final RegDate dateNaissance = date(1956, 4, 23);

		// le p'tit nouveau
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final MockIndividu osvalde = addIndividu(noIndividu, dateNaissance, "Zorro", "Alessandro", true);
				final MockAdresse adressePrincipal = addAdresse(osvalde, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, dateMajorite, null);
				final MockAdresse adresseAvant = addAdresse(osvalde, TypeAdresseCivil.SECONDAIRE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				addNationalite(osvalde, MockPays.Espagne, dateNaissance, null);
			}
		});

		doInNewTransactionAndSession(new ch.vd.registre.base.tx.TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique osvalde = addHabitant(noIndividu);
				addForPrincipal(osvalde, dateMajorite, MotifFor.MAJORITE, MockPays.Espagne);
				return null;
			}
		});

		// événement demenagement
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			return null;
		});
	}

	/**
	 * [SIFISC-6012] Cas d'une personne qui change de fraction dans une des communes de la vallée
	 */
	@Test
	public void testDemenagementEntreFractionsMemeCommuneFaitiere() throws Exception {

		final long noIndividu = 34278432576L;
		final RegDate dateNaissance = date(1980, 7, 31);
		final RegDate dateDemenagement = date(2014, 3, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Berney", "Alphonse", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.LAbbaye.LesBioux.BatimentLaGrandePartie, null, null, dateNaissance, dateDemenagement.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.LAbbaye.LePont.BatimentSurLesQuais, null, null, dateDemenagement, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Fraction.LesBioux);
			return pp.getNumero();
		});

		// événement civil de déménagement dans la commune de L'Abbaye (commune faîtière des fractions "Les Bioux" et "Le Pont")
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
			Assert.assertEquals(String.format("Traité comme une arrivée car les communes %s et %s ne sont pas différenciées dans les données civiles.",
			                                  MockCommune.Fraction.LesBioux.getNomOfficiel(), MockCommune.Fraction.LePont.getNomOfficiel()),
			                    evt.getCommentaireTraitement());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateDemenagement, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Fraction.LePont.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	/**
	 * [SIFISC-6012] Cas d'une personne pour laquelle un déménagement dans la commune est annoncé alors que les adresses de résidence indiquent
	 * plutôt un déménagement vaudois entre deux communes qui n'ont rien à voir l'une avec l'autre...
	 */
	@Test
	public void testDemenagementDansLaCommuneAvecCommunesTresDifferentes() throws Exception {

		final long noIndividu = 34278432576L;
		final RegDate dateNaissance = date(1980, 7, 31);
		final RegDate dateDemenagement = date(2014, 3, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Berney", "Alphonse", Sexe.MASCULIN);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.Echallens.BatimentRouteDeMoudon, null, null, dateNaissance, dateDemenagement.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockBatiment.YverdonLesBains.BatimentCheminDesMuguets, null, null, dateDemenagement, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Echallens);
			return pp.getNumero();
		});

		// événement civil de déménagement dans la commune de L'Abbaye (commune faîtière des fractions "Les Bioux" et "Le Pont")
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(14532L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateDemenagement);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.DEMENAGEMENT_DANS_COMMUNE);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement civil
		traiterEvenements(noIndividu);

		// vérification du résultat
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			Assert.assertNull(evt.getCommentaireTraitement());

			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());

			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			Assert.assertNotNull(erreur);
			Assert.assertEquals(String.format("Les communes %s et %s ne sont pas fusionnées en %d et ne sont pas des fractions de la même commune faîtière, pourtant c'est bien un événement de déménagement dans la commune qui a été reçu.",
			                                  MockCommune.Echallens.getNomOfficiel(), MockCommune.YverdonLesBains.getNomOfficiel(), dateDemenagement.year()),
			                    erreur.getMessage());

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertNotNull(pp);

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
			Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}
}
