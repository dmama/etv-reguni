package ch.vd.uniregctb.role;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.role.ProduireRolesResults.Erreur;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoCommune;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class ProduireRolesProcessorTest extends BusinessTest {

	private HibernateTemplate hibernateTemplate;
	private ProduireRolesProcessor processor;

	private static final String DB_UNIT_CTB_INVALIDE = "ContribuableInvalideTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");

		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ServiceCivilService serviceCivilService = getBean(ServiceCivilService.class, "serviceCivilService");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new ProduireRolesProcessor(hibernateTemplate, infraService, tiersDAO, transactionManager, adresseService, tiersService, serviceCivilService, validationService,
				assujettissementService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateIteratorOnContribuables() throws Exception {

		loadDatabase(DB_UNIT_CTB_INVALIDE); // rodolf

		/*
		 * Contribuable devant être pris en compte
		 */

		final Contribuable paul = newCtbVaudoisOrdinaire(MockCommune.Lausanne);
		final Contribuable incognito = newCtbVaudoisOrdinaireDepuis2007(MockCommune.Lausanne);
		final Contribuable raoul = newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune.Lausanne);
		final Contribuable arnold = newCtbVaudoisSourcierMixte(MockCommune.Lausanne);
		final Contribuable victor = newCtbVaudoisSourcier(MockCommune.Lausanne);
		final Contribuable geo = newCtbHorsCantonEtImmeuble(MockCommune.Lausanne);
		final Contribuable donald = newCtbHorsCantonEtDeuxImmeubles(MockCommune.Lausanne, MockCommune.Lausanne);
		final Contribuable johnny = newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune.Lausanne);
		final Contribuable tyler = newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune.Lausanne);
		final Contribuable marc = newCtbDiplomateSuisse(MockCommune.Lausanne);
		final Contribuable louis = newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune.Lausanne, MockCommune.Lausanne);
		final Contribuable albertine = newCtbVaudoisSourcierGris(MockCommune.Lausanne);
		final Contribuable rodolf = getCtbVaudoisOrdinaireEtImmeubleInvalide();

		/*
		 * Contribuable devant être ignorés
		 */

		final Contribuable didier = newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune.Lausanne);
		final Contribuable pierre = newCtbVaudoisOrdinairePartiHorsCantonEn1983(MockCommune.Lausanne);
		final Contribuable jean = newCtbVaudoisOrdinaireAnnule(MockCommune.Lausanne);
		final Contribuable jeans = newCtbHorsCantonSansForSecondaire();
		final Contribuable popol = newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune.Lausanne);
		final Contribuable rama = newCtbHorsCantonEtForImmeubleAnnule(MockCommune.Lausanne);
		assertNotNull(didier);
		assertNotNull(pierre);
		assertNotNull(jean);
		assertNotNull(jeans);
		assertNotNull(popol);
		assertNotNull(rama);

		final List<Long> list = processor.getIdsOfAllContribuables(2007);
		assertNotNull(list);
		final Iterator<Long> iter = list.iterator();
		assertNextIs(iter, paul.getNumero());
		assertNextIs(iter, incognito.getNumero());
		assertNextIs(iter, raoul.getNumero());
		assertNextIs(iter, arnold.getNumero());
		assertNextIs(iter, victor.getNumero());
		assertNextIs(iter, geo.getNumero());
		assertNextIs(iter, donald.getNumero());
		assertNextIs(iter, johnny.getNumero());
		assertNextIs(iter, tyler.getNumero());
		assertNextIs(iter, marc.getNumero());
		assertNextIs(iter, louis.getNumero());
		assertNextIs(iter, albertine.getNumero());
		assertNextIs(iter, rodolf.getNumero());
		assertFalse(iter.hasNext());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRun() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());

		class Ids {
			public Long paul;
			public Long incognito;
			public Long raoul;
			public Long didier;
			public Long arnold;
			public Long victor;
			public Long albertine;
			public Long geo;
			public Long donald;
			public Long johnny;
			public Long tyler;
			public Long pascal;
			public Long marc;
			public Long louis;
			public Long albert;
			public Long georges;
			public Long marie;
			public Long jean;
			public Long tom;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.paul = newCtbVaudoisOrdinaire(MockCommune.Lausanne).getNumero();
				ids.incognito = newCtbVaudoisOrdinaireDepuis2007(MockCommune.Lausanne).getNumero();
				ids.raoul = newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune.Lausanne).getNumero();
				ids.didier = newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune.Lausanne).getNumero();
				ids.arnold = newCtbVaudoisSourcierMixte(MockCommune.Lausanne).getNumero();
				ids.victor = newCtbVaudoisSourcier(MockCommune.Lausanne).getNumero();
				ids.albertine = newCtbVaudoisSourcierGris(MockCommune.Lausanne).getNumero();
				ids.geo = newCtbHorsCantonEtImmeuble(MockCommune.Lausanne).getNumero();
				ids.donald = newCtbHorsCantonEtDeuxImmeubles(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
				ids.johnny = newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune.Lausanne).getNumero();
				ids.tyler = newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune.Lausanne).getNumero();
				ids.pascal = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
				ids.marc = newCtbDiplomateSuisse(MockCommune.Lausanne).getNumero();
				ids.louis = newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
				ids.albert = newCtbVaudoisOrdinairePartiHorsSuisseEtImmeuble(MockCommune.Lausanne, MockCommune.Cossonay).getNumero();
				ids.georges = newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
				ids.marie = newCtbVaudoisPartiHorsCantonTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
				ids.jean = newCtbVaudoisPartiHorsSuisseTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
				ids.tom = newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune.Lausanne).getNumero();
				return null;
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(18, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(2, results.ctbsIgnores.size()); // le diplomate et le sourcier gris
		assertEquals(2, results.infosCommunes.size());

		{
			final int ofsCommune = MockCommune.Lausanne.getNoOFS();
			final InfoCommune infoLausanne = results.getInfoPourCommune(ofsCommune);
			assertNotNull(infoLausanne);

			final InfoContribuable infoPaul = infoLausanne.getInfoPourContribuable(ids.paul);
			final InfoContribuable infoIncognito = infoLausanne.getInfoPourContribuable(ids.incognito);
			final InfoContribuable infoRaoul = infoLausanne.getInfoPourContribuable(ids.raoul);
			final InfoContribuable infoDidier = infoLausanne.getInfoPourContribuable(ids.didier);
			final InfoContribuable infoArnold = infoLausanne.getInfoPourContribuable(ids.arnold);
			final InfoContribuable infoVictor = infoLausanne.getInfoPourContribuable(ids.victor);
			final InfoContribuable infoAlbertine = infoLausanne.getInfoPourContribuable(ids.albertine);
			final InfoContribuable infoGeo = infoLausanne.getInfoPourContribuable(ids.geo);
			final InfoContribuable infoDonald = infoLausanne.getInfoPourContribuable(ids.donald);
			final InfoContribuable infoJohnny = infoLausanne.getInfoPourContribuable(ids.johnny);
			final InfoContribuable infoTyler = infoLausanne.getInfoPourContribuable(ids.tyler);
			final InfoContribuable infoPascal = infoLausanne.getInfoPourContribuable(ids.pascal);
			final InfoContribuable infoMarc = infoLausanne.getInfoPourContribuable(ids.marc);
			final InfoContribuable infoLouis = infoLausanne.getInfoPourContribuable(ids.louis);
			final InfoContribuable infoAlbert = infoLausanne.getInfoPourContribuable(ids.albert);
			final InfoContribuable infoGeorges = infoLausanne.getInfoPourContribuable(ids.georges);
			final InfoContribuable infoMarie = infoLausanne.getInfoPourContribuable(ids.marie);
			final InfoContribuable infoJean = infoLausanne.getInfoPourContribuable(ids.jean);
			final InfoContribuable infoTom = infoLausanne.getInfoPourContribuable(ids.tom);

			assertInfo(ids.paul, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPaul);
			assertInfo(ids.incognito, TypeContribuable.ORDINAIRE, ofsCommune, date(2007, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoIncognito);
			assertInfo(ids.raoul, TypeContribuable.NON_ASSUJETTI, ofsCommune, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoRaoul);
			assertNull(infoDidier);
			assertInfo(ids.arnold, TypeContribuable.MIXTE, ofsCommune, date(1983, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoArnold);
			assertInfo(ids.victor, TypeContribuable.SOURCE, ofsCommune, date(1983, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoVictor);
			assertNull(infoAlbertine);
			assertInfo(ids.geo, TypeContribuable.HORS_CANTON, ofsCommune, date(2003, 3, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeo);
			assertInfo(ids.donald, TypeContribuable.HORS_CANTON, ofsCommune, date(1990, 1, 15), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoDonald);
			assertInfo(ids.johnny, TypeContribuable.HORS_CANTON, ofsCommune, date(2005, 11, 3), date(2007, 8, 30), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoJohnny);
			assertInfo(ids.tyler, TypeContribuable.HORS_CANTON, ofsCommune, date(2005, 11, 3), date(2007, 8, 30), MotifFor.DEBUT_EXPLOITATION, MotifFor.FIN_EXPLOITATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoTyler);
			assertInfo(ids.pascal, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, TypeContribuable.NON_ASSUJETTI, ofsCommune, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoLouis);
			assertInfo(ids.albert, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoAlbert);
			assertInfo(ids.georges, TypeContribuable.HORS_CANTON, ofsCommune, date(1980, 3, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeorges);
			assertInfo(ids.marie, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), date(2007, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoMarie);
			assertInfo(ids.jean, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), date(2007, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoJean);
			assertInfo(ids.tom, TypeContribuable.HORS_SUISSE, ofsCommune, date(2005, 11, 3), date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoTom);

			assertEquals(16, infoLausanne.getInfosContribuables().size());
		}

		{
			final int ofsCommune = MockCommune.Cossonay.getNoOFS();
			final InfoCommune infoCossonay = results.getInfoPourCommune(ofsCommune);
			assertNotNull(infoCossonay);

			final InfoContribuable infoPascal = infoCossonay.getInfoPourContribuable(ids.pascal);
			final InfoContribuable infoMarc = infoCossonay.getInfoPourContribuable(ids.marc);
			final InfoContribuable infoLouis = infoCossonay.getInfoPourContribuable(ids.louis);
			final InfoContribuable infoAlbert = infoCossonay.getInfoPourContribuable(ids.albert);

			assertInfo(ids.pascal, TypeContribuable.ORDINAIRE, ofsCommune, date(2000, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, TypeContribuable.HORS_CANTON, ofsCommune, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoLouis);
			assertInfo(ids.albert, TypeContribuable.HORS_SUISSE, ofsCommune, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoAlbert);

			assertEquals(3, infoCossonay.getInfosContribuables().size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRunAvecContribuableInvalide() throws Exception {

		loadDatabase(DB_UNIT_CTB_INVALIDE); // rodolf
		Contribuable rodolf = getCtbVaudoisOrdinaireEtImmeubleInvalide();

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(1, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(0, results.infosCommunes.size());

		final Erreur erreur = results.ctbsEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(rodolf.getNumero().longValue(), erreur.noCtb);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRunHorsSuisseRevenu() throws Exception {

		class Ids {
			public Long benjamin;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.benjamin = newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune.Lausanne).getNumero();
				return null;
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		{
			final InfoCommune infoLausanne = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuable infoBenjamin = infoLausanne.getInfoPourContribuable(ids.benjamin);

			assertInfo(ids.benjamin, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBenjamin);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	// Cas JIRA [UNIREG-536]
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRunCtbAvecForPrincipalEtForSecondaireDansMemeCommune() throws Exception {

		class Ids {
			public Long genevieve;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.genevieve = newCtbVaudoisOrdinaireAvecImmeubleDansCommune(MockCommune.Lausanne).getNumero();
				return null;
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		{
			final InfoCommune infoLausanne = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuable infoGeneview = infoLausanne.getInfoPourContribuable(ids.genevieve);
			assertInfo(ids.genevieve, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(2003, 10, 1), null, MotifFor.DEMENAGEMENT_VD, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeneview);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	/**
	 * Test pour UNIREG-2777
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRunCommunesCtbHorsCantonAvecPlusieursForsSecondaires() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777();
				return ctb.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(3, results.infosCommunes.size());

		{
			final InfoCommune infoLausanne = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuable info = infoLausanne.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.Lausanne.getNoOFS(), date(2005, 1, 1), date(2008, 5, 15), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}

		{
			final InfoCommune infoCroy = results.getInfoPourCommune(MockCommune.Croy.getNoOFS());
			assertNotNull(infoCroy);

			final InfoContribuable info = infoCroy.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.Croy.getNoOFS(), date(2004, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);

			assertEquals(1, infoCroy.getInfosContribuables().size());
		}

		{
			final InfoCommune infoRomainmotier = results.getInfoPourCommune(MockCommune.RomainmotierEnvy.getNoOFS());
			assertNotNull(infoRomainmotier);

			final InfoContribuable info = infoRomainmotier.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.RomainmotierEnvy.getNoOFS(), date(2003, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);

			assertEquals(1, infoRomainmotier.getInfosContribuables().size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetTypeContribuable() {
		// données bidon pour pouvoir instancier les assujettissements
		final Contribuable toto = addNonHabitant("Toto", "LaRapière", date(1973, 3, 21), Sexe.MASCULIN);
		addForPrincipal(toto, date(2000, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

		assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesProcessor.getTypeContribuable(new VaudoisOrdinaire(toto, null, null, null, null)));
		assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesProcessor.getTypeContribuable(new Indigent(toto, null, null, null, null)));
		assertEquals(TypeContribuable.DEPENSE, ProduireRolesProcessor.getTypeContribuable(new VaudoisDepense(toto, null, null, null, null)));
		assertEquals(TypeContribuable.MIXTE, ProduireRolesProcessor.getTypeContribuable(new SourcierMixteArt137Al1(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)));
		assertEquals(TypeContribuable.MIXTE, ProduireRolesProcessor.getTypeContribuable(new SourcierMixteArt137Al2(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)));
		assertEquals(TypeContribuable.HORS_CANTON, ProduireRolesProcessor.getTypeContribuable(new HorsCanton(toto, null, null, null, null)));
		assertEquals(TypeContribuable.HORS_SUISSE, ProduireRolesProcessor.getTypeContribuable(new HorsSuisse(toto, null, null, null, null)));
		assertEquals(TypeContribuable.SOURCE, ProduireRolesProcessor.getTypeContribuable(new SourcierPur(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)));
		assertNull(ProduireRolesProcessor.getTypeContribuable(new DiplomateSuisse(toto, null, null, null, null)));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPrioriteDesMotifs() throws Exception {

		final RegDate dateDebut = date(1980, 3, 5);
		final RegDate dateFin = date(2007, 9, 12);

		final Long ppId = doInNewTransaction(new TxCallback<Long>() {

			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1954, 3, 15), Sexe.MASCULIN);

				// sur Lausanne : domicile, activité indépendante et immeuble aux mêmes dates
				addForPrincipal(pp, dateDebut, MotifFor.ARRIVEE_HS, dateFin, MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForSecondaire(pp, dateDebut, MotifFor.ACHAT_IMMOBILIER, dateFin, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				// sur Cossonay : activité indépendante et immeuble aux mêmes dates
				addForSecondaire(pp, dateDebut, MotifFor.ACHAT_IMMOBILIER, dateFin, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.infosCommunes.size());

		// Lausanne
		{
			final InfoCommune infos = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infos);
			assertEquals(1, infos.getInfosContribuables().size());

			final InfoContribuable infosCtb = infos.getInfoPourContribuable(ppId);
			assertNotNull(infosCtb);
			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, dateFin, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
		}

		// Cossonay
		{
			final InfoCommune infos = results.getInfoPourCommune(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infos);
			assertEquals(1, infos.getInfosContribuables().size());

			final InfoContribuable infosCtb = infos.getInfoPourContribuable(ppId);
			assertNotNull(infosCtb);
			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Cossonay.getNoOFS(), dateDebut, dateFin, MotifFor.DEBUT_EXPLOITATION, MotifFor.FIN_EXPLOITATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSuccessionDeForsSurLaCommune() throws Exception {

		// Arrivée à Lausanne en 2005, DEPENSE
		// En 2006, changement MIXTE
		// Au 1er janvier 2007, passage à l'ORDINAIRE
		final RegDate arrivee = date(2005, 5, 12);
		final RegDate passageMixte = date(2006, 7, 1);
		final RegDate passageRole = date(2007, 1, 1);

		final long noCtb = doInNewTransaction(new TxCallback<Long>() {

			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysique toto = addNonHabitant("Toto", "Tartempion", date(1950, 9, 3), Sexe.MASCULIN);

				final ForFiscalPrincipal fficcd = addForPrincipal(toto, arrivee, MotifFor.ARRIVEE_HS, passageMixte.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
				fficcd.setModeImposition(ModeImposition.DEPENSE);

				final ForFiscalPrincipal ffmixte = addForPrincipal(toto, passageMixte, MotifFor.CHGT_MODE_IMPOSITION, passageRole.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
				ffmixte.setModeImposition(ModeImposition.MIXTE_137_2);

				final ForFiscalPrincipal fford = addForPrincipal(toto, passageRole, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
				fford.setModeImposition(ModeImposition.ORDINAIRE);

				return toto.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		final InfoCommune infos = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.getInfosContribuables().size());

		final InfoContribuable infosCtb = infos.getInfoPourContribuable(noCtb);
		assertNotNull(infosCtb);
		assertInfo(noCtb, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), arrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infosCtb);
	}

	/**
	 * SIFISC-1717
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateEtMotifFinSiDemenagementApresFinPeriode() throws Exception {

		// Arrivée en 2006 sur la commune de Lausanne
		// Déménagement en 2008 vers Vevey
		// -> le rôle de Lausanne 2007 devrait donner une fin de for en 2008 pour motif déménagement
		final RegDate arriveeLausanne = date(2006, 8, 1);
		final RegDate departLausanne = date(2008, 6, 30);
		final RegDate arriveeVevey = departLausanne.getOneDayAfter();

		final long idpp = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Pierre", "Haitleloup", date(1970, 5, 3), Sexe.MASCULIN);
				addForPrincipal(pp, arriveeLausanne, MotifFor.ARRIVEE_HS, departLausanne, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, arriveeVevey, MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		final InfoCommune infos = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.getInfosContribuables().size());

		final InfoContribuable infosCtb = infos.getInfoPourContribuable(idpp);
		assertNotNull(infosCtb);
		assertInfo(idpp, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), arriveeLausanne, departLausanne, MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
	}

	/**
	 * SIFISC-1717
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateEtMotifFinSiDemenagementApresFinPeriodeDansMemeOID() throws Exception {

		// Arrivée en 2006 sur la commune de Lausanne
		// Déménagement en 2008 vers Vevey
		// -> le rôle de Lausanne 2007 devrait donner une fin de for en 2008 pour motif déménagement
		final RegDate arriveeRenens = date(2006, 8, 1);
		final RegDate departRenens = date(2008, 6, 30);
		final RegDate arriveePrilly = departRenens.getOneDayAfter();

		final long idpp = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Pierre", "Haitleloup", date(1970, 5, 3), Sexe.MASCULIN);
				addForPrincipal(pp, arriveeRenens, MotifFor.ARRIVEE_HS, departRenens, MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
				addForPrincipal(pp, arriveePrilly, MotifFor.DEMENAGEMENT_VD, MockCommune.Prilly);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourUnOfficeImpot(2007, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		{
			final Map<Long, InfoContribuable> infosRegroupees = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.Prilly.getNoOFSEtendu(), MockCommune.Renens.getNoOFSEtendu()));
			assertNotNull(infosRegroupees);
			assertEquals(1, infosRegroupees.size());

			final InfoContribuable infoCtb = infosRegroupees.get(idpp);
			assertNotNull(infoCtb);
			assertInfo(idpp, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), arriveeRenens, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoCtb);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRunOID() throws Exception {
		
		serviceCivil.setUp(new DefaultMockServiceCivil());

		class Ids {
			public Long paul;
			public Long raoul;
			public Long didier;
			public Long arnold;
			public Long victor;
			public Long balthazar;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				ids.paul = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Lausanne, MockCommune.Aubonne).getNumero();
				ids.raoul = newCtbOrdinaireVaudoisEtImmeuble(MockCommune.Renens, MockCommune.Lausanne).getNumero();
				ids.didier = newCtbOrdinaireAvecDemenagement(MockCommune.Lausanne, MockCommune.Aubonne).getNumero();
				ids.arnold = newCtbOrdinaireAvecDemenagement(MockCommune.Renens, MockCommune.Lausanne).getNumero();
				ids.victor = newCtbOrdinaireAvecDemenagementEnGardantImmeuble(MockCommune.Lausanne, MockCommune.Croy, MockCommune.Renens).getNumero();
				ids.balthazar = newCtbOrdinaireAvecDemenagementAnterieur(MockCommune.Renens, MockCommune.Lausanne).getNumero();
				return null;
			}
		});

		final ProduireRolesResults results = processor.runPourUnOfficeImpot(2007, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), 1, null);
		assertNotNull(results);
		assertEquals(6, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());

		{
			final Map<Long, InfoContribuable> infosRegroupees = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.Lausanne.getNoOFSEtendu(), MockCommune.Renens.getNoOFSEtendu()));
			assertNotNull(infosRegroupees);
			assertEquals(6, infosRegroupees.size());

			final InfoContribuable infoPaul = infosRegroupees.get(ids.paul);
			final InfoContribuable infoRaoul = infosRegroupees.get(ids.raoul);
			final InfoContribuable infoDidier = infosRegroupees.get(ids.didier);
			final InfoContribuable infoArnold = infosRegroupees.get(ids.arnold);
			final InfoContribuable infoVictor = infosRegroupees.get(ids.victor);
			final InfoContribuable infoBalthazar = infosRegroupees.get(ids.balthazar);

			assertInfo(ids.paul, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPaul);
			assertInfo(ids.raoul, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoRaoul);
			assertInfo(ids.didier, TypeContribuable.NON_ASSUJETTI, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), date(2007, 5, 31), MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoDidier);
			assertInfo(ids.arnold, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoArnold);
			assertInfo(ids.victor, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoVictor);
			assertInfo(ids.balthazar, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBalthazar);
		}
	}

	private static void assertInfo(long id, TypeContribuable type, int ofsCommune, RegDate ouvertureFor, RegDate fermetureFor,
	                               MotifFor motifOuverture, MotifFor motifFermeture, InfoContribuable.TypeAssujettissement typeAssujettissement,
	                               TypeContribuable ancienTypeContribuable, InfoContribuable info) {
		assertNotNull(info);
		assertEquals(id, info.noCtb);
		assertEquals(type, info.getTypeCtb());
		assertEquals(ofsCommune, info.getNoOfsDerniereCommune());

		assertInfoExtremite(ouvertureFor, motifOuverture, info.getInfosOuverture());
		assertInfoExtremite(fermetureFor, motifFermeture, info.getInfosFermeture());
		assertEquals(ancienTypeContribuable, info.getAncienTypeContribuable());
		assertEquals(typeAssujettissement, info.getTypeAssujettissementAgrege());
	}

	private static void assertInfoExtremite(RegDate date, MotifFor motif, Pair<RegDate, MotifFor> infosExtremite) {
		if (infosExtremite != null) {
			assertEquals(date, infosExtremite.getFirst());
			assertEquals(motif, infosExtremite.getSecond());
		}
		else {
			assertNull(date);
			assertNull(motif);
		}
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années
	 */
	private Contribuable newCtbVaudoisOrdinaire(MockCommune commune) {
		final Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, commune);
		return paul;
	}

	/**
	 * @return un contribuable vaudois arrivé dans le canton en 2007
	 */
	private Contribuable newCtbVaudoisOrdinaireDepuis2007(MockCommune commune) {
		final Contribuable incognito = addNonHabitant("Incog", "Nito", null, null);
		addForPrincipal(incognito, date(2007, 4, 13), MotifFor.ARRIVEE_HC, commune);
		return incognito;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune commune) {
		final Contribuable raoul = addNonHabitant("Raoul", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton au 31.12.2007
	 */
	private Contribuable newCtbVaudoisPartiHorsCantonTrenteEtUnDecembre(MockCommune commune) {
		final Contribuable raoul = addNonHabitant("Marie", "Coller", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2008, 1, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse au 31.12.2007
	 */
	private Contribuable newCtbVaudoisPartiHorsSuisseTrenteEtUnDecembre(MockCommune commune) {
		final Contribuable marie = addNonHabitant("Marie", "Coller", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(marie, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(marie, date(2008, 1, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return marie;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007 mais qui a gardé un immeuble
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final Contribuable louis = addNonHabitant("Louis", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse en 2007 mais qui a gardé un immeuble
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsSuisseEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final Contribuable louis = addNonHabitant("Albert", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HS, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2006
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune commune) {
		final Contribuable didier = addNonHabitant("Didier", "Duvolet", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(didier, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(didier, date(2006, 10, 1), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		return didier;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune commune) {
		final Contribuable benjamin = addNonHabitant("Benjamin", "TientPasEnPlace", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(benjamin, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(benjamin, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne);
		addForPrincipal(benjamin, date(2007, 10, 1), MotifFor.ARRIVEE_HS, commune);
		return benjamin;
	}

	/**
	 * @return un contribuable vaudois ordinaire avec for principal et secondaire dans la même commune
	 */
	private Contribuable newCtbVaudoisOrdinaireAvecImmeubleDansCommune(MockCommune commune) {
		final Contribuable genevieve = addNonHabitant("Geneviève", "Maillefer", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(genevieve, date(2003, 10, 1), MotifFor.DEMENAGEMENT_VD, commune);
		addImmeuble(genevieve, commune, date(2003, 11, 25), null);
		return genevieve;
	}

	/**
	 * @return un diplomate suisse basé à l'étranger mais rattaché à une commune vaudoise
	 */
	private Contribuable newCtbDiplomateSuisse(MockCommune commune) {
		final Contribuable marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(marc, date(1968, 11, 3), MotifFor.MAJORITE, commune, MotifRattachement.DIPLOMATE_SUISSE);
		return marc;
	}

	private Contribuable newCtbOrdinaireVaudoisEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final Contribuable pascal = newCtbVaudoisOrdinaire(communeResidence);
		addImmeuble(pascal, communeImmeuble, date(2000, 1, 1), null);
		return pascal;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier-mixte
	 */
	private Contribuable newCtbVaudoisSourcierMixte(MockCommune commune) {
		final Contribuable arnold = addNonHabitant("Arnold", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		final ForFiscalPrincipal fors = addForPrincipal(arnold, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune);
		fors.setModeImposition(ModeImposition.MIXTE_137_2);
		return arnold;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (non gris!)
	 */
	private Contribuable newCtbVaudoisSourcier(MockCommune commune) {
		final PersonnePhysique victor = addNonHabitant("Victor", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		victor.setNumeroIndividu(263343L);
		final ForFiscalPrincipal fors = addForPrincipal(victor, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune);
		fors.setModeImposition(ModeImposition.SOURCE);
		return victor;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (gris)
	 */
	private Contribuable newCtbVaudoisSourcierGris(MockCommune commune) {
		final Contribuable albertine = addNonHabitant("Albertine", "Duplat", date(1969, 4, 13), Sexe.FEMININ);
		final ForFiscalPrincipal fors = addForPrincipal(albertine, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune);
		fors.setModeImposition(ModeImposition.SOURCE);
		return albertine;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton
	 */
	private Contribuable newCtbHorsCantonEtImmeuble(MockCommune commune) {
		final Contribuable geo = addNonHabitant("Geo", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(geo, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(geo, commune, date(2003, 3, 1), null);
		return geo;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, dont un acheté et vendu en 2007
	 */
	private Contribuable newCtbHorsCantonEtDeuxImmeubles(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final Contribuable donald = addNonHabitant("Donald", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(donald, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(donald, communeImmeuble1, date(2007, 3, 1), date(2007, 6, 30));
		addImmeuble(donald, communeImmeuble2, date(1990, 1, 15), null);
		return donald;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, l'un vendu en 2007, l'autre acheté en 2007, sans chevauchement
	 */
	private Contribuable newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final Contribuable georges = addNonHabitant("Georges", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(georges, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(georges, communeImmeuble1, date(1980, 3, 1), date(2007, 6, 30));
		addImmeuble(georges, communeImmeuble2, date(2007, 11, 15), null);
		return georges;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton qui a été vendu en 2007
	 */
	private Contribuable newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune commune) {
		final Contribuable johnny = addNonHabitant("Johnny", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(johnny, date(2005, 11, 3), null, MockCommune.Bern);
		addImmeuble(johnny, commune, date(2005, 11, 3), date(2007, 8, 30));
		return johnny;
	}

	/**
	 * @return un contribuable avec un for principal hors Suisse, et avec un immeuble dans le canton qui a été vendu le 31.12.2007
	 */
	private Contribuable newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune commune) {
		final Contribuable tom = addNonHabitant("Tom", "Cruise", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tom, date(2005, 11, 3), null, MockPays.Albanie);
		addImmeuble(tom, commune, date(2005, 11, 3), date(2007, 12, 31));
		return tom;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec une activité indépendente dans le canton qui a été stoppé en 2007
	 */
	private Contribuable newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune commune) {
		final Contribuable tyler = addNonHabitant("Tyler", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tyler, date(2005, 11, 3), null, MockCommune.Bern);
		addForSecondaire(tyler, date(2005, 11, 3), MotifFor.DEBUT_EXPLOITATION, date(2007, 8, 30), MotifFor.FIN_EXPLOITATION, commune.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return tyler;
	}

	/**
	 * @return un contribuable avec un for fermé en 1983
	 */
	private Contribuable newCtbVaudoisOrdinairePartiHorsCantonEn1983(MockCommune commune) {
		final Contribuable pierre = addNonHabitant("Pierre", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(pierre, date(1968, 11, 3), MotifFor.ARRIVEE_HC, date(1983, 7, 1), MotifFor.DEPART_HC, commune);
		return pierre;
	}

	/**
	 * @return un contribuable avec un for annulé
	 */
	private Contribuable newCtbVaudoisOrdinaireAnnule(MockCommune commune) {
		ForFiscalPrincipal fors;
		final Contribuable jean = addNonHabitant("Jean", "Duchmol", date(1948, 11, 3), Sexe.MASCULIN);
		fors = addForPrincipal(jean, date(1968, 11, 3), MotifFor.ARRIVEE_HC, commune);
		fors.setAnnulationDate(DateHelper.getDate(1967, 1, 1));
		return jean;
	}

	/**
	 * @return un contribuable avec un for hors canton
	 */
	private Contribuable newCtbHorsCantonSansForSecondaire() {
		final Contribuable jeans = addNonHabitant("Jean", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jeans, date(1968, 11, 3), null, MockCommune.Neuchatel);
		return jeans;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton mais vendu en 2005
	 */
	private Contribuable newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune commune) {
		final Contribuable popol = addNonHabitant("Popol", "Dillon", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(popol, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(popol, commune, date(2003, 3, 1), date(2005, 5, 31));
		return popol;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un for immeuble annulé
	 */
	private Contribuable newCtbHorsCantonEtForImmeubleAnnule(MockCommune commune) {
		final Contribuable rama = addNonHabitant("Rama", "Truelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(rama, date(1968, 11, 3), null, MockCommune.Neuchatel);
		ForFiscalSecondaire fs = addImmeuble(rama, commune, date(2003, 3, 1), null);
		fs.setAnnule(true);
		return rama;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre
	 */
	private Contribuable newCtbOrdinaireAvecDemenagement(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2007, 6, 1);
		final Contribuable ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois qui garde un immeuble de part et d'autre du déménagement
	 */
	private Contribuable newCtbOrdinaireAvecDemenagementEnGardantImmeuble(MockCommune avant, MockCommune apres, MockCommune communeImmeuble) {
		final Contribuable ctb = newCtbOrdinaireAvecDemenagement(avant, apres);
		addForSecondaire(ctb, date(2005, 6, 12), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre (avant 2007)
	 */
	private Contribuable newCtbOrdinaireAvecDemenagementAnterieur(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2005, 6, 1);
		final Contribuable ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable invalide
	 */
	private Contribuable getCtbVaudoisOrdinaireEtImmeubleInvalide() {
		return (Contribuable) hibernateTemplate.get(Contribuable.class, Long.valueOf(10000666));
	}

	/**
	 * UNIREG-2777
	 * @return un contribuable HC avec deux fors secondaires immeubles ouverts sur l'OID d'Orbe et un autre fermé l'année des rôles sur l'OID de Lausanne
	 */
	private Contribuable getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777() {
		final Contribuable pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
		addForPrincipal(pp, date(1988, 9, 12), MotifFor.MAJORITE, date(2007, 6, 11), MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(pp, date(2007, 6, 12), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.RomainmotierEnvy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2004, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
		return pp;
	}

	private ForFiscalSecondaire addImmeuble(final Contribuable ctb, MockCommune commune, RegDate debut, RegDate fin) {
		MotifFor motifFermeture = (fin == null ? null : MotifFor.VENTE_IMMOBILIER);
		return addForSecondaire(ctb, debut, MotifFor.ACHAT_IMMOBILIER, fin, motifFermeture, commune.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
	}

	private static <T> void assertNextIs(final Iterator<T> iter, T expected) {
		assertTrue(iter.hasNext());

		final T actual = iter.next();
		assertNotNull(actual);
		assertEquals(expected, actual);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisMarieDepartHS() throws Exception {

		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(1990, 4, 13), null);
				final MenageCommun mc = couple.getMenage();
				final ForFiscalPrincipal ffp = addForPrincipal(mc, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return mc.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.infosCommunes.size());

		final ProduireRolesResults.Ignore ignore = results.ctbsIgnores.get(0);
		assertNotNull(ignore);
		assertEquals(mcId, ignore.noCtb);
		assertEquals(ProduireRolesResults.IgnoreType.SOURCIER_GRIS, ignore.raison);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSourcierGrisCelibataireDepartHS() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.infosCommunes.size());

		final ProduireRolesResults.Ignore ignore = results.ctbsIgnores.get(0);
		assertNotNull(ignore);
		assertEquals(ppId, ignore.noCtb);
		assertEquals(ProduireRolesResults.IgnoreType.SOURCIER_GRIS, ignore.raison);
	}

	/**
	 * SIFISC-1797
	 */
	@Test
	public void testSourcierGrisCelibataireDepartHSAvecForHSRenseigne() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
				addForPrincipal(pp, date(2008, 9, 11), MotifFor.DEPART_HS, null, null, MockPays.PaysInconnu, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.infosCommunes.size());

		final ProduireRolesResults.Ignore ignore = results.ctbsIgnores.get(0);
		assertNotNull(ignore);
		assertEquals(ppId, ignore.noCtb);
		assertEquals(ProduireRolesResults.IgnoreType.SOURCIER_GRIS, ignore.raison);
	}

	/**
	 * SIFISC-1717 : pas de motif/date de fermeture alors que l'assujettissement est indiqué comme "terminé dans PF"
	 */
	@Test
	public void testMixtePasseOrdinaireAnneeApresRoles() throws Exception {

		final RegDate arrivee = date(2006, 1, 1);
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				final RegDate obtentionPermisC = date(2009, 9, 11);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, obtentionPermisC.getOneDayBefore(), MotifFor.PERMIS_C_SUISSE, MockCommune.Bussigny, MotifRattachement.DOMICILE, ModeImposition.MIXTE_137_2);
				addForPrincipal(pp, obtentionPermisC, MotifFor.PERMIS_C_SUISSE, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		{
			final InfoCommune infos = results.infosCommunes.get(MockCommune.Bussigny.getNoOFSEtendu());
			assertNotNull(infos);

			final InfoContribuable info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.MIXTE, MockCommune.Bussigny.getNoOFS(), arrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
		}
	}

	/**
	 * SIFISC-1717 : pas de motif/date de fermeture alors que l'assujettissement est indiqué comme "terminé dans PF"
	 */
	@Test
	public void testVaudoisPartiHCavecImmeubleDansCommuneDeDepart() throws Exception {

		final RegDate achat = date(2006, 1, 1);
		final RegDate arrivee = date(2007, 1, 1);
		final RegDate departHc = date(2009, 10, 12);
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);

				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Albanie);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, departHc, MotifFor.DEPART_HC, MockCommune.Cossonay);
				addForPrincipal(pp, departHc.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bern);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.infosCommunes.size());

		{
			final InfoCommune infos = results.infosCommunes.get(MockCommune.Cossonay.getNoOFSEtendu());
			assertNotNull(infos);

			final InfoContribuable info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Cossonay.getNoOFS(), achat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
		}
	}

	@Test
	@Ignore("SIFISC-4682")
	public void testArriveeHcAvecImmeublePresent() throws Exception {
		
		final long noIndividu = 3256783435623456L;
		final RegDate achat = date(2001, 3, 12);
		final RegDate arrivee = date(2007, 7, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, arrivee, null);
			}
		});
		
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, achat, MotifFor.ACHAT_IMMOBILIER, arrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Neuchatel);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HC, MockCommune.Moudon);
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesResults results = processor.runPourToutesCommunes(2011, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.infosCommunes.size());

		{
			final InfoCommune infos = results.infosCommunes.get(MockCommune.Echallens.getNoOFSEtendu());
			assertNotNull(infos);

			final InfoContribuable info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Echallens.getNoOFS(), achat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
		{
			final InfoCommune infos = results.infosCommunes.get(MockCommune.Moudon.getNoOFSEtendu());
			assertNotNull(infos);

			final InfoContribuable info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Moudon.getNoOFS(), achat, null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}
}
