package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase.assertRaisonAcquisition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RegistreFoncierServiceTest extends BusinessTest {

	private AyantDroitRFDAO ayantDroitRFDAO;
	private RegistreFoncierService serviceRF;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		serviceRF = getBean(RegistreFoncierService.class, "serviceRF");

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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "some egrid", gland, 4298);

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

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, false);
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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "some egrid", gland, 4298);
			final BienFondRF immeuble2 = addBienFondRF("03faeee", "some egrid", laSarraz, 580);
			final BienFondRF immeuble3 = addBienFondRF("04faeee", "some egrid", gland, 4299);
			final BienFondRF immeuble4 = addBienFondRF("05faeee", "some egrid", laSarraz, 581);
			final BienFondRF immeuble5 = addBienFondRF("06faeee", "some egrid", gland, 4300);

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

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb, false);
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
	 * Vérifie que les droits virtuels sont bien calculés même s'il n'y a pas de droits entre immeubles.
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
	public void testGetDroitsVirtuelsForTiersRFCasSansDroitEntreImmeuble() throws Exception {

		// mise en place foncière
		final Long tiersId = doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "CHE1", gland, 4298);

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
	public void testGetDroitsVirtuelsForTiersRFCasAvecUnDroitEntreDeuxImmeubles() throws Exception {

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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "CHE1", gland, 4298);

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
			// droit retournés :
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

			final DroitProprieteRFVirtuel droitVirtuel1 = (DroitProprieteRFVirtuel) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertNull(droitVirtuel1.getRegime());
			assertNull(droitVirtuel1.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitProprieteRF> chemin = droitVirtuel1.getChemin();                                // tiers RF -> immeuble 0 -> immeuble 1
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
	public void testGetDroitsVirtuelsForTiersRFCasAvecImmeublePossedeParDeuxAutresImmeubles() throws Exception {

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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "CHE1", laSarraz, 4298);
			final BienFondRF immeuble2 = addBienFondRF("03faeee", "CHE2", laSarraz, 4299);

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
			// droit retournés :
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

			final DroitProprieteRFVirtuel droitVirtuel1 = (DroitProprieteRFVirtuel) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertNull(droitVirtuel1.getRegime());
			assertNull(droitVirtuel1.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble2
			final List<DroitProprieteRF> chemin1 = droitVirtuel1.getChemin();
			assertEquals(2, chemin1.size());
			assertChemin(ids.droit0, 1, 1, chemin1.get(0));                         // tiers RF -> immeuble0
			assertChemin(ids.droit2, 17, 50, chemin1.get(1));                       // immeuble0 -> immeuble 2

			final DroitProprietePersonnePhysiqueRF droitReel2 = (DroitProprietePersonnePhysiqueRF) droits.get(2);
			assertNull(droitReel2.getCommunaute());
			assertEquals(GenrePropriete.COPROPRIETE, droitReel2.getRegime());
			assertEquals(new Fraction(1, 3), droitReel2.getPart());
			assertEquals(RegDate.get(2008, 3, 1), droitReel2.getDateDebutMetier());
			assertEquals("Achat", droitReel2.getMotifDebut());
			assertNull(droitReel2.getMotifFin());
			assertEquals("4782389c8e", droitReel2.getMasterIdRF());
			assertEquals("02faeee", droitReel2.getImmeuble().getIdRF());

			final DroitProprieteRFVirtuel droitVirtuel3 = (DroitProprieteRFVirtuel) droits.get(3);
			assertNull(droitVirtuel3.getCommunaute());
			assertNull(droitVirtuel3.getRegime());
			assertNull(droitVirtuel3.getPart());
			assertEquals(RegDate.get(2008, 3, 1), droitVirtuel3.getDateDebutMetier());   // date d'achat du droit1
			assertEquals("Achat", droitVirtuel3.getMotifDebut());
			assertNull(droitVirtuel3.getMotifFin());
			assertNull(droitVirtuel3.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel3.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel3.getImmeuble().getIdRF());                        // l'immeuble2
			final List<DroitProprieteRF> chemin3 = droitVirtuel3.getChemin();
			assertEquals(2, chemin3.size());
			assertChemin(ids.droit1, 1, 3, chemin3.get(0));                         // tiers RF -> immeuble1
			assertChemin(ids.droit3, 23, 50, chemin3.get(1));                       // immeuble1 -> immeuble 2

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
	public void testGetDroitsVirtuelsForTiersRFCasAvecTroisImmeublesQuiSePossedentEnCascade() throws Exception {

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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "CHE1", laSarraz, 4298);
			final BienFondRF immeuble2 = addBienFondRF("03faeee", "CHE2", laSarraz, 4299);

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
			// droit retournés :
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

			final DroitProprieteRFVirtuel droitVirtuel1 = (DroitProprieteRFVirtuel) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertNull(droitVirtuel1.getRegime());
			assertNull(droitVirtuel1.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitProprieteRF> chemin1 = droitVirtuel1.getChemin();
			assertEquals(2, chemin1.size());
			assertChemin(ids.droit0, 1, 3, chemin1.get(0));                         // tiers RF -> immeuble0
			assertChemin(ids.droit1, 1, 5, chemin1.get(1));                         // immeuble0 -> immeuble1

			final DroitProprieteRFVirtuel droitVirtuel2 = (DroitProprieteRFVirtuel) droits.get(2);
			assertNull(droitVirtuel2.getCommunaute());
			assertNull(droitVirtuel2.getRegime());
			assertNull(droitVirtuel2.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel2.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel2.getMotifDebut());
			assertNull(droitVirtuel2.getMotifFin());
			assertNull(droitVirtuel2.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel2.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("03faeee", droitVirtuel2.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitProprieteRF> chemin2 = droitVirtuel2.getChemin();
			assertEquals(3, chemin2.size());
			assertChemin(ids.droit0, 1, 3, chemin2.get(0));                         // tiers RF -> immeuble0
			assertChemin(ids.droit1, 1, 5, chemin2.get(1));                         // immeuble0 -> immeuble1
			assertChemin(ids.droit2, 33, 100, chemin2.get(2));                      // immeuble1 -> immeuble2

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
	public void testGetDroitsVirtuelsForTiersRFCasAvecDeuxImmeublesEtCycle() throws Exception {

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
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "CHE0", laSarraz, 579);
			final BienFondRF immeuble1 = addBienFondRF("02faeee", "CHE1", laSarraz, 4298);

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
			// droit retournés :
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

			final DroitProprieteRFVirtuel droitVirtuel1 = (DroitProprieteRFVirtuel) droits.get(1);
			assertNull(droitVirtuel1.getCommunaute());
			assertNull(droitVirtuel1.getRegime());
			assertNull(droitVirtuel1.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel1.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel1.getMotifDebut());
			assertNull(droitVirtuel1.getMotifFin());
			assertNull(droitVirtuel1.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel1.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("01faeee", droitVirtuel1.getImmeuble().getIdRF());                        // l'immeuble0
			final List<DroitProprieteRF> chemin2 = droitVirtuel1.getChemin();
			assertEquals(3, chemin2.size());
			assertChemin(ids.droit0, 1, 3, chemin2.get(0));                         // tiers RF -> immeuble0
			assertChemin(ids.droit1, 1, 5, chemin2.get(1));                         // immeuble0 -> immeuble1
			assertChemin(ids.droit2, 4, 7, chemin2.get(2));                         // immeuble1 -> immeuble0

			final DroitProprieteRFVirtuel droitVirtuel2 = (DroitProprieteRFVirtuel) droits.get(2);
			assertNull(droitVirtuel2.getCommunaute());
			assertNull(droitVirtuel2.getRegime());
			assertNull(droitVirtuel2.getPart());
			assertEquals(RegDate.get(2004, 5, 21), droitVirtuel2.getDateDebutMetier());   // date d'achat du droit0
			assertEquals("Achat", droitVirtuel2.getMotifDebut());
			assertNull(droitVirtuel2.getMotifFin());
			assertNull(droitVirtuel2.getMasterIdRF());
			assertEquals("78838e838ca92", droitVirtuel2.getAyantDroit().getIdRF());                // le tiers RF
			assertEquals("02faeee", droitVirtuel2.getImmeuble().getIdRF());                        // l'immeuble1
			final List<DroitProprieteRF> chemin1 = droitVirtuel2.getChemin();
			assertEquals(2, chemin1.size());
			assertChemin(ids.droit0, 1, 3, chemin1.get(0));                         // tiers RF -> immeuble0
			assertChemin(ids.droit1, 1, 5, chemin1.get(1));                         // immeuble0 -> immeuble1

			return null;
		});
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que la résolution des URLs vers Capitastra fonctionne correctement.
	 */
	@Test
	public void testGetCapitastraURL() throws Exception {

		class Ids {
			long bienFond;
			long ppe;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "some egrid", laSarraz, 579);
			final ProprieteParEtageRF immeuble1 = addProprieteParEtageRF("02faeee", "some egrid", new Fraction(1, 4), gland, 4298, 3, null, null);

			ids.bienFond = immeuble0.getId();
			ids.ppe = immeuble1.getId();
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=61&kr=0&n1=579&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.bienFond));
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
			long bienFond;
			long ppe;
		}
		final Ids ids = new Ids();

		// mise en place foncière
		doInNewTransaction(status -> {

			// un tiers RF avec deux immeubles
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final CommuneRF gland = addCommuneRF(242, "Gland", 5721);
			final BienFondRF immeuble0 = addBienFondRF("01faeee", "some egrid", laSarraz, 579);
			immeuble0.getSituations().iterator().next().setDateFin(RegDate.get(2015, 12, 31));
			final ProprieteParEtageRF immeuble1 = addProprieteParEtageRF("02faeee", "some egrid", new Fraction(1, 4), gland, 4298, 3, null, null);

			ids.bienFond = immeuble0.getId();
			ids.ppe = immeuble1.getId();
			return null;
		});

		// appel du service
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=61&kr=0&n1=579&n2=&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.bienFond));
				assertEquals("https://secure.vd.ch/territoire/intercapi/faces?bfs=242&kr=0&n1=4298&n2=3&n3=&n4=&type=grundstueck_grundbuch_auszug", serviceRF.getCapitastraURL(ids.ppe));
			}
		});
	}

	@Test
	public void testGetCommune() throws Exception {
		// mise en place fiscale
		final long idImmeuble = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
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
				Assert.assertSame(commune1990, commune2001);
			}
		});
	}

	@Test
	public void testNumeroParcelleComplet() throws Exception {
		// mise en place fiscale
		final long idImmeuble = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
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
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
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
				Assert.assertSame(situation, situ1990);

				// après la date de début
				final SituationRF situ2001 = serviceRF.getSituation(immeuble, date(2001, 1, 1));
				Assert.assertNotNull(situ2001);
				Assert.assertSame(situation, situ2001);
			}
		});
	}

	private static void assertChemin(Long id, int numerateur, int denominateur, DroitProprieteRF chemin) {
		assertEquals(id, chemin.getId());
		assertEquals(new Fraction(numerateur, denominateur), chemin.getPart());
	}
}