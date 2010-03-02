package ch.vd.uniregctb.role;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.role.ProduireRolesResults.Erreur;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoCommune;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ProduireRolesCommuneProcessorTest extends BusinessTest {

	private HibernateTemplate hibernateTemplate;
	private ProduireRolesCommuneProcessor processor;

	private static final String DB_UNIT_CTB_INVALIDE = "ContribuableInvalideTest.xml";

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");

		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ServiceCivilService serviceCivilService = getBean(ServiceCivilService.class, "serviceCivilService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new ProduireRolesCommuneProcessor(hibernateTemplate, infraService, tiersDAO, transactionManager, adresseService, tiersService, serviceCivilService);
	}

	@Test
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

		doInNewTransaction(new TxCallback() {
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
			final InfoCommune infoLausanne = results.getInfoPourCommune(MockCommune.Lausanne.getNoOFS());
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

			assertInfo(ids.paul, TypeContribuable.ORDINAIRE, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPaul);
			assertInfo(ids.incognito, TypeContribuable.ORDINAIRE, date(2007, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoIncognito);
			assertInfo(ids.raoul, TypeContribuable.NON_ASSUJETTI, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoRaoul);
			assertNull(infoDidier);
			assertInfo(ids.arnold, TypeContribuable.MIXTE, date(1983, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoArnold);
			assertInfo(ids.victor, TypeContribuable.SOURCE, date(1983, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoVictor);
			assertNull(infoAlbertine);
			assertInfo(ids.geo, TypeContribuable.HORS_CANTON, date(2003, 3, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeo);
			assertInfo(ids.donald, TypeContribuable.HORS_CANTON, date(1990, 1, 15), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoDonald);
			assertInfo(ids.johnny, TypeContribuable.HORS_CANTON, date(2005, 11, 3), date(2007, 8, 30), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoJohnny);
			assertInfo(ids.tyler, TypeContribuable.HORS_CANTON, date(2005, 11, 3), date(2007, 8, 30), MotifFor.DEBUT_EXPLOITATION, MotifFor.FIN_EXPLOITATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoTyler);
			assertInfo(ids.pascal, TypeContribuable.ORDINAIRE, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, TypeContribuable.NON_ASSUJETTI, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoLouis);
			assertInfo(ids.albert, TypeContribuable.ORDINAIRE, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoAlbert);
			assertInfo(ids.georges, TypeContribuable.HORS_CANTON, date(1980, 3, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeorges);
			assertInfo(ids.marie, TypeContribuable.ORDINAIRE, date(1983, 4, 13), date(2007, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoMarie);
			assertInfo(ids.jean, TypeContribuable.ORDINAIRE, date(1983, 4, 13), date(2007, 12, 31), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoJean);
			assertInfo(ids.tom, TypeContribuable.HORS_SUISSE, date(2005, 11, 3), date(2007, 12, 31), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoTom);

			assertEquals(16, infoLausanne.getInfosContribuables().size());
		}

		{
			final InfoCommune infoCossonay = results.getInfoPourCommune(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infoCossonay);

			final InfoContribuable infoPascal = infoCossonay.getInfoPourContribuable(ids.pascal);
			final InfoContribuable infoMarc = infoCossonay.getInfoPourContribuable(ids.marc);
			final InfoContribuable infoLouis = infoCossonay.getInfoPourContribuable(ids.louis);
			final InfoContribuable infoAlbert = infoCossonay.getInfoPourContribuable(ids.albert);

			assertInfo(ids.pascal, TypeContribuable.ORDINAIRE, date(2000, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, TypeContribuable.HORS_CANTON, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoLouis);
			assertInfo(ids.albert, TypeContribuable.HORS_SUISSE, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoAlbert);

			assertEquals(3, infoCossonay.getInfosContribuables().size());
		}
	}

	@Test
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
	public void testRunHorsSuisseRevenu() throws Exception {

		class Ids {
			public Long benjamin;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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

			assertInfo(ids.benjamin, TypeContribuable.ORDINAIRE, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBenjamin);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	// Cas JIRA [UNIREG-536]
	@Test
	public void testRunCtbAvecForPrincipalEtForSecondaireDansMemeCommune() throws Exception {

		class Ids {
			public Long genevieve;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback() {
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
			assertInfo(ids.genevieve, TypeContribuable.ORDINAIRE, date(2003, 10, 1), null, MotifFor.DEMENAGEMENT_VD, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeneview);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	@Test
	public void testGetTypeContribuable() {
		// données bidon pour pouvoir instancier les assujettissements
		final Contribuable toto = addNonHabitant("Toto", "LaRapière", date(1973, 3, 21), Sexe.MASCULIN);
		addForPrincipal(toto, date(2000, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
		final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete(toto, 2007);

		assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesCommuneProcessor.getTypeContribuable(new VaudoisOrdinaire(toto, null, null, null, null)));
		assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesCommuneProcessor.getTypeContribuable(new Indigent(toto, null, null, null, null)));
		assertEquals(TypeContribuable.DEPENSE, ProduireRolesCommuneProcessor.getTypeContribuable(new VaudoisDepense(toto, null, null, null, null)));
		assertEquals(TypeContribuable.MIXTE, ProduireRolesCommuneProcessor.getTypeContribuable(new SourcierMixte(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)));
		assertEquals(TypeContribuable.HORS_CANTON, ProduireRolesCommuneProcessor.getTypeContribuable(new HorsCanton(toto, null, null, null, null)));
		assertEquals(TypeContribuable.HORS_SUISSE, ProduireRolesCommuneProcessor.getTypeContribuable(new HorsSuisse(toto, null, null, null, null)));
		assertEquals(TypeContribuable.SOURCE, ProduireRolesCommuneProcessor.getTypeContribuable(new SourcierPur(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)));
		assertNull(ProduireRolesCommuneProcessor.getTypeContribuable(new DiplomateSuisse(toto, null, null, null, null)));
	}

	@Test
	public void testPrioriteDesMotifs() throws Exception {

		final RegDate dateDebut = date(1980, 3, 5);
		final RegDate dateFin = date(2007, 9, 12);

		final Long ppId = (Long) doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {
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
			assertInfo(ppId, TypeContribuable.ORDINAIRE, dateDebut, dateFin, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
		}

		// Cossonay
		{
			final InfoCommune infos = results.getInfoPourCommune(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infos);
			assertEquals(1, infos.getInfosContribuables().size());

			final InfoContribuable infosCtb = infos.getInfoPourContribuable(ppId);
			assertNotNull(infosCtb);
			assertInfo(ppId, TypeContribuable.ORDINAIRE, dateDebut, dateFin, MotifFor.DEBUT_EXPLOITATION, MotifFor.FIN_EXPLOITATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
		}
	}

	@Test
	public void testSuccessionDeForsSurLaCommune() throws Exception {

		// Arrivée à Lausanne en 2005, DEPENSE
		// En 2006, changement MIXTE
		// Au 1er janvier 2007, passage à l'ORDINAIRE
		final RegDate arrivee = date(2005, 5, 12);
		final RegDate passageMixte = date(2006, 7, 1);
		final RegDate passageRole = date(2007, 1, 1);

		final long noCtb = (Long) doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

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
		assertInfo(noCtb, TypeContribuable.ORDINAIRE, arrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infosCtb);
	}
	
	private static void assertInfo(long id, TypeContribuable type, RegDate ouvertureFor, RegDate fermetureFor,
	                               MotifFor motifOuverture, MotifFor motifFermeture, InfoContribuable.TypeAssujettissement typeAssujettissement,
	                               TypeContribuable ancienTypeContribuable, InfoContribuable info) {
		assertNotNull(info);
		assertEquals(id, info.noCtb);
		assertEquals(type, info.getTypeCtb());

		assertInfoExtremite(ouvertureFor, motifOuverture, info.getInfosOuverture());
		assertInfoExtremite(fermetureFor, motifFermeture, info.getInfosFermeture());
		assertEquals(ancienTypeContribuable, info.getAncienTypeContribuable());
		assertEquals(typeAssujettissement, info.getTypeAssujettissementDansCommune());
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
		addForPrincipal(marc, date(1968, 11, 3), MotifFor.MAJORITE, null, null, MockCommune.Lausanne.getNoOFS(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DIPLOMATE_SUISSE);
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
		addForPrincipal(geo, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addImmeuble(geo, commune, date(2003, 3, 1), null);
		return geo;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, dont un acheté et vendu en 2007
	 */
	private Contribuable newCtbHorsCantonEtDeuxImmeubles(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final Contribuable donald = addNonHabitant("Donald", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(donald, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addImmeuble(donald, communeImmeuble1, date(2007, 3, 1), date(2007, 6, 30));
		addImmeuble(donald, communeImmeuble2, date(1990, 1, 15), null);
		return donald;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, l'un vendu en 2007, l'autre acheté en 2007, sans chevauchement
	 */
	private Contribuable newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final Contribuable georges = addNonHabitant("Georges", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(georges, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addImmeuble(georges, communeImmeuble1, date(1980, 3, 1), date(2007, 6, 30));
		addImmeuble(georges, communeImmeuble2, date(2007, 11, 15), null);
		return georges;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton qui a été vendu en 2007
	 */
	private Contribuable newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune commune) {
		final Contribuable johnny = addNonHabitant("Johnny", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(johnny, date(2005, 11, 3), null, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addImmeuble(johnny, commune, date(2005, 11, 3), date(2007, 8, 30));
		return johnny;
	}

	/**
	 * @return un contribuable avec un for principal hors Suisse, et avec un immeuble dans le canton qui a été vendu le 31.12.2007
	 */
	private Contribuable newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune commune) {
		final Contribuable tom = addNonHabitant("Tom", "Cruise", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tom, date(2005, 11, 3), null, null, null, MockPays.Albanie.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE);
		addImmeuble(tom, commune, date(2005, 11, 3), date(2007, 12, 31));
		return tom;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec une activité indépendente dans le canton qui a été stoppé en 2007
	 */
	private Contribuable newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune commune) {
		final Contribuable tyler = addNonHabitant("Tyler", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tyler, date(2005, 11, 3), null, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
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
		addForPrincipal(jeans, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		return jeans;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton mais vendu en 2005
	 */
	private Contribuable newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune commune) {
		final Contribuable popol = addNonHabitant("Popol", "Dillon", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(popol, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		addImmeuble(popol, commune, date(2003, 3, 1), date(2005, 5, 31));
		return popol;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un for immeuble annulé
	 */
	private Contribuable newCtbHorsCantonEtForImmeubleAnnule(MockCommune commune) {
		final Contribuable rama = addNonHabitant("Rama", "Truelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(rama, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE);
		ForFiscalSecondaire fs = addImmeuble(rama, commune, date(2003, 3, 1), null);
		fs.setAnnule(true);
		return rama;
	}

	/**
	 * @return un contribuable invalide
	 */
	private Contribuable getCtbVaudoisOrdinaireEtImmeubleInvalide() {
		final Contribuable rodolf = (Contribuable) hibernateTemplate.get(Contribuable.class, Long.valueOf(10000666));
		return rodolf;
	}

	private ForFiscalSecondaire addImmeuble(final Contribuable ctb, MockCommune commune, RegDate debut, RegDate fin) {
		MotifFor motifFermeture = (fin == null ? null : MotifFor.VENTE_IMMOBILIER);
		ForFiscalSecondaire fs = addForSecondaire(ctb, debut, MotifFor.ACHAT_IMMOBILIER, fin, motifFermeture, commune.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return fs;
	}

	private static <T> void assertNextIs(final Iterator<T> iter, T expected) {
		assertTrue(iter.hasNext());

		final T actual = iter.next();
		assertNotNull(actual);
		assertEquals(expected, actual);
	}
}
