package ch.vd.uniregctb.role.before2016;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementPersonnesPhysiquesCalculator;
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
import ch.vd.uniregctb.role.before2016.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.role.before2016.ProduireRolesResults.Erreur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class ProduireRolesProcessorTest extends BusinessTest {

	private ProduireRolesProcessor processor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new ProduireRolesProcessor(hibernateTemplate, serviceInfra, tiersDAO, transactionManager, adresseService, tiersService, serviceCivil, validationService, assujettissementService);
	}

	@Test
	public void testCreateIteratorOnContribuables() throws Exception {

		class Ids {
			Long paul;
			Long incognito;
			Long raoul;
			Long arnold;
			Long victor;
			Long geo;
			Long donald;
			Long johnny;
			Long tyler;
			Long marc;
			Long louis;
			Long albertine;
			Long didier;
			Long pierre;
			Long jean;
			Long jeans;
			Long popol;
			Long rama;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// Contribuable devant être pris en compte
				ids.paul = newCtbVaudoisOrdinaire(MockCommune.Lausanne).getNumero();
				ids.incognito = newCtbVaudoisOrdinaireDepuis2007(MockCommune.Lausanne).getNumero();
				ids.raoul = newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune.Lausanne).getNumero();
				ids.arnold = newCtbVaudoisSourcierMixte(MockCommune.Lausanne).getNumero();
				ids.victor = newCtbVaudoisSourcier(MockCommune.Lausanne).getNumero();
				ids.geo = newCtbHorsCantonEtImmeuble(MockCommune.Lausanne).getNumero();
				ids.donald = newCtbHorsCantonEtDeuxImmeubles(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
				ids.johnny = newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune.Lausanne).getNumero();
				ids.tyler = newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune.Lausanne).getNumero();
				ids.marc = newCtbDiplomateSuisse(MockCommune.Lausanne).getNumero();
				ids.louis = newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune.Lausanne, MockCommune.Lausanne).getNumero();
				ids.albertine = newCtbVaudoisSourcierGris(MockCommune.Lausanne).getNumero();
				
				// Contribuable devant être ignorés
				ids.didier = newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune.Lausanne).getNumero();
				ids.pierre = newCtbVaudoisOrdinairePartiHorsCantonEn1983(MockCommune.Lausanne).getNumero();
				ids.jean = newCtbVaudoisOrdinaireAnnule(MockCommune.Lausanne).getNumero();
				ids.jeans = newCtbHorsCantonSansForSecondaire().getNumero();
				ids.popol = newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune.Lausanne).getNumero();
				ids.rama = newCtbHorsCantonEtForImmeubleAnnule(MockCommune.Lausanne).getNumero();
			}
		});

		// Contribuable invalide mais devant être pris en compte
		final Long rodolf = newCtbVaudoisOrdinaireEtImmeubleInvalide();

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				/*
				 * Contribuable devant être ignorés
				 */
				final List<Long> expected =
						Arrays.asList(ids.paul, ids.incognito, ids.raoul, ids.arnold, ids.victor, ids.geo, ids.donald, ids.johnny, ids.tyler, ids.marc, ids.louis, ids.albertine, rodolf);

				final List<Long> list = processor.getIdsOfAllContribuablesPP(2007);
				assertNotNull(list);
				assertEquals(expected, list);
			}
		});
	}

	@Test
	public void testRun() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil());

		class Ids {
			public Long paul;
			public Long incognito;
			public Long raoul;
			public Long didier;
			public Long laurent;
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
				ids.laurent = newCtbVaudoisOrdinairePartiHorsCantonEn2008(MockCommune.Lausanne).getNumero();
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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(19, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(2, results.ctbsIgnores.size()); // le diplomate et le sourcier gris
		assertEquals(2, results.getInfosCommunes().size());

		{
			final int ofsCommune = MockCommune.Lausanne.getNoOFS();
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(ofsCommune);
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoPaul = infoLausanne.getInfoPourContribuable(ids.paul);
			final InfoContribuablePP infoIncognito = infoLausanne.getInfoPourContribuable(ids.incognito);
			final InfoContribuablePP infoRaoul = infoLausanne.getInfoPourContribuable(ids.raoul);
			final InfoContribuablePP infoDidier = infoLausanne.getInfoPourContribuable(ids.didier);
			final InfoContribuablePP infoLaurent = infoLausanne.getInfoPourContribuable(ids.laurent);
			final InfoContribuablePP infoArnold = infoLausanne.getInfoPourContribuable(ids.arnold);
			final InfoContribuablePP infoVictor = infoLausanne.getInfoPourContribuable(ids.victor);
			final InfoContribuablePP infoAlbertine = infoLausanne.getInfoPourContribuable(ids.albertine);
			final InfoContribuablePP infoGeo = infoLausanne.getInfoPourContribuable(ids.geo);
			final InfoContribuablePP infoDonald = infoLausanne.getInfoPourContribuable(ids.donald);
			final InfoContribuablePP infoJohnny = infoLausanne.getInfoPourContribuable(ids.johnny);
			final InfoContribuablePP infoTyler = infoLausanne.getInfoPourContribuable(ids.tyler);
			final InfoContribuablePP infoPascal = infoLausanne.getInfoPourContribuable(ids.pascal);
			final InfoContribuablePP infoMarc = infoLausanne.getInfoPourContribuable(ids.marc);
			final InfoContribuablePP infoLouis = infoLausanne.getInfoPourContribuable(ids.louis);
			final InfoContribuablePP infoAlbert = infoLausanne.getInfoPourContribuable(ids.albert);
			final InfoContribuablePP infoGeorges = infoLausanne.getInfoPourContribuable(ids.georges);
			final InfoContribuablePP infoMarie = infoLausanne.getInfoPourContribuable(ids.marie);
			final InfoContribuablePP infoJean = infoLausanne.getInfoPourContribuable(ids.jean);
			final InfoContribuablePP infoTom = infoLausanne.getInfoPourContribuable(ids.tom);

			assertInfo(ids.paul, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPaul);
			assertInfo(ids.incognito, TypeContribuable.ORDINAIRE, ofsCommune, date(2007, 4, 13), null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoIncognito);
			assertInfo(ids.raoul, TypeContribuable.NON_ASSUJETTI, ofsCommune, date(1983, 4, 13), date(2007, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoRaoul);
			assertNull(infoDidier);
			assertInfo(ids.laurent, TypeContribuable.ORDINAIRE, ofsCommune, date(1983, 4, 13), date(2008, 9, 30), MotifFor.ARRIVEE_HC, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoLaurent);
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

			assertEquals(17, infoLausanne.getInfosContribuables().size());
		}

		{
			final int ofsCommune = MockCommune.Cossonay.getNoOFS();
			final InfoCommunePP infoCossonay = results.getInfosCommunes().get(ofsCommune);
			assertNotNull(infoCossonay);

			final InfoContribuablePP infoPascal = infoCossonay.getInfoPourContribuable(ids.pascal);
			final InfoContribuablePP infoMarc = infoCossonay.getInfoPourContribuable(ids.marc);
			final InfoContribuablePP infoLouis = infoCossonay.getInfoPourContribuable(ids.louis);
			final InfoContribuablePP infoAlbert = infoCossonay.getInfoPourContribuable(ids.albert);

			assertInfo(ids.pascal, TypeContribuable.ORDINAIRE, ofsCommune, date(2000, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPascal);
			assertNull(infoMarc);
			assertInfo(ids.louis, TypeContribuable.HORS_CANTON, ofsCommune, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoLouis);
			assertInfo(ids.albert, TypeContribuable.HORS_SUISSE, ofsCommune, date(2001, 3, 2), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoAlbert);

			assertEquals(3, infoCossonay.getInfosContribuables().size());
		}
	}

	@Test
	public void testRunAvecContribuableInvalide() throws Exception {

		Long rodolf = newCtbVaudoisOrdinaireEtImmeubleInvalide();

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(1, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(0, results.getNoOfsCommunesTraitees().size());

		final Erreur erreur = results.ctbsEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(rodolf.longValue(), erreur.noCtb);
	}

	@Test
	public void testRunHorsSuisseRevenuDansMemeCommuneLaMemeAnnee() throws Exception {

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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoBenjamin = infoLausanne.getInfoPourContribuable(ids.benjamin);

			assertInfo(ids.benjamin, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBenjamin);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	@Test
	public void testRunHorsSuisseRevenuDansAutreCommuneLaMemeAnnee() throws Exception {

		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune.Lausanne, MockCommune.Bussigny);
				return ctb.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoBenjamin = infoLausanne.getInfoPourContribuable(idCtb);

			assertInfo(idCtb, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), date(2007, 3, 31), MotifFor.MAJORITE, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoBenjamin);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
		{
			final InfoCommunePP infoBussigny = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);

			final InfoContribuablePP infoBenjamin = infoBussigny.getInfoPourContribuable(idCtb);

			assertInfo(idCtb, TypeContribuable.ORDINAIRE, MockCommune.Bussigny.getNoOFS(), date(2007, 10, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBenjamin);

			assertEquals(1, infoBussigny.getInfosContribuables().size());
		}
	}

	@Test
	public void testRunSourcierPartiHorsSuisseEtRevenuDansAutreCommuneLaMemeAnnee() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Marie", "Lasource", Sexe.FEMININ);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bussigny.RueDeLIndustrie, null, date(2007, 10, 1), null);
			}
		});

		final long idCtb = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = newCtbVaudoisSourcierPartiHorsSuisseEtRevenuDansLaMemeAnnee(noIndividu, MockCommune.Lausanne, MockCommune.Bussigny);
				return ctb.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoBenjamin = infoLausanne.getInfoPourContribuable(idCtb);

			assertInfo(idCtb, TypeContribuable.SOURCE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), date(2007, 3, 31), MotifFor.MAJORITE, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoBenjamin);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
		{
			final InfoCommunePP infoBussigny = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);

			final InfoContribuablePP infoBenjamin = infoBussigny.getInfoPourContribuable(idCtb);

			assertInfo(idCtb, TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), date(2007, 10, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBenjamin);

			assertEquals(1, infoBussigny.getInfosContribuables().size());
		}
	}

	@Test
	public void testRunSourcierPartiDansAutreCommuneLaMemeAnneePuisRetourPremiereCommuneDebutAnneeSuivante() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Marie", "Lasource", Sexe.FEMININ);
			}
		});

		final long idMarie = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique m = addHabitant(noIndividu);
				addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
				addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(m, date(2008, 1, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, ModeImposition.SOURCE);
				return m.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoCtb = infoLausanne.getInfoPourContribuable(idMarie);

			assertInfo(idMarie, TypeContribuable.SOURCE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), date(2007, 3, 31), MotifFor.MAJORITE, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoCtb);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
		{
			final InfoCommunePP infoBussigny = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);

			final InfoContribuablePP infoCtb = infoBussigny.getInfoPourContribuable(idMarie);

			assertInfo(idMarie, TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), date(2007, 10, 1), date(2007, 12, 31), MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoCtb);

			assertEquals(1, infoBussigny.getInfosContribuables().size());
		}
	}

	@Test
	public void testRunSourcierPartiHorsSuisseEtRevenuDansAutreCommuneLaMemeAnneePuisRetourPremiereCommuneAnneeSuivante() throws Exception {

		final long noIndividu = 183747L;
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Marie", "Lasource", Sexe.FEMININ);
			}
		});

		final long idMarie = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique m = addHabitant(noIndividu);
				addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
				addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
				addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 15), MotifFor.DEMENAGEMENT_VD, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(m, date(2008, 5, 16), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, ModeImposition.SOURCE);
				return m.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoCtb = infoLausanne.getInfoPourContribuable(idMarie);

			assertInfo(idMarie, TypeContribuable.SOURCE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), date(2007, 3, 31), MotifFor.MAJORITE, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoCtb);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
		{
			final InfoCommunePP infoBussigny = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infoBussigny);

			final InfoContribuablePP infoCtb = infoBussigny.getInfoPourContribuable(idMarie);

			assertInfo(idMarie, TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), date(2007, 10, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoCtb);

			assertEquals(1, infoBussigny.getInfosContribuables().size());
		}
	}

	@Test
	public void testSourcierPartiHCDansAnneeRoles() throws Exception {

		final long noIndividu = 463467849L;
		final int pfRoles = 2011;
		final RegDate dateArrivee = date(pfRoles - 2, 1, 1);
		final RegDate dateDepartHc = date(pfRoles, 3, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Drüstiene", "Helmut", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, dateDepartHc);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, dateDepartHc);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Neuchatel.RueDesBeauxArts, null, dateDepartHc.getOneDayAfter(), null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDepartHc, MotifFor.DEPART_HC, MockCommune.Cossonay, ModeImposition.SOURCE);
				addForPrincipal(pp, dateDepartHc.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Neuchatel, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(pfRoles, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infoCommune);

			final InfoContribuablePP infoCtb = infoCommune.getInfoPourContribuable(ppId);

			assertInfo(ppId, TypeContribuable.SOURCE, MockCommune.Cossonay.getNoOFS(), dateArrivee, dateDepartHc, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HC,
			           InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infoCtb);

			assertEquals(1, infoCommune.getInfosContribuables().size());
		}

	}

	/**
	 * Cas qui a une date et un motif de fermeture de for indiqué mais un assujettissement poursuivi
	 */
	@Test
	public void testDemenagementFinAnneeEtRetourConnuAnneeSuivante() throws Exception {

		final long noIndividu = 2376352L;
		final int pfRoles = 2011;
		final RegDate dateArrivee = date(pfRoles - 2, 1, 1);
		final RegDate dateDemenagement = date(pfRoles, 12, 31);
		final RegDate dateRetour = date(pfRoles + 1, 3, 25);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Drüstiene", "Helmut", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, dateDemenagement);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateDemenagement.getOneDayAfter(), dateRetour.getOneDayBefore());
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateRetour, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
				addForPrincipal(pp, dateDemenagement.getOneDayAfter(), MotifFor.DEMENAGEMENT_VD, dateRetour.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens);
				addForPrincipal(pp, dateRetour, MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(pfRoles, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Cossonay.getNoOFS());
		assertNotNull(infoCommune);

		final InfoContribuablePP infoCtb = infoCommune.getInfoPourContribuable(ppId);

		assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Cossonay.getNoOFS(), dateArrivee, dateDemenagement, MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD,
		           InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoCtb);

		assertEquals(1, infoCommune.getInfosContribuables().size());
	}

	// Cas JIRA [UNIREG-536]
	@Test
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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP infoGeneview = infoLausanne.getInfoPourContribuable(ids.genevieve);
			assertInfo(ids.genevieve, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(2003, 10, 1), null, MotifFor.DEMENAGEMENT_VD, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoGeneview);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}
	}

	/**
	 * Test pour UNIREG-2777
	 */
	@Test
	public void testRunCommunesCtbHorsCantonAvecPlusieursForsSecondaires() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Contribuable ctb = getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777();
				return ctb.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(3, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infoLausanne = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoLausanne);

			final InfoContribuablePP info = infoLausanne.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.Lausanne.getNoOFS(), date(2005, 1, 1), date(2008, 5, 15), MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);

			assertEquals(1, infoLausanne.getInfosContribuables().size());
		}

		{
			final InfoCommunePP infoCroy = results.getInfosCommunes().get(MockCommune.Croy.getNoOFS());
			assertNotNull(infoCroy);

			final InfoContribuablePP info = infoCroy.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.Croy.getNoOFS(), date(2004, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);

			assertEquals(1, infoCroy.getInfosContribuables().size());
		}

		{
			final InfoCommunePP infoRomainmotier = results.getInfosCommunes().get(MockCommune.RomainmotierEnvy.getNoOFS());
			assertNotNull(infoRomainmotier);

			final InfoContribuablePP info = infoRomainmotier.getInfoPourContribuable(ppId);
			assertInfo(ppId, TypeContribuable.HORS_CANTON, MockCommune.RomainmotierEnvy.getNoOFS(), date(2003, 1, 1), null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);

			assertEquals(1, infoRomainmotier.getInfosContribuables().size());
		}
	}

	@Test
	public void testGetTypeContribuable() throws Exception {

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// données bidon pour pouvoir instancier les assujettissements
				final PersonnePhysique toto = addNonHabitant("Toto", "LaRapière", date(1973, 3, 21), Sexe.MASCULIN);
				addForPrincipal(toto, date(2000, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

				assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesProcessor.getTypeContribuable(new VaudoisOrdinaire(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.ORDINAIRE, ProduireRolesProcessor.getTypeContribuable(new Indigent(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.DEPENSE, ProduireRolesProcessor.getTypeContribuable(new VaudoisDepense(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.MIXTE, ProduireRolesProcessor.getTypeContribuable(new SourcierMixteArt137Al1(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.MIXTE, ProduireRolesProcessor.getTypeContribuable(new SourcierMixteArt137Al2(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.HORS_CANTON, ProduireRolesProcessor.getTypeContribuable(new HorsCanton(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.HORS_SUISSE, ProduireRolesProcessor.getTypeContribuable(new HorsSuisse(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertEquals(TypeContribuable.SOURCE, ProduireRolesProcessor.getTypeContribuable(new SourcierPur(toto, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
				assertNull(ProduireRolesProcessor.getTypeContribuable(new DiplomateSuisse(toto, null, null, null, null, AssujettissementPersonnesPhysiquesCalculator.COMMUNE_ANALYZER)));
			}
		});
	}

	@Test
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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		// Lausanne
		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infos);
			assertEquals(1, infos.getInfosContribuables().size());

			final InfoContribuablePP infosCtb = infos.getInfoPourContribuable(ppId);
			assertNotNull(infosCtb);
			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, dateFin, MotifFor.ARRIVEE_HS, MotifFor.DEPART_HS, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
		}

		// Cossonay
		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infos);
			assertEquals(1, infos.getInfosContribuables().size());

			final InfoContribuablePP infosCtb = infos.getInfoPourContribuable(ppId);
			assertNotNull(infosCtb);
			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Cossonay.getNoOFS(), dateDebut, dateFin, MotifFor.DEBUT_EXPLOITATION, MotifFor.FIN_EXPLOITATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
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

		final long noCtb = doInNewTransaction(new TxCallback<Long>() {

			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysique toto = addNonHabitant("Toto", "Tartempion", date(1950, 9, 3), Sexe.MASCULIN);

				addForPrincipal(toto, arrivee, MotifFor.ARRIVEE_HS, passageMixte.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.DEPENSE);
				addForPrincipal(toto, passageMixte, MotifFor.CHGT_MODE_IMPOSITION, passageRole.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne, ModeImposition.MIXTE_137_2);
				addForPrincipal(toto, passageRole, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Lausanne);
				return toto.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2007, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
		assertNotNull(infos);
		assertEquals(1, infos.getInfosContribuables().size());

		final InfoContribuablePP infosCtb = infos.getInfoPourContribuable(idpp);
		assertNotNull(infosCtb);
		assertInfo(idpp, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), arriveeLausanne, departLausanne, MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, infosCtb);
	}

	/**
	 * SIFISC-1717
	 */
	@Test
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

		final ProduireRolesOIDsResults results = processor.runPourUnOfficeImpot(2007, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final Collection<InfoContribuablePP> infosRegroupees = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Prilly.getNoOFS(), MockCommune.Renens.getNoOFS()));
			assertNotNull(infosRegroupees);
			assertEquals(1, infosRegroupees.size());

			final InfoContribuablePP infoCtb = infosRegroupees.iterator().next();
			assertNotNull(infoCtb);
			assertInfo(idpp, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), arriveeRenens, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoCtb);
		}
	}

	@Test
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

		final ProduireRolesOIDsResults results = processor.runPourUnOfficeImpot(2007, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), 1, null);
		assertNotNull(results);
		assertEquals(6, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());

		{
			final Collection<InfoContribuablePP> infosRegroupees = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Lausanne.getNoOFS(), MockCommune.Renens.getNoOFS()));
			assertNotNull(infosRegroupees);
			assertEquals(6, infosRegroupees.size());
			final Map<Long, InfoContribuablePP> map = new HashMap<>(infosRegroupees.size());
			for (InfoContribuablePP info : infosRegroupees) {
				map.put(info.noCtb, info);
			}

			final InfoContribuablePP infoPaul = map.get(ids.paul);
			final InfoContribuablePP infoRaoul = map.get(ids.raoul);
			final InfoContribuablePP infoDidier = map.get(ids.didier);
			final InfoContribuablePP infoArnold = map.get(ids.arnold);
			final InfoContribuablePP infoVictor = map.get(ids.victor);
			final InfoContribuablePP infoBalthazar = map.get(ids.balthazar);

			assertInfo(ids.paul, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoPaul);
			assertInfo(ids.raoul, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), date(1983, 4, 13), null, MotifFor.MAJORITE, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoRaoul);
			assertInfo(ids.didier, TypeContribuable.NON_ASSUJETTI, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), date(2007, 5, 31), MotifFor.ARRIVEE_HS, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, infoDidier);
			assertInfo(ids.arnold, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoArnold);
			assertInfo(ids.victor, TypeContribuable.ORDINAIRE, MockCommune.Renens.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null,
			           InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoVictor);
			assertInfo(ids.balthazar, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), date(1990, 2, 1), null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, infoBalthazar);
		}
	}

	private static void assertInfo(long id, TypeContribuable type, int ofsCommune, RegDate ouvertureFor, @Nullable RegDate fermetureFor,
	                               MotifFor motifOuverture, @Nullable MotifFor motifFermeture, InfoContribuable.TypeAssujettissement typeAssujettissement,
	                               @Nullable TypeContribuable ancienTypeContribuable, InfoContribuable<?> info) {
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
			assertEquals(date, infosExtremite.getLeft());
			assertEquals(motif, infosExtremite.getRight());
		}
		else {
			assertNull(date);
			assertNull(motif);
		}
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années
	 */
	private PersonnePhysique newCtbVaudoisOrdinaire(MockCommune commune) {
		final PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.MAJORITE, commune);
		return paul;
	}

	/**
	 * @return un contribuable vaudois arrivé dans le canton en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireDepuis2007(MockCommune commune) {
		final PersonnePhysique incognito = addNonHabitant("Incog", "Nito", null, null);
		addForPrincipal(incognito, date(2007, 4, 13), MotifFor.ARRIVEE_HC, commune);
		return incognito;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2007(MockCommune commune) {
		final PersonnePhysique raoul = addNonHabitant("Raoul", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton au 31.12.2007
	 */
	private PersonnePhysique newCtbVaudoisPartiHorsCantonTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique raoul = addNonHabitant("Marie", "Coller", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(raoul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HC, commune);
		addForPrincipal(raoul, date(2008, 1, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		return raoul;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse au 31.12.2007
	 */
	private PersonnePhysique newCtbVaudoisPartiHorsSuisseTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique marie = addNonHabitant("Marie", "Coller", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(marie, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 12, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(marie, date(2008, 1, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		return marie;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2007 mais qui a gardé un immeuble
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique louis = addNonHabitant("Louis", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HC, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse en 2007 mais qui a gardé un immeuble
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique louis = addNonHabitant("Albert", "Coller", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(louis, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2007, 9, 30), MotifFor.DEPART_HS, communeResidence);
		addForPrincipal(louis, date(2007, 10, 1), MotifFor.DEPART_HS, MockPays.PaysInconnu);
		addForSecondaire(louis, date(2001, 3, 2), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return louis;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2006
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2006(MockCommune commune) {
		final PersonnePhysique didier = addNonHabitant("Didier", "Duvolet", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(didier, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2006, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(didier, date(2006, 10, 1), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		return didier;
	}

	/**
	 * @return un contribuable vaudois parti hors-canton en 2008
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn2008(MockCommune commune) {
		final PersonnePhysique laurent = addNonHabitant("Laurent", "Du", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(laurent, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2008, 9, 30), MotifFor.DEPART_HC, commune);
		addForPrincipal(laurent, date(2008, 10, 1), MotifFor.DEPART_HC, MockCommune.Neuchatel);
		return laurent;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune commune) {
		final PersonnePhysique benjamin = addNonHabitant("Benjamin", "TientPasEnPlace", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(benjamin, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, commune);
		addForPrincipal(benjamin, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne);
		addForPrincipal(benjamin, date(2007, 10, 1), MotifFor.ARRIVEE_HS, commune);
		return benjamin;
	}

	/**
	 * @return un contribuable vaudois parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsSuisseEtRevenuDansLaMemeAnnee(MockCommune communeAvant, MockCommune communeApres) {
		final PersonnePhysique benjamin = addNonHabitant("Benjamin", "TientPasEnPlace", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(benjamin, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, communeAvant);
		addForPrincipal(benjamin, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne);
		addForPrincipal(benjamin, date(2007, 10, 1), MotifFor.ARRIVEE_HS, communeApres);
		return benjamin;
	}

	/**
	 * @return un contribuable sourcier parti hors-Suisse et revenue en Suisse la même anneée en 2007
	 */
	private PersonnePhysique newCtbVaudoisSourcierPartiHorsSuisseEtRevenuDansLaMemeAnnee(long noIndividu, MockCommune communeAvant, MockCommune communeApres) {
		final PersonnePhysique m = addHabitant(noIndividu);
		addForPrincipal(m, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 3, 31), MotifFor.DEPART_HS, communeAvant, ModeImposition.SOURCE);
		addForPrincipal(m, date(2007, 4, 1), MotifFor.DEPART_HS, date(2007, 9, 30), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
		addForPrincipal(m, date(2007, 10, 1), MotifFor.ARRIVEE_HS, communeApres, ModeImposition.SOURCE);
		return m;
	}

	/**
	 * @return un contribuable vaudois ordinaire avec for principal et secondaire dans la même commune
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireAvecImmeubleDansCommune(MockCommune commune) {
		final PersonnePhysique genevieve = addNonHabitant("Geneviève", "Maillefer", date(1965, 4, 13), Sexe.FEMININ);
		addForPrincipal(genevieve, date(2003, 10, 1), MotifFor.DEMENAGEMENT_VD, commune);
		addImmeuble(genevieve, commune, date(2003, 11, 25), null);
		return genevieve;
	}

	/**
	 * @return un diplomate suisse basé à l'étranger mais rattaché à une commune vaudoise
	 */
	private PersonnePhysique newCtbDiplomateSuisse(MockCommune commune) {
		final PersonnePhysique marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(marc, date(1968, 11, 3), MotifFor.MAJORITE, commune, MotifRattachement.DIPLOMATE_SUISSE);
		return marc;
	}

	private PersonnePhysique newCtbOrdinaireVaudoisEtImmeuble(MockCommune communeResidence, MockCommune communeImmeuble) {
		final PersonnePhysique pascal = newCtbVaudoisOrdinaire(communeResidence);
		addImmeuble(pascal, communeImmeuble, date(2000, 1, 1), null);
		return pascal;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier-mixte
	 */
	private PersonnePhysique newCtbVaudoisSourcierMixte(MockCommune commune) {
		final PersonnePhysique arnold = addNonHabitant("Arnold", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(arnold, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.MIXTE_137_2);
		return arnold;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (non gris!)
	 */
	private PersonnePhysique newCtbVaudoisSourcier(MockCommune commune) {
		final PersonnePhysique victor = addNonHabitant("Victor", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		victor.setNumeroIndividu(263343L);
		addForPrincipal(victor, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.SOURCE);
		return victor;
	}

	/**
	 * @return un contribuable vaudois dans le canton depuis des années avec un mode d'imposition sourcier pur (gris)
	 */
	private PersonnePhysique newCtbVaudoisSourcierGris(MockCommune commune) {
		final PersonnePhysique albertine = addNonHabitant("Albertine", "Duplat", date(1969, 4, 13), Sexe.FEMININ);
		addForPrincipal(albertine, date(1983, 4, 13), MotifFor.ARRIVEE_HC, commune, ModeImposition.SOURCE);
		return albertine;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeuble(MockCommune commune) {
		final PersonnePhysique geo = addNonHabitant("Geo", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(geo, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(geo, commune, date(2003, 3, 1), null);
		return geo;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, dont un acheté et vendu en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtDeuxImmeubles(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final PersonnePhysique donald = addNonHabitant("Donald", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(donald, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(donald, communeImmeuble1, date(2007, 3, 1), date(2007, 6, 30));
		addImmeuble(donald, communeImmeuble2, date(1990, 1, 15), null);
		return donald;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec deux immeubles dans le canton, l'un vendu en 2007, l'autre acheté en 2007, sans chevauchement
	 */
	private PersonnePhysique newCtbHorsCantonDeuxImmeublesNonChevauchant(MockCommune communeImmeuble1, MockCommune communeImmeuble2) {
		final PersonnePhysique georges = addNonHabitant("Georges", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(georges, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(georges, communeImmeuble1, date(1980, 3, 1), date(2007, 6, 30));
		addImmeuble(georges, communeImmeuble2, date(2007, 11, 15), null);
		return georges;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton qui a été vendu en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeubleVenduEn2007(MockCommune commune) {
		final PersonnePhysique johnny = addNonHabitant("Johnny", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(johnny, date(2005, 11, 3), null, MockCommune.Bern);
		addImmeuble(johnny, commune, date(2005, 11, 3), date(2007, 8, 30));
		return johnny;
	}

	/**
	 * @return un contribuable avec un for principal hors Suisse, et avec un immeuble dans le canton qui a été vendu le 31.12.2007
	 */
	private PersonnePhysique newCtbHorsSuisseImmeubleVenduTrenteEtUnDecembre(MockCommune commune) {
		final PersonnePhysique tom = addNonHabitant("Tom", "Cruise", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tom, date(2005, 11, 3), null, MockPays.Albanie);
		addImmeuble(tom, commune, date(2005, 11, 3), date(2007, 12, 31));
		return tom;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec une activité indépendente dans le canton qui a été stoppé en 2007
	 */
	private PersonnePhysique newCtbHorsCantonEtActiviteIndStoppeeEn2007(MockCommune commune) {
		final PersonnePhysique tyler = addNonHabitant("Tyler", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tyler, date(2005, 11, 3), null, MockCommune.Bern);
		addForSecondaire(tyler, date(2005, 11, 3), MotifFor.DEBUT_EXPLOITATION, date(2007, 8, 30), MotifFor.FIN_EXPLOITATION, commune.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return tyler;
	}

	/**
	 * @return un contribuable avec un for fermé en 1983
	 */
	private PersonnePhysique newCtbVaudoisOrdinairePartiHorsCantonEn1983(MockCommune commune) {
		final PersonnePhysique pierre = addNonHabitant("Pierre", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(pierre, date(1968, 11, 3), MotifFor.ARRIVEE_HC, date(1983, 7, 1), MotifFor.DEPART_HC, commune);
		return pierre;
	}

	/**
	 * @return un contribuable avec un for annulé
	 */
	private PersonnePhysique newCtbVaudoisOrdinaireAnnule(MockCommune commune) {
		final PersonnePhysique jean = addNonHabitant("Jean", "Duchmol", date(1948, 11, 3), Sexe.MASCULIN);
		final ForFiscalPrincipal fors = addForPrincipal(jean, date(1968, 11, 3), MotifFor.ARRIVEE_HC, commune);
		fors.setAnnulationDate(DateHelper.getDate(1967, 1, 1));
		return jean;
	}

	/**
	 * @return un contribuable avec un for hors canton
	 */
	private PersonnePhysique newCtbHorsCantonSansForSecondaire() {
		final PersonnePhysique jeans = addNonHabitant("Jean", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jeans, date(1968, 11, 3), null, MockCommune.Neuchatel);
		return jeans;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un immeuble dans le canton mais vendu en 2005
	 */
	private PersonnePhysique newCtbHorsCantonEtImmeubleVenduEn2005(MockCommune commune) {
		final PersonnePhysique popol = addNonHabitant("Popol", "Dillon", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(popol, date(1968, 11, 3), null, MockCommune.Neuchatel);
		addImmeuble(popol, commune, date(2003, 3, 1), date(2005, 5, 31));
		return popol;
	}

	/**
	 * @return un contribuable avec un for principal hors canton, et avec un for immeuble annulé
	 */
	private PersonnePhysique newCtbHorsCantonEtForImmeubleAnnule(MockCommune commune) {
		final PersonnePhysique rama = addNonHabitant("Rama", "Truelle", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(rama, date(1968, 11, 3), null, MockCommune.Neuchatel);
		ForFiscalSecondaire fs = addImmeuble(rama, commune, date(2003, 3, 1), null);
		fs.setAnnule(true);
		return rama;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagement(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2007, 6, 1);
		final PersonnePhysique ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois qui garde un immeuble de part et d'autre du déménagement
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagementEnGardantImmeuble(MockCommune avant, MockCommune apres, MockCommune communeImmeuble) {
		final PersonnePhysique ctb = newCtbOrdinaireAvecDemenagement(avant, apres);
		addForSecondaire(ctb, date(2005, 6, 12), MotifFor.ACHAT_IMMOBILIER, communeImmeuble.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return ctb;
	}

	/**
	 * @return un contribuable avec un déménagement vaudois d'une commune à l'autre (avant 2007)
	 */
	private PersonnePhysique newCtbOrdinaireAvecDemenagementAnterieur(MockCommune avant, MockCommune apres) {
		final RegDate demenagement = date(2005, 6, 1);
		final PersonnePhysique ctb = addNonHabitant("Turlu", "Tutu", date(1947, 3, 25), Sexe.MASCULIN);
		addForPrincipal(ctb, date(1990, 2, 1), MotifFor.ARRIVEE_HS, demenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, avant);
		addForPrincipal(ctb, demenagement, MotifFor.DEMENAGEMENT_VD, apres);
		return ctb;
	}

	/**
	 * @return un contribuable invalide
	 */
	private Long newCtbVaudoisOrdinaireEtImmeubleInvalide() throws Exception {
		return doInNewTransactionAndSessionWithoutValidation(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique pp = addNonHabitant("Rodolf", "Piedbor", date(1953,12,18), Sexe.MASCULIN);
				addForPrincipal(pp, date(1971,12,18), MotifFor.MAJORITE, MockCommune.Lausanne);
				// le for secondaire n'est pas couvert par le for principal
				addForSecondaire(pp, date(1920,1,1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});
	}

	/**
	 * UNIREG-2777
	 * @return un contribuable HC avec deux fors secondaires immeubles ouverts sur l'OID d'Orbe et un autre fermé l'année des rôles sur l'OID de Lausanne
	 */
	private PersonnePhysique getCtbHorsCantonAvecDeuxForsImmeublesOuvertsPourJIRA2777() {
		final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
		addForPrincipal(pp, date(1988, 9, 12), MotifFor.MAJORITE, date(2007, 6, 11), MotifFor.DEPART_HC, MockCommune.Lausanne);
		addForPrincipal(pp, date(2007, 6, 12), MotifFor.DEPART_HC, MockCommune.Bern);
		addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 15), MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2003, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		addForSecondaire(pp, date(2004, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
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
	public void testSourcierGrisMarieDepartHS() throws Exception {

		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, date(1990, 4, 13), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				return mc.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.getNoOfsCommunesTraitees().size());

		final ProduireRolesResults.Ignore ignore = results.ctbsIgnores.get(0);
		assertNotNull(ignore);
		assertEquals(mcId, ignore.noCtb);
		assertEquals(ProduireRolesResults.IgnoreType.SOURCIER_GRIS, ignore.raison);
	}

	@Test
	public void testSourcierGrisCelibataireDepartHS() throws Exception {

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Fifi", "Brindacier", date(1970, 9, 12), Sexe.FEMININ);
				addForPrincipal(pp, date(2006, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 9, 10), MotifFor.DEPART_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.getNoOfsCommunesTraitees().size());

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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(1, results.ctbsIgnores.size());
		assertEquals(0, results.getNoOfsCommunesTraitees().size());

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

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.MIXTE, MockCommune.Bussigny.getNoOFS(), arrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	/**
	 * SIFISC-1717 : pas de motif/date de fermeture alors que l'assujettissement est indiqué comme "terminé dans PF"
	 * SIFISC-7798 : si l'immeuble reste dans la commune de départ, l'assujettissement doit être considéré comme "poursuivi"
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
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2008, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Cossonay.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Cossonay.getNoOFS(), achat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	// [SIFISC-4682] Vérifie que le calcul de l'assujettissement pour une commune se passe bien dans le cas d'un hors-canton avec immeuble qui arrive dans une commune vaudoise.
	@Test
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
				addForSecondaire(pp, achat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2011, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Echallens.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Echallens.getNoOFS(), achat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Moudon.getNoOFS());
			assertNotNull(infos);

			final InfoContribuable info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Moudon.getNoOFS(), arrivee, null, MotifFor.ARRIVEE_HC, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	/**
	 * Documente comment le passage d'une commune avant à après fusion agit sur l'édition des rôles de l'ancienne commune
	 */
	@Test
	public void testFusionDeCommunes() throws Exception {

		final long noIndividu = 324561L;
		final RegDate arrivee = date(2007, 7, 1);
		final MockCommune communeAvant = MockCommune.Villette;
		final MockCommune communeApres = MockCommune.BourgEnLavaux;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Villette.CheminDeCreuxBechet, null, arrivee, null);
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, arrivee, MotifFor.ARRIVEE_HS, communeAvant.getDateFinValidite(), MotifFor.FUSION_COMMUNES, communeAvant);
				addForPrincipal(pp, communeApres.getDateDebutValidite(), MotifFor.FUSION_COMMUNES, communeApres);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(communeAvant.getDateFinValidite().year(), 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Villette.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.ORDINAIRE, MockCommune.Villette.getNoOFS(), arrivee, communeAvant.getDateFinValidite(), MotifFor.ARRIVEE_HS, MotifFor.FUSION_COMMUNES, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
		}
	}

	/**
	 * SIFISC-8671
	 */
	@Test
	public void testSourcierPurVaudoisAvecForSecondaire() throws Exception {

		final long noIndividu = 32372567L;
		final RegDate dateArrivee = date(2000, 4, 1);
		final RegDate dateAchat = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addNationalite(ind, MockPays.France, date(1976, 3, 11), null);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2012, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.SOURCE, MockCommune.Bussigny.getNoOFS(), dateArrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Aigle.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			// c'est assez bizarre de trouver un NON_ASSUJETTI sur Aigle alors qu'il y a un immeuble, mais c'est dû au
			// problème de données -> le mode d'imposition du for de domicile ne devrait pas être à SOURCE (mais à MIXTE)
			// -> voir pour le véritable cas le test testSourcierPurVaudoisAvecForSecondaireEtChangementModeImposition() plus bas
			assertInfo(ppId, TypeContribuable.NON_ASSUJETTI, MockCommune.Aigle.getNoOFS(), dateAchat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.SOURCE, info);
		}
	}

	/**
	 * SIFISC-8671
	 */
	@Test
	public void testSourcierPurVaudoisAvecForSecondaireEtChangementModeImposition() throws Exception {

		final long noIndividu = 32372567L;
		final RegDate dateArrivee = date(2000, 4, 1);
		final RegDate dateAchat = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, date(1976, 3, 11), "Dantès", "Edmond", true);
				addNationalite(ind, MockPays.France, date(1976, 3, 11), null);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, dateAchat.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Bussigny, ModeImposition.SOURCE);
				addForPrincipal(pp, dateAchat, MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Bussigny, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(2012, 1, null);
		assertNotNull(results);
		assertEquals(1, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(2, results.getNoOfsCommunesTraitees().size());

		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Bussigny.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.MIXTE, MockCommune.Bussigny.getNoOFS(), dateArrivee, null, MotifFor.ARRIVEE_HS, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
		{
			final InfoCommunePP infos = results.getInfosCommunes().get(MockCommune.Aigle.getNoOFS());
			assertNotNull(infos);

			final InfoContribuablePP info = infos.getInfoPourContribuable(ppId);
			assertNotNull(info);

			assertInfo(ppId, TypeContribuable.MIXTE, MockCommune.Aigle.getNoOFS(), dateAchat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	/**
	 * [SIFISC-11991] Trouvé deux cas en production lors du tir des rôles 2013, les deux sont des PP dont l'assujettissement est "Source Pure", qui
	 * se marient avec une personne pour constituer un couple à l'ordinaire dans l'année des rôles
	 * <p/>
	 * Cas 1 : cas de la personne qui était HC (source), arrive sur VD en début d'année (= ordinaire) et se marie, toujours la même année
	 */
	@Test
	public void testArriveeHCSourceVersOrdinairePuisMariageAnneeRole() throws Exception {

		final long noIndividu = 125626L;
		final int anneeRoles = 2013;
		final RegDate dateArrivee = date(anneeRoles, 1, 15);
		final RegDate dateMariage = date(anneeRoles, 5, 3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Dupont", "Philippe", Sexe.MASCULIN);
				marieIndividu(individu, dateMariage);
			}
		});

		final class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.DEPART_HC, dateArrivee.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern, ModeImposition.SOURCE);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Leysin, ModeImposition.ORDINAIRE);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.mcId = mc.getNumero();
				return ids;
			}
		});

		// calcul des rôles
		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(anneeRoles, 1, null);
		assertNotNull(results);
		assertEquals(2, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(1, results.getNoOfsCommunesTraitees().size());

		// quelle information obtenue sur la commune de Leysin ? (= le couple seulement)
		// (en effet, la personne physique, bien que résidente célibataire un bon moment sur Leysin, n'y a pas d'assujettissement, et comme elle n'en avait pas
		// non plus l'année d'avant, elle est tout simplement omise...)
		{
			final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Leysin.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePP info = infoCommune.getInfoPourContribuable(ids.mcId);
			assertNotNull(info);

			assertInfo(ids.mcId, TypeContribuable.ORDINAIRE, MockCommune.Leysin.getNoOFS(), dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	/**
	 * [SIFISC-11991] Trouvé deux cas en production lors du tir des rôles 2013, les deux sont des PP dont l'assujettissement est "Source Pure", qui
	 * se marient avec une personne pour constituer un couple à l'ordinaire dans l'année des rôles
	 * <p/>
	 * Cas 2 : mixte 1 avec immeuble (acheté en début d'année) qui se marie et forme un couple à l'ordinaire
	 */
	@Test
	public void testMixte1AchatImmeublePuisMariageAnneeRole() throws Exception {

		final long noIndividu = 125626L;
		final int anneeRoles = 2013;
		final RegDate dateChangementModeImposition = date(anneeRoles, 1, 1);
		final RegDate dateAchat = date(anneeRoles, 2, 15);
		final RegDate dateMariage = date(anneeRoles, 5, 3);
		final RegDate dateDemenagement = date(anneeRoles, 10, 6);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Dupont", "Philippe", Sexe.MASCULIN);
				marieIndividu(individu, dateMariage);
			}
		});

		final class Ids {
			long ppId;
			long mcId;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.INDETERMINE, dateChangementModeImposition.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Fraction.LeSentier, ModeImposition.SOURCE);
				addForPrincipal(pp, dateChangementModeImposition, MotifFor.CHGT_MODE_IMPOSITION, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LeSentier, ModeImposition.MIXTE_137_1);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Fraction.LeSentier, ModeImposition.ORDINAIRE);
				addForPrincipal(mc, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Fraction.LePont, ModeImposition.ORDINAIRE);
				addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Fraction.LAbbaye.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.ppId = pp.getNumero();
				ids.mcId = mc.getNumero();
				return ids;
			}
		});

		// calcul des rôles
		final ProduireRolesPPCommunesResults results = processor.runPPPourToutesCommunes(anneeRoles, 1, null);
		assertNotNull(results);
		assertEquals(2, results.ctbsTraites);
		assertEquals(0, results.ctbsEnErrors.size());
		assertEquals(0, results.ctbsIgnores.size());
		assertEquals(3, results.getNoOfsCommunesTraitees().size());      // le sentier, l'abbaye et le pont

		// informations obtenues sur les communes ?
		{
			// le sentier
			final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Fraction.LeSentier.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePP info = infoCommune.getInfoPourContribuable(ids.ppId);
			assertNotNull(info);

			assertInfo(ids.ppId, TypeContribuable.SOURCE, MockCommune.Fraction.LeSentier.getNoOFS(), date(2000, 1, 1), dateMariage.getOneDayBefore(), MotifFor.INDETERMINE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
		}
		{
			// l'abbaye
			final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Fraction.LAbbaye.getNoOFS());
			assertNotNull(infoCommune);
    		assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePP info = infoCommune.getInfoPourContribuable(ids.mcId);
			assertNotNull(info);

			assertInfo(ids.mcId, TypeContribuable.ORDINAIRE, MockCommune.Fraction.LAbbaye.getNoOFS(), dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
		{
			// le pont
			final InfoCommunePP infoCommune = results.getInfosCommunes().get(MockCommune.Fraction.LePont.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePP info = infoCommune.getInfoPourContribuable(ids.mcId);
			assertNotNull(info);

			assertInfo(ids.mcId, TypeContribuable.ORDINAIRE, MockCommune.Fraction.LePont.getNoOFS(), dateDemenagement, null, MotifFor.DEMENAGEMENT_VD, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
		}
	}

	/**
	 * [SIFISC-13803] NPE dans le calcul des rôles de l'OID si on avait, dans le même OID, un for fermé suivi par un for ouvert dans la liste
	 * présentée par {@link ch.vd.uniregctb.tiers.Tiers#getForsFiscauxValidAt(RegDate)}
	 */
	@Test
	public void testContribuableHorsSuisseAvecPlusieursImmeublesDansMemeOID() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		final RegDate dateAchat = date(2011, 6, 4);
		final RegDate dateVente1 = date(2013, 12, 11);
		final RegDate dateVente2 = date(2014, 3, 18);

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alberto", "Tamburini", null, Sexe.MASCULIN);
				addForPrincipal(pp, dateAchat, null, MockPays.Allemagne);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente1, MotifFor.VENTE_IMMOBILIER, MockCommune.Renens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente2, MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.VufflensLaVille.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// rôles 2014 de l'OID de Lausanne
		final ProduireRolesOIDsResults res = processor.runPourUnOfficeImpot(2013, MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);

		final Collection<InfoContribuablePP> infos = res.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Renens.getNoOFS(), MockCommune.Prilly.getNoOFS(), MockCommune.VufflensLaVille.getNoOFS()));
		assertNotNull(infos);
		assertEquals(1, infos.size());
		assertEquals(ppId, infos.iterator().next().noCtb);
	}

	@Test
	public void testRolesPMSimple() throws Exception {

		// entreprise HC qui possède un immeuble dans une commune vaudoise
		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
				addForPrincipal(pm, dateAchat, null, MockCommune.Bern);
				addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Grandson.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // juste Grandson

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Grandson.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2015, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.HORS_CANTON, MockCommune.Grandson.getNoOFS(), dateAchat, null, MotifFor.ACHAT_IMMOBILIER, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Bern.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, info.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesPMVenteAvantFinExercice() throws Exception {

		// entreprise HC qui possède un immeuble dans une commune vaudoise
		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
				addForPrincipal(pm, dateAchat, null, MockCommune.Bern);
				addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // juste Grandson

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Grandson.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2015, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.HORS_CANTON, MockCommune.Grandson.getNoOFS(), dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Bern.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, info.getTafForPrincipal());
		}
	}

	/**
	 * Au début, on pensait que pour les entreprises vaudoises, les fors secondaires ne devait pas intervenir...
	 * Et puis on est revenu sur cette décision (mail de PTF, 07.09.2016), donc maintenant, c
	 */
	@Test
	public void testRolesPMEntrepriseVaudoiseAvecForSecondaires() throws Exception {

		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);
		final String ide = "CHE213456789";

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addIdentificationEntreprise(pm, ide);
				addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(2, res.getNoOfsCommunesTraitees().size());      // Lausanne et Grandson

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2015, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertEquals(ide, info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Grandson.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2015, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Grandson.getNoOFS(), dateAchat, dateVente, MotifFor.ACHAT_IMMOBILIER, MotifFor.VENTE_IMMOBILIER, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertEquals(ide, info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	/**
	 * Au début, on pensait que pour les entreprises vaudoises, les fors secondaires ne devait pas intervenir...
	 * Et puis on est revenu sur cette décision (mail de PTF, 07.09.2016), donc maintenant, c
	 */
	@Test
	public void testRolesOIPMEntrepriseVaudoiseAvecForSecondaires() throws Exception {

		final RegDate dateDebut = date(2010, 5, 6);
		final RegDate dateAchat = date(2012, 8, 14);
		final RegDate dateVente = date(2015, 2, 4);
		final String ide = "CHE213456789";

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addIdentificationEntreprise(pm, ide);
				addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				addForSecondaire(pm, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Grandson.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesOIPMResults res = processor.runPourOfficePersonnesMorales(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(2, res.getNoOfsCommunesTraitees().size());      // Lausanne et Grandson

		final Collection<InfoContribuablePM> data = res.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Lausanne.getNoOFS(), MockCommune.Grandson.getNoOFS()));
		assertNotNull(data);
		assertEquals(1, data.size());

		{
			final InfoContribuablePM info = data.iterator().next();
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertEquals(ide, info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	@Test
	public void testFailliteAnneePrecedentBouclement() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);
		final RegDate dateFaillite = date(2014, 12, 6);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(6, 30), 12);
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Lausanne);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // juste Lausanne...

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2015, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, dateFaillite, MotifFor.DEBUT_EXPLOITATION, MotifFor.FAILLITE, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
			assertEquals(date(2015, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesPMPremiereAnneeSansBouclement() throws Exception {

		final RegDate dateDebut = date(2015, 5, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut.addYears(1), DayMonth.get(6, 30), 12);      // bouclements depuis 2016 seulement
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois -> rien
		final ProduireRolesPMCommunesResults res2015 = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res2015);
		assertEquals(0, res2015.ctbsEnErrors.size());
		assertEquals(0, res2015.ctbsIgnores.size());
		assertEquals(0, res2015.ctbsTraites);
		assertEquals(0, res2015.getNoOfsCommunesTraitees().size());

		// rôles 2016 vaudois -> là l'entreprise apparaît
		final ProduireRolesPMCommunesResults res2016 = processor.runPMPourToutesCommunes(2016, 1, null);
		assertNotNull(res2016);
		assertEquals(0, res2016.ctbsEnErrors.size());
		assertEquals(0, res2016.ctbsIgnores.size());
		assertEquals(1, res2016.ctbsTraites);
		assertEquals(1, res2016.getNoOfsCommunesTraitees().size());      // juste Lausanne...

		{
			final InfoCommunePM infoCommune = res2016.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info = infoCommune.getInfoPourContribuable(pmId, date(2016, 6, 30));
			assertNotNull(info);

			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2016, 6, 30), info.getDateBouclement());
			assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesPMDeuxBouclementsDansAnnee() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);              // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
				addBouclement(pm, date(2015, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // juste Lausanne...

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(2, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info03 = infoCommune.getInfoPourContribuable(pmId, date(2015, 3, 31));
			assertNotNull(info03);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info03);
			assertEquals(date(2015, 3, 31), info03.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info03.getFormeJuridique());
			assertNull(info03.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info03.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info03.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info03.getTafForPrincipal());

			final InfoContribuablePM info12 = infoCommune.getInfoPourContribuable(pmId, date(2015, 12, 31));
			assertNotNull(info12);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info12);
			assertEquals(date(2015, 12, 31), info12.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info12.getFormeJuridique());
			assertNull(info12.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info12.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info12.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info12.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesOIPMDeuxBouclementsDansAnnee() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);              // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
				addBouclement(pm, date(2015, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesOIPMResults res = processor.runPourOfficePersonnesMorales(2015, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // juste Lausanne...

		final Collection<InfoContribuablePM> data = res.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Lausanne.getNoOFS(), MockCommune.Morges.getNoOFS()));
		assertNotNull(data);
		assertEquals(2, data.size());       // les deux bouclements

		final List<InfoContribuablePM> sortedData = new ArrayList<>(data);
		Collections.sort(sortedData, new Comparator<InfoContribuablePM>() {
			@Override
			public int compare(InfoContribuablePM o1, InfoContribuablePM o2) {
				return NullDateBehavior.LATEST.compare(o1.getDateBouclement(), o2.getDateBouclement());
			}
		});

		{
			final InfoContribuablePM info = sortedData.get(0);
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2015, 3, 31), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
		{
			final InfoContribuablePM info = sortedData.get(1);
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(2015, 12, 31), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesPMDeuxBouclementsDansAnneeAvecDemenagementEntreDeux() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);                    // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
				addBouclement(pm, date(anneeRoles, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pm, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(anneeRoles, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(2, res.getNoOfsCommunesTraitees().size());      // Lausanne et Morges

		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(2, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info03 = infoCommune.getInfoPourContribuable(pmId, date(anneeRoles, 3, 31));
			assertNotNull(info03);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, dateDemenagement.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info03);
			assertEquals(date(anneeRoles, 3, 31), info03.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info03.getFormeJuridique());
			assertNull(info03.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info03.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info03.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info03.getTafForPrincipal());

			final InfoContribuablePM info12 = infoCommune.getInfoPourContribuable(pmId, date(anneeRoles, 12, 31));
			assertNotNull(info12);
			assertInfo(pmId, TypeContribuable.NON_ASSUJETTI, MockCommune.Lausanne.getNoOFS(), dateDebut, dateDemenagement.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.DEMENAGEMENT_VD, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, TypeContribuable.ORDINAIRE, info12);
			assertEquals(date(anneeRoles, 12, 31), info12.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info12.getFormeJuridique());
			assertNull(info12.getNoIde());
			assertEquals((Integer) MockCommune.Morges.getNoOFS(), info12.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info12.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info12.getTafForPrincipal());
		}
		{
			final InfoCommunePM infoCommune = res.getInfosCommunes().get(MockCommune.Morges.getNoOFS());
			assertNotNull(infoCommune);
			assertEquals(1, infoCommune.getInfosContribuables().size());

			final InfoContribuablePM info12 = infoCommune.getInfoPourContribuable(pmId, date(anneeRoles, 12, 31));
			assertNotNull(info12);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Morges.getNoOFS(), dateDemenagement, null, MotifFor.DEMENAGEMENT_VD, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info12);
			assertEquals(date(anneeRoles, 12, 31), info12.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info12.getFormeJuridique());
			assertNull(info12.getNoIde());
			assertEquals((Integer) MockCommune.Morges.getNoOFS(), info12.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info12.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info12.getTafForPrincipal());
		}
	}

	@Test
	public void testRolesOIPMDeuxBouclementsDansAnneeAvecDemenagementEntreDeux() throws Exception {

		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(3, 31), 12);                    // bouclements tous les 31.03 depuis 2011 jusqu'à 2015
				addBouclement(pm, date(anneeRoles, 12, 1), DayMonth.get(12, 31), 12);     // dès 2015, bouclements au 31.12 -> 2 bouclements en 2015
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pm, dateDemenagement, MotifFor.DEMENAGEMENT_VD, MockCommune.Morges);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesOIPMResults res = processor.runPourOfficePersonnesMorales(anneeRoles, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(2, res.getNoOfsCommunesTraitees().size());      // Lausanne et Morges

		final Collection<InfoContribuablePM> data = res.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Lausanne.getNoOFS(), MockCommune.Morges.getNoOFS()));
		assertNotNull(data);
		assertEquals(2, data.size());       // les deux bouclements

		final List<InfoContribuablePM> sortedData = new ArrayList<>(data);
		Collections.sort(sortedData, new Comparator<InfoContribuablePM>() {
			@Override
			public int compare(InfoContribuablePM o1, InfoContribuablePM o2) {
				return NullDateBehavior.LATEST.compare(o1.getDateBouclement(), o2.getDateBouclement());
			}
		});

		// avec le rôles pour l'OIPM, on se place au niveau cantonal global ; c'est ainsi que le rôle présenté pour
		// la commune de Lausanne se poursuit dans la PF suivante (= à Morges, toujours dans le canton), et que le for
		// présenté pour la commune de Morges débute déjà à la fondation de l'entreprise (= arrivée dans le canton...)

		{
			final InfoContribuablePM info = sortedData.get(0);
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(anneeRoles, 3, 31), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
		{
			final InfoContribuablePM info = sortedData.get(1);
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Morges.getNoOFS(), dateDebut, null, MotifFor.DEBUT_EXPLOITATION, null, InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, null, info);
			assertEquals(date(anneeRoles, 12, 31), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Morges.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.getTafForPrincipal());
		}
	}

	@Test
	public void testDepartHCPM() throws Exception {
		final RegDate dateDebut = date(2010, 5, 10);
		final int anneeRoles = 2015;
		final RegDate dateDemenagement = date(anneeRoles, 7, 18);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, dateDebut, null, "Raison d'un jour dure toujours");
				addFormeJuridique(pm, dateDebut, null, FormeJuridiqueEntreprise.SARL);
				addRegimeFiscalVD(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addBouclement(pm, dateDebut, DayMonth.get(12, 31), 12);                    // bouclements tous les 31.12 depuis 2010
				addForPrincipal(pm, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDemenagement.getOneDayBefore(), MotifFor.DEPART_HC, MockCommune.Lausanne);
				addForPrincipal(pm, dateDemenagement, MotifFor.DEPART_HC, MockCommune.Neuchatel);
				return pm.getNumero();
			}
		});

		// rôles 2015 vaudois
		final ProduireRolesPMCommunesResults res = processor.runPMPourToutesCommunes(anneeRoles, 1, null);
		assertNotNull(res);
		assertEquals(0, res.ctbsEnErrors.size());
		assertEquals(0, res.ctbsIgnores.size());
		assertEquals(1, res.ctbsTraites);
		assertEquals(1, res.getNoOfsCommunesTraitees().size());      // Lausanne

		final InfoCommunePM infoLausanne = res.getInfosCommunes().get(MockCommune.Lausanne.getNoOFS());
		assertNotNull(infoLausanne);

		final Collection<InfoContribuablePM> infos = infoLausanne.getInfosContribuables();
		assertNotNull(infos);
		assertEquals(1, infos.size());

		{
			final InfoContribuablePM info = infos.iterator().next();
			assertNotNull(info);
			assertInfo(pmId, TypeContribuable.ORDINAIRE, MockCommune.Lausanne.getNoOFS(), dateDebut, dateDemenagement.getOneDayBefore(), MotifFor.DEBUT_EXPLOITATION, MotifFor.DEPART_HC, InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, null, info);
			assertEquals(date(anneeRoles, 12, 31), info.getDateBouclement());
			assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, info.getFormeJuridique());
			assertNull(info.getNoIde());
			assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), info.getNoOfsForPrincipal());
			assertEquals("Raison d'un jour dure toujours", info.getRaisonSociale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, info.getTafForPrincipal());
		}
	}
}