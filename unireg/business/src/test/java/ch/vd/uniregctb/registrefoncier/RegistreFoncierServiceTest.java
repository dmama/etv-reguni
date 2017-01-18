package ch.vd.uniregctb.registrefoncier;

import java.util.List;

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
			public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
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

			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("38383830ae3ff", "Charles", "Widmer", date(1970, 7, 2));

			addDroitPropriete(tiersRF, immeuble0, null,
			                  GenrePropriete.INDIVIDUELLE, new Fraction(1, 1),
			                  RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, "Achat", null,
			                  new IdentifiantAffaireRF(123, 2004, 202, 3), "48390a0e044");

			addDroitPropriete(tiersRF, immeuble1, null,
			                  GenrePropriete.COPROPRIETE, new Fraction(1, 3),
			                  RegDate.get(1997, 10, 7), RegDate.get(1997, 7, 2), RegDate.get(2010, 2, 23), "Achat", "Achat",
			                  new IdentifiantAffaireRF(23, 1997, 13, 0), "47e7d7e773");

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
				assertEquals(RegDate.get(1997, 7, 2), droit0.getDateDebutOfficielle());
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
				assertEquals(RegDate.get(2004, 4, 12), droit1.getDateDebutOfficielle());
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
}