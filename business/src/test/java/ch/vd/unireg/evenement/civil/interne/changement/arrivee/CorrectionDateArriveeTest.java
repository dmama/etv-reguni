package ch.vd.unireg.evenement.civil.interne.changement.arrivee;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;

public class CorrectionDateArriveeTest extends AbstractEvenementCivilInterneTest {

	private static final RegDate DATE_EVT = RegDate.get(2009, 10, 1);
	private static final long NO_IND_MINEUR = 1L;
	private static final long NO_IND_MAJEUR_SANS_FOR = 2L;
	private static final long NO_IND_HS = 3L;
	private static final long NO_IND_MAUVAISE_COMMUNE = 4L;
	private static final long NO_IND_PAS_ARRIVEE = 5L;
	private static final long NO_IND_CHANGE_ANNEE = 6L;
	private static final long NO_IND_DEJA_BONNE_DATE = 7L;
	private static final long NO_IND_MARIE = 8L;
	private static final long NO_IND_CELIBATAIRE = 9L;
	private static final long NO_IND_INCONNU = 9999L;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {

				final RegDate dateNaissanceMineur = DATE_EVT.addYears(-16);
				final RegDate dateNaissanceJusteMajeur = DATE_EVT.addYears(-18);
				final RegDate dateNaissance = DATE_EVT.addYears(-25);

				addIndividu(NO_IND_MINEUR, dateNaissanceMineur, "Scarrabée", "Petit", true);
				addIndividu(NO_IND_MAJEUR_SANS_FOR, dateNaissanceJusteMajeur, "Scarrabée", "Grand", true);
				addIndividu(NO_IND_HS, dateNaissance, "A l'étranger", "Parti", true);
				addIndividu(NO_IND_MAUVAISE_COMMUNE, dateNaissance, "Commune", "Mauvaise", false);
				addIndividu(NO_IND_PAS_ARRIVEE, dateNaissance, "Motif", "Mauvais", true);
				addIndividu(NO_IND_CHANGE_ANNEE, dateNaissance, "Année", "Change", false);
				addIndividu(NO_IND_DEJA_BONNE_DATE, dateNaissance, "Elève", "Bon", true);
				addIndividu(NO_IND_MARIE, dateNaissance, "Marié", "Lui", true);
				addIndividu(NO_IND_CELIBATAIRE, dateNaissance, "Sainte", "Catherine", false);
				addIndividu(NO_IND_INCONNU, dateNaissance, "Incognito", "Maestro", true);
			}
		});
	}

	private CorrectionDateArrivee createValidEvenement(long noIndividu, int ofsCommune, Long principalId) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		return new CorrectionDateArrivee(individu, null, DATE_EVT, ofsCommune, context);
	}

	@Test
	public void testMineur() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_MINEUR);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_MINEUR, 123454, ppId);
				assertSansErreurNiWarning(evt);
				return null;
			}
		});

		// check des fors
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final Set<ForFiscal> ff = pp.getForsFiscaux();
			Assert.assertNotNull(ff);
			Assert.assertEquals(0, ff.size());
			return null;
		});
	}

	@Test
	public void testMajeurSansFor() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_MAJEUR_SANS_FOR);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_MAJEUR_SANS_FOR, 123454, ppId);
				assertErreurs(evt, Collections.singletonList("L'individu n'a pas de for fiscal principal connu."));
				return null;
			}
		});
	}

	@Test
	public void testHorsSuisse() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_HS);
			addForPrincipal(pp, DATE_EVT.addDays(10), MotifFor.DEMENAGEMENT_VD, MockPays.France);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_HS, 123454, ppId);
				assertErreurs(evt, Collections.singletonList(String.format("Le dernier for principal du contribuable %s est hors-Suisse.", FormatNumeroHelper.numeroCTBToDisplay(ppId))));
				return null;
			}
		});
	}

	@Test
	public void testMauvaiseCommuneAnnonce() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_MAUVAISE_COMMUNE);
			addForPrincipal(pp, DATE_EVT.addDays(10), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_MAUVAISE_COMMUNE, MockCommune.Aubonne.getNoOFS(), ppId);
				assertErreurs(evt, Collections.singletonList(String.format("Le dernier for principal du contribuable %s n'est pas sur la commune d'annonce de l'événement.", FormatNumeroHelper.numeroCTBToDisplay(ppId))));
				return null;
			}
		});
	}

	@Test
	public void testMauvaisMotifOuverture() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_PAS_ARRIVEE);
			addForPrincipal(pp, DATE_EVT.addYears(-4), MotifFor.ARRIVEE_HC, DATE_EVT.addDays(9), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Cossonay);
			addForPrincipal(pp, DATE_EVT.addDays(10), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_PAS_ARRIVEE, MockCommune.Cossonay.getNoOFS(), ppId);
				final String msg = String.format("Le dernier for principal sur le contribuable %s n'a pas été ouvert pour un motif d'arrivée (trouvé : %s).",
						FormatNumeroHelper.numeroCTBToDisplay(ppId), MotifFor.CHGT_MODE_IMPOSITION.getDescription(true));
				assertErreurs(evt, Collections.singletonList(msg));
				return null;
			}
		});
	}

	@Test
	public void testChangementAnnee() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_PAS_ARRIVEE);
			addForPrincipal(pp, DATE_EVT.addYears(-1), MotifFor.ARRIVEE_HS, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_PAS_ARRIVEE, MockCommune.Cossonay.getNoOFS(), ppId);
				assertErreurs(evt, Collections.singletonList("La date d'ouverture du for principal ne peut pas changer d'année avec le traitement automatique. Veuillez traiter ce cas manuellement."));
				return null;
			}
		});
	}

	@Test
	public void testDejaBonneDate() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_DEJA_BONNE_DATE);
			addForPrincipal(pp, DATE_EVT, MotifFor.ARRIVEE_HS, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_DEJA_BONNE_DATE, MockCommune.Cossonay.getNoOFS(), ppId);
				assertSansErreurNiWarning(evt);
				return null;
			}
		});

		// check des fors
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final Set<ForFiscal> ff = pp.getForsFiscaux();
			Assert.assertNotNull(ff);
			Assert.assertEquals(1, ff.size());
			return null;
		});
	}

	@Test
	public void testCasSimpleCelibataire() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_CELIBATAIRE);
			addForPrincipal(pp, DATE_EVT.addDays(-10), MotifFor.ARRIVEE_HS, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_CELIBATAIRE, MockCommune.Cossonay.getNoOFS(), ppId);
				assertSansErreurNiWarning(evt);
				return null;
			}
		});

		// check des fors
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final List<ForFiscal> ff = pp.getForsFiscauxSorted();
			Assert.assertNotNull(ff);
			Assert.assertEquals(2, ff.size());

			// annulé d'abord car sa date de début est antérieure à la date de début du for après correction

			final ForFiscalPrincipal ffAnnule = (ForFiscalPrincipal) ff.get(0);
			Assert.assertTrue(ffAnnule.isAnnule());
			Assert.assertEquals(DATE_EVT.addDays(-10), ffAnnule.getDateDebut());
			Assert.assertNull(ffAnnule.getDateFin());

			final ForFiscalPrincipal ffRestant = (ForFiscalPrincipal) ff.get(1);
			Assert.assertFalse(ffRestant.isAnnule());
			Assert.assertEquals(DATE_EVT, ffRestant.getDateDebut());
			Assert.assertNull(ffRestant.getDateFin());
			return null;
		});
	}

	@Test
	public void testCasSimpleCouple() throws Exception {

		class Ids {
			long ppal;
			long menage;
		}
		final Ids ids = new Ids();

		// mise en place fiscale
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique m = addHabitant(NO_IND_MARIE);
			final PersonnePhysique mme = addNonHabitant("Célestine", "Dupont", date(1985, 6, 12), Sexe.FEMININ);
			final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(m, mme, DATE_EVT.addYears(-1), null);
			final MenageCommun mc = ensemble.getMenage();
			addForPrincipal(mc, DATE_EVT.addDays(-10), MotifFor.ARRIVEE_HS, MockCommune.Cossonay);

			ids.ppal = m.getId();
			ids.menage = mc.getNumero();
			return null;
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_MARIE, MockCommune.Cossonay.getNoOFS(), ids.ppal);
				assertSansErreurNiWarning(evt);
				return null;
			}
		});

		// check des fors
		doInNewTransactionAndSession(status -> {
			final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.menage);
			final List<ForFiscal> ff = mc.getForsFiscauxSorted();
			Assert.assertNotNull(ff);
			Assert.assertEquals(2, ff.size());

			// annulé d'abord car sa date de début est antérieure à la date de début du for après correction

			final ForFiscalPrincipal ffAnnule = (ForFiscalPrincipal) ff.get(0);
			Assert.assertTrue(ffAnnule.isAnnule());
			Assert.assertEquals(DATE_EVT.addDays(-10), ffAnnule.getDateDebut());
			Assert.assertNull(ffAnnule.getDateFin());

			final ForFiscalPrincipal ffRestant = (ForFiscalPrincipal) ff.get(1);
			Assert.assertFalse(ffRestant.isAnnule());
			Assert.assertEquals(DATE_EVT, ffRestant.getDateDebut());
			Assert.assertNull(ffRestant.getDateFin());
			return null;
		});
	}

	@Test
	public void testModificationForPrecedent() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(NO_IND_CELIBATAIRE);
			addForPrincipal(pp, DATE_EVT.addYears(-4), MotifFor.ARRIVEE_HC, DATE_EVT.addDays(-11), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
			addForPrincipal(pp, DATE_EVT.addDays(-10), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_CELIBATAIRE, MockCommune.Cossonay.getNoOFS(), ppId);
				assertSansErreurNiWarning(evt);
				return null;
			}
		});

		// check des fors
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final List<ForFiscal> ff = pp.getForsFiscauxSorted();
			Assert.assertNotNull(ff);
			Assert.assertEquals(4, ff.size());

			// annulé d'abord car sa date de début est antérieure à la date de début du for après correction

			final ForFiscalPrincipal ffPrecedentAnnule = (ForFiscalPrincipal) ff.get(0);
			Assert.assertTrue(ffPrecedentAnnule.isAnnule());
			Assert.assertEquals(DATE_EVT.addYears(-4), ffPrecedentAnnule.getDateDebut());
			Assert.assertEquals(DATE_EVT.addDays(-11), ffPrecedentAnnule.getDateFin());
			Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffPrecedentAnnule.getNumeroOfsAutoriteFiscale());

			final ForFiscalPrincipal ffPrecedent = (ForFiscalPrincipal) ff.get(1);
			Assert.assertFalse(ffPrecedent.isAnnule());
			Assert.assertEquals(DATE_EVT.addYears(-4), ffPrecedent.getDateDebut());
			Assert.assertEquals(DATE_EVT.getOneDayBefore(), ffPrecedent.getDateFin());
			Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffPrecedent.getNumeroOfsAutoriteFiscale());

			final ForFiscalPrincipal ffAnnule = (ForFiscalPrincipal) ff.get(2);
			Assert.assertTrue(ffAnnule.isAnnule());
			Assert.assertEquals(DATE_EVT.addDays(-10), ffAnnule.getDateDebut());
			Assert.assertNull(ffAnnule.getDateFin());
			Assert.assertEquals(MockCommune.Cossonay.getNoOFS(), (int) ffAnnule.getNumeroOfsAutoriteFiscale());

			final ForFiscalPrincipal ffRestant = (ForFiscalPrincipal) ff.get(3);
			Assert.assertFalse(ffRestant.isAnnule());
			Assert.assertEquals(DATE_EVT, ffRestant.getDateDebut());
			Assert.assertNull(ffRestant.getDateFin());
			Assert.assertEquals(MockCommune.Cossonay.getNoOFS(), (int) ffRestant.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	@Test
	public void testIndividuInconnu() throws Exception {
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final CorrectionDateArrivee evt = createValidEvenement(NO_IND_INCONNU, MockCommune.Cossonay.getNoOFS(), null);
				assertErreurs(evt, Collections.singletonList(String.format("Aucun tiers contribuable ne correspond au numéro d'individu %d", NO_IND_INCONNU)));
				return null;
			}
		});
	}
}
