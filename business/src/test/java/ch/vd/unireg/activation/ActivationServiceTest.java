package ch.vd.unireg.activation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.BusinessTestingConstants;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.AnnuleEtRemplace;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.type.TypeTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES  // Depuis [SIFISC-2690] l'annulation des taches qui était effectuée par l'ActivationService
															// est traitée par le TacheSynchronizerInterceptor, on a donc besoin d'un vrai bean tacheService et non pas d'un mock vide
															// pour valider ce fonctionnement.
})
public class ActivationServiceTest extends BusinessTest {

	private ActivationService activationService;
	private TiersService tiersService;
	private TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		activationService = getBean(ActivationService.class, "activationService");
		tiersService = getBean(TiersService.class, "tiersService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
	}

	@Test
	public void testDesactiveTiersSansForActif() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Paco", "Lephantaume", date(1978, 3, 13), Sexe.MASCULIN);
			return pp.getNumero();
		});

		// désactivation du tiers
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertFalse(tiers.isAnnule());
			Assert.assertNull(tiers.getDateDesactivation());
			activationService.desactiveTiers(tiers, date(2010, 4, 1));
			return null;
		});

		// test des valeurs après désactivation (= tiers complètement annulé !)
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertTrue(tiers.isAnnule());
			Assert.assertNull(tiers.getDateDesactivation());
			return null;
		});
	}

	@Test
	public void testReactiveTiersCompletementAnnule() throws Exception {

		// mise en place du tiers annulé
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Nick", "Nearly-Headless", date(1978, 3, 13), Sexe.MASCULIN);
			pp.setAnnule(true);
			return pp.getNumero();
		});

		// réactivation du tiers annulé
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertTrue(tiers.isAnnule());
			Assert.assertTrue(tiers.isDesactive(null));
			Assert.assertNull(tiers.getDateDesactivation());
			activationService.reactiveTiers(tiers, date(2010, 4, 12));
			return null;
		});

		// test des valeurs après ré-activation
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertFalse(tiers.isAnnule());
			Assert.assertFalse(tiers.isDesactive(null));
			Assert.assertNull(tiers.getDateDesactivation());
			return null;
		});
	}

	@Test
	public void testDesactiveTiers() throws Exception {


		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Momo", "Bizuth", date(1980, 5, 12), Sexe.MASCULIN);
			addForPrincipal(pp, date(1998, 5, 12), MotifFor.MAJORITE, date(2000, 12, 3), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
			addForPrincipal(pp, date(2000, 12, 4), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForSecondaire(pp, date(2002, 1, 16), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, date(2004, 8, 12), MotifFor.ACHAT_IMMOBILIER, date(2006, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

			for (int pf = 2003; pf <= RegDate.get().year(); ++pf) {
				addPeriodeFiscale(pf);
			}

			final CollectiviteAdministrative colAdm = tiersService.getOfficeImpot(ServiceInfrastructureRaw.noACI);
			addTacheControleDossier(TypeEtatTache.TRAITE, date(2002, 4, 1), pp, colAdm);
			addTacheControleDossier(TypeEtatTache.EN_INSTANCE, date(2010, 1, 5), pp, colAdm);
			return pp.getNumero();
		});

		// depuis [SIFISC-2690] L'annulation des taches est traitée par le TacheSynchronizerInterceptor
		setWantSynchroTache(true);

		final RegDate dateDesactivation = date(2009, 12, 31);

		// désactivation
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);

			activationService.desactiveTiers(tiers, dateDesactivation);
			return null;
		});

		// tests des valeurs après désactivation
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertFalse(tiers.isAnnule());

			final List<ForFiscal> ff = tiers.getForsFiscauxSorted();
			Assert.assertNotNull(ff);
			Assert.assertEquals(4, ff.size());

			// for 1 : for principal pas touché
			{
				final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(date(2000, 12, 3), ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
			}

			// for 2 : for principal fermé pour annulation
			{
				final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateDesactivation, ffp.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ffp.getMotifFermeture());
			}

			// for 3 : for secondaire fermé pour annulation
			{
				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff.get(2);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateDesactivation, ffs.getDateFin());
				Assert.assertEquals(MotifFor.ANNULATION, ffs.getMotifFermeture());
			}

			// for 4 : for secondaire pas touché
			{
				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff.get(3);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), (int) ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(date(2006, 12, 31), ffs.getDateFin());
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
			}

			// tâche traitée : elle ne doit pas avoir changé
			{
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable((Contribuable) tiers);
				criterion.setEtatTache(TypeEtatTache.TRAITE);
				criterion.setInclureTachesAnnulees(true);

				final List<Tache> taches = tacheDAO.find(criterion);
				Assert.assertNotNull(taches);
				Assert.assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				Assert.assertNotNull(tache);
				Assert.assertFalse(tache.isAnnule());
			}

			// tâche en instance : elle doit avoir été annulée
			{
				final TacheCriteria criterion = new TacheCriteria();
				criterion.setContribuable((Contribuable) tiers);
				criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
				criterion.setInclureTachesAnnulees(true);

				final List<Tache> taches = tacheDAO.find(criterion);
				for (Tache tache : taches) {
					Assert.assertNotNull(tache);
					if (tache.getTypeTache() != TypeTache.TacheAnnulationDeclarationImpot) {
						Assert.assertTrue("La tâche devrait être annulée", tache.isAnnule());
					}
				}
			}

			// attributs calculés sur le tiers
			Assert.assertFalse(tiers.isDesactive(dateDesactivation.getOneDayBefore()));
			Assert.assertFalse(tiers.isDesactive(dateDesactivation));
			Assert.assertTrue(tiers.isDesactive(dateDesactivation.getOneDayAfter()));
			Assert.assertTrue(tiers.isDesactive(null));
			Assert.assertEquals(dateDesactivation, tiers.getDateDesactivation());
			return null;
		});
	}

	@Test
	public void testRemplaceTiers() throws Exception {

		class Ids {
			long remplaceId;
			long remplacantId;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique aRemplacer = addNonHabitant("Pénéloppe", "Solette", date(1967, 5, 2), Sexe.FEMININ);
			addForPrincipal(aRemplacer, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Renens);

			final PersonnePhysique remplacant = addNonHabitant("Dabord", "Moi", date(1966, 2, 25), Sexe.FEMININ);

			ids.remplaceId = aRemplacer.getNumero();
			ids.remplacantId = remplacant.getNumero();
			return null;
		});

		final RegDate dateRemplacement = RegDate.get(2010, 1, 1);

		// remplacement de l'un par l'autre
		doInNewTransactionAndSession(status -> {
			final Tiers tiersRemplace = tiersService.getTiers(ids.remplaceId);
			final Tiers tiersRemplacant = tiersService.getTiers(ids.remplacantId);
			Assert.assertNotNull(tiersRemplace);
			Assert.assertNotNull(tiersRemplacant);

			activationService.remplaceTiers(tiersRemplace, tiersRemplacant, dateRemplacement);
			return null;
		});

		// tests
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique tiersRemplace = (PersonnePhysique) tiersService.getTiers(ids.remplaceId);
			final PersonnePhysique tiersRemplacant = (PersonnePhysique) tiersService.getTiers(ids.remplacantId);
			Assert.assertNotNull(tiersRemplace);
			Assert.assertNotNull(tiersRemplacant);

			final ForFiscalPrincipal forFiscalPrincipal = tiersRemplace.getForFiscalPrincipalAt(dateRemplacement);
			Assert.assertEquals(MotifFor.ANNULATION, forFiscalPrincipal.getMotifFermeture());

			final AnnuleEtRemplace annuleEtRemplace = (AnnuleEtRemplace) tiersRemplacant.getRapportObjetValidAt(null, TypeRapportEntreTiers.ANNULE_ET_REMPLACE);
			Assert.assertNotNull(annuleEtRemplace);
			Assert.assertEquals(dateRemplacement.getOneDayAfter(), annuleEtRemplace.getDateDebut());
			Assert.assertNull(annuleEtRemplace.getDateFin());
			Assert.assertEquals(tiersRemplace.getNumero(), annuleEtRemplace.getSujetId());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReactiveTiers() throws Exception {

		final RegDate dateDesactivation = date(2009, 12, 31);

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Fée", "Nixe", date(1956, 3, 12), Sexe.FEMININ);
			addForPrincipal(pp, date(2000, 4, 1), MotifFor.ARRIVEE_HS, dateDesactivation, MotifFor.ANNULATION, MockCommune.Bussigny);
			addForSecondaire(pp, date(2000, 10, 1), MotifFor.ACHAT_IMMOBILIER, dateDesactivation, MotifFor.ANNULATION, MockCommune.CheseauxSurLausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, date(2000, 10, 1), MotifFor.ACHAT_IMMOBILIER, dateDesactivation, MotifFor.VENTE_IMMOBILIER, MockCommune.Croy, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		final RegDate dateReactivation = dateDesactivation.getOneDayAfter().addMonths(2);

		// réactivation
		doInNewTransaction(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertFalse(tiers.isAnnule());
			Assert.assertEquals(dateDesactivation, tiers.getDateDesactivation());

			activationService.reactiveTiers(tiers, dateReactivation);
			return null;
		});

		// tests
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersService.getTiers(ppId);
			Assert.assertNotNull(tiers);
			Assert.assertFalse(tiers.isAnnule());
			Assert.assertNull(tiers.getDateDesactivation());

			final List<ForFiscal> ff = tiers.getForsFiscauxValidAt(dateReactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(2, ff.size());

			boolean forPrincipalTrouve = false;
			boolean forSecondaireTrouve = false;
			for (ForFiscal forFiscal : ff) {
				Assert.assertTrue(forFiscal instanceof ForFiscalRevenuFortune);
				Assert.assertEquals(MotifFor.REACTIVATION, ((ForFiscalRevenuFortune) forFiscal).getMotifOuverture());
				Assert.assertEquals(dateReactivation, forFiscal.getDateDebut());

				if (forFiscal instanceof ForFiscalPrincipal) {
					Assert.assertFalse(forPrincipalTrouve);     // déjà vu un ?
					forPrincipalTrouve = true;

					Assert.assertEquals(MockCommune.Bussigny.getNoOFS(), (int) forFiscal.getNumeroOfsAutoriteFiscale());
				}
				else if (forFiscal instanceof ForFiscalSecondaire) {
					Assert.assertFalse(forSecondaireTrouve);    // déjà vu un ?
					forSecondaireTrouve = true;

					Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFS(), (int) forFiscal.getNumeroOfsAutoriteFiscale());
				}
				else {
					Assert.fail("Classe de for fiscale innattendue : " + forFiscal.getClass().getName());
				}
			}
			Assert.assertTrue(forPrincipalTrouve);
			Assert.assertTrue(forSecondaireTrouve);
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationInterditePourCauseDeDeclarationUlterieure() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			final PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
			final PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
			final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
			final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2008);
			addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
			addDeclarationImpot(pp, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			try {
				activationService.desactiveTiers(pp, date(2008, 12, 30));
				Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'il a encore des DI ouvertes");
			}
			catch (ActivationServiceException e) {
				Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.", e.getMessage());
				status.setRollbackOnly();           // suite à cette exception, la transaction doit être annulée
			}
			return null;
		});

		// mais on doit pouvoir l'annuler après le 31.12.2008 (y compris le jour même)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final RegDate dateDesactivation = date(2008, 12, 31);
			activationService.desactiveTiers(pp, dateDesactivation);

			final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(dateDesactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(dateDesactivation, ff.getDateFin());
			Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationAutoriseeAvecDeclarationUlterieureAnnulee() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			final PeriodeFiscale pf2007 = addPeriodeFiscale(2007);
			final PeriodeFiscale pf2008 = addPeriodeFiscale(2008);
			final ModeleDocument modele2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2007);
			final ModeleDocument modele2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf2008);
			addDeclarationImpot(pp, pf2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2007);
			addDeclarationImpot(pp, pf2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2008);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			try {
				activationService.desactiveTiers(pp, date(2008, 12, 30));
				Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'il a encore des DI ouvertes");
			}
			catch (ActivationServiceException e) {
				Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe encore des déclarations couvrant une période postérieure à la date d'annulation souhaitée.", e.getMessage());
				status.setRollbackOnly();           // suite à cette exception, la transaction doit être annulée
			}
			return null;
		});

		// mais on doit pouvoir l'annuler si la DI 2008 est annulée
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final List<Declaration> declarations2008 = pp.getDeclarationsDansPeriode(Declaration.class, 2008, false);
			Assert.assertNotNull(declarations2008);
			Assert.assertEquals(1, declarations2008.size());
			declarations2008.get(0).setAnnule(true);

			final RegDate dateDesactivation = date(2008, 12, 30);
			activationService.desactiveTiers(pp, dateDesactivation);

			final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(dateDesactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(dateDesactivation, ff.getDateFin());
			Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationInterditePourCauseDeForOuvertApresDateAnnulation() throws Exception {

		final class Ids {
			long achille;
			long yoko;
		}
		final Ids ids = new Ids();

		// mise en place (on essaie avec un for encore ouvert, et un autre déjà refermé)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique achile = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			addForPrincipal(achile, date(2002, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
			addForPrincipal(achile, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			ids.achille = achile.getNumero();

			final PersonnePhysique yoko = addNonHabitant("Yoko", "Tsuno", date(1970, 4, 12), Sexe.FEMININ);
			addForPrincipal(yoko, date(2002, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Aubonne);
			addForPrincipal(yoko, date(2009, 1, 1), MotifFor.DEMENAGEMENT_VD, date(2009, 10, 31), MotifFor.DEPART_HS, MockCommune.Bussigny);
			ids.yoko = yoko.getNumero();
			return null;
		});

		// on ne doit pouvoir annuler aucun des deux tiers avant le 31.12.2008
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique achille = (PersonnePhysique) tiersService.getTiers(ids.achille);
			try {
				activationService.desactiveTiers(achille, date(2008, 12, 30));
				Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
			}
			catch (ActivationServiceException e) {
				Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
			}

			final PersonnePhysique yoko = (PersonnePhysique) tiersService.getTiers(ids.yoko);
			try {
				activationService.desactiveTiers(yoko, date(2008, 12, 30));
				Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
			}
			catch (ActivationServiceException e) {
				Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
			}

			status.setRollbackOnly();           // suite aux exceptions attendues, la transaction doit être annulée;
			return null;
		});

		// mais on doit pouvoir l'annuler si le for 2009 est annulé
		doInNewTransactionAndSession(status -> {
			// d'abord Achille
			final RegDate dateDesactivation = date(2008, 12, 30);
			{
				final PersonnePhysique achille = (PersonnePhysique) tiersService.getTiers(ids.achille);
				final Set<ForFiscal> fors = achille.getForsFiscaux();
				Assert.assertNotNull(fors);
				Assert.assertEquals(2, fors.size());

				final ForFiscalPrincipal ffp = achille.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);
				activationService.desactiveTiers(achille, dateDesactivation);

				final ForFiscalPrincipal ffApresAnnulation = achille.getForFiscalPrincipalAt(dateDesactivation.getOneDayAfter());
				Assert.assertNull(ffApresAnnulation);

				Assert.assertFalse(achille.isAnnule());
				Assert.assertEquals(dateDesactivation, achille.getDateDesactivation());
			}

			// puis Yoko
			{
				final PersonnePhysique yoko = (PersonnePhysique) tiersService.getTiers(ids.yoko);
				final Set<ForFiscal> fors = yoko.getForsFiscaux();
				Assert.assertNotNull(fors);
				Assert.assertEquals(2, fors.size());

				final ForFiscalPrincipal ffp = yoko.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);
				activationService.desactiveTiers(yoko, dateDesactivation);

				final ForFiscalPrincipal ffApresAnnulation = yoko.getForFiscalPrincipalAt(dateDesactivation.getOneDayAfter());
				Assert.assertNull(ffApresAnnulation);

				Assert.assertFalse(yoko.isAnnule());
				Assert.assertEquals(dateDesactivation, yoko.getDateDesactivation());
			}
			;
			return null;
		});

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationInterditePourCauseDeForOuvertAvantEtFermeApresDateAnnulation() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 12, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// on ne doit pas pouvoir l'annuler avant le 31.12.2008
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			try {
				activationService.desactiveTiers(pp, date(2008, 12, 30));
				Assert.fail("Il devrait être interdit d'annuler le tiers alors qu'un for est ouvert après la date d'annulation souhaitée");
			}
			catch (ActivationServiceException e) {
				Assert.assertEquals("Il est interdit d'annuler un tiers pour lequel il existe des fors dont la date d'ouverture ou de fermeture est postérieure à la date d'annulation souhaitée.", e.getMessage());
				status.setRollbackOnly();           // suite à cette exception, la transaction doit être annulée
			}
			return null;
		});

		// mais on doit pouvoir l'annuler après le 31.12.2008 (y compris le jour même)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final RegDate dateDesactivation = date(2008, 12, 31);
			activationService.desactiveTiers(pp, dateDesactivation);

			final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(dateDesactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(dateDesactivation, ff.getDateFin());
			Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationAutoriseeSiForOuvertAvantDateAnnulationEtPasEncoreFerme() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// on doit pouvoir l'annuler après le 2007.01.01
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final RegDate dateDesactivation = date(2008, 12, 31);
			activationService.desactiveTiers(pp, dateDesactivation);

			final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(dateDesactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(dateDesactivation, ff.getDateFin());
			Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());
			return null;
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationAutoriseeSiForOuvertExactementDateAnnulationEtPasEncoreFerme() throws Exception {

		// mise en place
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			addForPrincipal(pp, date(2007, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// on doit pouvoir l'annuler dès le 01.01.2007
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			final RegDate dateDesactivation = date(2007, 1, 1);
			activationService.desactiveTiers(pp, dateDesactivation);

			final ForFiscalPrincipal ff = pp.getForFiscalPrincipalAt(dateDesactivation);
			Assert.assertNotNull(ff);
			Assert.assertEquals(dateDesactivation, ff.getDateDebut());
			Assert.assertEquals(MotifFor.ARRIVEE_HS, ff.getMotifOuverture());
			Assert.assertEquals(dateDesactivation, ff.getDateFin());
			Assert.assertEquals(MotifFor.ANNULATION, ff.getMotifFermeture());

			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());
			return null;
		});
	}

	private static class ForFiscalComparator extends DateRangeComparator<ForFiscal> {
		@Override
		public int compare(ForFiscal o1, ForFiscal o2) {
			final boolean isPrincipal1 = o1 instanceof ForFiscalPrincipal;
			final boolean isPrincipal2 = o2 instanceof ForFiscalPrincipal;
			if (isPrincipal1 == isPrincipal2) {
			    return super.compare(o1, o2);
			}
			else if (isPrincipal1) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testReactivationTiersAvecPlusieursFors() throws Exception {

		final RegDate dateDesactivation = date(2009, 2, 22);

		// mise en place
		final long ppId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1948, 1, 26), Sexe.MASCULIN);
			addForPrincipal(pp, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(1990, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(pp, date(1991, 1, 1), MotifFor.DEMENAGEMENT_VD, dateDesactivation, MotifFor.ANNULATION, MockCommune.Lausanne);
			addForSecondaire(pp, date(1991, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateDesactivation, MotifFor.ANNULATION, MockCommune.Lausanne, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateDesactivation, MotifFor.VENTE_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
			addForSecondaire(pp, date(2001, 1, 1), MotifFor.ACHAT_IMMOBILIER, dateDesactivation.addMonths(-1), MotifFor.ANNULATION, MockCommune.Bex, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// vérification de l'état du tiers
		doInNewTransaction(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			Assert.assertFalse(pp.isAnnule());
			Assert.assertEquals(dateDesactivation, pp.getDateDesactivation());

			final Set<ForFiscal> fors = pp.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(5, fors.size());

			final List<ForFiscal> forsTries = new ArrayList<>(fors);
			Collections.sort(forsTries, new ForFiscalComparator());

			// ne doit pas être ré-ouvert
			final ForFiscalPrincipal ffDemenagement = (ForFiscalPrincipal) forsTries.get(0);
			Assert.assertNotNull(ffDemenagement);
			Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffDemenagement.getMotifFermeture());

			// doit être ré-ouvert
			final ForFiscalPrincipal ffpAnnulation = (ForFiscalPrincipal) forsTries.get(1);
			Assert.assertNotNull(ffpAnnulation);
			Assert.assertEquals(MotifFor.ANNULATION, ffpAnnulation.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffpAnnulation.getDateFin());

			// doit être ré-ouvert
			final ForFiscalSecondaire ffsAnnulation = (ForFiscalSecondaire) forsTries.get(2);
			Assert.assertNotNull(ffsAnnulation);
			Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulation.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffsAnnulation.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulation.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffsAnnulation.getNumeroOfsAutoriteFiscale());

			// ne doit pas être ré-ouvert (pas bon motif de fermeture)
			final ForFiscalSecondaire ffsVente = (ForFiscalSecondaire) forsTries.get(3);
			Assert.assertNotNull(ffsVente);
			Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffsVente.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffsVente.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsVente.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Bussigny.getNoOFS(), (int) ffsVente.getNumeroOfsAutoriteFiscale());

			// ne doit pas être ré-ouvert (pas bonne date de fermeture)
			final ForFiscalSecondaire ffsAnnulationPrecedente = (ForFiscalSecondaire) forsTries.get(4);
			Assert.assertNotNull(ffsAnnulationPrecedente);
			Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulationPrecedente.getMotifFermeture());
			Assert.assertEquals(dateDesactivation.addMonths(-1), ffsAnnulationPrecedente.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulationPrecedente.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffsAnnulationPrecedente.getNumeroOfsAutoriteFiscale());
			return null;
		});

		// réactivation et tests
		doInNewTransaction(status -> {
			final RegDate dateReactivation = dateDesactivation.addMonths(6);
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(ppId);
			activationService.reactiveTiers(pp, dateReactivation);

			Assert.assertFalse(pp.isAnnule());
			Assert.assertNull(pp.getDateDesactivation());

			final Set<ForFiscal> fors = pp.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(7, fors.size());

			final List<ForFiscal> forsTries = new ArrayList<>(fors);
			Collections.sort(forsTries, new ForFiscalComparator());

			final ForFiscalPrincipal ffDemenagement = (ForFiscalPrincipal) forsTries.get(0);
			Assert.assertNotNull(ffDemenagement);
			Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffDemenagement.getMotifFermeture());

			final ForFiscalPrincipal ffpAnnulation = (ForFiscalPrincipal) forsTries.get(1);
			Assert.assertNotNull(ffpAnnulation);
			Assert.assertEquals(MotifFor.ANNULATION, ffpAnnulation.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffpAnnulation.getDateFin());

			final ForFiscalPrincipal ffpReactivation = (ForFiscalPrincipal) forsTries.get(2);
			Assert.assertNotNull(ffpReactivation);
			Assert.assertEquals(MotifFor.REACTIVATION, ffpReactivation.getMotifOuverture());
			Assert.assertEquals(dateReactivation, ffpReactivation.getDateDebut());
			Assert.assertNull(ffpReactivation.getMotifFermeture());
			Assert.assertNull(ffpReactivation.getDateFin());

			final ForFiscalSecondaire ffsAnnulation = (ForFiscalSecondaire) forsTries.get(3);
			Assert.assertNotNull(ffsAnnulation);
			Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulation.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffsAnnulation.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulation.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffsAnnulation.getNumeroOfsAutoriteFiscale());

			// ne doit pas être ré-ouvert (pas bon motif de fermeture)
			final ForFiscalSecondaire ffsVente = (ForFiscalSecondaire) forsTries.get(4);
			Assert.assertNotNull(ffsVente);
			Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffsVente.getMotifFermeture());
			Assert.assertEquals(dateDesactivation, ffsVente.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsVente.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Bussigny.getNoOFS(), (int) ffsVente.getNumeroOfsAutoriteFiscale());

			// ne doit pas être ré-ouvert (pas bonne date de fermeture)
			final ForFiscalSecondaire ffsAnnulationPrecedente = (ForFiscalSecondaire) forsTries.get(5);
			Assert.assertNotNull(ffsAnnulationPrecedente);
			Assert.assertEquals(MotifFor.ANNULATION, ffsAnnulationPrecedente.getMotifFermeture());
			Assert.assertEquals(dateDesactivation.addMonths(-1), ffsAnnulationPrecedente.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsAnnulationPrecedente.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Bex.getNoOFS(), (int) ffsAnnulationPrecedente.getNumeroOfsAutoriteFiscale());

			final ForFiscalSecondaire ffsReactivation = (ForFiscalSecondaire) forsTries.get(6);
			Assert.assertNotNull(ffsReactivation);
			Assert.assertEquals(MotifFor.REACTIVATION, ffsReactivation.getMotifOuverture());
			Assert.assertEquals(dateReactivation, ffsReactivation.getDateDebut());
			Assert.assertNull(ffsReactivation.getMotifFermeture());
			Assert.assertNull(ffsReactivation.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffsReactivation.getTypeAutoriteFiscale());
			Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffsReactivation.getNumeroOfsAutoriteFiscale());
			return null;
		});
	}

	@Test
	public void testDesactivationDebiteurPrestationImposable() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateDesactivation = date(2009, 10, 31);

		// mise en place
		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

			final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

			addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
			addRapportPrestationImposable(dpi, pp2, dateDebut, dateDesactivation.addMonths(-1), false);
			return dpi.getNumero();
		});

		// désactivation
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			activationService.desactiveTiers(dpi, dateDesactivation);
			return null;
		});

		// vérification de l'état des fors et des rapports de travail
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);

			// plus de for ouvert au jour d'aujourd'hui
			final ForDebiteurPrestationImposable forCourant = dpi.getForDebiteurPrestationImposableAt(null);
			assertNull(forCourant);

			// le for précédemment ouvert a été fermé à la date de désactivation
			final ForDebiteurPrestationImposable forFerme = dpi.getForDebiteurPrestationImposableAt(dateDesactivation);
			assertNotNull(forFerme);
			assertEquals(dateDesactivation, forFerme.getDateFin());
			assertEquals(MotifFor.ANNULATION, forFerme.getMotifFermeture());
			assertEquals(dateDebut, forFerme.getDateDebut());
			assertEquals(MotifFor.INDETERMINE, forFerme.getMotifOuverture());
			assertFalse(forFerme.isAnnule());

			// les rapports de travail encore ouverts ont été fermés à la date de désactivation
			final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(2, rapports.size());

			boolean foundExOpen = false;
			boolean foundAlreadyClosed = false;
			for (RapportEntreTiers r : rapports) {
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertFalse(r.isAnnule());
				assertNotNull(r.getDateFin());
				if (dateDesactivation.equals(r.getDateFin())) {
					assertFalse(foundExOpen);
					foundExOpen = true;
				}
				else {
					assertFalse(foundAlreadyClosed);
					assertEquals(dateDesactivation.addMonths(-1), r.getDateFin());
					foundAlreadyClosed = true;
				}
			}
			assertTrue(foundExOpen);
			assertTrue(foundAlreadyClosed);
			return null;
		});
	}

	@Test
	public void testDesactivationEtReactivationDebiteurPrestationImposable() throws Exception {

		final RegDate dateDebut = date(2009, 1, 1);
		final RegDate dateDesactivation = date(2009, 10, 31);

		// mise en place
		final long dpiId = (Long) doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebut);
			addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Bex);

			final PersonnePhysique pp1 = addNonHabitant("Draco", "Malfoy", date(1980, 10, 25), Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Weasley", "Ronnald", date(1980, 5, 12), Sexe.MASCULIN);

			addRapportPrestationImposable(dpi, pp1, dateDebut, null, false);
			addRapportPrestationImposable(dpi, pp2, dateDebut, dateDesactivation.addMonths(-1), false);
			return dpi.getNumero();
		});

		// désactivation
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			activationService.desactiveTiers(dpi, dateDesactivation);
			return null;
		});

		// vérification de l'état des fors et des rapports de travail
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);

			// plus de for ouvert au jour d'aujourd'hui
			final ForDebiteurPrestationImposable forCourant = dpi.getForDebiteurPrestationImposableAt(null);
			assertNull(forCourant);

			// le for précédemment ouvert a été fermé à la date de désactivation
			final ForDebiteurPrestationImposable forFerme = dpi.getForDebiteurPrestationImposableAt(dateDesactivation);
			assertNotNull(forFerme);
			assertEquals(dateDesactivation, forFerme.getDateFin());
			assertEquals(MotifFor.ANNULATION, forFerme.getMotifFermeture());
			assertEquals(dateDebut, forFerme.getDateDebut());
			assertEquals(MotifFor.INDETERMINE, forFerme.getMotifOuverture());
			assertFalse(forFerme.isAnnule());

			// les rapports de travail encore ouverts ont été fermés à la date de désactivation
			final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(2, rapports.size());

			boolean foundExOpen = false;
			boolean foundAlreadyClosed = false;
			for (RapportEntreTiers r : rapports) {
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertFalse(r.isAnnule());
				assertNotNull(r.getDateFin());
				if (dateDesactivation.equals(r.getDateFin())) {
					assertFalse(foundExOpen);
					foundExOpen = true;
				}
				else {
					assertFalse(foundAlreadyClosed);
					assertEquals(dateDesactivation.addMonths(-1), r.getDateFin());
					foundAlreadyClosed = true;
				}
			}
			assertTrue(foundExOpen);
			assertTrue(foundAlreadyClosed);
			return null;
		});

		final RegDate dateReactivation = date(2010, 5, 1);

		// réactivation du tiers
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
			activationService.reactiveTiers(dpi, dateReactivation);
			return null;
		});

		// vérification des fors et rapports de travail après ré-activation
		doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);

			// il y a maintenant un for ouvert depuis la réactivation
			final ForDebiteurPrestationImposable forCourant = dpi.getForDebiteurPrestationImposableAt(null);
			assertNotNull(forCourant);
			assertEquals(dateReactivation, forCourant.getDateDebut());
			assertEquals(MotifFor.REACTIVATION, forCourant.getMotifOuverture());
			assertFalse(forCourant.isAnnule());

			// le for précédemment ouvert est resté fermé à la date de désactivation
			final ForDebiteurPrestationImposable forFerme = dpi.getForDebiteurPrestationImposableAt(dateDesactivation);
			assertNotNull(forFerme);
			assertEquals(dateDesactivation, forFerme.getDateFin());
			assertEquals(MotifFor.ANNULATION, forFerme.getMotifFermeture());
			assertEquals(dateDebut, forFerme.getDateDebut());
			assertEquals(MotifFor.INDETERMINE, forFerme.getMotifOuverture());
			assertFalse(forFerme.isAnnule());

			// le rapport de travail qui avait été fermé doit avoir été ré-ouvert à la date de réactivation
			final Set<RapportEntreTiers> rapports = dpi.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(3, rapports.size());

			final List<RapportEntreTiers> rapportsTries = new ArrayList<>(rapports);
			Collections.sort(rapportsTries, new DateRangeComparator<>());

			{
				final RapportEntreTiers r = rapportsTries.get(0);
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertEquals(dateDesactivation.addMonths(-1), r.getDateFin());
				assertFalse(r.isAnnule());
			}
			{
				final RapportEntreTiers r = rapportsTries.get(1);
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateDebut, r.getDateDebut());
				assertEquals(dateDesactivation, r.getDateFin());
				assertFalse(r.isAnnule());
			}
			{
				final RapportEntreTiers r = rapportsTries.get(2);
				assertNotNull(r);
				assertInstanceOf(RapportPrestationImposable.class, r);
				assertEquals(dateReactivation, r.getDateDebut());
				assertNull(r.getDateFin());
				assertFalse(r.isAnnule());
			}
			return null;
		});
	}
}
