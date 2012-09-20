package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsEnMasseProcessor.DeclarationsCache;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults.Ignore;
import ch.vd.uniregctb.declaration.ordinaire.EnvoiDIsResults.IgnoreType;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;

import static ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireTest.assertCodeControleIsValid;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class EnvoiDIsEnMasseProcessorTest extends BusinessTest {

	private EnvoiDIsEnMasseProcessor processor;
	private HibernateTemplate hibernateTemplate;
	private ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");

		serviceCivil.setUp(new DefaultMockServiceCivil());

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiDIsEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, delaisService,
				diService, 100, transactionManager, parametreAppService, serviceCivilCacheWarmer);

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
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
	public void testEstAssujettiDansLeCanton() {

		// Contribuable sans for fiscal
		Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(EnvoiDIsEnMasseProcessor.estAssujettiDansLeCanton(erich, null));

		// Contribuable avec un for fiscal principal à Neuchâtel
		Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), null, MockCommune.Neuchatel);
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
	@Transactional(rollbackFor = Throwable.class)
	public void testInitCachePeriodeFiscaleInexistante() {

		try {
			processor.initCache(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE);
			fail();
		}
		catch (DeclarationException e) {
			assertEquals("La période fiscale [2007] n'existe pas dans la base de données.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInitCacheModelDocumentInexistant() throws Exception {

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addPeriodeFiscale(2007);
				return null;
			}
		});

		try {
			processor.initCache(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE);
			fail();
		}
		catch (DeclarationException e) {
			assertEquals("Impossible de trouver le modèle de document pour une déclaration d'impôt "
					+ "pour la période [2007] et le type de document [DECLARATION_IMPOT_COMPLETE_BATCH].", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreeDIForGestionInconnu() throws Exception {

		class Ids {
			Long ctb;
			Long tache;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockCollectiviteAdministrative.ACI);

				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

				// Contribuable sans for de gestion
				Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
				ids.ctb = contribuable.getNumero();

				TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null, null, colAdm);
				ids.tache = tache.getId();
				return null;
			}
		});

		final RegDate dateTraitement = date(2008, 1, 23);
		final EnvoiDIsResults rapport = new EnvoiDIsResults(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null);
		final DeclarationsCache cache = processor.new DeclarationsCache(2007, Arrays.asList(ids.ctb));
		final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.tache);

		processor.setRapport(rapport);
		assertNull(processor.creeDI(tache, cache, false));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetDeclarationsInRange() throws Exception {

		class Ids {
			long marcId;
			long jeanId;
			long jacquesId;
			long pierreId;
			long alfredId;
			long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un tiers sans déclaration
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();

				// Un tiers avec une déclaration sur toute l'année 2007
				final Contribuable jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				addDeclarationImpot(jean, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// Un tiers avec une déclaration sur une partie de l'année 2007
				final Contribuable jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jacquesId = jacques.getNumero();
				addDeclarationImpot(jacques, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// Un tiers avec deux déclarations partielles dans l'année 2007
				final Contribuable pierre = addNonHabitant("Pierre", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.pierreId = pierre.getNumero();
				addDeclarationImpot(pierre, periode2007, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				addDeclarationImpot(pierre, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// un tiers avec trois déclarations dont une annulée dans l'année 2007
				final Contribuable alfred = addNonHabitant("Alfred", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.alfredId = alfred.getNumero();
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(alfred, periode2007, date(2007, 2, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				diAnnulee.setAnnule(true);
				addDeclarationImpot(alfred, periode2007, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				addDeclarationImpot(alfred, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				return null;
			}
		});

		{
			List<Long> idsList = new ArrayList<Long>();
			idsList.add(ids.marcId);
			idsList.add(ids.jeanId);
			idsList.add(ids.jacquesId);
			idsList.add(ids.pierreId);
			idsList.add(ids.alfredId);

			DeclarationsCache cache = processor.new DeclarationsCache(2007, idsList);

			final Contribuable marc = hibernateTemplate.get(Contribuable.class, ids.marcId);
			assertNotNull(marc);
			assertEmpty(cache.getDeclarationsInRange(marc, new Range(date(2007, 1, 1), date(2007, 12, 31)), true));

			final Contribuable jean = hibernateTemplate.get(Contribuable.class, ids.jeanId);
			assertNotNull(jean);
			final List<DeclarationImpotOrdinaire> jeanDIs = cache.getDeclarationsInRange(jean, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(1, jeanDIs.size());
			assertDI(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, jeanDIs.get(0));

			final Contribuable jacques = hibernateTemplate.get(Contribuable.class, ids.jacquesId);
			assertNotNull(jacques);
			final List<DeclarationImpotOrdinaire> jacquesDIs = cache.getDeclarationsInRange(jacques, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(1, jacquesDIs.size());
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, jacquesDIs.get(0));

			final Contribuable pierre = hibernateTemplate.get(Contribuable.class, ids.pierreId);
			assertNotNull(pierre);
			final List<DeclarationImpotOrdinaire> pierreDIs = cache.getDeclarationsInRange(pierre, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(2, pierreDIs.size());
			assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, pierreDIs.get(0));
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, pierreDIs.get(1));

			final Contribuable alfred = hibernateTemplate.get(Contribuable.class, ids.alfredId);
			assertNotNull(alfred);
			final List<DeclarationImpotOrdinaire> alfredIdAvecAnnulees = cache.getDeclarationsInRange(alfred, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(3, alfredIdAvecAnnulees.size());
			assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(0));
			assertDI(date(2007, 2, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(1));
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(2));

			final List<DeclarationImpotOrdinaire> alfredIdSansAnnulees = cache.getDeclarationsInRange(alfred, new Range(date(2007, 1, 1), date(2007, 12, 31)), false);
			assertEquals(2, alfredIdSansAnnulees.size());
			assertDI(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdSansAnnulees.get(0));
			assertDI(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdSansAnnulees.get(1));
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPlusieursDiSurMemePeriodeFiscaleEnMemeTemps() throws Exception {
		class Ids {
			Long marcId;
			Long tache1Id;
			Long tache2Id;
		}
		final Ids ids = new Ids();

		// initialisation des contribuables, fors, tâches
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);

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
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tache1Id = tache1.getId();
				final TacheEnvoiDeclarationImpot tache2 = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tache2Id = tache2.getId();
				return null;
			}
		});

		// traitement des tâches
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateTraitement = date(2009, 1, 15);
				final List<Long> idsCtb = Arrays.asList(ids.marcId);

				final EnvoiDIsResults rapport = new EnvoiDIsResults(2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null);
				processor.setRapport(rapport);
				processor.traiterBatch(idsCtb, 2007, CategorieEnvoiDI.VAUDOIS_COMPLETE, dateTraitement);
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
				@Override
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
	@Transactional(rollbackFor = Throwable.class)
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

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);

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
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tacheMarcId = tacheMarc.getId();

				final TacheEnvoiDeclarationImpot tacheJean = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jean, null, null, colAdm);
				ids.tacheJeanId = tacheJean.getId();

				final TacheEnvoiDeclarationImpot tacheJacques = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1,
						1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jacques, null, null, colAdm);
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

			final EnvoiDIsResults rapport = new EnvoiDIsResults(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null);
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
			final Ignore ignore = (Ignore) rapport.ctbsIgnores.get(0);
			assertEquals(IgnoreType.CTB_EXCLU, ignore.raison);
		}
	}

	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIsContribuableDeuxTachesSuiteDecesTraiteTardivement() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
			Long premiereTacheId;
			Long secondeTacheId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

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
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement de décès en retard -> fermeture du for principal au milieu 2008 et génération d'une nouvelle tâche correspondante
				ffp.setDateFin(dateDeces);
				ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});

		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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
		assertDI(date(2008, 1, 1), dateDeces, TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi,
				calculerDateDelaiImprime(dateTraitement, 3, 60), decl);
	}

	private static RegDate calculerDateDelaiImprime(RegDate dateTraitement, int delaiExpedition, int delaiRetour) {
		final RegDate dateExpedition = ajouteJours(dateTraitement, delaiExpedition, true);
		return ajouteJours(dateExpedition, delaiRetour, true);
	}

	private static RegDate ajouteJours(RegDate date, int nbJours, boolean shiftSiWeekEnd) {
		final RegDate dateDecaleeBrute = date.addDays(nbJours);
		final RegDate dateDecalee;
		if (shiftSiWeekEnd) {
			final RegDate.WeekDay weekDay = dateDecaleeBrute.getWeekDay();
			switch (weekDay) {
				case SATURDAY:
					dateDecalee = dateDecaleeBrute.addDays(2);
					break;
				case SUNDAY:
					dateDecalee = dateDecaleeBrute.addDays(1);
					break;
				default:
					dateDecalee = dateDecaleeBrute;
					break;
			}
		}
		else {
			dateDecalee = dateDecaleeBrute;
		}
		return dateDecalee;
	}

	/**
	 * [UNIREG-1791] Si deux tâches se chevauchent, il faut commencer par traiter la plus récente (qui a plus de chances d'être la bonne).
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIsContribuableDeuxTachesSuiteAnnulationDecesTraiteTardivement() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
			Long premiereTacheId;
			Long secondeTacheId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

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
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement d'annulation de décès en retard -> réouverture du for principal et génération d'une nouvelle tâche sur toute l'année 2008
				ffp.setDateFin(null);
				ffp.setMotifFermeture(null);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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
				ids.oidCedi, date(2009, 3, 31), decl);
	}

	/**
	 * [UNIREG-1740] Vérifie le calcul des délais accordés (imprimés sur la DI et stockés dans la base) pour un habitant décédés dans l'année.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDelaisRetoursDIsHabitantDecede() throws Exception {

		final RegDate dateDeces = date(2008, 7, 31);

		class Ids {
			Long marcId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne décédé courant 2008
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche jusqu'au décès
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
						dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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
				ids.oidCedi, calculerDateDelaiImprime(dateTraitement, 3 , 60), decl);
	}

	/**
	 * [UNIREG-1740] Vérifie le calcul des délais accordés (imprimés sur la DI et stockés dans la base) pour un habitant standard.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDelaisRetoursDIsHabitantStandard() throws Exception {

		class Ids {
			Long marcId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);

				// Un contribuable habitant à Lausanne
				final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur toute l'année
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
						marc, null, null, colAdm);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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
				ids.oidCedi, date(2009, 3, 31), decl);
	}

	/**
	 * [UNIREG-1976] Vérifie les nouvelles règles sur les délais en cas d'envoi en masse
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAjouterDelaisDeRetourInitial() throws Exception {

		addCollAdm(MockCollectiviteAdministrative.CEDI);
		final PeriodeFiscale periode = addPeriodeFiscale(2008);
		final ModeleDocument modeleComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		final ModeleDocument modeleVaudtax = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
		final ModeleDocument modeleDepense = addModeleDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE, periode);
		final ModeleDocument modeleHCImmeuble = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode);

		final PersonnePhysique vd = addNonHabitant("Julien", "Brouette", date(1970, 1, 1), Sexe.MASCULIN);
		addForPrincipal(vd, date(2000,1,1), MotifFor.ARRIVEE_HC, MockCommune.Renens);

		final PersonnePhysique hc = addNonHabitant("Arnold", "Kunz", date(1970, 1, 1), Sexe.MASCULIN);
		addForPrincipal(hc, date(2000,1,1), MotifFor.ARRIVEE_HC, MockCommune.Zurich);

		final PersonnePhysique hs = addNonHabitant("Pedro", "Gonzales", date(1970, 1, 1), Sexe.MASCULIN);
		addForPrincipal(hs, date(2000,1,1), MotifFor.ARRIVEE_HC, MockPays.Espagne);

		final RegDate delaiReglementaire = date(2009, 3, 31);
		final RegDate delaiEffectif = date(2009, 6, 30);
		final RegDate dateTraitement = date(2009, 1, 15);
		final RegDate dateExpedition = date(2009, 1, 31);

		// Vaudois ordinaire - DI complète
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Vaudois ordinaire - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Vaudois dépense - DI dépense
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modeleDepense);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_DEPENSE, modeleDepense);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton immeuble - DI HC immeuble
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleHCImmeuble);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleHCImmeuble);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton activité indépendante - DI complète
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton activité indépendante - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-Suisse - DI complète
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-Suisse - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Diplomate Suisse - DI complète
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Diplomate Suisse - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinaire di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}
	}

	/**
	 * [UNIREG-1852] Teste que les déclarations des indigents décédés dans l'année sont bien envoyées et non pas retournée immédiatement.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIContribuableIndigentDecede() throws Exception {

		final RegDate dateDeces = date(2008, 5, 23);

		class Ids {
			Long marcId;
			Long aci;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative aci = addCollAdm(MockOfficeImpot.ACI);
				ids.aci = aci.getId();

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
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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
				ids.aci, calculerDateDelaiImprime(dateTraitement, 3, 60), decl);
	}

	/**
	 * [UNIREG-1952] Teste que les déclarations des  décédés en fin d'année[15.11 - 31.12] ne sont pas envoyées.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIContribuableDecedeFinAnnee() throws Exception {

		final RegDate dateDeces = date(2008, 11, 26);

		class Ids {
			Long marcId;
			Long aci;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative aci = addCollAdm(MockOfficeImpot.ACI);
				ids.aci = aci.getId();

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
				ffp.setModeImposition(ModeImposition.ORDINAIRE);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, true, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);
		assertEquals(1, results.ctbsIgnores.size());
		Ignore ignore =(Ignore)results.ctbsIgnores.get(0);
		assertEquals("Décédé en fin d'année",ignore.details);


		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		//[UNIREG-1952] il ne doit pas y avoir de DI émise
		assertEmpty(declarations);
	}

	/**
	 * [UNIREG-1952] Teste que les déclarations des  décédés en fin d'année[15.11 - 31.12] ne sont pas envoyées.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIContribuableDecedeMilieuAnnee() throws Exception {

		final RegDate dateDeces = date(2008, 5, 23);

		class Ids {
			Long marcId;
			Long aci;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative aci = addCollAdm(MockOfficeImpot.ACI);
				ids.aci = aci.getId();

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
				ffp.setModeImposition(ModeImposition.ORDINAIRE);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, true, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire");
		//[UNIREG-1952] Dans le cas ou l'exclusion des décédés est activée, il faut qu'un décédé avant la date d'eclusion soit traité normalement 
		assertNotNull(declarations);
		assertEquals(1, declarations.size());
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDI(date(2008, 1, 1), dateDeces, TypeEtatDeclaration.EMISE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ids.aci, calculerDateDelaiImprime(dateTraitement, 3, 60), decl);
	}

	/**
	 * [UNIREG-1980] Teste que les déclarations des indigents possèdent à la fois l'état 'émis' et l'état 'retourné'
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEnvoiDIContribuableIndigent() throws Exception {

		class Ids {
			Long marcId;
			Long oidCedi;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final CollectiviteAdministrative cedi = addCollAdm(MockCollectiviteAdministrative.CEDI);
				ids.oidCedi = cedi.getId();

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
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(2008, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
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

		final EtatDeclarationRetournee retour1 = (EtatDeclarationRetournee) etat1;
		assertEquals(dateTraitement, retour1.getDateObtention());
		assertEquals("INDIGENT", retour1.getSource());

		assertDI(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDeclaration.RETOURNEE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				ids.oidCedi, date(2009, 3, 31), decl);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroSequenceApresDiAnnulee() throws Exception {

		final int annee = 2009;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diAnnulee.setAnnule(true);
				assertEquals(1, (int) diAnnulee.getNumero());

				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final TacheEnvoiDeclarationImpot t = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON,
						TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(annee, CategorieEnvoiDI.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decls.get(0);
				assertNotNull(di);
				assertFalse(di.isAnnule());
				assertEquals(2, (int) di.getNumero());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1368] Vérifie qu'aucun code de contrôle n'est générée ou assignée lors des différents scénarios d'ajout d'une déclaration d'impôt ordinaire à un tiers pour les périodes fiscales avant 2011.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationCodeControleAvant2011() throws Exception {

		final int annee = 2010;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(annee, CategorieEnvoiDI.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decls.get(0);
				assertNotNull(di);
				assertNull(di.getCodeControle());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1368] Vérifie que le code de contrôle d'une première déclaration pour une période fiscale (et pour un contribuable) est bien généré et attribué.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationCodeControleSansDeclarationPreexistante() throws Exception {

		final int annee = 2011;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(annee, CategorieEnvoiDI.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decls.get(0);
				assertNotNull(di);
				assertCodeControleIsValid(di.getCodeControle());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1368] Vérifie que le code de contrôle d'une deuxième déclaration dans la même période fiscale (et pour le même contribuable) est le même que celui de la première déclaration.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationCodeControleAvecDeclarationPreexistante() throws Exception {

		final int annee = 2011;
		final String codeControle = DeclarationImpotOrdinaire.generateCodeControle();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, date(annee,3,31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(pp, date(annee, 4, 1), MotifFor.DEPART_HS, date(annee,8,31), MotifFor.ARRIVEE_HS, MockPays.Colombie);
				addForPrincipal(pp, date(annee, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				final DeclarationImpotOrdinaire decl = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				decl.setCodeControle(codeControle);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 9, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(annee, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
				assertNotNull(decls);
				assertEquals(2, decls.size());

				final DeclarationImpotOrdinaire di0 = (DeclarationImpotOrdinaire) decls.get(0);
				assertNotNull(di0);
				assertEquals(codeControle, di0.getCodeControle());

				// on vérifie que le code de contrôle de la deuxième DI est le même que celui de la première
				final DeclarationImpotOrdinaire di1 = (DeclarationImpotOrdinaire) decls.get(1);
				assertNotNull(di1);
				assertEquals(codeControle, di1.getCodeControle());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-1368] Vérifie que le code de contrôle est bien généré et assigné sur toutes les déclarations d'une année fiscale (cas des déclarations préexistantes sans code de contrôle).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGenerationCodeControleAvecDeclarationPreexistanteMaisSansCode() throws Exception {

		final int annee = 2011;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, date(annee,3,31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(pp, date(annee, 4, 1), MotifFor.DEPART_HS, date(annee,8,31), MotifFor.ARRIVEE_HS, MockPays.Colombie);
				addForPrincipal(pp, date(annee, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addCollAdm(MockCollectiviteAdministrative.CEDI);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final CollectiviteAdministrative colAdm = addCollAdm(MockOfficeImpot.OID_LAUSANNE_OUEST);
				// une déclaration sans code de contrôle
				addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 9, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsResults results = doInNewTransaction(new TxCallback<EnvoiDIsResults>() {
			@Override
			public EnvoiDIsResults execute(TransactionStatus status) throws Exception {
				return processor.run(annee, CategorieEnvoiDI.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, null);
			}
		});
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// on vérifie que les deux DIs ont reçu le même code de contrôle
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
				assertNotNull(decls);
				assertEquals(2, decls.size());

				final DeclarationImpotOrdinaire di0 = (DeclarationImpotOrdinaire) decls.get(0);
				assertNotNull(di0);
				final String codeControle = di0.getCodeControle();
				assertCodeControleIsValid(codeControle);

				final DeclarationImpotOrdinaire di1 = (DeclarationImpotOrdinaire) decls.get(1);
				assertNotNull(di1);
				assertEquals(codeControle, di1.getCodeControle());
				return null;
			}
		});
	}

}
