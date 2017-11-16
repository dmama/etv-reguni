package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalCommunaute;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ModeleCommunauteRFDAO;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase.assertRaisonAcquisition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class RegistreFoncierServiceTest extends BusinessTest {

	private AyantDroitRFDAO ayantDroitRFDAO;
	private RegistreFoncierServiceImpl serviceRF;
	private ModeleCommunauteRFDAO modeleCommunauteRFDAO;
	private EvenementFiscalDAO evenementFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		serviceRF = (RegistreFoncierServiceImpl) getBean(RegistreFoncierService.class, "serviceRF");
		modeleCommunauteRFDAO = getBean(ModeleCommunauteRFDAO.class, "modeleCommunauteRFDAO");
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService() {
			@Override
			public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
				Assert.assertNull(parametres);
				return "https://secure.vd.ch/territoire/intercapi/faces?bfs={noCommune}&kr=0&n1={noParcelle}&n2={index1}&n3={index2}&n4={index3}&type=grundstueck_grundbuch_auszug";
			}
		});
	}

	/**
	 * Ce test vérifie que la méthode getDroitsForCtb fonctionne dans le cas passant.
	 */
	@Test
	public void testGetDroitsForCtb() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransaction(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "some egrid", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);

			addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null,
			                           "48390a0e044", "48390a0e043", new IdentifiantAffaireRF(123, 2004, 202, 3),
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiersRF, immeuble0, null);

			addDroitPersonnePhysiqueRF(RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), RegDate.get(2010, 2, 20), "Achat", "Achat",
			                           "47e7d7e773", "47e7d7e772", new IdentifiantAffaireRF(23, 1997, 13, 0),
			                           new Fraction(1, 3), GenrePropriete.COPROPRIETE, tiersRF, immeuble1, null);

			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
			addRapprochementRF(ctb, tiersRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				assertNotNull(ctb);

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, false, false);
				assertEquals(2, droits.size());

				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				assertNull(droit0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());
				assertEquals(new Fraction(1, 3), droit0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), droit0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), droit0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 23), droit0.getDateFin());
				assertEquals(RegDate.get(2010, 2, 20), droit0.getDateFinMetier());
				assertEquals("Achat", droit0.getMotifDebut());
				assertEquals("Achat", droit0.getMotifFin());
				assertEquals("47e7d7e773", droit0.getMasterIdRF());
				assertEquals("02faeee", droit0.getImmeuble().getIdRF());

				final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
				assertEquals(1, raisons0.size());
				assertRaisonAcquisition(RegDate.get(1997, 7, 2), "Achat", new IdentifiantAffaireRF(23, 1997, 13, 0), raisons0.iterator().next());

				final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertNull(droit1.getDateFinMetier());
				assertEquals("Achat", droit1.getMotifDebut());
				assertNull(droit1.getMotifFin());
				assertEquals("48390a0e044", droit1.getMasterIdRF());
				assertEquals("01faeee", droit1.getImmeuble().getIdRF());

				final Set<RaisonAcquisitionRF> raisons1 = droit1.getRaisonsAcquisition();
				assertEquals(1, raisons1.size());
				assertRaisonAcquisition(RegDate.get(2004, 4, 12), "Achat", new IdentifiantAffaireRF(123, 2004, 202, 3), raisons1.iterator().next());
			}
		});
	}

	/**
	 * Ce test vérifie que la méthode getDroitsForCtb fonctionne dans le cas passant.
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForCtb() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);

		class Ids {
			long droit0;
			long droit1;
			long droit2;
		}
		final Ids ids = new Ids();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransaction(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles + un lien de propriété entre les deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "some egrid", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);

			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null,
			                                                                           "48390a0e044", "48390a0e043", new IdentifiantAffaireRF(123, 2004, 202, 3),
			                                                                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiersRF, immeuble0, null);

			final DroitProprietePersonnePhysiqueRF droit1 = addDroitPersonnePhysiqueRF(RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), RegDate.get(2010, 2, 20), "Achat", "Achat",
			                                                                           "47e7d7e773", "47e7d7e772", new IdentifiantAffaireRF(23, 1997, 13, 0),
			                                                                           new Fraction(1, 3), GenrePropriete.COPROPRIETE, tiersRF, immeuble1, null);

			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(12, 345),
			                                                          null, RegDate.get(2000, 1, 1), null, "Constitution PPE", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 121, 2), "39393939", "1");
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();

			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
			addRapprochementRF(ctb, tiersRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				assertNotNull(ctb);

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, true, false);
				droits.sort(new DroitRFRangeMetierComparator());
				assertEquals(3, droits.size());

				// le premier droit réel
				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				assertNull(droit0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());
				assertEquals(new Fraction(1, 3), droit0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), droit0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), droit0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 23), droit0.getDateFin());
				assertEquals(RegDate.get(2010, 2, 20), droit0.getDateFinMetier());
				assertEquals("Achat", droit0.getMotifDebut());
				assertEquals("Achat", droit0.getMotifFin());
				assertEquals("47e7d7e773", droit0.getMasterIdRF());
				assertEquals("02faeee", droit0.getImmeuble().getIdRF());

				final Set<RaisonAcquisitionRF> raisons0 = droit0.getRaisonsAcquisition();
				assertEquals(1, raisons0.size());
				assertRaisonAcquisition(RegDate.get(1997, 7, 2), "Achat", new IdentifiantAffaireRF(23, 1997, 13, 0), raisons0.iterator().next());

				// le deuxième droit réel
				final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertNull(droit1.getDateFinMetier());
				assertEquals("Achat", droit1.getMotifDebut());
				assertNull(droit1.getMotifFin());
				assertEquals("48390a0e044", droit1.getMasterIdRF());
				assertEquals("01faeee", droit1.getImmeuble().getIdRF());

				final Set<RaisonAcquisitionRF> raisons1 = droit1.getRaisonsAcquisition();
				assertEquals(1, raisons1.size());
				assertRaisonAcquisition(RegDate.get(2004, 4, 12), "Achat", new IdentifiantAffaireRF(123, 2004, 202, 3), raisons1.iterator().next());

				// le droit virtuel
				final DroitProprieteVirtuelRF droit2 = (DroitProprieteVirtuelRF) droits.get(2);
				assertNull(droit2.getCommunaute());
				assertNull(droit2.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit2.getDateDebutMetier());
				assertNull(droit2.getDateFin());
				assertNull(droit2.getDateFinMetier());
				assertEquals("Achat", droit2.getMotifDebut());
				assertNull(droit2.getMotifFin());
				assertNull(droit2.getMasterIdRF());
				assertEquals("02faeee", droit2.getImmeuble().getIdRF());

				final List<DroitRF> chemin = droit2.getChemin();
				assertEquals(2, chemin.size());
				assertDroitPropChemin(ids.droit0, 1, 1, chemin.get(0));
				assertDroitPropChemin(ids.droit2, 12, 345, chemin.get(1));
			}
		});
	}

	/**
	 * Ce test vérifie que la méthode getDroitsForCtb fonctionne dans le cas où un contribuable a été rapproché successivement à deux tiers RF.
	 */
	@Test
	public void testGetDroitsForCtbMultipleRapprochements() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateNaissance = date(1970, 7, 2);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1970, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long pp = doInNewTransaction(status -> {
			final PersonnePhysique ctb = tiersService.createNonHabitantFromIndividu(noIndividu);
			return ctb.getNumero();
		});

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "some egrid", gland, 4298);
			final BienFondsRF immeuble2 = addBienFondsRF("03faeee", "some egrid", laSarraz, 580);
			final BienFondsRF immeuble3 = addBienFondsRF("04faeee", "some egrid", gland, 4299);
			final BienFondsRF immeuble4 = addBienFondsRF("05faeee", "some egrid", laSarraz, 581);
			final BienFondsRF immeuble5 = addBienFondsRF("06faeee", "some egrid", gland, 4300);

			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);


			final PersonnePhysiqueRF tiersRF1 = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));
			addRapprochementRF(ctb, tiersRF1, RegDate.get(1997, 10, 7), RegDate.get(2005, 12, 31), TypeRapprochementRF.MANUEL);

			addDroitPropriete(tiersRF1, immeuble0, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "48390a0e043");

			addDroitPropriete(tiersRF1, immeuble1, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2008, 3, 1), null, RegDate.get(2008, 3, 1), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2008, 2, 23), "4782389c8e", "4782389c8d");

			addDroitPropriete(tiersRF1, immeuble2, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(1997, 10, 7), RegDate.get(2002, 2, 23), RegDate.get(1997, 7, 2), null, "Achat", "Vente",
			                  new IdentifiantAffaireRF(23, 1997, 13, 0), "47e7d7e773", "47e7d7e772");

			final PersonnePhysiqueRF tiersRF2 = addPersonnePhysiqueRF("38383830ae3ff", "Charles", "Widmer", date(1970, 7, 2));
			addRapprochementRF(ctb, tiersRF2, RegDate.get(2006, 1, 1), null, TypeRapprochementRF.MANUEL);

			addDroitPropriete(tiersRF2, immeuble3, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(1960, 2, 7), RegDate.get(1994, 4, 5), RegDate.get(1960, 2, 7), null, "Achat", "Expropriation",
			                  new IdentifiantAffaireRF(123, 1960, 4, 3), "48392345234234", "48392345234233");

			addDroitPropriete(tiersRF2, immeuble4, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 5, 1), null, RegDate.get(2003, 5, 1), null, "Achat", "Vente",
			                  new IdentifiantAffaireRF(123, 2003, 22, 7), "834838c", "834838b");

			addDroitPropriete(tiersRF2, immeuble5, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(2010, 6, 6), null, RegDate.get(2010, 6, 6), null, "Achat", null,
			                  new IdentifiantAffaireRF(23, 2010, 33, 30), "c83839e", "c83839d");

			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(pp);
				assertNotNull(ctb);

				// Tiers RF 1 :
				// -------------
				// Rapprochement         1997-10-07 |=============================| 2005-12-31
				// Droit A                                         2004-05-21 |------------------------------->
				// Droit B                                                      2008-03-01 |------------------>
				// Droit C               1997-10-07 |----------| 2002-02-23
				//
				// Tiers RF 2 :
				// -------------
				// Rapprochement                                       2006-01-01 |===========================>
				// Droit D          1960-02-07 |-----| 1994-04-05
				// Droit E                                    2003-05-01 |------------------------------------>
				// Droit F                                                                  2010-06-06 |------>
				//
				// Droits du contribuable
				// ---------------------
				// Droit A                                         2004-05-21 |------------------------------->
				// Droit C               1997-10-07 |----------| 2002-02-23
				// Droit E                                    2003-05-01 |------------------------------------>
				// Droit F                                                                  2010-06-06 |------>
				//

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, false, false);
				assertEquals(4, droits.size());

				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				assertNull(droit0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());
				assertEquals(new Fraction(1, 3), droit0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), droit0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), droit0.getDateDebutMetier());
				assertEquals(RegDate.get(2002, 2, 23), droit0.getDateFin());
				assertEquals("Achat", droit0.getMotifDebut());
				assertEquals("Vente", droit0.getMotifFin());
				assertEquals("47e7d7e773", droit0.getMasterIdRF());
				assertEquals("03faeee", droit0.getImmeuble().getIdRF());

				final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2003, 5, 1), droit1.getDateDebut());
				assertEquals(RegDate.get(2003, 5, 1), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertEquals("Achat", droit1.getMotifDebut());
				assertEquals("Vente", droit1.getMotifFin());
				assertEquals("834838c", droit1.getMasterIdRF());
				assertEquals("05faeee", droit1.getImmeuble().getIdRF());

				final DroitProprietePersonnePhysiqueRF droit2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
				assertNull(droit2.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit2.getRegime());
				assertEquals(new Fraction(1, 1), droit2.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit2.getDateDebut());
				assertEquals(RegDate.get(2004, 5, 21), droit2.getDateDebutMetier());
				assertNull(droit2.getDateFin());
				assertEquals("Achat", droit2.getMotifDebut());
				assertNull(droit2.getMotifFin());
				assertEquals("48390a0e044", droit2.getMasterIdRF());
				assertEquals("01faeee", droit2.getImmeuble().getIdRF());

				final DroitProprietePersonnePhysiqueRF droit3 = (DroitProprietePersonnePhysiqueRF) droits.get(3);
				assertNull(droit3.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, droit3.getRegime());
				assertEquals(new Fraction(1, 3), droit3.getPart());
				assertEquals(RegDate.get(2010, 6, 6), droit3.getDateDebut());
				assertEquals(RegDate.get(2010, 6, 6), droit3.getDateDebutMetier());
				assertNull(droit3.getDateFin());
				assertEquals("Achat", droit3.getMotifDebut());
				assertNull(droit3.getMotifFin());
				assertEquals("c83839e", droit3.getMasterIdRF());
				assertEquals("06faeee", droit3.getImmeuble().getIdRF());
			}
		});
	}

	/**
	 * Vérifie qu'aucun droit de propriété virtuel n'est trouvé s'il n'y a pas de droits entre immeubles.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                        individuelle (1/1)   +------------+
	 *     +----------+    +---------------------->| Immeuble 0 |
	 *     |          |----+                       +------------+
	 *     | Tiers RF |
	 *     |          |----+  copropriété (1/3)    +------------+
	 *     +----------+    +---------------------->| Immeuble 1 |
	 *                                             +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels (pas de différence)</b>
	 * <pre>
	 *                        individuelle (1/1)   +------------+
	 *     +----------+    +---------------------->| Immeuble 0 |
	 *     |          |----+                       +------------+
	 *     | Tiers RF |
	 *     |          |----+  copropriété (1/3)    +------------+
	 *     +----------+    +---------------------->| Immeuble 1 |
	 *                                             +------------+
	 * </pre>
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForTiersRFCasSansDroitEntreImmeuble() throws Exception {

		// mise en place foncière
		final Long tiersId = doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			addDroitPropriete(tiersRF, immeuble0, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			addDroitPropriete(tiersRF, immeuble1, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(2008, 3, 1), null, RegDate.get(2008, 3, 1), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2008, 2, 23), "4782389c8e", "1");
			return tiersRF.getId();
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(tiersId);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels. Comme il n'y a pas de droits entre immeubles, on ne devrait recevoir que les deux droits réels.
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			assertNotNull(droits);
			droits.sort(new DroitRFRangeMetierComparator());
			assertEquals(2, droits.size());

			final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
			assertNull(droit0.getCommunaute());
			assertEquals(GenrePropriete.INDIVIDUELLE, droit0.getRegime());
			assertEquals(new Fraction(1, 1), droit0.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droit0.getDateDebutMetier());
			assertEquals("Achat", droit0.getMotifDebut());
			assertNull(droit0.getMotifFin());
			assertEquals("48390a0e044", droit0.getMasterIdRF());
			assertEquals("01faeee", droit0.getImmeuble().getIdRF());

			final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
			assertNull(droit1.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droit1.getRegime());
			assertEquals(new Fraction(1, 3), droit1.getPart());
			assertEquals(RegDate.get(2008, 3, 1), droit1.getDateDebutMetier());
			assertEquals("Achat", droit1.getMotifDebut());
			assertNull(droit1.getMotifFin());
			assertEquals("4782389c8e", droit1.getMasterIdRF());
			assertEquals("02faeee", droit1.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que les droits virtuels sont bien calculés dans le cas où il y a un droit entre deux immeubles.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                        individuelle (1/1)   +------------+
	 *                     +---------------------->| Immeuble 0 |
	 *     +----------+    |                       +------------+
	 *     |          |----+                             |
	 *     | Tiers RF |                                  | fond dominant (20/100)
	 *     |          |----+                             v
	 *     +----------+    |  copropriété (1/3)    +------------+
	 *                     +---------------------->| Immeuble 1 |
	 *                                             +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *                        individuelle (1/1)   +------------+
	 *                     +---------------------->| Immeuble 0 |
	 *     +----------+    |                       +------------+
	 *     |          |----+                             |
	 *     | Tiers RF |                                  | fond dominant (20/100)
	 *     |          |----+                             v
	 *     +----------+    |  copropriété (1/3)    +------------+
	 *           :         +---------------------->| Immeuble 1 |
	 *           :                                 +------------+
	 *           :                                       ^
	 *           :      droit virtuel (1/1 * 20/100)     :
	 *           +.......................................+
	 * </pre>
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForTiersRFCasAvecUnDroitEntreDeuxImmeubles() throws Exception {

		class Ids {
			Long tiers;
			Long droit0;
			Long droit1;
			Long droit2;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles ayant un lien entre eux
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPropriete(tiersRF, immeuble0, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                                                                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			final DroitProprieteImmeubleRF droit1 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(20, 100),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");

			final DroitProprietePersonnePhysiqueRF droit2 = addDroitPropriete(tiersRF, immeuble1, null, GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                                                                  RegDate.get(2008, 3, 1), null, RegDate.get(2008, 3, 1), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2008, 2, 23), "4782389c8e", "1");

			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels.
			// droits retournés :
			//  - 2 droits réels
			//  - 1 droit virtuel (Tiers RF -> Immeuble 0 -> Immeuble 1)
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			droits.sort(new DroitRFRangeMetierComparator());
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final DroitProprietePersonnePhysiqueRF droitReel0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
			assertNull(droitReel0.getCommunaute());
			assertEquals(GenrePropriete.INDIVIDUELLE, droitReel0.getRegime());
			assertEquals(new Fraction(1, 1), droitReel0.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitReel0.getDateDebutMetier());
			assertEquals("Achat", droitReel0.getMotifDebut());
			assertNull(droitReel0.getMotifFin());
			assertEquals("48390a0e044", droitReel0.getMasterIdRF());
			assertEquals("01faeee", droitReel0.getImmeuble().getIdRF());

			final DroitProprieteVirtuelRF droitVirtuel1 = (DroitProprieteVirtuelRF) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitRF> chemin = droitVirtuel1.getChemin();                                // tiers RF -> immeuble 0 -> immeuble 1
			assertEquals(2, chemin.size());
			assertEquals(ids.droit0, chemin.get(0).getId());
			assertEquals(ids.droit1, chemin.get(1).getId());

			final DroitProprietePersonnePhysiqueRF droitReel2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
			assertNull(droitReel2.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droitReel2.getRegime());
			assertEquals(new Fraction(1, 3), droitReel2.getPart());
			assertEquals(RegDate.get(2008, 3, 1), droitReel2.getDateDebutMetier());
			assertEquals("Achat", droitReel2.getMotifDebut());
			assertNull(droitReel2.getMotifFin());
			assertEquals("4782389c8e", droitReel2.getMasterIdRF());
			assertEquals("02faeee", droitReel2.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que les droits virtuels sont bien calculés dans le cas où un tiers possède deux immeubles qui possèdent chacun un bout d'un troisième immeuble.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                        individuelle (1/1)   +------------+   fond dominant (17/50)
	 *                     +---------------------->| Immeuble 0 |---------------------------+
	 *     +----------+    |                       +------------+                           |    +------------+
	 *     |          |----+                                                                +--->|            |
	 *     | Tiers RF |                                                                          | Immeuble 3 |
	 *     |          |----+                                                                +--->|            |
	 *     +----------+    |  copropriété (1/3)    +------------+   fond dominant (23/50)   |    +------------+
	 *                     +---------------------->| Immeuble 1 |---------------------------+
	 *                                             +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *                   droit virtuel (1/1 * 17/50)
	 *           +....................................................................................+
	 *           :                                                                                    :
	 *           :            individuelle (1/1)   +------------+   fond dominant (17/50)             :
	 *           :         +---------------------->| Immeuble 0 |---------------------------+         v
	 *     +----------+    |                       +------------+                           |    +------------+
	 *     |          |----+                                                                +--->|            |
	 *     | Tiers RF |                                                                          | Immeuble 3 |
	 *     |          |----+                                                                +--->|            |
	 *     +----------+    |  copropriété (1/3)    +------------+   fond dominant (23/50)   |    +------------+
	 *           :         +---------------------->| Immeuble 1 |---------------------------+         ^
	 *           :                                 +------------+                                     :
	 *           :                                                                                    :
	 *           :       droit virtuel (1/3 * 23/50)                                                  :
	 *           +....................................................................................+
	 * </pre>
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForTiersRFCasAvecImmeublePossedeParDeuxAutresImmeubles() throws Exception {

		class Ids {
			Long tiers;
			Long droit0;
			Long droit1;
			Long droit2;
			Long droit3;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles ayant un lien entre eux
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", laSarraz, 4298);
			final BienFondsRF immeuble2 = addBienFondsRF("03faeee", "CHE2", laSarraz, 4299);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			// tiers RF -> immeuble0
			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPropriete(tiersRF, immeuble0, null, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                                                                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			// tiers RF -> immeuble1
			final DroitProprietePersonnePhysiqueRF droit1 = addDroitPropriete(tiersRF, immeuble1, null, GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                                                                  RegDate.get(2008, 3, 1), null, RegDate.get(2008, 3, 1), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2008, 2, 23), "4782389c8e", "1");

			// immeuble0 -> immeuble 2
			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble0, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(17, 50),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");

			// immeuble1 -> immeuble 2
			final DroitProprieteImmeubleRF droit3 = addDroitPropriete(immeuble1, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(23, 50),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "6680384444", "1");


			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();
			ids.droit3 = droit3.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels.
			// droits retournés :
			//  - 2 droits réels
			//  - 2 droits virtuels :
			//       - Tiers RF -> Immeuble 0 -> Immeuble 2
			//       - Tiers RF -> Immeuble 1 -> Immeuble 2
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			droits.sort(new DroitRFRangeMetierComparator());
			assertNotNull(droits);
			assertEquals(4, droits.size());

			final DroitProprietePersonnePhysiqueRF droitReel0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
			assertNull(droitReel0.getCommunaute());
			assertEquals(GenrePropriete.INDIVIDUELLE, droitReel0.getRegime());
			assertEquals(new Fraction(1, 1), droitReel0.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitReel0.getDateDebutMetier());
			assertEquals("Achat", droitReel0.getMotifDebut());
			assertNull(droitReel0.getMotifFin());
			assertEquals("48390a0e044", droitReel0.getMasterIdRF());
			assertEquals("01faeee", droitReel0.getImmeuble().getIdRF());

			final DroitProprieteVirtuelRF droitVirtuel1 = (DroitProprieteVirtuelRF) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble2
			final List<DroitRF> chemin1 = droitVirtuel1.getChemin();
			assertEquals(2, chemin1.size());
			assertDroitPropChemin(ids.droit0, 1, 1, chemin1.get(0));                         // tiers RF -> immeuble0
			assertDroitPropChemin(ids.droit2, 17, 50, chemin1.get(1));                       // immeuble0 -> immeuble 2

			final DroitProprietePersonnePhysiqueRF droitReel2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
			assertNull(droitReel2.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droitReel2.getRegime());
			assertEquals(new Fraction(1, 3), droitReel2.getPart());
			assertEquals(RegDate.get(2008, 3, 1), droitReel2.getDateDebutMetier());
			assertEquals("Achat", droitReel2.getMotifDebut());
			assertNull(droitReel2.getMotifFin());
			assertEquals("4782389c8e", droitReel2.getMasterIdRF());
			assertEquals("02faeee", droitReel2.getImmeuble().getIdRF());

			final DroitProprieteVirtuelRF droitVirtuel3 = (DroitProprieteVirtuelRF) droits.get(3);
			assertNull(droitVirtuel3.getCommunaute());
			assertEquals(RegDate.get(2008, 3, 1), droitVirtuel3.getDateDebutMetier());   // date d'achat du droit1
			assertEquals("Achat", droitVirtuel3.getMotifDebut());
			assertNull(droitVirtuel3.getMotifFin());
			assertNull(droitVirtuel3.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel3.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel3.getImmeuble().getIdRF());                        // l'immeuble2
			final List<DroitRF> chemin3 = droitVirtuel3.getChemin();
			assertEquals(2, chemin3.size());
			assertDroitPropChemin(ids.droit1, 1, 3, chemin3.get(0));                         // tiers RF -> immeuble1
			assertDroitPropChemin(ids.droit3, 23, 50, chemin3.get(1));                       // immeuble1 -> immeuble 2

			return null;
		});
	}

	/**
	 * Vérifie que les droits virtuels sont bien calculés dans le cas où un tiers possède un immeuble qui possède un deuxième immeuble qui possède lui-même un troisième immeuble.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *     +----------+
	 *     | Tiers RF |---------+
	 *     +----------+         | copropriété (1/3)
	 *                          v
	 *                     +------------+
	 *                     | Immeuble 0 |--------------+
	 *                     +------------+              | fond dominant (1/5)
	 *                                                 v
	 *                                            +------------+
	 *                                            | Immeuble 1 |---------+
	 *                                            +------------+         | fond dominant (33/100)
	 *                                                                   v
	 *                                                              +------------+
	 *                                                              | Immeuble 2 |
	 *                                                              +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *     +----------+
	 *     | Tiers RF |---------+
	 *     +----------+         | copropriété (1/3)
	 *        :    :            v
	 *        :    :       +------------+
	 *        :    :       | Immeuble 0 |--------------+
	 *        :    :       +------------+              | fond dominant (1/5)
	 *        :    :                                   v
	 *        :    : droit virtuel (1/3 * 1/5)    +------------+
	 *        :    +.............................>| Immeuble 1 |---------+
	 *        :                                   +------------+         | fond dominant (33/100)
	 *        :                                                          v
	 *        :      droit virtuel (1/3 * 1/5 * 33/100)             +------------+
	 *        +....................................................>| Immeuble 2 |
	 *                                                              +------------+
	 * </pre>
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForTiersRFCasAvecTroisImmeublesQuiSePossedentEnCascade() throws Exception {

		class Ids {
			Long tiers;
			Long droit0;
			Long droit1;
			Long droit2;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles ayant un lien entre eux
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", laSarraz, 4298);
			final BienFondsRF immeuble2 = addBienFondsRF("03faeee", "CHE2", laSarraz, 4299);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			// tiers RF -> immeuble0
			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPropriete(tiersRF, immeuble0, null, GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                                                                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			// immeuble0 -> immeuble 1
			final DroitProprieteImmeubleRF droit1 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(1, 5),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");

			// immeuble1 -> immeuble 2
			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble1, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(33, 100),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "6680384444", "1");


			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels.
			// droits retournés :
			//  - 1 droit réel
			//  - 2 droits virtuels :
			//       - Tiers RF -> Immeuble 0 -> Immeuble 1
			//       - Tiers RF -> Immeuble 0 -> Immeuble 1 -> Immeuble 2
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			droits.sort(new DroitRFRangeMetierComparator());
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final DroitProprietePersonnePhysiqueRF droitReel0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
			assertNull(droitReel0.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droitReel0.getRegime());
			assertEquals(new Fraction(1, 3), droitReel0.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitReel0.getDateDebutMetier());
			assertEquals("Achat", droitReel0.getMotifDebut());
			assertNull(droitReel0.getMotifFin());
			assertEquals("48390a0e044", droitReel0.getMasterIdRF());
			assertEquals("01faeee", droitReel0.getImmeuble().getIdRF());

			final DroitProprieteVirtuelRF droitVirtuel1 = (DroitProprieteVirtuelRF) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitRF> chemin1 = droitVirtuel1.getChemin();
			assertEquals(2, chemin1.size());
			assertDroitPropChemin(ids.droit0, 1, 3, chemin1.get(0));                         // tiers RF -> immeuble0
			assertDroitPropChemin(ids.droit1, 1, 5, chemin1.get(1));                         // immeuble0 -> immeuble1

			final DroitProprieteVirtuelRF droitVirtuel2 = (DroitProprieteVirtuelRF) droits.get(2);
			assertNull(droitVirtuel2.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel2.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel2.getMotifDebut());
			assertNull(droitVirtuel2.getMotifFin());
			assertNull(droitVirtuel2.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel2.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel2.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitRF> chemin2 = droitVirtuel2.getChemin();
			assertEquals(3, chemin2.size());
			assertDroitPropChemin(ids.droit0, 1, 3, chemin2.get(0));                         // tiers RF -> immeuble0
			assertDroitPropChemin(ids.droit1, 1, 5, chemin2.get(1));                         // immeuble0 -> immeuble1
			assertDroitPropChemin(ids.droit2, 33, 100, chemin2.get(2));                      // immeuble1 -> immeuble2

			return null;
		});
	}

	/**
	 * Vérifie que les droits virtuels sont bien calculés dans le cas où un tiers possède un immeuble qui fait partie d'un cycle de possession avec un autre immeuble.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *     +----------+
	 *     | Tiers RF |---------+
	 *     +----------+         | copropriété (1/3)
	 *                          v
	 *                     +------------+
	 *                     | Immeuble 0 |--------------------+
	 *                     +------------+                    | ppe (1/5)
	 *                          ^                            v
	 *                          | fond dominant (4/7)   +------------+
	 *                          +-----------------------| Immeuble 1 |
	 *                                                  +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *     +----------+
	 *     | Tiers RF |---------------------------------------+
	 *     +----------+                                       | copropriété (1/3)
	 *        :    :                                          v
	 *        :    :  droit virtuel (1/3 * 1/5 * 4/7)    +------------+
	 *        :    +....................................>| Immeuble 0 |--------------------+
	 *        :                                          +------------+                    | ppe (1/5)
	 *        :                                               ^                            v
	 *        :                                               | fond dominant (4/7)   +------------+
	 *        :                                               +-----------------------| Immeuble 1 |
	 *        :                                                                       +------------+
	 *        :                                                                            ^
	 *        :  droit virtuel (1/3 * 1/5)                                                 |
	 *        +............................................................................+
	 * </pre>
	 */
	@Test
	public void testGetDroitsVirtuelsTransitifsForTiersRFCasAvecDeuxImmeublesEtCycle() throws Exception {

		class Ids {
			Long tiers;
			Long droit0;
			Long droit1;
			Long droit2;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec un immeuble qui fait partie d'un cycle de possession avec un autre immeuble.
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", laSarraz, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			// tiers RF -> immeuble0
			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPropriete(tiersRF, immeuble0, null, GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                                                                  RegDate.get(2004, 5, 21), null, RegDate.get(2004, 5, 21), null, "Achat", null,
			                                                                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044", "1");

			// immeuble0 -> immeuble 1
			final DroitProprieteImmeubleRF droit1 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.PPE, new Fraction(1, 5),
			                                                          null, RegDate.get(2000, 1, 1), null, "Constitution de PPE", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");

			// immeuble1 -> immeuble 0
			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble1, immeuble0, GenrePropriete.FONDS_DOMINANT, new Fraction(4, 7),
			                                                          null, RegDate.get(2000, 1, 1), null, "Constitution de PPE", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "6680384444", "1");

			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels.
			// droits retournés :
			//  - 1 droit réel
			//  - 2 droits virtuels :
			//       - Tiers RF -> Immeuble 0 -> Immeuble 1
			//       - Tiers RF -> Immeuble 0 -> Immeuble 1 -> Immeuble 0
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			droits.sort(new DroitRFRangeMetierComparator());
			assertNotNull(droits);
			assertEquals(3, droits.size());

			final DroitProprietePersonnePhysiqueRF droitReel0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
			assertNull(droitReel0.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droitReel0.getRegime());
			assertEquals(new Fraction(1, 3), droitReel0.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitReel0.getDateDebutMetier());
			assertEquals("Achat", droitReel0.getMotifDebut());
			assertNull(droitReel0.getMotifFin());
			assertEquals("48390a0e044", droitReel0.getMasterIdRF());
			assertEquals("01faeee", droitReel0.getImmeuble().getIdRF());

			final DroitProprieteVirtuelRF droitVirtuel1 = (DroitProprieteVirtuelRF) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("01faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble0
			final List<DroitRF> chemin2 = droitVirtuel1.getChemin();
			assertEquals(3, chemin2.size());
			assertDroitPropChemin(ids.droit0, 1, 3, chemin2.get(0));                         // tiers RF -> immeuble0
			assertDroitPropChemin(ids.droit1, 1, 5, chemin2.get(1));                         // immeuble0 -> immeuble1
			assertDroitPropChemin(ids.droit2, 4, 7, chemin2.get(2));                         // immeuble1 -> immeuble0

			final DroitProprieteVirtuelRF droitVirtuel2 = (DroitProprieteVirtuelRF) droits.get(2);
			assertNull(droitVirtuel2.getCommunaute());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel2.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel2.getMotifDebut());
			assertNull(droitVirtuel2.getMotifFin());
			assertNull(droitVirtuel2.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel2.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel2.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitRF> chemin1 = droitVirtuel2.getChemin();
			assertEquals(2, chemin1.size());
			assertDroitPropChemin(ids.droit0, 1, 3, chemin1.get(0));                         // tiers RF -> immeuble0
			assertDroitPropChemin(ids.droit1, 1, 5, chemin1.get(1));                         // immeuble0 -> immeuble1

			return null;
		});
	}

	/**
	 * Vérifie qu'aucun usufruit virtuel n'est trouvé s'il n'y a pas de droits entre immeubles.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                                    +------------+
	 *     +----------+             +---->| Immeuble 0 |
	 *     |          |   usufruit  |     +------------+
	 *     | Tiers RF |-------------+
	 *     |          |             |     +------------+
	 *     +----------+             +---->| Immeuble 1 |
	 *                                    +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels (pas de différence)</b>
	 * <pre>
	 *                                    +------------+
	 *     +----------+             +---->| Immeuble 0 |
	 *     |          |   usufruit  |     +------------+
	 *     | Tiers RF |-------------+
	 *     |          |             |     +------------+
	 *     +----------+             +---->| Immeuble 1 |
	 *                                    +------------+
	 * </pre>
	 */
	@Test
	public void testGetUsufruitVirtuelsTransitifsForTiersRFCasSansDroitEntreImmeuble() throws Exception {

		// mise en place foncière
		final Long tiersId = doInNewTransaction(status -> {

			// un tiers RF avec un usufruit sur deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			addUsufruitRF(null, RegDate.get(2004, 5, 21), null, null, "Convention", null, "783626161", "1",
			              new IdentifiantAffaireRF(123, 2004, 2, 23), new IdentifiantDroitRF(123, 2004, 1),
			              Collections.singletonList(tiersRF), Arrays.asList(immeuble0, immeuble1));
			return tiersRF.getId();
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(tiersId);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels. Comme il n'y a pas de droits entre immeubles, on ne devrait recevoir que l'usufruit réel
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			assertNotNull(droits);
			assertEquals(1, droits.size());

			final UsufruitRF droit0 = (UsufruitRF) droits.get(0);
			Set<AyantDroitRF> ayantDroits0 = droit0.getAyantDroits();
			assertEquals(1, ayantDroits0.size());
			assertEquals(tiersId, ayantDroits0.iterator().next().getId());
			assertEquals(RegDate.get(2004, 5, 21), droit0.getDateDebutMetier());
			assertEquals("Convention", droit0.getMotifDebut());
			assertNull(droit0.getMotifFin());
			assertEquals("783626161", droit0.getMasterIdRF());

			final List<ImmeubleRF> immeubles = new ArrayList<>(droit0.getImmeubles());
			assertEquals(2, immeubles.size());
			immeubles.sort(Comparator.comparing(ImmeubleRF::getIdRF));
			assertEquals("01faeee", immeubles.get(0).getIdRF());
			assertEquals("02faeee", immeubles.get(1).getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que un usufruit virtuel est bien trouvé lorsqu'il y a un droit entre deux immeubles.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                                    +------------+  fond dominant (20/100)    +------------+
	 *     +----------+             +---->| Immeuble 0 |--------------------------->| Immeuble 2 |
	 *     |          |   usufruit  |     +------------+                            +------------+
	 *     | Tiers RF |-------------+
	 *     |          |             |     +------------+
	 *     +----------+             +---->| Immeuble 1 |
	 *                                    +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *                    usufruit virtuel (usufruit + 20/100)
	 *             +......................................................................+
	 *             :                                                                      :
	 *             :                                                                      v
	 *             :                      +------------+  fond dominant (20/100)    +------------+
	 *     +----------+             +---->| Immeuble 0 |--------------------------->| Immeuble 2 |
	 *     |          |   usufruit  |     +------------+                            +------------+
	 *     | Tiers RF |-------------+
	 *     |          |             |     +------------+
	 *     +----------+             +---->| Immeuble 1 |
	 *                                    +------------+
	 * </pre>
	 */
	@Test
	public void testGetUsufruitVirtuelsTransitifsForTiersRFCasAvecUnDroitEntreDeuxImmeubles() throws Exception {

		class Ids {
			Long tiers;
			long droit0;
			long droit1;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec un usufruit sur deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", gland, 4298);
			final BienFondsRF immeuble2 = addBienFondsRF("03faeee", "CHE2", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));


			final UsufruitRF droit0 = addUsufruitRF(null, RegDate.get(2004, 5, 21), null, null, "Convention", null, "783626161", "1",
			                                        new IdentifiantAffaireRF(123, 2004, 2, 23), new IdentifiantDroitRF(123, 2004, 1),
			                                        Collections.singletonList(tiersRF), Arrays.asList(immeuble0, immeuble1));

			final DroitProprieteImmeubleRF droit1 = addDroitPropriete(immeuble0, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(20, 100),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");
			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels. 
			// droits retournés :
			//  - 1 usufruit réel
			//  - 1 usufruit virtuel (Tiers RF -> Immeuble 0 -> Immeuble 2)
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			assertNotNull(droits);
			assertEquals(2, droits.size());
			droits.sort(new DroitRFRangeMetierComparator());

			final UsufruitVirtuelRF droit0 = (UsufruitVirtuelRF) droits.get(0);
			final AyantDroitRF ayantDroit0 = droit0.getAyantDroit();
			assertEquals(ids.tiers, ayantDroit0.getId());
			assertEquals(RegDate.get(2004, 5, 21), droit0.getDateDebutMetier());
			assertEquals("Convention", droit0.getMotifDebut());
			assertNull(droit0.getMotifFin());
			assertNull(droit0.getMasterIdRF());

			final ImmeubleRF immeuble0 = droit0.getImmeuble();
			assertEquals("03faeee", immeuble0.getIdRF());

			final List<DroitRF> chemin = droit0.getChemin();
			assertNotNull(chemin);
			assertEquals(2, chemin.size());
			assertServitudeChemin(ids.droit0, (ServitudeRF) chemin.get(0));
			assertDroitPropChemin(ids.droit1, 20, 100, chemin.get(1));

			final UsufruitRF droit1 = (UsufruitRF) droits.get(1);
			final Set<AyantDroitRF> ayantDroits0 = droit1.getAyantDroits();
			assertEquals(1, ayantDroits0.size());
			assertEquals(ids.tiers, ayantDroits0.iterator().next().getId());
			assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebutMetier());
			assertEquals("Convention", droit1.getMotifDebut());
			assertNull(droit1.getMotifFin());
			assertEquals("783626161", droit1.getMasterIdRF());

			final List<ImmeubleRF> immeubles1 = new ArrayList<>(droit1.getImmeubles());
			assertEquals(2, immeubles1.size());
			immeubles1.sort(Comparator.comparing(ImmeubleRF::getIdRF));
			assertEquals("01faeee", immeubles1.get(0).getIdRF());
			assertEquals("02faeee", immeubles1.get(1).getIdRF());

			return null;
		});
	}

	/**
	 * Vérifie que un usufruit virtuel est bien trouvé lorsqu'il y a un droit entre deux immeubles.
	 * <p/>
	 * <b>Situation réelle</b>
	 * <pre>
	 *                                    +------------+  fond dominant (20/100)    +------------+
	 *     +----------+             +---->| Immeuble 0 |--------------------------->| Immeuble 2 |
	 *     |          |   usufruit  |     +------------+                            +------------+
	 *     | Tiers RF |-------------+                                                     ^
	 *     |          |             |     +------------+  fond dominant (3/100)           |
	 *     +----------+             +---->| Immeuble 1 |----------------------------------+
	 *                                    +------------+
	 * </pre>
	 * <b>Situation avec droits virtuels</b>
	 * <pre>
	 *                    usufruit virtuel (usufruit + 20/100)
	 *             +......................................................................+
	 *             :                                                                      :
	 *             :                                                                      v
	 *             :                      +------------+  fond dominant (20/100)    +------------+
	 *     +----------+             +---->| Immeuble 0 |--------------------------->| Immeuble 2 |
	 *     |          |   usufruit  |     +------------+                            +------------+
	 *     | Tiers RF |-------------+                                                     ^   ^
	 *     |          |             |     +------------+  fond dominant (3/100)           |   :
	 *     +----------+             +---->| Immeuble 1 |----------------------------------+   :
	 *             :                      +------------+                                      :
	 *             :                                                                          :
	 *             :      usufruit virtuel (usufruit + 3/100)                                 :
	 *             +..........................................................................+
	 *
	 * </pre>
	 */
	@Test
	public void testGetUsufruitVirtuelsTransitifsForTiersRFCasAvecUnDeuxImmeublesAyantChacunUnDroitSurUnTroisiemeImmeuble() throws Exception {

		class Ids {
			Long tiers;
			long droit0;
			long droit1;
			long droit2;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec un usufruit sur deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "CHE1", gland, 4298);
			final BienFondsRF immeuble2 = addBienFondsRF("03faeee", "CHE2", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("78838e838ca92", "Charles-Jean", "Widmer", date(1970, 1, 2));

			final UsufruitRF droit0 = addUsufruitRF(null, RegDate.get(2004, 5, 21), null, null, "Convention", null, "783626161", "1",
			                                        new IdentifiantAffaireRF(123, 2004, 2, 23), new IdentifiantDroitRF(123, 2004, 1),
			                                        Collections.singletonList(tiersRF), Arrays.asList(immeuble0, immeuble1));
			final DroitProprieteImmeubleRF droit1 = addDroitPropriete(immeuble0, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(20, 100),
			                                                          null, RegDate.get(2000, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "7686758448", "1");
			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble1, immeuble2, GenrePropriete.FONDS_DOMINANT, new Fraction(3, 100),
			                                                          null, RegDate.get(2005, 1, 1), null, "Remaniement parcellaire", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 6, 1), "122244543", "1");

			ids.tiers = tiersRF.getId();
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();
			return null;
		});

		// appel du service
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF tiersRF = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.tiers);
			assertNotNull(tiersRF);

			// on demande les droits, y compris les droits virtuels.
			// droits retournés :
			//  - 1 usufruit réel
			//  - 2 usufruit virtuel (Tiers RF -> Immeuble 0 -> Immeuble 2 et Tiers RF -> Immeuble 1 -> Immeuble 2)
			final List<DroitRF> droits = serviceRF.getDroitsForTiersRF(tiersRF, false, true);
			assertNotNull(droits);
			assertEquals(3, droits.size());
			droits.sort(new DroitRFRangeMetierComparator());

			// usufruit virtuel (Tiers RF -> Immeuble 0 -> Immeuble 2)
			final UsufruitVirtuelRF droit0 = (UsufruitVirtuelRF) droits.get(0);
			final AyantDroitRF ayantDroit0 = droit0.getAyantDroit();
			assertEquals(ids.tiers, ayantDroit0.getId());
			assertEquals(RegDate.get(2004, 5, 21), droit0.getDateDebutMetier());
			assertEquals("Convention", droit0.getMotifDebut());
			assertNull(droit0.getMotifFin());
			assertNull(droit0.getMasterIdRF());

			final ImmeubleRF immeuble0 = droit0.getImmeuble();
			assertEquals("03faeee", immeuble0.getIdRF());

			final List<DroitRF> chemin0 = droit0.getChemin();
			assertNotNull(chemin0);
			assertEquals(2, chemin0.size());
			assertServitudeChemin(ids.droit0, (ServitudeRF) chemin0.get(0));
			assertDroitPropChemin(ids.droit1, 20, 100, chemin0.get(1));

			// usufruit réel
			final UsufruitRF droit1 = (UsufruitRF) droits.get(1);
			final Set<AyantDroitRF> ayantDroits2 = droit1.getAyantDroits();
			assertEquals(1, ayantDroits2.size());
			assertEquals(ids.tiers, ayantDroits2.iterator().next().getId());
			assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebutMetier());
			assertEquals("Convention", droit1.getMotifDebut());
			assertNull(droit1.getMotifFin());
			assertEquals("783626161", droit1.getMasterIdRF());

			final List<ImmeubleRF> immeubles1 = new ArrayList<>(droit1.getImmeubles());
			assertEquals(2, immeubles1.size());
			immeubles1.sort(Comparator.comparing(ImmeubleRF::getIdRF));
			assertEquals("01faeee", immeubles1.get(0).getIdRF());
			assertEquals("02faeee", immeubles1.get(1).getIdRF());

			// usufruit virtuel (Tiers RF -> Immeuble 1 -> Immeuble 2)
			final UsufruitVirtuelRF droit2 = (UsufruitVirtuelRF) droits.get(2);
			final AyantDroitRF ayantDroit2 = droit2.getAyantDroit();
			assertEquals(ids.tiers, ayantDroit2.getId());
			assertEquals(RegDate.get(2005, 1, 1), droit2.getDateDebutMetier());
			assertEquals("Remaniement parcellaire", droit2.getMotifDebut());
			assertNull(droit2.getMotifFin());
			assertNull(droit2.getMasterIdRF());

			final ImmeubleRF immeuble2 = droit2.getImmeuble();
			assertEquals("03faeee", immeuble2.getIdRF());

			final List<DroitRF> chemin2 = droit2.getChemin();
			assertNotNull(chemin2);
			assertEquals(2, chemin2.size());
			assertServitudeChemin(ids.droit0, (ServitudeRF) chemin2.get(0));
			assertDroitPropChemin(ids.droit2, 3, 100, chemin2.get(1));

			return null;
		});
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que la résolution des URLs vers Capitastra fonctionne correctement.
	 */
	@Test
	public void testGetCapitastraURL() throws Exception {

		class Ids {
			long bienFonds;
			long ppe;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final ProprieteParEtageRF immeuble1 = addProprieteParEtageRF("02faeee", "some egrid", new Fraction(1, 4), gland, 4298, 3, null, null);

			ids.bienFonds = immeuble0.getId();
			ids.ppe = immeuble1.getId();
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=61&kr=0&n1=579&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.bienFonds));
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=242&kr=0&n1=4298&n2=3&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.ppe));
			}
		});
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que la résolution des URLs vers Capitastra fonctionne correctement lorsque l'immeuble n'a plus de situation active.
	 */
	@Test
	public void testGetCapitastraURLDerniereSituationFermee() throws Exception {

		class Ids {
			long bienFonds;
			long ppe;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			immeuble0.getSituations().iterator().next().setDateFin(RegDate.get(2015, 12, 31));
			final ProprieteParEtageRF immeuble1 = addProprieteParEtageRF("02faeee", "some egrid", new Fraction(1, 4), gland, 4298, 3, null, null);

			ids.bienFonds = immeuble0.getId();
			ids.ppe = immeuble1.getId();
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=61&kr=0&n1=579&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.bienFonds));
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=242&kr=0&n1=4298&n2=3&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.ppe));
			}
		});
	}

	@Test
	public void testGetCommune() throws Exception {
		// mise en place fiscale
		final long idImmeuble = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
			return immeuble.getId();
		});

		// interrogation du service
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, idImmeuble);
				Assert.assertNotNull(immeuble);

				final Set<SituationRF> allSituations = immeuble.getSituations();
				Assert.assertNotNull(allSituations);
				Assert.assertEquals(1, allSituations.size());
				final SituationRF situation = allSituations.iterator().next();
				Assert.assertNotNull(situation);
				Assert.assertEquals(date(2000, 1, 1), situation.getDateDebut());
				Assert.assertNull(situation.getDateFin());

				// avant la date de début de la situation
				final Commune commune1990 = serviceRF.getCommune(immeuble, date(1990, 1, 1));
				Assert.assertNotNull(commune1990);
				Assert.assertEquals(MockCommune.Echallens.getNoOFS(), commune1990.getNoOFS());

				// après la date de début
				final Commune commune2001 = serviceRF.getCommune(immeuble, date(2001, 1, 1));
				Assert.assertNotNull(commune2001);
				assertSame(commune1990, commune2001);
			}
		});
	}

	@Test
	public void testNumeroParcelleComplet() throws Exception {
		// mise en place fiscale
		final long idImmeuble = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
			return immeuble.getId();
		});

		// interrogation du service
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, idImmeuble);
				Assert.assertNotNull(immeuble);

				final Set<SituationRF> allSituations = immeuble.getSituations();
				Assert.assertNotNull(allSituations);
				Assert.assertEquals(1, allSituations.size());
				final SituationRF situation = allSituations.iterator().next();
				Assert.assertNotNull(situation);
				Assert.assertEquals(date(2000, 1, 1), situation.getDateDebut());
				Assert.assertNull(situation.getDateFin());

				// avant la date de début de la situation
				final String noParcelle1990 = serviceRF.getNumeroParcelleComplet(immeuble, date(1990, 1, 1));
				Assert.assertNotNull(noParcelle1990);
				Assert.assertEquals("4514-4-2-1", noParcelle1990);

				// après la date de début
				final String noParcelle2001 = serviceRF.getNumeroParcelleComplet(immeuble, date(2001, 1, 1));
				Assert.assertNotNull(noParcelle2001);
				Assert.assertEquals("4514-4-2-1", noParcelle2001);
			}
		});
	}

	@Test
	public void testGetSituation() throws Exception {

		// mise en place fiscale
		final long idImmeuble = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
			return immeuble.getId();
		});

		// interrogation du service
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final ImmeubleRF immeuble = hibernateTemplate.get(ImmeubleRF.class, idImmeuble);
				Assert.assertNotNull(immeuble);

				final Set<SituationRF> allSituations = immeuble.getSituations();
				Assert.assertNotNull(allSituations);
				Assert.assertEquals(1, allSituations.size());
				final SituationRF situation = allSituations.iterator().next();
				Assert.assertNotNull(situation);
				Assert.assertEquals(date(2000, 1, 1), situation.getDateDebut());
				Assert.assertNull(situation.getDateFin());

				// avant la date de début de la situation
				final SituationRF situ1990 = serviceRF.getSituation(immeuble, date(1990, 1, 1));
				Assert.assertNotNull(situ1990);
				assertSame(situation, situ1990);

				// après la date de début
				final SituationRF situ2001 = serviceRF.getSituation(immeuble, date(2001, 1, 1));
				Assert.assertNotNull(situ2001);
				assertSame(situation, situ2001);
			}
		});
	}

	@Test
	public void testFindOrCreateModeleCommunauteModeleInexistant() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();
		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("1", "Rodolf", "Stuecki", RegDate.get(1960, 1, 1));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2", "Annette", "Bruecke", RegDate.get(1960, 1, 1));
			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			return null;
		});

		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp1);
			final PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp2);
			final HashSet<PersonnePhysiqueRF> membres = new HashSet<>(Arrays.asList(pp1, pp2));

			// on demande le modèle
			final ModeleCommunauteRF modele = serviceRF.findOrCreateModeleCommunaute(membres);

			// on s'assure que c'est le bon
			assertNotNull(modele);
			assertEquals(ModeleCommunauteRF.hashCode(membres), modele.getMembresHashCode());
			final List<AyantDroitRF> membresModele = modele.getMembres().stream()
					.sorted(Comparator.comparing(AyantDroitRF::getId))
					.collect(Collectors.toList());
			assertNotNull(membresModele);
			assertEquals(2, membresModele.size());
			assertSame(pp1, membresModele.get(0));
			assertSame(pp2, membresModele.get(1));

			// on s'assure qu'il n'y en a qu'un dans le base
			assertEquals(1, modeleCommunauteRFDAO.getCount(ModeleCommunauteRF.class));
			return null;
		});

		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp1);
			final PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp2);
			final HashSet<PersonnePhysiqueRF> membres = new HashSet<>(Arrays.asList(pp1, pp2));

			// on demande une deuxième fois le modèle
			final ModeleCommunauteRF modele = serviceRF.findOrCreateModeleCommunaute(membres);

			// on s'assure que c'est le bon
			assertNotNull(modele);
			assertEquals(ModeleCommunauteRF.hashCode(membres), modele.getMembresHashCode());
			final List<AyantDroitRF> membresModele = modele.getMembres().stream()
					.sorted(Comparator.comparing(AyantDroitRF::getId))
					.collect(Collectors.toList());
			assertNotNull(membresModele);
			assertEquals(2, membresModele.size());
			assertSame(pp1, membresModele.get(0));
			assertSame(pp2, membresModele.get(1));

			// on s'assure qu'il y en a toujours qu'un dans le base
			assertEquals(1, modeleCommunauteRFDAO.getCount(ModeleCommunauteRF.class));
			return null;
		});
	}

	@Test
	public void testFindOrCreateModeleCommunauteModelePreexistant() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("1", "Rodolf", "Stuecki", RegDate.get(1960, 1, 1));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2", "Annette", "Bruecke", RegDate.get(1960, 1, 1));
			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();

			// on crée le modèle de communauté qui va bien
			modeleCommunauteRFDAO.createWith(new HashSet<>(Arrays.asList(ids.pp1, ids.pp2)));
			return null;
		});

		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp1);
			final PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp2);
			final HashSet<PersonnePhysiqueRF> membres = new HashSet<>(Arrays.asList(pp1, pp2));

			// on demande le modèle
			final ModeleCommunauteRF modele = serviceRF.findOrCreateModeleCommunaute(membres);

			// on s'assure que c'est le bon
			assertNotNull(modele);
			assertEquals(ModeleCommunauteRF.hashCode(membres), modele.getMembresHashCode());
			final List<AyantDroitRF> membresModele = modele.getMembres().stream()
					.sorted(Comparator.comparing(AyantDroitRF::getId))
					.collect(Collectors.toList());
			assertNotNull(membresModele);
			assertEquals(2, membresModele.size());
			assertSame(pp1, membresModele.get(0));
			assertSame(pp2, membresModele.get(1));

			// on s'assure qu'il y en a toujours qu'un dans le base
			assertEquals(1, modeleCommunauteRFDAO.getCount(ModeleCommunauteRF.class));
			return null;
		});
	}

	@Test
	public void testFindOrCreateModeleCommunauteMultithreads() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("1", "Rodolf", "Stuecki", RegDate.get(1960, 1, 1));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2", "Annette", "Bruecke", RegDate.get(1960, 1, 1));
			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			return null;
		});

		// on créé 20 threads différents qui vont demander le même modèle tous en même
		ForkJoinPool forkJoinPool = new ForkJoinPool(20);
		final ForkJoinTask<?> task = forkJoinPool.submit(() -> IntStream.range(0, 20)
				.parallel()
				.mapToObj(i -> loadModelInTx(ids.pp1, ids.pp2))
				.collect(Collectors.toList()));

		// on vérifie qu'on a chargé 20 fois le même modèle
		//noinspection unchecked
		final List<ModeleCommunauteRF> modeles = (List<ModeleCommunauteRF>) task.join();
		assertEquals(20, modeles.size());
		final Long id = modeles.get(0).getId();
		for (int i = 0; i < 20; ++i) {
			assertEquals(id, modeles.get(i).getId());
		}

		doInNewTransaction(status -> {
			// on s'assure qu'il y a toujours qu'un seul modèle dans le base
			assertEquals(1, modeleCommunauteRFDAO.getCount(ModeleCommunauteRF.class));
			return null;
		});
	}

	@Test
	public void testGetCommunauteMembreInfoCommunauteAvecPrincipalParDefaut() throws Exception {

		final class Ids {
			Long ppId1;
			Long ppId2;
			long communaute;
		}

		final RegDate dateDebutCommunaute = date(2005, 3, 2);

		// on créé une communauté de 2 personnes qui ne change pas dans le temps (= un seul regroupement)
		final Ids ids = doInNewTransactionAndSession(status -> {

			// pp
			final PersonnePhysique nonhab1 = addNonHabitant("Philip", "Linconnu", date(1967, 4, 2), Sexe.MASCULIN);
			final PersonnePhysique nonhab2 = addNonHabitant("Elodie", "Loongle", date(1980, 11, 22), Sexe.FEMININ);
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("43423872389", "Philip", "Linconnu", date(1967, 4, 2));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2252415156", "Elodie", "Loongle", date(1980, 11, 22));
			addRapprochementRF(nonhab1, pp1, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(nonhab2, pp2, null, null, TypeRapprochementRF.AUTO);

			// communauté
			final CommunauteRF communaute = addCommunauteRF("538534zugwhj", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			final ImmeubleRF immeuble = addImmeubleRF("5r37858725g3b");
			final DroitProprietePersonnePhysiqueRF droit1 =
					addDroitPropriete(pp1, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "573853733gdbtq", "1");
			final DroitProprietePersonnePhysiqueRF droit2 =
					addDroitPropriete(pp2, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "43938383838", "1");
			communaute.addMembre(droit1);
			communaute.addMembre(droit2);

			final ModeleCommunauteRF modele = addModeleCommunauteRF(pp1, pp2);
			addRegroupementRF(communaute, modele, dateDebutCommunaute, null);

			final Ids res = new Ids();
			res.ppId1 = nonhab1.getId();
			res.ppId2 = nonhab2.getId();
			res.communaute = communaute.getId();
			return res;
		});

		// on demande les infos de la communauté
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final CommunauteRF communaute = hibernateTemplate.get(CommunauteRF.class, ids.communaute);
				assertNotNull(communaute);

				// la communauté avec Philip comme membre principal (parce que Linconnu arrive avant Loongle)
				final CommunauteRFMembreInfo info = serviceRF.getCommunauteMembreInfo(communaute);
				assertNotNull(info);
				assertEquals(2, info.getCount());
				assertEquals(Arrays.asList(ids.ppId1, ids.ppId2), info.getCtbIds());

				final List<CommunauteRFPrincipalInfo> principaux = info.getPrincipaux();
				assertEquals(1, principaux.size());

				final CommunauteRFPrincipalInfo principal0 = principaux.get(0);
				assertEquals(ids.ppId1.longValue(), principal0.getCtbId());
				assertEquals(dateDebutCommunaute, principal0.getDateDebut());
				assertNull(principal0.getDateFin());
			}
		});
	}

	@Test
	public void testGetCommunauteMembreInfoCommunauteAvecPrincipalChoisi() throws Exception {

		final class Ids {
			Long ppId1;
			Long ppId2;
			long communaute;
		}

		final RegDate dateDebutCommunaute = date(2005, 3, 2);

		// on créé une communauté de 2 personnes qui ne change pas dans le temps (= un seul regroupement)
		final Ids ids = doInNewTransactionAndSession(status -> {

			// pp
			final PersonnePhysique nonhab1 = addNonHabitant("Philip", "Linconnu", date(1967, 4, 2), Sexe.MASCULIN);
			final PersonnePhysique nonhab2 = addNonHabitant("Elodie", "Loongle", date(1980, 11, 22), Sexe.FEMININ);
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("43423872389", "Philip", "Linconnu", date(1967, 4, 2));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2252415156", "Elodie", "Loongle", date(1980, 11, 22));
			addRapprochementRF(nonhab1, pp1, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(nonhab2, pp2, null, null, TypeRapprochementRF.AUTO);

			// communauté
			final CommunauteRF communaute = addCommunauteRF("538534zugwhj", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			final ImmeubleRF immeuble = addImmeubleRF("5r37858725g3b");
			final DroitProprietePersonnePhysiqueRF droit1 =
					addDroitPropriete(pp1, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "573853733gdbtq", "1");
			final DroitProprietePersonnePhysiqueRF droit2 =
					addDroitPropriete(pp2, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "43938383838", "1");
			communaute.addMembre(droit1);
			communaute.addMembre(droit2);

			final ModeleCommunauteRF modele = addModeleCommunauteRF(pp1, pp2);

			// on choisit explicitement Elodie comme principal pour ce modèle de communauté
			final PrincipalCommunauteRF principal = new PrincipalCommunauteRF();
			modele.addPrincipal(principal);
			principal.setPrincipal(pp2);
			principal.setDateDebut(dateDebutCommunaute);

			addRegroupementRF(communaute, modele, dateDebutCommunaute, null);

			final Ids res = new Ids();
			res.ppId1 = nonhab1.getId();
			res.ppId2 = nonhab2.getId();
			res.communaute = communaute.getId();
			return res;
		});

		// on demande les infos de la communauté
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final CommunauteRF communaute = hibernateTemplate.get(CommunauteRF.class, ids.communaute);
				assertNotNull(communaute);

				// la communauté avec Elodie comme membre principal (parce qu'elle est explicitement choisie)
				final CommunauteRFMembreInfo info = serviceRF.getCommunauteMembreInfo(communaute);
				assertNotNull(info);
				assertEquals(2, info.getCount());
				assertEquals(Arrays.asList(ids.ppId2, ids.ppId1), info.getCtbIds());

				final List<CommunauteRFPrincipalInfo> principaux = info.getPrincipaux();
				assertEquals(1, principaux.size());

				final CommunauteRFPrincipalInfo principal0 = principaux.get(0);
				assertEquals(ids.ppId2.longValue(), principal0.getCtbId());
				assertEquals(dateDebutCommunaute, principal0.getDateDebut());
				assertNull(principal0.getDateFin());
			}
		});
	}

	@Test
	public void testGetCommunauteMembreInfoCommunauteAvecPlusieursRegroupement() throws Exception {

		final class Ids {
			Long ppId1;
			Long ppId2;
			Long ppId3;
			long communaute;
		}

		final RegDate dateDebutCommunaute = date(2005, 3, 2);
		final RegDate datePartage = date(2008, 7, 1);

		// on créé une communauté de 3 personnes qui passe ensuite à 2 personnes (= deux regroupements)
		final Ids ids = doInNewTransactionAndSession(status -> {

			// pp
			final PersonnePhysique nonhab1 = addNonHabitant("Philip", "Linconnu", date(1967, 4, 2), Sexe.MASCULIN);
			final PersonnePhysique nonhab2 = addNonHabitant("Elodie", "Loongle", date(1980, 11, 22), Sexe.FEMININ);
			final PersonnePhysique nonhab3 = addNonHabitant("Edouard", "Loongle", date(1990,6, 8), Sexe.MASCULIN);
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("43423872389", "Philip", "Linconnu", date(1967, 4, 2));
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("2252415156", "Elodie", "Loongle", date(1980, 11, 22));
			final PersonnePhysiqueRF pp3 = addPersonnePhysiqueRF("263564617", "Edouard", "Loongle", date(1990,6, 8));
			addRapprochementRF(nonhab1, pp1, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(nonhab2, pp2, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(nonhab3, pp3, null, null, TypeRapprochementRF.AUTO);

			// communauté
			final CommunauteRF communaute = addCommunauteRF("538534zugwhj", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			final ImmeubleRF immeuble = addImmeubleRF("5r37858725g3b");
			final DroitProprietePersonnePhysiqueRF droit1 =
					addDroitPropriete(pp1, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, datePartage,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "573853733gdbtq", "1");
			final DroitProprietePersonnePhysiqueRF droit2 =
					addDroitPropriete(pp2, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "43938383838", "1");
			final DroitProprietePersonnePhysiqueRF droit3 =
					addDroitPropriete(pp3, immeuble, communaute, GenrePropriete.COMMUNE, new Fraction(1, 1),
					                  date(2005, 3, 2), null,
					                  dateDebutCommunaute, null,
					                  "Succession", null,
					                  new IdentifiantAffaireRF(42, 2005, 32, 1),
					                  "3838322111", "1");
			communaute.addMembre(droit1);
			communaute.addMembre(droit2);
			communaute.addMembre(droit3);

			final ModeleCommunauteRF modele1 = addModeleCommunauteRF(pp1, pp2, pp3);
			final ModeleCommunauteRF modele2 = addModeleCommunauteRF(pp2, pp3);

			// on choisit explicitement Elodie comme principal pour le second modèle de communauté
			final PrincipalCommunauteRF principal = new PrincipalCommunauteRF();
			modele2.addPrincipal(principal);
			principal.setPrincipal(pp2);
			principal.setDateDebut(dateDebutCommunaute);

			addRegroupementRF(communaute, modele1, dateDebutCommunaute, datePartage.getOneDayBefore());
			addRegroupementRF(communaute, modele2, datePartage, null);

			final Ids res = new Ids();
			res.ppId1 = nonhab1.getId();
			res.ppId2 = nonhab2.getId();
			res.ppId3 = nonhab3.getId();
			res.communaute = communaute.getId();
			return res;
		});

		// on demande les infos de la communauté
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final CommunauteRF communaute = hibernateTemplate.get(CommunauteRF.class, ids.communaute);
				assertNotNull(communaute);

				// la communauté des 3 membres
				final CommunauteRFMembreInfo info = serviceRF.getCommunauteMembreInfo(communaute);
				assertNotNull(info);
				assertEquals(3, info.getCount());
				assertEquals(Arrays.asList(ids.ppId2, ids.ppId1, ids.ppId3), info.getCtbIds());

				final List<CommunauteRFPrincipalInfo> principaux = info.getPrincipaux();
				assertEquals(2, principaux.size());

				// la période où Philip est le principal
				final CommunauteRFPrincipalInfo principal0 = principaux.get(0);
				assertEquals(ids.ppId1.longValue(), principal0.getCtbId());
				assertEquals(dateDebutCommunaute, principal0.getDateDebut());
				assertEquals(datePartage.getOneDayBefore(), principal0.getDateFin());

				// la période où Elodie est la principale
				final CommunauteRFPrincipalInfo principal1 = principaux.get(1);
				assertEquals(ids.ppId2.longValue(), principal1.getCtbId());
				assertEquals(datePartage, principal1.getDateDebut());
				assertNull(principal1.getDateFin());
			}
		});
	}

	/**
	 * [SIFISC-24595] Vérifie que l'ajout d'un principal sur une communauté sans principal défini fonctionne bien.
	 */
	@Test
	public void testAddPrincipalToModeleCommunauteSansPrincipal() throws Exception {

		final RegDate dateDebut = RegDate.get(2005, 4, 12);

		final class Ids {
			Long pp1;
			Long pp2;
			Long pp3;
			Long modele;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("Brigitte", "Widmer", date(1970, 7, 2), "434545", 411451L, null);
			final PersonnePhysiqueRF pp3 = addPersonnePhysiqueRF("Noémie", "Widmer", date(1970, 7, 2), "010289290", 777L, null);

			ModeleCommunauteRF modele = new ModeleCommunauteRF();
			modele.addMembre(pp1);
			modele.addMembre(pp2);
			modele.addMembre(pp3);
			modele.setMembresHashCode(ModeleCommunauteRF.hashCode(modele.getMembres()));
			modele = modeleCommunauteRFDAO.save(modele);

			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			ids.pp3 = pp3.getId();
			ids.modele = modele.getId();
			return null;
		});

		doInNewTransaction(status -> {
			PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp2);
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);

			serviceRF.addPrincipalToModeleCommunaute(pp2, modele, dateDebut);
			return null;
		});

		doInNewTransaction(status -> {
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);

			final Set<PrincipalCommunauteRF> principaux = modele.getPrincipaux();
			assertNotNull(principaux);
			assertEquals(1, principaux.size());

			final PrincipalCommunauteRF principal0 = principaux.iterator().next();
			assertNotNull(principal0);
			assertEquals(ids.pp2, principal0.getPrincipal().getId());
			assertEquals(dateDebut, principal0.getDateDebut());
			assertNull(principal0.getDateFin());
			return null;
		});

		// on vérifie qu'aucun événement fiscal correspondants n'a été émis car le modèle n'est regroupé avec aucune communauté
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(0, events.size());
			return null;
		});

	}

	/**
	 * [SIFISC-24595] Vérifie que l'ajout d'un principal sur une communauté avec des principaux préxistants fonctionne bien.
	 */
	@Test
	public void testAddPrincipalToModeleCommunauteAvecPrincipauxExistants() throws Exception {

		final RegDate dateDebut1 = RegDate.get(2003, 7, 28);
		final RegDate dateDebut2 = RegDate.get(2005, 4, 12);
		final RegDate dateDebut3 = RegDate.get(2012, 11, 19);

		final class Ids {
			Long pp1;
			Long pp2;
			Long pp3;
			Long modele;
			Long communaute1;
			Long communaute2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			final BienFondsRF immeuble = addImmeubleRF("3893983");
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("Brigitte", "Widmer", date(1970, 7, 2), "434545", 411451L, null);
			final PersonnePhysiqueRF pp3 = addPersonnePhysiqueRF("Noémie", "Widmer", date(1970, 7, 2), "010289290", 777L, null);

			// le modèle de communauté
			ModeleCommunauteRF modele = new ModeleCommunauteRF();
			modele.addMembre(pp1);
			modele.addMembre(pp2);
			modele.addMembre(pp3);
			modele.setMembresHashCode(ModeleCommunauteRF.hashCode(modele.getMembres()));
			modele = modeleCommunauteRFDAO.save(modele);

			final PrincipalCommunauteRF p1 = new PrincipalCommunauteRF();
			p1.setModeleCommunaute(modele);
			p1.setPrincipal(pp1);
			p1.setDateDebut(dateDebut1);
			p1.setDateFin(dateDebut3.getOneDayBefore());
			modele.addPrincipal(p1);

			final PrincipalCommunauteRF p3 = new PrincipalCommunauteRF();
			p3.setModeleCommunaute(modele);
			p3.setPrincipal(pp3);
			p3.setDateDebut(dateDebut3);
			modele.addPrincipal(p3);

			// deux communautés qui utilisent le modèle
			final CommunauteRF communaute1 = addCommunauteRF("2892929", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			final DroitProprietePersonnePhysiqueRF droit1 = addDroitPropriete(pp1, immeuble, communaute1, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  null, null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "573853733gdbtq", "1");
			communaute1.addMembre(droit1);
			addRegroupementRF(communaute1, modele, null, null);

			final CommunauteRF communaute2 = addCommunauteRF("478382", TypeCommunaute.INDIVISION);
			final DroitProprietePersonnePhysiqueRF droit2 = addDroitPropriete(pp1, immeuble, communaute2, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  RegDate.get(2000, 1, 1), null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "348348238238", "1");
			communaute2.addMembre(droit2);
			addRegroupementRF(communaute2, modele, RegDate.get(2000, 1, 1), null);

			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			ids.pp3 = pp3.getId();
			ids.modele = modele.getId();
			ids.communaute1 = communaute1.getId();
			ids.communaute2 = communaute2.getId();
			return null;
		});

		doInNewTransaction(status -> {
			// on ajoute le principal pp2 intercalé entre le pp1 et le pp3
			PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(ids.pp2);
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);

			serviceRF.addPrincipalToModeleCommunaute(pp2, modele, dateDebut2);
			return null;
		});

		doInNewTransaction(status -> {
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);

			// on vérifie qu'on a bien trois principaux et que les dates de début/fin sont bien correctes
			final List<PrincipalCommunauteRF> principaux = new ArrayList<>(modele.getPrincipaux());
			assertNotNull(principaux);
			assertEquals(3, principaux.size());
			principaux.sort(new DateRangeComparator<>());

			final PrincipalCommunauteRF principal1 = principaux.get(0);
			assertNotNull(principal1);
			assertEquals(ids.pp1, principal1.getPrincipal().getId());
			assertEquals(dateDebut1, principal1.getDateDebut());
			assertEquals(dateDebut2.getOneDayBefore(), principal1.getDateFin());

			final PrincipalCommunauteRF principal2 = principaux.get(1);
			assertNotNull(principal2);
			assertEquals(ids.pp2, principal2.getPrincipal().getId());
			assertEquals(dateDebut2, principal2.getDateDebut());
			assertEquals(dateDebut3.getOneDayBefore(), principal2.getDateFin());

			final PrincipalCommunauteRF principal3 = principaux.get(2);
			assertNotNull(principal3);
			assertEquals(ids.pp3, principal3.getPrincipal().getId());
			assertEquals(dateDebut3, principal3.getDateDebut());
			assertNull(principal3.getDateFin());
			return null;
		});

		// on vérifie que les événements fiscaux correspondants sont partis (le modèle est regroupés avec deux communautés -> 2 événements)
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalCommunaute)e).getCommunaute().getId()));

			final EvenementFiscalCommunaute event0 = (EvenementFiscalCommunaute) events.get(0);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL, event0.getType());
			assertEquals(dateDebut2, event0.getDateValeur());
			assertEquals(ids.communaute1, event0.getCommunaute().getId());

			final EvenementFiscalCommunaute event1 = (EvenementFiscalCommunaute) events.get(1);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL, event1.getType());
			assertEquals(dateDebut2, event1.getDateValeur());
			assertEquals(ids.communaute2, event1.getCommunaute().getId());
			return null;
		});
	}

	/**
	 * [SIFISC-24595] Vérifie que l'annulation d'un principal sur une communauté fonctionne bien.
	 */
	@Test
	public void testCancelPrincipalCommunaute() throws Exception {

		final RegDate dateDebut1 = RegDate.get(2003, 7, 28);
		final RegDate dateDebut2 = RegDate.get(2005, 4, 12);
		final RegDate dateDebut3 = RegDate.get(2012, 11, 19);

		final class Ids {
			Long pp1;
			Long pp2;
			Long pp3;
			Long modele;
			Long communaute1;
			Long communaute2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			final BienFondsRF immeuble = addImmeubleRF("3893983");
			final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);
			final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("Brigitte", "Widmer", date(1970, 7, 2), "434545", 411451L, null);
			final PersonnePhysiqueRF pp3 = addPersonnePhysiqueRF("Noémie", "Widmer", date(1970, 7, 2), "010289290", 777L, null);

			// le modèle de communauté avec deux principaux existants
			ModeleCommunauteRF modele = new ModeleCommunauteRF();
			modele.addMembre(pp1);
			modele.addMembre(pp2);
			modele.addMembre(pp3);
			modele.setMembresHashCode(ModeleCommunauteRF.hashCode(modele.getMembres()));
			modele = modeleCommunauteRFDAO.save(modele);

			final PrincipalCommunauteRF p1 = new PrincipalCommunauteRF();
			p1.setModeleCommunaute(modele);
			p1.setPrincipal(pp1);
			p1.setDateDebut(dateDebut1);
			p1.setDateFin(dateDebut3.getOneDayBefore());
			modele.addPrincipal(p1);

			final PrincipalCommunauteRF p3 = new PrincipalCommunauteRF();
			p3.setModeleCommunaute(modele);
			p3.setPrincipal(pp3);
			p3.setDateDebut(dateDebut3);
			modele.addPrincipal(p3);

			// deux communautés qui utilisent le modèle
			final CommunauteRF communaute1 = addCommunauteRF("2892929", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			final DroitProprietePersonnePhysiqueRF droit1 = addDroitPropriete(pp1, immeuble, communaute1, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  null, null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "573853733gdbtq", "1");
			communaute1.addMembre(droit1);
			addRegroupementRF(communaute1, modele, null, null);

			final CommunauteRF communaute2 = addCommunauteRF("478382", TypeCommunaute.INDIVISION);
			final DroitProprietePersonnePhysiqueRF droit2 = addDroitPropriete(pp1, immeuble, communaute2, GenrePropriete.COMMUNE, new Fraction(1, 1),
			                                                                  date(2005, 3, 2), null,
			                                                                  RegDate.get(2000, 1, 1), null,
			                                                                  "Succession", null,
			                                                                  new IdentifiantAffaireRF(42, 2005, 32, 1),
			                                                                  "348348238238", "1");
			communaute2.addMembre(droit2);
			addRegroupementRF(communaute2, modele, RegDate.get(2000, 1, 1), null);

			ids.pp1 = pp1.getId();
			ids.pp2 = pp2.getId();
			ids.pp3 = pp3.getId();
			ids.modele = modele.getId();
			ids.communaute1 = communaute1.getId();
			ids.communaute2 = communaute2.getId();
			return null;
		});

		doInNewTransaction(status -> {
			// on annule le principal pp3
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);
			final PrincipalCommunauteRF pp3 = modele.getPrincipaux().stream()
					.filter(p -> p.getPrincipal().getId().equals(ids.pp3))
					.findFirst()
					.orElseThrow(IllegalArgumentException::new);

			serviceRF.cancelPrincipalCommunaute(pp3);
			return null;
		});

		doInNewTransaction(status -> {
			final ModeleCommunauteRF modele = modeleCommunauteRFDAO.get(ids.modele);

			// on vérifie que le principal pp3 est annulé et que la date de fin du principal pp1 est resettée
			final List<PrincipalCommunauteRF> principaux = new ArrayList<>(modele.getPrincipaux());
			assertNotNull(principaux);
			assertEquals(2, principaux.size());
			principaux.sort(new DateRangeComparator<>());

			final PrincipalCommunauteRF principal1 = principaux.get(0);
			assertNotNull(principal1);
			assertFalse(principal1.isAnnule());
			assertEquals(ids.pp1, principal1.getPrincipal().getId());
			assertEquals(dateDebut1, principal1.getDateDebut());
			assertNull(principal1.getDateFin());    // <-- date de fin resettée

			final PrincipalCommunauteRF principal2 = principaux.get(1);
			assertNotNull(principal2);
			assertTrue(principal2.isAnnule());      // <-- principal annulé
			assertEquals(ids.pp3, principal2.getPrincipal().getId());
			assertEquals(dateDebut3, principal2.getDateDebut());
			assertNull(principal2.getDateFin());
			return null;
		});

		// on vérifie que les événements fiscaux correspondants sont partis (le modèle est regroupés avec deux communautés -> 2 événements)
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(e -> ((EvenementFiscalCommunaute)e).getCommunaute().getId()));

			final EvenementFiscalCommunaute event0 = (EvenementFiscalCommunaute) events.get(0);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL, event0.getType());
			assertNull(event0.getDateValeur());
			assertEquals(ids.communaute1, event0.getCommunaute().getId());

			final EvenementFiscalCommunaute event1 = (EvenementFiscalCommunaute) events.get(1);
			assertEquals(EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL, event1.getType());
			assertNull(event1.getDateValeur());
			assertEquals(ids.communaute2, event1.getCommunaute().getId());
			return null;
		});
	}

	/**
	 * [SIFISC-24999] Ce test vérifie que la méthode getDroitsForCtb fonctionne bien quand on demande les droits virtuels hérités lors d'une relatio d'héritage.
	 */
	@Test
	public void testGetDroitsVirtuelsHeritesCasHeritage() throws Exception {

		final long noIndividu = 481548L;
		final RegDate dateHeritage = RegDate.get(2001, 4, 17);

		class Ids {
			long decede;
			long heritier;
			long droit0;
			long droit1;
			long droit2;
		}
		final Ids ids = new Ids();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1920, 7, 2), "Charles", "Widmer", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(1920, 7, 2), null);
			}
		});

		// création de deux tiers, dont un décécé, avec une relation d'héritage
		doInNewTransaction(status -> {
			final PersonnePhysique decede = tiersService.createNonHabitantFromIndividu(noIndividu);
			ids.decede = decede.getNumero();

			final PersonnePhysique heritier = addNonHabitant("Benjamin", "Widmer", date(1970, 1, 1), Sexe.MASCULIN);
			ids.heritier = heritier.getNumero();

			addHeritage(heritier, decede, dateHeritage, null, true);
			return null;
		});

		// mise en place foncière
		doInNewTransaction(status -> {

			// le tiers RF décédé avec deux immeubles + un lien de propriété entre les deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "some egrid", gland, 4298);

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Charles", "Widmer", date(1970, 7, 2), "38383830ae3ff", 411451546L, null);

			final DroitProprietePersonnePhysiqueRF droit0 = addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null,
			                                                                           "48390a0e044", "48390a0e043", new IdentifiantAffaireRF(123, 2004, 202, 3),
			                                                                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiersRF, immeuble0, null);

			final DroitProprietePersonnePhysiqueRF droit1 = addDroitPersonnePhysiqueRF(RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), RegDate.get(2010, 2, 20), "Achat", "Achat",
			                                                                           "47e7d7e773", "47e7d7e772", new IdentifiantAffaireRF(23, 1997, 13, 0),
			                                                                           new Fraction(1, 3), GenrePropriete.COPROPRIETE, tiersRF, immeuble1, null);

			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(12, 345),
			                                                          null, RegDate.get(2000, 1, 1), null, "Constitution PPE", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 121, 2), "39393939", "1");
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();

			final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ids.decede);
			addRapprochementRF(ctb, tiersRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);
			return null;
		});

		// on demande les droits sur l'héritier
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique ctb = (PersonnePhysique) tiersDAO.get(ids.heritier);
				assertNotNull(ctb);

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, true, true);
				droits.sort(new DroitRFRangeMetierComparator());
				assertEquals(3, droits.size());

				// le premier droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel0 = (DroitVirtuelHeriteRF) droits.get(0);
				assertEquals(dateHeritage, droitVirtuel0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 20), droitVirtuel0.getDateFinMetier());
				assertEquals("Succession", droitVirtuel0.getMotifDebut());
				assertEquals("Achat", droitVirtuel0.getMotifFin());

				// la référence du droit hérité
				final DroitProprietePersonnePhysiqueRF reference0 = (DroitProprietePersonnePhysiqueRF) droitVirtuel0.getReference();
				assertNull(reference0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, reference0.getRegime());
				assertEquals(new Fraction(1, 3), reference0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), reference0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), reference0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 23), reference0.getDateFin());
				assertEquals(RegDate.get(2010, 2, 20), reference0.getDateFinMetier());
				assertEquals("Achat", reference0.getMotifDebut());
				assertEquals("Achat", reference0.getMotifFin());
				assertEquals("47e7d7e773", reference0.getMasterIdRF());
				assertEquals("02faeee", reference0.getImmeuble().getIdRF());

				// le deuxième droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel1 = (DroitVirtuelHeriteRF) droits.get(1);
				assertEquals(RegDate.get(2004, 4, 12), droitVirtuel1.getDateDebutMetier());
				assertNull(droitVirtuel1.getDateFinMetier());
				assertEquals("Achat", droitVirtuel1.getMotifDebut());
				assertNull(droitVirtuel1.getMotifFin());

				// la référence du droit hérité
				final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droitVirtuel1.getReference();
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertNull(droit1.getDateFinMetier());
				assertEquals("Achat", droit1.getMotifDebut());
				assertNull(droit1.getMotifFin());
				assertEquals("48390a0e044", droit1.getMasterIdRF());
				assertEquals("01faeee", droit1.getImmeuble().getIdRF());

				// le troisième droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel2 = (DroitVirtuelHeriteRF) droits.get(2);
				assertEquals(RegDate.get(2004, 4, 12), droitVirtuel2.getDateDebutMetier());
				assertNull(droitVirtuel2.getDateFinMetier());
				assertEquals("Achat", droitVirtuel2.getMotifDebut());
				assertNull(droitVirtuel2.getMotifFin());

				// la référence du droit hérité (lui-même un droit virtuel transitif)
				final DroitProprieteVirtuelRF droit2 = (DroitProprieteVirtuelRF) droitVirtuel2.getReference();
				assertNull(droit2.getCommunaute());
				assertNull(droit2.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit2.getDateDebutMetier());
				assertNull(droit2.getDateFin());
				assertNull(droit2.getDateFinMetier());
				assertEquals("Achat", droit2.getMotifDebut());
				assertNull(droit2.getMotifFin());
				assertNull(droit2.getMasterIdRF());
				assertEquals("02faeee", droit2.getImmeuble().getIdRF());
			}
		});
	}

	/**
	 * [SIFISC-24999] Ce test vérifie que la méthode getDroitsForCtb fonctionne bien quand on demande les droits virtuels hérités lors d'une relation de fusion d'entreprise.
	 */
	@Test
	public void testGetDroitsVirtuelsHeritesCasFusionEntreprise() throws Exception {

		final RegDate dateFusion = RegDate.get(2001, 4, 17);

		class Ids {
			long absorbee;
			long absorbante;
			long droit0;
			long droit1;
			long droit2;
		}
		final Ids ids = new Ids();

		// création de deux entreprises, une absorbée et l'autre absorbante
		doInNewTransaction(status -> {
			final Entreprise absorbee = addEntrepriseInconnueAuCivil("Fantôme", RegDate.get(1990, 1, 1));
			final Entreprise absorbante = addEntrepriseInconnueAuCivil("Pacman", RegDate.get(1990, 1, 1));
			addFusionEntreprises(absorbante, absorbee, dateFusion);
			ids.absorbee = absorbee.getNumero();
			ids.absorbante = absorbante.getNumero();
			return null;
		});

		// mise en place foncière
		doInNewTransaction(status -> {

			// l'entreprise absorbée RF avec deux immeubles + un lien de propriété entre les deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble1 = addBienFondsRF("02faeee", "some egrid", gland, 4298);

			final PersonneMoraleRF absorbeeRF = addPersonneMoraleRF("Fantôme", "1", "111", 111, null);

			final DroitProprietePersonneMoraleRF droit0 = addDroitPersonneMoraleRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null,
			                                                                           "48390a0e044", "48390a0e043", new IdentifiantAffaireRF(123, 2004, 202, 3),
			                                                                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, absorbeeRF, immeuble0, null);

			final DroitProprietePersonneMoraleRF droit1 = addDroitPersonneMoraleRF(RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), RegDate.get(2010, 2, 20), "Achat", "Achat",
			                                                                           "47e7d7e773", "47e7d7e772", new IdentifiantAffaireRF(23, 1997, 13, 0),
			                                                                           new Fraction(1, 3), GenrePropriete.COPROPRIETE, absorbeeRF, immeuble1, null);

			final DroitProprieteImmeubleRF droit2 = addDroitPropriete(immeuble0, immeuble1, GenrePropriete.FONDS_DOMINANT, new Fraction(12, 345),
			                                                          null, RegDate.get(2000, 1, 1), null, "Constitution PPE", null,
			                                                          new IdentifiantAffaireRF(123, 2000, 121, 2), "39393939", "1");
			ids.droit0 = droit0.getId();
			ids.droit1 = droit1.getId();
			ids.droit2 = droit2.getId();

			final Entreprise ctb = (Entreprise) tiersDAO.get(ids.absorbee);
			addRapprochementRF(ctb, absorbeeRF, RegDate.get(1990, 1, 1), null, TypeRapprochementRF.MANUEL);
			return null;
		});

		// on demande les droits sur l'entreprise absorbante
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final Entreprise ent = (Entreprise) tiersDAO.get(ids.absorbante);
				assertNotNull(ent);

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ent, true, true);
				droits.sort(new DroitRFRangeMetierComparator());
				assertEquals(3, droits.size());

				// le premier droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel0 = (DroitVirtuelHeriteRF) droits.get(0);
				assertEquals(dateFusion, droitVirtuel0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 20), droitVirtuel0.getDateFinMetier());
				assertEquals("Fusion", droitVirtuel0.getMotifDebut());
				assertEquals("Achat", droitVirtuel0.getMotifFin());

				// la référence du droit hérité
				final DroitProprietePersonneMoraleRF reference0 = (DroitProprietePersonneMoraleRF) droitVirtuel0.getReference();
				assertNull(reference0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, reference0.getRegime());
				assertEquals(new Fraction(1, 3), reference0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), reference0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), reference0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 23), reference0.getDateFin());
				assertEquals(RegDate.get(2010, 2, 20), reference0.getDateFinMetier());
				assertEquals("Achat", reference0.getMotifDebut());
				assertEquals("Achat", reference0.getMotifFin());
				assertEquals("47e7d7e773", reference0.getMasterIdRF());
				assertEquals("02faeee", reference0.getImmeuble().getIdRF());

				// le deuxième droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel1 = (DroitVirtuelHeriteRF) droits.get(1);
				assertEquals(RegDate.get(2004, 4, 12), droitVirtuel1.getDateDebutMetier());
				assertNull(droitVirtuel1.getDateFinMetier());
				assertEquals("Achat", droitVirtuel1.getMotifDebut());
				assertNull(droitVirtuel1.getMotifFin());

				// la référence du droit hérité
				final DroitProprietePersonneMoraleRF droit1 = (DroitProprietePersonneMoraleRF) droitVirtuel1.getReference();
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertNull(droit1.getDateFinMetier());
				assertEquals("Achat", droit1.getMotifDebut());
				assertNull(droit1.getMotifFin());
				assertEquals("48390a0e044", droit1.getMasterIdRF());
				assertEquals("01faeee", droit1.getImmeuble().getIdRF());

				// le troisième droit virtuel
				final DroitVirtuelHeriteRF droitVirtuel2 = (DroitVirtuelHeriteRF) droits.get(2);
				assertEquals(RegDate.get(2004, 4, 12), droitVirtuel2.getDateDebutMetier());
				assertNull(droitVirtuel2.getDateFinMetier());
				assertEquals("Achat", droitVirtuel2.getMotifDebut());
				assertNull(droitVirtuel2.getMotifFin());

				// la référence du droit hérité (lui-même un droit virtuel transitif)
				final DroitProprieteVirtuelRF droit2 = (DroitProprieteVirtuelRF) droitVirtuel2.getReference();
				assertNull(droit2.getCommunaute());
				assertNull(droit2.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit2.getDateDebutMetier());
				assertNull(droit2.getDateFin());
				assertNull(droit2.getDateFinMetier());
				assertEquals("Achat", droit2.getMotifDebut());
				assertNull(droit2.getMotifFin());
				assertNull(droit2.getMasterIdRF());
				assertEquals("02faeee", droit2.getImmeuble().getIdRF());
			}
		});
	}

	private ModeleCommunauteRF loadModelInTx(Long idPP1, Long idPP2) {
		AuthenticationHelper.pushPrincipal("test-user");
		try {
			return doInNewTransaction(status -> {
				final PersonnePhysiqueRF pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(idPP1);
				final PersonnePhysiqueRF pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.get(idPP2);
				final HashSet<PersonnePhysiqueRF> membres = new HashSet<>(Arrays.asList(pp1, pp2));

				// on demande le modèle
				final ModeleCommunauteRF modele = serviceRF.findOrCreateModeleCommunaute(membres);

				// on s'assure que c'est le bon
				assertNotNull(modele);
				assertEquals(ModeleCommunauteRF.hashCode(membres), modele.getMembresHashCode());
				final List<AyantDroitRF> membresModele = modele.getMembres().stream()
						.sorted(Comparator.comparing(AyantDroitRF::getId))
						.collect(Collectors.toList());
				assertNotNull(membresModele);
				assertEquals(2, membresModele.size());
				assertSame(pp1, membresModele.get(0));
				assertSame(pp2, membresModele.get(1));
				return modele;
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private static void assertDroitPropChemin(Long id, int numerateur, int denominateur, DroitRF chemin) {
		assertTrue(chemin instanceof DroitProprieteRF);
		assertEquals(id, chemin.getId());
		assertEquals(new Fraction(numerateur, denominateur), ((DroitProprieteRF) chemin).getPart());
	}

	private static void assertServitudeChemin(Long id, ServitudeRF chemin) {
		assertEquals(id, chemin.getId());
	}
}