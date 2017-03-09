package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RegistreFoncierServiceTest extends BusinessTest {

	private RegistreFoncierService serviceRF;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
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

			addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, "Achat", null,
			                           "48390a0e044", new IdentifiantAffaireRF(123, 2004, 202, 3),
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiersRF, immeuble0, null);

			addDroitPersonnePhysiqueRF(RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), "Achat", "Achat",
			                           "47e7d7e773", new IdentifiantAffaireRF(23, 1997, 13, 0),
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

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb);
				assertEquals(2, droits.size());

				final DroitProprietePersonnePhysiqueRF droit0 = (DroitProprietePersonnePhysiqueRF) droits.get(0);
				assertNull(droit0.getCommunaute());
				assertEquals(GenrePropriete.COPROPRIETE, droit0.getRegime());
				assertEquals(new Fraction(1, 3), droit0.getPart());
				assertEquals(RegDate.get(1997, 10, 7), droit0.getDateDebut());
				assertEquals(RegDate.get(1997, 7, 2), droit0.getDateDebutMetier());
				assertEquals(RegDate.get(2010, 2, 23), droit0.getDateFin());
				assertEquals("Achat", droit0.getMotifDebut());
				assertEquals("Achat", droit0.getMotifFin());
				assertEquals(new IdentifiantAffaireRF(23, 1997, 13, 0), droit0.getNumeroAffaire());
				assertEquals("47e7d7e773", droit0.getMasterIdRF());
				assertEquals("02faeee", droit0.getImmeuble().getIdRF());

				final DroitProprietePersonnePhysiqueRF droit1 = (DroitProprietePersonnePhysiqueRF) droits.get(1);
				assertNull(droit1.getCommunaute());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit1.getRegime());
				assertEquals(new Fraction(1, 1), droit1.getPart());
				assertEquals(RegDate.get(2004, 5, 21), droit1.getDateDebut());
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutMetier());
				assertNull(droit1.getDateFin());
				assertEquals("Achat", droit1.getMotifDebut());
				assertNull(droit1.getMotifFin());
				assertEquals(new IdentifiantAffaireRF(123, 2004, 202, 3), droit1.getNumeroAffaire());
				assertEquals("48390a0e044", droit1.getMasterIdRF());
				assertEquals("01faeee", droit1.getImmeuble().getIdRF());
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
			                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044");

			addDroitPropriete(tiersRF1, immeuble1, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2008, 3, 1), null, RegDate.get(2008, 3, 1), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2008, 2, 23), "4782389c8e");

			addDroitPropriete(tiersRF1, immeuble2, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(1997, 10, 7), RegDate.get(2002, 2, 23), RegDate.get(1997, 7, 2), null, "Achat", "Vente",
			                  new IdentifiantAffaireRF(23, 1997, 13, 0), "47e7d7e773");

			final PersonnePhysiqueRF tiersRF2 = addPersonnePhysiqueRF("38383830ae3ff", "Charles", "Widmer", date(1970, 7, 2));
			addRapprochementRF(ctb, tiersRF2, RegDate.get(2006, 1, 1), null, TypeRapprochementRF.MANUEL);

			addDroitPropriete(tiersRF2, immeuble3, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(1960, 2, 7), RegDate.get(1994, 4, 5), RegDate.get(1960, 2, 7), null, "Achat", "Expropriation",
			                  new IdentifiantAffaireRF(123, 1960, 4, 3), "48392345234234");

			addDroitPropriete(tiersRF2, immeuble4, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2003, 5, 1), null, RegDate.get(2003, 5, 1), null, "Achat", "Vente",
			                  new IdentifiantAffaireRF(123, 2003, 22, 7), "834838c");

			addDroitPropriete(tiersRF2, immeuble5, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(2010, 6, 6), null, RegDate.get(2010, 6, 6), null, "Achat", null,
			                  new IdentifiantAffaireRF(23, 2010, 33, 30), "c83839e");

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

				final List<DroitRF> droits = serviceRF.getDroitsForCtb(ctb);
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
				assertEquals(new IdentifiantAffaireRF(23, 1997, 13, 0), droit0.getNumeroAffaire());
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
				assertEquals(new IdentifiantAffaireRF(123, 2003, 22, 7), droit1.getNumeroAffaire());
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
				assertEquals(new IdentifiantAffaireRF(123, 2004, 202, 3), droit2.getNumeroAffaire());
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
				assertEquals(new IdentifiantAffaireRF(23, 2010, 33, 30), droit3.getNumeroAffaire());
				assertEquals("c83839e", droit3.getMasterIdRF());
				assertEquals("06faeee", droit3.getImmeuble().getIdRF());
			}
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
}