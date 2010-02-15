package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.uniregctb.declaration.*;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.DeclarationsCache;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults.Ignore;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults.IgnoreType;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;

@SuppressWarnings({"JavaDoc"})
public class EnvoiDIsEnMasseProcessorTest extends BusinessTest {

	private EnvoiDIsEnMasseProcessor processor;
	private HibernateTemplate hibernateTemplate;
	private ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		TiersService tiersService = getBean(TiersService.class, "tiersService");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, delaisService,
				diService, 100, transactionManager);

		// évite de logger plein d'erreurs pendant qu'on teste le comportement du processor
		Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.FATAL);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();

		// réactive le log normal
		Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.ERROR);
	}

	@Test
	public void testCalculerDateExpedition() {

		final RegDate lundi = RegDate.get(2008, 1, 7);
		final RegDate mercredi = RegDate.get(2008, 1, 9);

		int delai = parametreAppService.getDelaiCadevImpressionDeclarationImpot();

		// lundi + 3 => jeudi
		assertEquals(lundi.addDays(delai), processor.calculerDateExpedition(lundi));

		// mercredi + 3 => samedi, poussé au lundi suivant (+2)
		assertEquals(mercredi.addDays(delai).addDays(2), processor.calculerDateExpedition(mercredi));

// TODO (FNR) A supprimer ou à decommenter suivant la reponse de thierry concernant les delais attente CADEV
//		final RegDate mercredi8Avril2009 = RegDate.get(2009, 4, 8);
//		final RegDate mercredi24Dec2008 = RegDate.get(2008, 12, 24);
//		// test avec un jour férié mobile :
//		// vendredi 8 avril 2009 + 3 => C'est le mercredi avant le weekend de Pâques,
//		// le délai doit être décalé de 4 jours (vendredi-saint + week-end + Lundi de pâques)
//		assertEquals(
//				mercredi8Avril2009.addDays(delai).addDays(4),
//				processor.calculerDateExpedition(mercredi8Avril2009)
//		);
//
//		// test avec un jour férié fixe :
//		// mercredi 24 décembre 2008 + 3 => C'est le mercredi avant noel,
//		// le délai doit être décalé de 3 jours (noel + week-end)
//		assertEquals(
//				mercredi24Dec2008.addDays(delai).addDays(3),
//				processor.calculerDateExpedition(mercredi24Dec2008)
//		);
	}

	@Test
	public void testEstIndigent() {

		// Contribuable sans for fiscal
		Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(EnvoiDIsEnMasseProcessor.estIndigent(erich, null));

		// Contribuable avec for fiscal et mode d'imposition normal
		Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Orbe);
		assertFalse(EnvoiDIsEnMasseProcessor.estIndigent(maxwell, null));

		// Contribuable avec for fiscal et mode d'imposition indigent
		Contribuable job = addNonHabitant("Job", "Berger", date(1955, 1, 1), Sexe.MASCULIN);
		ForFiscalPrincipal f = addForPrincipal(job, date(1980, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.LesClees);
		f.setModeImposition(ModeImposition.INDIGENT);
		assertTrue(EnvoiDIsEnMasseProcessor.estIndigent(job, null));

		// Contribuable avec plusieurs fors fiscaux, indigents ou non
		Contribuable girou = addNonHabitant("Girou", "Ette", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(girou, date(1973, 1, 1), MotifFor.MAJORITE, date(1979, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.LesClees);
		f = addForPrincipal(girou, date(1980, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, date(1982, 12, 31), MotifFor.CHGT_MODE_IMPOSITION,
				MockCommune.LesClees);
		f.setModeImposition(ModeImposition.INDIGENT);
		addForPrincipal(girou, date(1983, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.LesClees);
		assertFalse(EnvoiDIsEnMasseProcessor.estIndigent(girou, date(1975, 1, 1)));
		assertTrue(EnvoiDIsEnMasseProcessor.estIndigent(girou, date(1981, 1, 1)));
		assertFalse(EnvoiDIsEnMasseProcessor.estIndigent(girou, date(2000, 1, 1)));
		assertFalse(EnvoiDIsEnMasseProcessor.estIndigent(girou, null));
	}

	@Test
	public void testEstAssujettiDansLeCanton() {

		// Contribuable sans for fiscal
		Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(erich, null));

		// Contribuable avec un for fiscal principal à Neuchâtel
		Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), null, null, null, MockCommune.Neuchatel);
		assertFalse(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(maxwell, null));

		// Contribuable avec un for fiscal principal ouvert à Lausanne
		Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(felicien, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		assertTrue(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(felicien, null));

		// Contribuable avec un for fiscal principal fermé à Lausanne
		Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(bernard, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(1990, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		assertTrue(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(bernard, date(1985, 1, 1)));
		assertFalse(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(bernard, null));
	}

	@Test
	public void testInitCachePeriodeFiscaleInexistante() {

		try {
			processor.initCache(2007, TypeContribuableDI.VAUDOIS_ORDINAIRE);
			fail();
		}
		catch (DeclarationException e) {
			assertEquals("La période fiscale [2007] n'existe pas dans la base de données.", e.getMessage());
		}
	}

	@Test
	public void testInitCacheModelDocumentInexistant() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addPeriodeFiscale(2007);
				return null;
			}
		});

		try {
			processor.initCache(2007, TypeContribuableDI.VAUDOIS_ORDINAIRE);
			fail();
		}
		catch (DeclarationException e) {
			assertEquals("Impossible de trouver le modèle de document pour une déclaration d'impôt "
					+ "pour la période [2007] et le type de document [DECLARATION_IMPOT_COMPLETE_BATCH].", e.getMessage());
		}
	}

	@Test
	public void testCreeDIForGestionInconnu() throws Exception {

		class Ids {
			Long ctb;
			Long tache;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				// Contribuable sans for de gestion
				Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
				ids.ctb = contribuable.getNumero();

				TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null);
				ids.tache = tache.getId();
				return null;
			}
		});

		final RegDate dateTraitement = date(2008, 1, 23);
		final EnvoiDIsResults rapport = new EnvoiDIsResults(2007, TypeContribuableDI.VAUDOIS_ORDINAIRE, dateTraitement, 10, null, null);
		final DeclarationsCache cache = processor.new DeclarationsCache(2007, Arrays.asList(ids.ctb));
		final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.tache);

		processor.setRapport(rapport);
		assertNull(processor.creeDI(tache, cache, false));
	}

	@Test
	public void testGetDeclarationsInRange() throws Exception {

		class Ids {
			Long marcId;
			Long jeanId;
			Long jacquesId;
			Long pierreId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un tiers sans déclaration
				Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();

				// Un tiers avec une déclaration sur toute l'année 2007
				Contribuable jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				addDeclarationImpot(jean, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete);

				// Un tiers avec une déclaration sur une partie de l'année 2007
				Contribuable jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jacquesId = jacques.getNumero();
				addDeclarationImpot(jacques, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete);

				// Un tiers avec deux déclarations partielles dans l'année 2007
				Contribuable pierre = addNonHabitant("Pierre", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.pierreId = pierre.getNumero();
				addDeclarationImpot(pierre, periode2007, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete);
				addDeclarationImpot(pierre, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete);
				return null;
			}
		});

		{
			List<Long> idsList = new ArrayList<Long>();
			idsList.add(ids.marcId);
			idsList.add(ids.jeanId);
			idsList.add(ids.jacquesId);
			idsList.add(ids.pierreId);

			DeclarationsCache cache = processor.new DeclarationsCache(2007, idsList);

			final Contribuable marc = (Contribuable) hibernateTemplate.get(Contribuable.class, ids.marcId);
			assertNotNull(marc);
			assertEmpty(cache.getDeclarationsInRange(marc, new Range(date(2007, 1, 1), date(2007, 12, 31))));

			final Contribuable jean = (Contribuable) hibernateTemplate.get(Contribuable.class, ids.jeanId);
			assertNotNull(jean);
			final List<DeclarationImpotOrdinaire> jeanDIs = cache.getDeclarationsInRange(jean, new Range(date(2007, 1, 1), date(2007, 12,
					31)));
			assertEquals(1, jeanDIs.size());
			assertDI(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI, null, jeanDIs.get(0));

			final Contribuable jacques = (Contribuable) hibernateTemplate.get(Contribuable.class, ids.jacquesId);
			assertNotNull(jacques);
			final List<DeclarationImpotOrdinaire> jacquesDIs = cache.getDeclarationsInRange(jacques, new Range(date(2007, 1, 1), date(2007,
					12, 31)));
			assertEquals(1, jacquesDIs.size());
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI, null, jacquesDIs.get(0));

			final Contribuable pierre = (Contribuable) hibernateTemplate.get(Contribuable.class, ids.pierreId);
			assertNotNull(pierre);
			final List<DeclarationImpotOrdinaire> pierreDIs = cache.getDeclarationsInRange(pierre, new Range(date(2007, 1, 1), date(2007,
					12, 31)));
			assertEquals(2, pierreDIs.size());
			assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI, null, pierreDIs.get(0));
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI, null, pierreDIs.get(1));
		}
	}

	@Test
	public void testPlusieursDiSurMemePeriodeFiscaleEnMemeTemps() throws Exception {
		class Ids {
			Long marcId;
			Long tache1Id;
			Long tache2Id;
		}
		final Ids ids = new Ids();

		// initialisation des contribuables, fors, tâches
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// for fiscal ouvert à Lausanne -> assujetti
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1961, 3, 12), Sexe.MASCULIN);
				addForPrincipal(marc, date(1980, 1, 1), MotifFor.ARRIVEE_HC, date(2007,3,31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(marc, date(2007, 7, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				final TacheEnvoiDeclarationImpot tache1 = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.tache1Id = tache1.getId();
				final TacheEnvoiDeclarationImpot tache2 = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.tache2Id = tache2.getId();
				return null;
			}
		});

		// traitement des tâches
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateTraitement = date(2009, 1, 15);
				final List<Long> idsCtb = Arrays.asList(ids.marcId);

				final EnvoiDIsResults rapport = new EnvoiDIsResults(2007, TypeContribuableDI.VAUDOIS_ORDINAIRE, dateTraitement, 10, null, null);
				processor.setRapport(rapport);
				processor.traiterBatch(idsCtb, 2007, TypeContribuableDI.VAUDOIS_ORDINAIRE, dateTraitement);
				return null;
			}
		});

		// vérification des DIs
		{
			final List<DeclarationImpotOrdinaire> dis = hibernateTemplate.loadAll(DeclarationImpotOrdinaire.class);
			assertNotNull(dis);
			assertEquals(2, dis.size());

			// tri selon le numéro de séquence
			final List<DeclarationImpotOrdinaire> diTriees = new ArrayList<DeclarationImpotOrdinaire>(dis);
			Collections.sort(diTriees, new Comparator<DeclarationImpotOrdinaire>() {
				public int compare(DeclarationImpotOrdinaire o1, DeclarationImpotOrdinaire o2) {
					return o1.getNumero() - o2.getNumero();
				}
			});

			assertEquals(1, (int) diTriees.get(0).getNumero());
			assertEquals(2, (int) diTriees.get(1).getNumero());
		}
	}

	/**
	 * Vérifie que le batch d'envoi des dis ne traite pas les contribuables qui possède une date d'exclusion non échue.
	 */
	@Test
	public void testTraiterTacheCtbExclu() throws Exception {

		class Ids {
			Long marcId;
			Long jeanId;
			Long jacquesId;
			Long tacheMarcId;
			Long tacheJeanId;
			Long tacheJacquesId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un tiers sans exclusion
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();

				// Un tiers avec une exclusion passée
				final Contribuable jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				jean.setDateLimiteExclusionEnvoiDeclarationImpot(date(1995, 1, 1));
				ids.jeanId = jean.getNumero();

				// Un tiers avec une exclusion dans le futur
				final Contribuable jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				jacques.setDateLimiteExclusionEnvoiDeclarationImpot(date(2010, 1, 1));
				ids.jacquesId = jacques.getNumero();

				final TacheEnvoiDeclarationImpot tacheMarc = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.tacheMarcId = tacheMarc.getId();

				final TacheEnvoiDeclarationImpot tacheJean = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jean, null);
				ids.tacheJeanId = tacheJean.getId();

				final TacheEnvoiDeclarationImpot tacheJacques = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1,
						1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jacques, null);
				ids.tacheJacquesId = tacheJacques.getId();

				return null;
			}
		});

		{
			final List<Long> idsList = new ArrayList<Long>();
			idsList.add(ids.marcId);
			idsList.add(ids.jeanId);
			idsList.add(ids.jacquesId);

			final RegDate dateTraitement = date(2009, 1, 15);
			final DeclarationsCache cache = processor.new DeclarationsCache(2008, idsList);

			final EnvoiDIsResults rapport = new EnvoiDIsResults(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, dateTraitement, 10, null, null);
			processor.setRapport(rapport);

			// Le tiers sans exclusion
			final TacheEnvoiDeclarationImpot tacheMarc = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(
					TacheEnvoiDeclarationImpot.class, ids.tacheMarcId);
			assertTrue(processor.traiterTache(tacheMarc, dateTraitement, cache));
			assertEmpty(rapport.ctbsIgnores);

			// Le tiers avec une exclusion passée
			final TacheEnvoiDeclarationImpot tacheJean = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(
					TacheEnvoiDeclarationImpot.class, ids.tacheJeanId);
			assertTrue(processor.traiterTache(tacheJean, dateTraitement, cache));
			assertEmpty(rapport.ctbsIgnores);

			// Le tiers avec une exclusion dans le futur -> doit être exclu
			final TacheEnvoiDeclarationImpot tacheJacques = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(
					TacheEnvoiDeclarationImpot.class, ids.tacheJacquesId);
			assertFalse(processor.traiterTache(tacheJacques, dateTraitement, cache));
			assertEquals(1, rapport.ctbsIgnores.size());
			final Ignore ignore = rapport.ctbsIgnores.get(0);
			assertEquals(IgnoreType.CTB_EXCLU, ignore.raison);
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	public void testEnvoiDIsContribuableDeuxTachesSuiteDecesTraiteTardivement() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
			Long premiereTacheId;
			Long secondeTacheId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur toute l'année 2008
				final TacheEnvoiDeclarationImpot premiereTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement de décès en retard -> fermeture du for principal au milieu 2008 et génération d'une nouvelle tâche correspondante
				ffp.setDateFin(dateDeces);
				ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});

		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// la première tâche est en erreur
		final List<EnvoiDIsResults.Erreur> erreurs = results.ctbsEnErrors;
		assertEquals(1, erreurs.size());
		final EnvoiDIsResults.Erreur erreur = erreurs.get(0);
		assertEquals(ids.marcId.longValue(), erreur.noCtb);
		assertEquals("La tâche [id=" + ids.premiereTacheId + ", période=2008.01.01-2008.12.31] est en conflit avec 1 déclaration(s) d'impôt pré-existante(s) sur le contribuable [" + ids.marcId +
				"]. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.", erreur.details);

		final TacheEnvoiDeclarationImpot premiereTache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.premiereTacheId);
		assertEquals(TypeEtatTache.EN_INSTANCE, premiereTache.getEtat());

		// la seconde tâche est traitée
		final List<Long> traites = results.ctbsTraites;
		assertEquals(1, traites.size());
		assertEquals(ids.marcId, traites.get(0));

		final TacheEnvoiDeclarationImpot secondeTache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.secondeTacheId);
		assertEquals(TypeEtatTache.TRAITE, secondeTache.getEtat());

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), dateDeces, TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ServiceInfrastructureService.noCEDI,
				dateTraitement.addDays(60), decl);
	}

	/**
	 * [UNIREG-1791] Si deux tâches se chevauchent, il faut commencer par traiter la plus récente (qui a plus de chances d'être la bonne).
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testEnvoiDIsContribuableDeuxTachesSuiteAnnulationDecesTraiteTardivement() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
			Long premiereTacheId;
			Long secondeTacheId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne décédé courant 2008
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche jusqu'au décès
				final TacheEnvoiDeclarationImpot premiereTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement d'annulation de décès en retard -> réouverture du for principal et génération d'une nouvelle tâche sur toute l'année 2008
				ffp.setDateFin(null);
				ffp.setMotifFermeture(null);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// la première tâche est en erreur
		final List<EnvoiDIsResults.Erreur> erreurs = results.ctbsEnErrors;
		assertEquals(1, erreurs.size());
		final EnvoiDIsResults.Erreur erreur = erreurs.get(0);
		assertEquals(ids.marcId.longValue(), erreur.noCtb);
		assertEquals("La tâche [id=" + ids.premiereTacheId + ", période=2008.01.01-2008.07.31] est en conflit avec 1 déclaration(s) d'impôt pré-existante(s) sur le contribuable [" + ids.marcId +
				"]. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.", erreur.details);

		final TacheEnvoiDeclarationImpot premiereTache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.premiereTacheId);
		assertEquals(TypeEtatTache.EN_INSTANCE, premiereTache.getEtat());

		// la seconde tâche est traitée
		final List<Long> traites = results.ctbsTraites;
		assertEquals(1, traites.size());
		assertEquals(ids.marcId, traites.get(0));

		final TacheEnvoiDeclarationImpot secondeTache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.secondeTacheId);
		assertEquals(TypeEtatTache.TRAITE, secondeTache.getEtat());

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ServiceInfrastructureService.noCEDI, date(2009, 3, 31), decl);
	}

	/**
	 * [UNIREG-1740] Vérifie le calcul des délais accordés (imprimés sur la DI et stockés dans la base) pour un habitant décédés dans l'année.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testDelaisRetoursDIsHabitantDecede() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne décédé courant 2008
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche jusqu'au décès
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// le délai de retour imprimé doit être dateTraitement + 60 jours
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), dateDeces, TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ServiceInfrastructureService.noCEDI, dateTraitement.addDays(60), decl);
	}

	/**
	 * [UNIREG-1740] Vérifie le calcul des délais accordés (imprimés sur la DI et stockés dans la base) pour un habitant standard.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testDelaisRetoursDIsHabitantStandard() throws Exception {

		class Ids {
			Long marcId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur toute l'année
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						marc, null);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// le délai de retour imprimé est le délai réglementaire
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ServiceInfrastructureService.noCEDI, date(2009, 3, 31), decl);
	}

	/**
	 * [UNIREG-1852] Teste que les déclarations des indigents décédés dans l'année sont bien envoyées et non pas retournée immédiatement.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testEnvoiDIContribuableIndigentDecede() throws Exception {

		final RegDate dateDeces = date(2008, 5, 23);

		class Ids {
			Long marcId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable indigent habitant à Lausanne, décédé en 2008
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.INDIGENT);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// [UNIREG-1852] la déclaration doit être émise (et non-retournée immédiatement comme pour les indigents non-décédés) avec la cellule registre comme adresse de retour
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), dateDeces, TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ServiceInfrastructureService.noACI, dateTraitement.addDays(60), decl);
	}

	/**
	 * [UNIREG-1980] Teste que les déclarations des indigents possèdent à la fois l'état 'émis' et l'état 'retourné'
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testEnvoiDIContribuableIndigent() throws Exception {

		class Ids {
			Long marcId;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable indigent habitant à Lausanne
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ffp.setModeImposition(ModeImposition.INDIGENT);
				ids.marcId = marc.getNumero();

				// simulation du traitement du batch de détermination des DIs
				TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = (EnvoiDIsResults) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				return processor.run(2008, TypeContribuableDI.VAUDOIS_ORDINAIRE, null, null, 1000, dateTraitement, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// [UNIREG-1980] la déclaration doit être émise et retournée immédiatement (les deux états)
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertNotNull(decl);

		final List<EtatDeclaration> etats = decl.getEtatsSorted();
		assertNotNull(etats);
		assertEquals(2, etats.size());

		final EtatDeclaration etat0 = etats.get(0);
		assertEquals(TypeEtatDeclaration.EMISE, etat0.getEtat());
		assertEquals(dateTraitement, etat0.getDateObtention());

		final EtatDeclaration etat1 = etats.get(1);
		assertEquals(TypeEtatDeclaration.RETOURNEE, etat1.getEtat());
		assertEquals(dateTraitement, etat1.getDateObtention());

		assertDI(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ServiceInfrastructureService.noCEDI, null, decl);
	}
}
