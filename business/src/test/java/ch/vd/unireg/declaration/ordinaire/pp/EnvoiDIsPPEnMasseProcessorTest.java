package ch.vd.unireg.declaration.ordinaire.pp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.ordinaire.pp.AbstractEnvoiDIsPPResults.Ignore;
import ch.vd.unireg.declaration.ordinaire.pp.AbstractEnvoiDIsPPResults.IgnoreType;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiDIsPPEnMasseProcessor.DeclarationsCache;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPP;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Qualification;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.validation.EntityValidator;
import ch.vd.unireg.validation.ValidationService;

import static ch.vd.unireg.declaration.DeclarationImpotOrdinairePPTest.assertCodeControleIsValid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class EnvoiDIsPPEnMasseProcessorTest extends BusinessTest {

	private static final int TAILLE_LOT = 100;

	private EnvoiDIsPPEnMasseProcessor processor;
	private ParametreAppService parametreAppService;
	private AdresseService adresseService;
	private ModificationInterceptor modificationInterceptor;
	private ValidationService validationService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		adresseService = getBean(AdresseService.class, "adresseService");
		final TicketService ticketService = getBean(TicketService.class, "ticketService");

		serviceCivil.setUp(new DefaultMockServiceCivil());

		modificationInterceptor = getBean(ModificationInterceptor.class, "modificationInterceptor");
		validationService = getBean(ValidationService.class, "validationService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiDIsPPEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, delaisService,
		                                         diService, TAILLE_LOT, transactionManager, parametreAppService, serviceCivilCacheWarmer, adresseService, ticketService);
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
		PersonnePhysique erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(EnvoiDIsPPEnMasseProcessor.estIndigent(erich, null));

		// Contribuable avec for fiscal et mode d'imposition normal
		PersonnePhysique maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Orbe);
		assertFalse(EnvoiDIsPPEnMasseProcessor.estIndigent(maxwell, null));

		// Contribuable avec for fiscal et mode d'imposition indigent
		PersonnePhysique job = addNonHabitant("Job", "Berger", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(job, date(1980, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.LesClees, ModeImposition.INDIGENT);
		assertTrue(EnvoiDIsPPEnMasseProcessor.estIndigent(job, null));

		// Contribuable avec plusieurs fors fiscaux, indigents ou non
		PersonnePhysique girou = addNonHabitant("Girou", "Ette", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(girou, date(1973, 1, 1), MotifFor.MAJORITE, date(1979, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.LesClees);
		addForPrincipal(girou, date(1980, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, date(1982, 12, 31), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.LesClees, ModeImposition.INDIGENT);
		addForPrincipal(girou, date(1983, 1, 1), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.LesClees);
		assertFalse(EnvoiDIsPPEnMasseProcessor.estIndigent(girou, date(1975, 1, 1)));
		assertTrue(EnvoiDIsPPEnMasseProcessor.estIndigent(girou, date(1981, 1, 1)));
		assertFalse(EnvoiDIsPPEnMasseProcessor.estIndigent(girou, date(2000, 1, 1)));
		assertFalse(EnvoiDIsPPEnMasseProcessor.estIndigent(girou, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testEstAssujettiDansLeCanton() {

		// Contribuable sans for fiscal
		PersonnePhysique erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(EnvoiDIsPPEnMasseProcessor.estAssujettiDansLeCanton(erich, null));

		// Contribuable avec un for fiscal principal à Neuchâtel
		PersonnePhysique maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), null, MockCommune.Neuchatel);
		assertFalse(EnvoiDIsPPEnMasseProcessor.estAssujettiDansLeCanton(maxwell, null));

		// Contribuable avec un for fiscal principal ouvert à Lausanne
		PersonnePhysique felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(felicien, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		assertTrue(EnvoiDIsPPEnMasseProcessor.estAssujettiDansLeCanton(felicien, null));

		// Contribuable avec un for fiscal principal fermé à Lausanne
		PersonnePhysique bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(bernard, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(1990, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		assertTrue(EnvoiDIsPPEnMasseProcessor.estAssujettiDansLeCanton(bernard, date(1985, 1, 1)));
		assertFalse(EnvoiDIsPPEnMasseProcessor.estAssujettiDansLeCanton(bernard, null));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testInitCachePeriodeFiscaleInexistante() {

		try {
			processor.initCache(2007, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
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
			processor.initCache(2007, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACI);

				ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, addPeriodeFiscale(2007));
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2007);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2007);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2007);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2007);

				// Contribuable sans for de gestion
				PersonnePhysique contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
				ids.ctb = contribuable.getNumero();

				TacheEnvoiDeclarationImpotPP tache = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                       TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null, null, colAdm);
				ids.tache = tache.getId();
				return null;
			}
		});

		final RegDate dateTraitement = date(2008, 1, 23);
		final EnvoiDIsPPResults rapport = new EnvoiDIsPPResults(2007, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null, 1, tiersService, adresseService);
		final DeclarationsCache dcache = processor.new DeclarationsCache(2007, Collections.singletonList(ids.ctb));
		final EnvoiDIsPPEnMasseProcessor.Cache cache = processor.initCache(2007, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
		final TacheEnvoiDeclarationImpotPP tache = hibernateTemplate.get(TacheEnvoiDeclarationImpotPP.class, ids.tache);

		assertNull(processor.creeDI(tache, rapport, cache, dcache, false));
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

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un tiers sans déclaration
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();

				// Un tiers avec une déclaration sur toute l'année 2007
				final PersonnePhysique jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jeanId = jean.getNumero();
				addDeclarationImpot(jean, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// Un tiers avec une déclaration sur une partie de l'année 2007
				final PersonnePhysique jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.jacquesId = jacques.getNumero();
				addDeclarationImpot(jacques, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// Un tiers avec deux déclarations partielles dans l'année 2007
				final PersonnePhysique pierre = addNonHabitant("Pierre", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.pierreId = pierre.getNumero();
				addDeclarationImpot(pierre, periode2007, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				addDeclarationImpot(pierre, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);

				// un tiers avec trois déclarations dont une annulée dans l'année 2007
				final PersonnePhysique alfred = addNonHabitant("Alfred", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.alfredId = alfred.getNumero();
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(alfred, periode2007, date(2007, 2, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				diAnnulee.setAnnule(true);
				addDeclarationImpot(alfred, periode2007, date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				addDeclarationImpot(alfred, periode2007, date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete);
				return null;
			}
		});

		{
			List<Long> idsList = new ArrayList<>();
			idsList.add(ids.marcId);
			idsList.add(ids.jeanId);
			idsList.add(ids.jacquesId);
			idsList.add(ids.pierreId);
			idsList.add(ids.alfredId);

			DeclarationsCache cache = processor.new DeclarationsCache(2007, idsList);

			final PersonnePhysique marc = hibernateTemplate.get(PersonnePhysique.class, ids.marcId);
			assertNotNull(marc);
			assertEmpty(cache.getDeclarationsInRange(marc, new Range(date(2007, 1, 1), date(2007, 12, 31)), true));

			final PersonnePhysique jean = hibernateTemplate.get(PersonnePhysique.class, ids.jeanId);
			assertNotNull(jean);
			final List<DeclarationImpotOrdinairePP> jeanDIs = cache.getDeclarationsInRange(jean, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(1, jeanDIs.size());
			assertDIPP(date(2007, 1, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, jeanDIs.get(0));

			final PersonnePhysique jacques = hibernateTemplate.get(PersonnePhysique.class, ids.jacquesId);
			assertNotNull(jacques);
			final List<DeclarationImpotOrdinairePP> jacquesDIs = cache.getDeclarationsInRange(jacques, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(1, jacquesDIs.size());
			assertDIPP(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, jacquesDIs.get(0));

			final PersonnePhysique pierre = hibernateTemplate.get(PersonnePhysique.class, ids.pierreId);
			assertNotNull(pierre);
			final List<DeclarationImpotOrdinairePP> pierreDIs = cache.getDeclarationsInRange(pierre, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(2, pierreDIs.size());
			assertDIPP(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, pierreDIs.get(0));
			assertDIPP(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, pierreDIs.get(1));

			final PersonnePhysique alfred = hibernateTemplate.get(PersonnePhysique.class, ids.alfredId);
			assertNotNull(alfred);
			final List<DeclarationImpotOrdinairePP> alfredIdAvecAnnulees = cache.getDeclarationsInRange(alfred, new Range(date(2007, 1, 1), date(2007, 12, 31)), true);
			assertEquals(3, alfredIdAvecAnnulees.size());
			assertDIPP(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(0));
			assertDIPP(date(2007, 2, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(1));
			assertDIPP(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdAvecAnnulees.get(2));

			final List<DeclarationImpotOrdinairePP> alfredIdSansAnnulees = cache.getDeclarationsInRange(alfred, new Range(date(2007, 1, 1), date(2007, 12, 31)), false);
			assertEquals(2, alfredIdSansAnnulees.size());
			assertDIPP(date(2007, 1, 1), date(2007, 6, 30), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdSansAnnulees.get(0));
			assertDIPP(date(2007, 7, 1), date(2007, 12, 31), null, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi, null, alfredIdSansAnnulees.get(1));
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

		final int annee = 2012;

		// initialisation des contribuables, fors, tâches
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				final PeriodeFiscale periode = addPeriodeFiscale(annee);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// for fiscal ouvert à Lausanne -> assujetti
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1961, 3, 12), Sexe.MASCULIN);
				addForPrincipal(marc, date(1980, 1, 1), MotifFor.ARRIVEE_HC, date(annee, 3, 31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(marc, date(annee, 7, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				final TacheEnvoiDeclarationImpot tache1 = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 31), date(annee, 1, 1), date(annee, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                            TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tache1Id = tache1.getId();
				final TacheEnvoiDeclarationImpot tache2 = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 31), date(annee, 7, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                            TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tache2Id = tache2.getId();
				return null;
			}
		});

		// traitement des tâches
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final RegDate dateTraitement = date(annee + 1, 1, 15);
				final List<Long> idsCtb = Collections.singletonList(ids.marcId);

				final EnvoiDIsPPResults rapport = new EnvoiDIsPPResults(annee, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null, 1, tiersService, adresseService);
				processor.traiterBatch(idsCtb, rapport, annee, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, dateTraitement);
				return null;
			}
		});

		// vérification des DIs
		{
			final List<DeclarationImpotOrdinairePP> dis = hibernateTemplate.find("from DeclarationImpotOrdinairePP", null);
			assertNotNull(dis);
			assertEquals(2, dis.size());

			// tri selon le numéro de séquence
			final List<DeclarationImpotOrdinairePP> diTriees = new ArrayList<>(dis);
			Collections.sort(diTriees, new Comparator<DeclarationImpotOrdinaire>() {
				@Override
				public int compare(DeclarationImpotOrdinaire o1, DeclarationImpotOrdinaire o2) {
					return o1.getNumero() - o2.getNumero();
				}
			});

			// les numéros de séquence doivent se suivre...
			assertEquals(1, (int) diTriees.get(0).getNumero());
			assertEquals(2, (int) diTriees.get(1).getNumero());

			// .. et les codes de contrôle doivent être les mêmes
			assertNotNull(diTriees.get(0).getCodeControle());
			assertNotNull(diTriees.get(1).getCodeControle());
			assertEquals(diTriees.get(0).getCodeControle(), 6, diTriees.get(0).getCodeControle().length());
			assertEquals(diTriees.get(1).getCodeControle(), 6, diTriees.get(1).getCodeControle().length());
			assertEquals(diTriees.get(0).getCodeControle(), diTriees.get(1).getCodeControle());
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un tiers sans exclusion
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				ids.marcId = marc.getNumero();

				// Un tiers avec une exclusion passée
				final PersonnePhysique jean = addNonHabitant("Jean", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				jean.setDateLimiteExclusionEnvoiDeclarationImpot(date(1995, 1, 1));
				ids.jeanId = jean.getNumero();

				// Un tiers avec une exclusion dans le futur
				final PersonnePhysique jacques = addNonHabitant("Jacques", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				jacques.setDateLimiteExclusionEnvoiDeclarationImpot(date(2010, 1, 1));
				ids.jacquesId = jacques.getNumero();

				final TacheEnvoiDeclarationImpotPP tacheMarc = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                 date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.tacheMarcId = tacheMarc.getId();

				final TacheEnvoiDeclarationImpotPP tacheJean = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                 date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jean, null, null, colAdm);
				ids.tacheJeanId = tacheJean.getId();

				final TacheEnvoiDeclarationImpotPP tacheJacques = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1,
				                                                                                                                      1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, jacques,
				                                                                    null, null, colAdm);
				ids.tacheJacquesId = tacheJacques.getId();

				return null;
			}
		});

		{
			final List<Long> idsList = new ArrayList<>();
			idsList.add(ids.marcId);
			idsList.add(ids.jeanId);
			idsList.add(ids.jacquesId);

			final RegDate dateTraitement = date(2009, 1, 15);
			final DeclarationsCache dcache = processor.new DeclarationsCache(2008, idsList);
			final EnvoiDIsPPEnMasseProcessor.Cache cache = processor.initCache(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);

			final EnvoiDIsPPResults rapport = new EnvoiDIsPPResults(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, dateTraitement, 10, null, null, null, 1, tiersService, adresseService);

			// Le tiers sans exclusion
			final TacheEnvoiDeclarationImpotPP tacheMarc = hibernateTemplate.get(TacheEnvoiDeclarationImpotPP.class, ids.tacheMarcId);
			assertTrue(processor.traiterTache(tacheMarc, dateTraitement, rapport, cache, dcache));
			assertEmpty(rapport.ctbsIgnores);

			// Le tiers avec une exclusion passée
			final TacheEnvoiDeclarationImpotPP tacheJean = hibernateTemplate.get(TacheEnvoiDeclarationImpotPP.class, ids.tacheJeanId);
			assertTrue(processor.traiterTache(tacheJean, dateTraitement, rapport, cache, dcache));
			assertEmpty(rapport.ctbsIgnores);

			// Le tiers avec une exclusion dans le futur -> doit être exclu
			final TacheEnvoiDeclarationImpotPP tacheJacques = hibernateTemplate.get(TacheEnvoiDeclarationImpotPP.class, ids.tacheJacquesId);
			assertFalse(processor.traiterTache(tacheJacques, dateTraitement, rapport, cache, dcache));
			assertEquals(1, rapport.ctbsIgnores.size());
			final Ignore ignore = rapport.ctbsIgnores.get(0);
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable habitant à Lausanne
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur toute l'année 2008
				final TacheEnvoiDeclarationImpot premiereTache = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                   date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement de décès en retard -> fermeture du for principal au milieu 2008 et génération d'une nouvelle tâche correspondante
				ffp.setDateFin(dateDeces);
				ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                  dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});

		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// la première tâche est en erreur
		final List<EnvoiDIsPPResults.Erreur> erreurs = results.ctbsEnErrors;
		assertEquals(1, erreurs.size());
		final EnvoiDIsPPResults.Erreur erreur = erreurs.get(0);
		assertEquals(ids.marcId.longValue(), erreur.noCtb);
		assertEquals("La tâche [id=" + ids.premiereTacheId + ", période=2008.01.01-2008.12.31] est en conflit avec 1 déclaration(s) d'impôt pré-existante(s) sur le contribuable [" + ids.marcId +
				"]. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.", erreur.details);

		final TacheEnvoiDeclarationImpot premiereTache = hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.premiereTacheId);
		assertEquals(TypeEtatTache.EN_INSTANCE, premiereTache.getEtat());

		// la seconde tâche est traitée
		final List<Long> traites = results.ctbsAvecDiGeneree;
		assertEquals(1, traites.size());
		assertEquals(ids.marcId, traites.get(0));

		final TacheEnvoiDeclarationImpot secondeTache = hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.secondeTacheId);
		assertEquals(TypeEtatTache.TRAITE, secondeTache.getEtat());

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), dateDeces, TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, ids.oidCedi,
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable habitant à Lausanne décédé courant 2008
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche jusqu'au décès
				final TacheEnvoiDeclarationImpot premiereTache = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                   dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.premiereTacheId = premiereTache.getId();

				// arrivée d'un événement d'annulation de décès en retard -> réouverture du for principal et génération d'une nouvelle tâche sur toute l'année 2008
				ffp.setDateFin(null);
				ffp.setMotifFermeture(null);
				final TacheEnvoiDeclarationImpot secondeTache = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                                                                  date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				ids.secondeTacheId = secondeTache.getId();

				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// la première tâche est en erreur
		final List<EnvoiDIsPPResults.Erreur> erreurs = results.ctbsEnErrors;
		assertEquals(1, erreurs.size());
		final EnvoiDIsPPResults.Erreur erreur = erreurs.get(0);
		assertEquals(ids.marcId.longValue(), erreur.noCtb);
		assertEquals("La tâche [id=" + ids.premiereTacheId + ", période=2008.01.01-2008.07.31] est en conflit avec 1 déclaration(s) d'impôt pré-existante(s) sur le contribuable [" + ids.marcId +
				"]. Aucune nouvelle déclaration n'est créée et la tâche reste en instance.", erreur.details);

		final TacheEnvoiDeclarationImpot premiereTache = hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.premiereTacheId);
		assertEquals(TypeEtatTache.EN_INSTANCE, premiereTache.getEtat());

		// la seconde tâche est traitée
		final List<Long> traites = results.ctbsAvecDiGeneree;
		assertEquals(1, traites.size());
		assertEquals(ids.marcId, traites.get(0));

		final TacheEnvoiDeclarationImpot secondeTache = hibernateTemplate.get(TacheEnvoiDeclarationImpot.class, ids.secondeTacheId);
		assertEquals(TypeEtatTache.TRAITE, secondeTache.getEtat());

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable habitant à Lausanne décédé courant 2008
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche jusqu'au décès
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1),
				                  dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// le délai de retour imprimé doit être dateTraitement + 60 jours
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), dateDeces, TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
		           ids.oidCedi, calculerDateDelaiImprime(dateTraitement, 3, 60), decl);
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable habitant à Lausanne
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur toute l'année
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				                  marc, null, null, colAdm);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// le délai de retour imprimé est le délai réglementaire
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
		           ids.oidCedi, date(2009, 3, 31), decl);
	}

	/**
	 * [UNIREG-1976] Vérifie les nouvelles règles sur les délais en cas d'envoi en masse
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAjouterDelaisDeRetourInitial() throws Exception {

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
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Vaudois ordinaire - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Vaudois dépense - DI dépense
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_DEPENSE, modeleDepense);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.VAUDOIS_DEPENSE, modeleDepense);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton immeuble - DI HC immeuble
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleHCImmeuble);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleHCImmeuble);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton activité indépendante - DI complète
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-canton activité indépendante - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_CANTON, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(hc, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_CANTON, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-Suisse - DI complète
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Hors-Suisse - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.HORS_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(hs, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.HORS_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Diplomate Suisse - DI complète
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleComplete);
			processor.ajouterDelaisDeRetourInitial(di1, dateTraitement, dateExpedition);
			assertEquals(dateExpedition.addDays(60), di1.getDelaiAccordeAu());
			assertEquals(dateExpedition.addDays(60), di1.getDelaiRetourImprime());
			di1.setAnnule(true);
		}

		// Diplomate Suisse - DI vaudtax
		{
			// période complète
			final DeclarationImpotOrdinairePP di0 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleVaudtax);
			processor.ajouterDelaisDeRetourInitial(di0, dateTraitement, dateExpedition);
			assertEquals(delaiEffectif, di0.getDelaiAccordeAu());
			assertEquals(delaiReglementaire, di0.getDelaiRetourImprime());
			di0.setAnnule(true);

			// [UNIREG-1740] [UNIREG-1861] période incomplète
			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(vd, periode, date(2008, 1, 1), date(2008, 7, 31), TypeContribuable.DIPLOMATE_SUISSE, modeleVaudtax);
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(MockOfficeImpot.ACI.getNoColAdm());
				ids.aci = aci.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable indigent habitant à Lausanne, décédé en 2008
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne, ModeImposition.INDIGENT);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpotPP t = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                   TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// [UNIREG-1852] la déclaration doit être émise (et non-retournée immédiatement comme pour les indigents non-décédés) avec la cellule registre comme adresse de retour
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), dateDeces, TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(MockOfficeImpot.ACI.getNoColAdm());
				ids.aci = aci.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable indigent habitant à Lausanne, décédé en 2008
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpotPP t = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                   TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, true, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);
		assertEquals(1, results.ctbsIgnores.size());
		Ignore ignore = results.ctbsIgnores.get(0);
		assertEquals("Décédé en fin d'année",ignore.details);


		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative aci = tiersService.getCollectiviteAdministrative(MockOfficeImpot.ACI.getNoColAdm());
				ids.aci = aci.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable indigent habitant à Lausanne, décédé en 2008
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				ids.marcId = marc.getNumero();

				// traitement du batch de détermination des DIs -> création d'une tâche sur une fraction d'année
				TacheEnvoiDeclarationImpotPP t = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), dateDeces, TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                   TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, true, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		//[UNIREG-1952] Dans le cas ou l'exclusion des décédés est activée, il faut qu'un décédé avant la date d'eclusion soit traité normalement 
		assertNotNull(declarations);
		assertEquals(1, declarations.size());
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertDIPP(date(2008, 1, 1), dateDeces, TypeEtatDocumentFiscal.EMIS, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
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

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				ids.oidCedi = cedi.getId();

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete);

				// Un contribuable indigent habitant à Lausanne
				final PersonnePhysique marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
				marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
				addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.INDIGENT);
				ids.marcId = marc.getNumero();

				// simulation du traitement du batch de détermination des DIs
				TacheEnvoiDeclarationImpotPP t = addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(2009, 1, 1), date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                                                   TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, marc, null, null, colAdm);
				t.setAdresseRetour(TypeAdresseRetour.ACI);
				return null;
			}
		});


		final RegDate dateTraitement = date(2009, 1, 15);
		final EnvoiDIsPPResults results = processor.run(2008, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		final List<DeclarationImpotOrdinaire> declarations = hibernateTemplate.find("from DeclarationImpotOrdinaire", null);
		assertNotNull(declarations);
		assertEquals(1, declarations.size());

		// [UNIREG-1980] la déclaration doit être émise et retournée immédiatement (les deux états)
		final DeclarationImpotOrdinaire decl = declarations.get(0);
		assertNotNull(decl);

		final List<EtatDeclaration> etats = decl.getEtatsDeclarationSorted();
		assertNotNull(etats);
		assertEquals(2, etats.size());

		final EtatDeclaration etat0 = etats.get(0);
		assertEquals(TypeEtatDocumentFiscal.EMIS, etat0.getEtat());
		assertEquals(dateTraitement, etat0.getDateObtention());

		final EtatDeclaration etat1 = etats.get(1);
		assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat1.getEtat());

		final EtatDeclarationRetournee retour1 = (EtatDeclarationRetournee) etat1;
		assertEquals(dateTraitement, retour1.getDateObtention());
		assertEquals("INDIGENT", retour1.getSource());

		assertDIPP(date(2008, 1, 1), date(2008, 12, 31), TypeEtatDocumentFiscal.RETOURNE, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
		           ids.oidCedi, date(2009, 3, 31), decl);
	}

	@Test
	public void testNumeroSequenceApresDiAnnulee() throws Exception {

		final int annee = 2009;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final DeclarationImpotOrdinaire diAnnulee = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, md);
				diAnnulee.setAnnule(true);
				assertEquals(1, (int) diAnnulee.getNumero());

				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
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
	public void testGenerationCodeControleAvant2011() throws Exception {

		final int annee = 2010;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
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
	public void testGenerationCodeControleSansDeclarationPreexistante() throws Exception {

		final int annee = 2011;

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(annee, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny, MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pf);
				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.HORS_CANTON, TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.HC_IMMEUBLE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
				assertNotNull(decls);
				assertEquals(1, decls.size());

				final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
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
		final String codeControle = DeclarationImpotOrdinairePP.generateCodeControle();

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Jules", "Tartempion", date(1947, 1, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEMENAGEMENT_VD, date(annee,3,31), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(pp, date(annee, 4, 1), MotifFor.DEPART_HS, date(annee, 8, 31), MotifFor.ARRIVEE_HS, MockPays.Colombie);
				addForPrincipal(pp, date(annee, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final DeclarationImpotOrdinairePP decl = addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				decl.setCodeControle(codeControle);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 9, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
				assertNotNull(decls);
				assertEquals(2, decls.size());

				final DeclarationImpotOrdinairePP di0 = (DeclarationImpotOrdinairePP) decls.get(0);
				assertNotNull(di0);
				assertEquals(codeControle, di0.getCodeControle());

				// on vérifie que le code de contrôle de la deuxième DI est le même que celui de la première
				final DeclarationImpotOrdinairePP di1 = (DeclarationImpotOrdinairePP) decls.get(1);
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

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pf);
				final CollectiviteAdministrative colAdm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				// une déclaration sans code de contrôle
				addDeclarationImpot(pp, pf, date(annee, 1, 1), date(annee, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 9, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, pp, Qualification.AUTOMATIQUE, 0, colAdm);

				return pp.getNumero();
			}
		});

		final RegDate dateTraitement = date(annee + 1, 1, 15);
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_COMPLETE, null, null, 1000, dateTraitement, false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.nbCtbsTotal);

		// on vérifie que les deux DIs ont reçu le même code de contrôle
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
				assertNotNull(decls);
				assertEquals(2, decls.size());

				final DeclarationImpotOrdinairePP di0 = (DeclarationImpotOrdinairePP) decls.get(0);
				assertNotNull(di0);
				final String codeControle = di0.getCodeControle();
				assertCodeControleIsValid(codeControle);

				final DeclarationImpotOrdinairePP di1 = (DeclarationImpotOrdinairePP) decls.get(1);
				assertNotNull(di1);
				assertEquals(codeControle, di1.getCodeControle());
				return null;
			}
		});
	}

	/**
	 * Test de 10*TAILLE_LOT contribuables répartis sur 7 threads
	 */
	@Test
	public void testMultithread() throws Exception {

		final int annee = 2011;
		final int nbCtbs = 10 * TAILLE_LOT;
		final int nbThreads = 7;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final CollectiviteAdministrative oid = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				for (int i = 0 ; i < nbCtbs ; ++ i) {
					final String prenom = "Jean-" + Integer.toString(i + 1);
					final PersonnePhysique pp = addNonHabitant(prenom, "Dupont", null, Sexe.MASCULIN);
					final RegDate ouverture = date(annee, 1, 1).addDays(i / 7);
					addForPrincipal(pp, ouverture, MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
					addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, RegDate.get().addDays(1), ouverture, date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, pp, null, null, oid);
				}
				return null;
			}
		});

		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 0, RegDate.get(), false, nbThreads, null);
		assertNotNull(results);
		assertEquals(nbCtbs, results.ctbsAvecDiGeneree.size());
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (Long ctbId : results.ctbsAvecDiGeneree) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ctbId);
					final String prenom = pp.getPrenomUsuel();
					assertTrue(prenom.startsWith("Jean-"));
					final int index = Integer.parseInt(prenom.substring(5)) - 1;
					final List<Declaration> dis = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					final String message = "Contribuable " + pp.getNumero() + " (" + index + ")";
					assertEquals(message, 1, dis.size());
					assertNotNull(message, dis.get(0));
					assertEquals(message, date(annee, 1, 1).addDays(index / 7), dis.get(0).getDateDebut());
					assertEquals(message, date(annee, 12, 31), dis.get(0).getDateFin());
				}
				return null;
			}
		});
	}

	/**
	 * [SIFISC-14297] il n'y a plus de for vaudois au 31.12 de l'année, mais l'assujettissement va bien jusqu'à la fin de l'année -> le calcul du for de gestion doit bien faire attention
	 * (on se retrouve dans le même cas que le SIFISC-4923, mais dans le batch d'envoi, cette fois, au lieu de l'interactif)
	 */
	@Test
	public void testMixte2PartiHorsCantonEnDecembre() throws Exception {

		final int annee = 2014;
		final RegDate dateDepartHC = date(annee, 12, 1);

		// mise en place civile... rien du tout
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);

				final PersonnePhysique pp = addNonHabitant("Alfred", "D'Isigny", date(1978, 5, 12), Sexe.MASCULIN);
				addForPrincipal(pp, date(2012, 1, 1), MotifFor.INDETERMINE, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				addForPrincipal(pp, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bale, ModeImposition.SOURCE);

				final CollectiviteAdministrative oid = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, RegDate.get().getOneDayAfter(), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX, pp, null, 0, oid);

				return pp.getNumero();
			}
		});

		// envoi de la DI ?
		final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 0, RegDate.get(), false, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsAvecDiGeneree.size());
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(0, results.ctbsIndigents.size());

		assertEquals(Collections.singletonList(ppId), results.ctbsAvecDiGeneree);
	}

	/**
	 * Permet de vérifier le nombre de flushes effectués lors de l'envoi d'un lot de DI
	 */
	@Test
	public void testNombreFlushes() throws Exception {

		final Map<Class<?>, Map<Serializable, MutableInt>> changed = new HashMap<>();
		final class Interceptor implements ModificationSubInterceptor {
			@Override
			public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
				synchronized (changed) {
					final Map<Serializable, MutableInt> clazzMap = changed.get(entity.getClass());
					if (clazzMap == null) {
						final Map<Serializable, MutableInt> newClazzMap = new HashMap<>();
						newClazzMap.put(id, new MutableInt(1));
						changed.put(entity.getClass(), newClazzMap);
					}
					else {
						final MutableInt count = clazzMap.get(id);
						if (count == null) {
							clazzMap.put(id, new MutableInt(1));
						}
						else {
							count.increment();
						}
					}
				}
				return false;
			}

			@Override
			public void postFlush() throws CallbackException {
			}

			@Override
			public void suspendTransaction() {
			}

			@Override
			public void resumeTransaction() {
			}

			@Override
			public void preTransactionCommit() {
			}

			@Override
			public void postTransactionCommit() {
			}

			@Override
			public void postTransactionRollback() {
			}
		}

		final long noIndividu1 = 24648724567L;
		final long noIndividu2 = 467423L;
		final int annee = 2014;

		// initialisation des données civiles
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind1 = addIndividu(noIndividu1, null, "Dugland", "Aristide", Sexe.MASCULIN);
				addAdresse(ind1, TypeAdresseCivil.PRINCIPALE, MockRue.Morges.RueDeLAvenir, null, date(2000, 1, 1), null);
				addNationalite(ind1, MockPays.Suisse, date(2000, 1, 1), null);

				final MockIndividu ind2 = addIndividu(noIndividu2, null, "Glandu", "Iphigénie", Sexe.FEMININ);
				addAdresse(ind2, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, date(2010, 4, 14), null);
				addNationalite(ind2, MockPays.Suisse, date(2000, 1, 1), null);
			}
		});

		final class Ids {
			long pp1;
			long pp2;
		}

		// initialisation des données fiscales
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final CollectiviteAdministrative oidLausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative oidMorges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());

				final PersonnePhysique pp1 = addHabitant(noIndividu1);
				addForPrincipal(pp1, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Morges);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
				                  pp1, Qualification.AUTOMATIQUE, 0, oidMorges);

				final PersonnePhysique pp2 = addHabitant(noIndividu2);
				addForPrincipal(pp2, date(2010, 4, 14), MotifFor.INDETERMINE, MockCommune.Renens);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
				                  pp2, Qualification.AUTOMATIQUE, 0, oidLausanneOuest);

				final Ids ids = new Ids();
				ids.pp1 = pp1.getNumero();
				ids.pp2 = pp2.getNumero();
				return ids;
			}
		});

		final Interceptor sub = new Interceptor();
		modificationInterceptor.register(sub);
		try {

			//
			// A partir de maintenant, tous les flushes seront passés par mon joli intercepteur "sub", qui va mettre à jour la map "changed"
			//

			// lancement du traitement
			final RegDate dateTraitement = date(annee + 1, 1, 15);
			final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 1000, dateTraitement, false, 1, null);
			assertNotNull(results);
			assertEquals(2, results.nbCtbsTotal);

			// contrôle du nombre de flushs
			assertNotNull(changed);
			for (Map.Entry<Class<?>, Map<Serializable, MutableInt>> mainEntry : changed.entrySet()) {
				for (Map.Entry<Serializable, MutableInt> entry : mainEntry.getValue().entrySet()) {
					if (DeclarationImpotOrdinaire.class.isAssignableFrom(mainEntry.getKey())) {
						// une fois à la création, puis à la clôture de la transaction car les délais et états (par exemple) ont été rajoutés après
						assertEquals("Les déclarations d'impôt devraient avoir été flushées 2 fois", 2, entry.getValue().intValue());
					}
					else {
						assertEquals("Les entités " + mainEntry.getClass().getSimpleName() + " devraient n'avoir été flushées qu'une seule fois", 1, entry.getValue().intValue());
					}
				}
			}
		}
		finally {
			modificationInterceptor.unregister(sub);
		}

		// vérification que toutes les DI ont bien été générées comme attendu
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp1);
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					assertNotNull(decls);
					assertEquals(1, decls.size());

					final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
					assertNotNull(di);
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp2);
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					assertNotNull(decls);
					assertEquals(1, decls.size());

					final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
					assertNotNull(di);
				}
				return null;
			}
		});
	}

	/**
	 * Permet de vérifier le nombre de validations de tiers effectués lors de l'envoi d'un lot de DI
	 */
	@Test
	public void testNombreValidationsTiers() throws Exception {

		final Map<Long, MutableInt> validated = new HashMap<>();
		final class Validator implements EntityValidator<PersonnePhysique> {
			@Override
			public ValidationResults validate(PersonnePhysique entity) {
				synchronized (validated) {
					final MutableInt count = validated.get(entity.getNumero());
					if (count == null) {
						validated.put(entity.getNumero(), new MutableInt(1));
					}
					else {
						count.increment();
					}
				}
				return null;
			}
		}

		final long noIndividu1 = 24648724567L;
		final long noIndividu2 = 467423L;
		final int annee = 2014;

		// initialisation des données civiles
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind1 = addIndividu(noIndividu1, null, "Dugland", "Aristide", Sexe.MASCULIN);
				addAdresse(ind1, TypeAdresseCivil.PRINCIPALE, MockRue.Morges.RueDeLAvenir, null, date(2000, 1, 1), null);
				addNationalite(ind1, MockPays.Suisse, date(2000, 1, 1), null);

				final MockIndividu ind2 = addIndividu(noIndividu2, null, "Glandu", "Iphigénie", Sexe.FEMININ);
				addAdresse(ind2, TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, date(2010, 4, 14), null);
				addNationalite(ind2, MockPays.Suisse, date(2000, 1, 1), null);
			}
		});

		final class Ids {
			long pp1;
			long pp2;
		}

		// initialisation des données fiscales
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final CollectiviteAdministrative oidLausanneOuest = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm());
				final CollectiviteAdministrative oidMorges = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_MORGES.getNoColAdm());

				final PersonnePhysique pp1 = addHabitant(noIndividu1);
				addForPrincipal(pp1, date(2000, 1, 1), MotifFor.INDETERMINE, MockCommune.Morges);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
				                  pp1, Qualification.AUTOMATIQUE, 0, oidMorges);

				final PersonnePhysique pp2 = addHabitant(noIndividu2);
				addForPrincipal(pp2, date(2010, 4, 14), MotifFor.INDETERMINE, MockCommune.Renens);
				addTacheEnvoiDIPP(TypeEtatTache.EN_INSTANCE, date(annee + 1, 1, 1), date(annee, 1, 1), date(annee, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_VAUDTAX,
				                  pp2, Qualification.AUTOMATIQUE, 0, oidLausanneOuest);

				final Ids ids = new Ids();
				ids.pp1 = pp1.getNumero();
				ids.pp2 = pp2.getNumero();
				return ids;
			}
		});

		final Validator sub = new Validator();
		validationService.registerValidator(PersonnePhysique.class, sub);
		try {

			//
			// A partir de maintenant, toutes les validations sur des personnes physiques seront passées par mon joli validateur "sub", qui va mettre à jour la map "validated"
			//

			// lancement du traitement
			final RegDate dateTraitement = date(annee + 1, 1, 15);
			final EnvoiDIsPPResults results = processor.run(annee, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX, null, null, 1000, dateTraitement, false, 1, null);
			assertNotNull(results);
			assertEquals(2, results.nbCtbsTotal);

			// contrôle du nombre de validations des tiers
			assertNotNull(validated);
			assertEquals(2, validated.size());
			for (Map.Entry<Long, MutableInt> entry : validated.entrySet()) {
				assertEquals("Le tiers " + entry.getKey() + " aurait dû être validé 4 fois (c'est beaucoup, mais c'est ce qu'on fait de mieux pour le moment)", 4, entry.getValue().intValue());
			}
		}
		finally {
			validationService.unregisterValidator(PersonnePhysique.class, sub);
		}

		// vérification que toutes les DI ont bien été générées comme attendu
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp1);
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					assertNotNull(decls);
					assertEquals(1, decls.size());

					final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
					assertNotNull(di);
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp2);
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					assertNotNull(decls);
					assertEquals(1, decls.size());

					final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decls.get(0);
					assertNotNull(di);
				}
				return null;
			}
		});
	}
}
