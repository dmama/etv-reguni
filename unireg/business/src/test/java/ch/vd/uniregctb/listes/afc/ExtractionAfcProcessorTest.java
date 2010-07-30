package ch.vd.uniregctb.listes.afc;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

public class ExtractionAfcProcessorTest extends BusinessTest {

	private ExtractionAfcProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		processor = new ExtractionAfcProcessor(hibernateTemplate, transactionManager, tiersService, serviceCivil, tiersDAO, serviceInfra);
	}

	@Test
	public void testRevenuRoleVaudois() throws Exception {

		final long noIndOrdinaire = 6341423L;
		final long noIndMixte1 = 6341424L;
		final long noIndMixte2 = 6341425L;
		final long noIndIndigent = 6341426L;
		final long noIndDepense = 6341427L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndOrdinaire, date(1965, 2, 21), "Ordinaire", "Toto", true);
				addIndividu(noIndMixte1, date(1965, 2, 21), "MixteUn", "Toto", true);
				addIndividu(noIndMixte2, date(1965, 2, 21), "MixteDeux", "Toto", true);
				addIndividu(noIndIndigent, date(1965, 2, 21), "Indigent", "Toto", true);
				addIndividu(noIndDepense, date(1965, 2, 21), "Dépense", "Toto", true);
			}
		});

		final class Ids {
			long idOrdinaire;
			long idMixte1;
			long idMixte2;
			long idIndigent;
			long idDepense;
		}
		final Ids ids = new Ids();

		// mise en place fiscale
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique ppOrd = addHabitant(noIndOrdinaire);
				addForPrincipal(ppOrd, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				final PersonnePhysique ppMixte1 = addHabitant(noIndMixte1);
				addForPrincipal(ppMixte1, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Aubonne);
				final PersonnePhysique ppMixte2 = addHabitant(noIndMixte2);
				addForPrincipal(ppMixte2, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.CheseauxSurLausanne);
				final PersonnePhysique ppIndigent = addHabitant(noIndIndigent);
				addForPrincipal(ppIndigent, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				final PersonnePhysique ppDepense = addHabitant(noIndDepense);
				addForPrincipal(ppDepense, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);

				ids.idOrdinaire = ppOrd.getNumero();
				ids.idMixte1 = ppMixte1.getNumero();
				ids.idMixte2 = ppMixte2.getNumero();
				ids.idIndigent = ppIndigent.getNumero();
				ids.idDepense = ppDepense.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(5, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idOrdinaire, elt.noCtb);
			Assert.assertEquals("Toto Ordinaire", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte1, elt.noCtb);
			Assert.assertEquals("Toto MixteUn", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(2);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte2, elt.noCtb);
			Assert.assertEquals("Toto MixteDeux", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(3);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idIndigent, elt.noCtb);
			Assert.assertEquals("Toto Indigent", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(4);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idDepense, elt.noCtb);
			Assert.assertEquals("Toto Dépense", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}

	@Test
	public void testFortuneRoleVaudois() throws Exception {

		final long noIndOrdinaire = 6341423L;
		final long noIndMixte1 = 6341424L;
		final long noIndMixte2 = 6341425L;
		final long noIndIndigent = 6341426L;
		final long noIndDepense = 6341427L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndOrdinaire, date(1965, 2, 21), "Ordinaire", "Toto", true);
				addIndividu(noIndMixte1, date(1965, 2, 21), "MixteUn", "Toto", true);
				addIndividu(noIndMixte2, date(1965, 2, 21), "MixteDeux", "Toto", true);
				addIndividu(noIndIndigent, date(1965, 2, 21), "Indigent", "Toto", true);
				addIndividu(noIndDepense, date(1965, 2, 21), "Dépense", "Toto", true);
			}
		});

		final class Ids {
			long idOrdinaire;
			long idMixte1;
			long idMixte2;
			long idIndigent;
			long idDepense;
		}
		final Ids ids = new Ids();

		// mise en place fiscale
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique ppOrd = addHabitant(noIndOrdinaire);
				addForPrincipal(ppOrd, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				final PersonnePhysique ppMixte1 = addHabitant(noIndMixte1);
				addForPrincipal(ppMixte1, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Aubonne);
				final PersonnePhysique ppMixte2 = addHabitant(noIndMixte2);
				addForPrincipal(ppMixte2, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.CheseauxSurLausanne);
				final PersonnePhysique ppIndigent = addHabitant(noIndIndigent);
				addForPrincipal(ppIndigent, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				final PersonnePhysique ppDepense = addHabitant(noIndDepense);
				addForPrincipal(ppDepense, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);

				ids.idOrdinaire = ppOrd.getNumero();
				ids.idMixte1 = ppMixte1.getNumero();
				ids.idMixte2 = ppMixte2.getNumero();
				ids.idIndigent = ppIndigent.getNumero();
				ids.idDepense = ppDepense.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(5, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idOrdinaire, elt.noCtb);
			Assert.assertEquals("Toto Ordinaire", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte1, elt.noCtb);
			Assert.assertEquals("Toto MixteUn", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(2);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte2, elt.noCtb);
			Assert.assertEquals("Toto MixteDeux", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(3);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idIndigent, elt.noCtb);
			Assert.assertEquals("Toto Indigent", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(4);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idDepense, elt.noCtb);
			Assert.assertEquals("Toto Dépense", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}

	@Test
	public void testRevenuNonAssujettiDuTout() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HC, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
	}

	@Test
	public void testFortuneNonAssujettiDuTout() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HC, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
	}

	@Test
	public void testRevenuNonAssujettiFinAnnee() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
	}

	@Test
	public void testFortuneNonAssujettiFinAnnee() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
	}

	@Test
	public void testRevenuSourcierPur() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas extrait par la requête initiale
		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneSourcierPur() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas extrait par la requête initiale
		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testRevenuHorsCanton() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Hors canton", elt.raisonIgnore);
	}

	@Test
	public void testFortuneHorsCanton() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(1, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListeSecondaire().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
		Assert.assertEquals(MockCommune.Croy.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
	}

	@Test
	public void testRevenuHorsSuisse() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
	}

	@Test
	public void testFortuneHorsSuisse() throws Exception {

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getListePrincipale().size());
		Assert.assertEquals(1, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListeSecondaire().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
	}

	@Test
	public void testRevenuCouple() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
			}
		});

		// mise en place
		final long mcId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				return mc.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(mcId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);      // principal seulement
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);

	}

	@Test
	public void testFortuneCouple() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
			}
		});

		// mise en place
		final long mcId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				return mc.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(mcId, elt.noCtb);
		Assert.assertEquals("Toto Tartempion", elt.nomPrenom);      // principal seulement
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);

	}

	@Test
	public void testRevenuCoupleDivorceDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
				divorceIndividus(toto, tata, date(2008, 6, 30));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 6, 30));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 6, 30), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				addForPrincipal(toto, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
				addForPrincipal(tata, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.CheseauxSurLausanne);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idToto, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
	}


	@Test
	public void testFortuneCoupleDivorceDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
				divorceIndividus(toto, tata, date(2008, 6, 30));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 6, 30));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 6, 30), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				addForPrincipal(toto, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
				addForPrincipal(tata, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.CheseauxSurLausanne);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idToto, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
	}

	@Test
	public void testRevenuCoupleVeuvageDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
				toto.setDateDeces(date(2008, 10, 24));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 10, 24));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 10, 24), MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				addForPrincipal(tata, date(2008, 10, 25), MotifFor.VEUVAGE_DECES, MockCommune.CheseauxSurLausanne);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}

	@Test
	public void testFortuneCoupleVeuvageDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2000, 4, 1));
				toto.setDateDeces(date(2008, 10, 24));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 10, 24));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 10, 24), MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				addForPrincipal(tata, date(2008, 10, 25), MotifFor.VEUVAGE_DECES, MockCommune.CheseauxSurLausanne);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
	}

	@Test
	public void testRevenuCoupleMariageDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2008, 4, 1));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2008, 4, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(toto, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(tata, date(2006, 6, 15), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.CheseauxSurLausanne);
				addForPrincipal(mc, date(2008, 4, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(2, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idToto, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
	}

	@Test
	public void testFortuneCoupleMariageDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempion", "Tata", false);
				marieIndividus(toto, tata, date(2008, 4, 1));
			}
		});

		final class Ids {
			long idToto;
			long idTata;
			long idMenage;
		}
		final Ids ids = new Ids();

		// mise en place
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2008, 4, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(toto, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(tata, date(2006, 6, 15), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.CheseauxSurLausanne);
				addForPrincipal(mc, date(2008, 4, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();

				return null;
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(2, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idToto, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
		{
			final ExtractionAfcResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idTata, elt.noCtb);
			Assert.assertEquals("Non-assujetti ou sourcier pur", elt.raisonIgnore);
		}
	}

	@Test
	public void testRevenuFractionCommune() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Fraction.LesCharbonnieres);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.REVENU, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.LeLieu.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}

	@Test
	public void testFortuneFractionCommune() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Fraction.LesCharbonnieres);
				return pp.getNumero();
			}
		});

		final ExtractionAfcResults res = processor.run(RegDate.get(), 2008, TypeExtractionAfc.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.getListePrincipale().size());
		Assert.assertEquals(0, res.getListeSecondaire().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionAfcResults.InfoCtbListe elt = res.getListePrincipale().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto Tartempion", elt.nomPrenom);
			Assert.assertEquals(MockCommune.LeLieu.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}
}
