package ch.vd.uniregctb.listes.afc;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ExtractionDonneesRptProcessorTest extends BusinessTest {

	private ExtractionDonneesRptProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		processor = new ExtractionDonneesRptProcessor(hibernateTemplate, transactionManager, tiersService, serviceCivilCacheWarmer, tiersDAO, serviceInfra, assujettissementService,
				periodeImpositionService);
	}

	@Test
	public void testRevenuOrdinaireContribuableVaudois() throws Exception {

		final long noIndOrdinaire = 6341423L;
		final long noIndMixte1 = 6341424L;
		final long noIndMixte2 = 6341425L;
		final long noIndIndigent = 6341426L;
		final long noIndDepense = 6341427L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ord = addIndividu(noIndOrdinaire, date(1965, 2, 21), "Ordinaire", "Toto", true);
				ord.setNouveauNoAVS("7562025802593");

				final MockIndividu m1 = addIndividu(noIndMixte1, date(1965, 2, 22), "MixteUn", "Toto", true);
				m1.setNouveauNoAVS("7568935457472");

				final MockIndividu m2 = addIndividu(noIndMixte2, date(1965, 2, 23), "MixteDeux", "Toto", true);
				m2.setNouveauNoAVS("7568700351431");

				final MockIndividu ind = addIndividu(noIndIndigent, date(1965, 2, 24), "Indigent", "Toto", true);
				ind.setNouveauNoAVS("7569528331315");

				final MockIndividu dep = addIndividu(noIndDepense, date(1965, 2, 25), "Dépense", "Toto", true);
				dep.setNouveauNoAVS("7567902948722");
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
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique ppOrd = addHabitant(noIndOrdinaire);
				addForPrincipal(ppOrd, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				final PersonnePhysique ppMixte1 = addHabitant(noIndMixte1);
				final ForFiscalPrincipal ffpMixte1 = addForPrincipal(ppMixte1, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Aubonne);
				ffpMixte1.setModeImposition(ModeImposition.MIXTE_137_1);
				final PersonnePhysique ppMixte2 = addHabitant(noIndMixte2);
				final ForFiscalPrincipal ffpMixte2 = addForPrincipal(ppMixte2, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.CheseauxSurLausanne);
				ffpMixte2.setModeImposition(ModeImposition.MIXTE_137_2);
				final PersonnePhysique ppIndigent = addHabitant(noIndIndigent);
				final ForFiscalPrincipal ffpIndigent = addForPrincipal(ppIndigent, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ffpIndigent.setModeImposition(ModeImposition.INDIGENT);
				final PersonnePhysique ppDepense = addHabitant(noIndDepense);
				final ForFiscalPrincipal ffpDepense = addForPrincipal(ppDepense, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
				ffpDepense.setModeImposition(ModeImposition.DEPENSE);

				ids.idOrdinaire = ppOrd.getNumero();
				ids.idMixte1 = ppMixte1.getNumero();
				ids.idMixte2 = ppMixte2.getNumero();
				ids.idIndigent = ppIndigent.getNumero();
				ids.idDepense = ppDepense.getNumero();

				return null;
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(5, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idOrdinaire, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Ordinaire", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte1, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("MixteUn", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 22), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.MIXTE_137_1, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(2);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte2, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("MixteDeux", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 23), elt.identification.dateNaissance);
			Assert.assertEquals("7568700351431", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.MIXTE_137_2, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(3);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idIndigent, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Indigent", elt.identification.nom);
			Assert.assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 24), elt.identification.dateNaissance);
			Assert.assertEquals("7569528331315", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.INDIGENT, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(4);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idDepense, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Dépense", elt.identification.nom);
			Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 25), elt.identification.dateNaissance);
			Assert.assertEquals("7567902948722", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.DEPENSE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}
	@Test
	public void testRevenuSourcePureContribuableVaudois() throws Exception {

		final long noIndOrdinaire = 6341423L;
		final long noIndMixte1 = 6341424L;
		final long noIndMixte2 = 6341425L;
		final long noIndIndigent = 6341426L;
		final long noIndDepense = 6341427L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ord = addIndividu(noIndOrdinaire, date(1965, 2, 21), "Ordinaire", "Toto", true);
				ord.setNouveauNoAVS("7562025802593");

				final MockIndividu m1 = addIndividu(noIndMixte1, date(1965, 2, 22), "MixteUn", "Toto", true);
				m1.setNouveauNoAVS("7568935457472");

				final MockIndividu m2 = addIndividu(noIndMixte2, date(1965, 2, 23), "MixteDeux", "Toto", true);
				m2.setNouveauNoAVS("7568700351431");

				final MockIndividu ind = addIndividu(noIndIndigent, date(1965, 2, 24), "Indigent", "Toto", true);
				ind.setNouveauNoAVS("7569528331315");

				final MockIndividu dep = addIndividu(noIndDepense, date(1965, 2, 25), "Dépense", "Toto", true);
				dep.setNouveauNoAVS("7567902948722");
			}
		});

		// mise en place fiscale
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique ppOrd = addHabitant(noIndOrdinaire);
				addForPrincipal(ppOrd, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				final PersonnePhysique ppMixte1 = addHabitant(noIndMixte1);
				final ForFiscalPrincipal ffpMixte1 = addForPrincipal(ppMixte1, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Aubonne);
				ffpMixte1.setModeImposition(ModeImposition.MIXTE_137_1);
				final PersonnePhysique ppMixte2 = addHabitant(noIndMixte2);
				final ForFiscalPrincipal ffpMixte2 = addForPrincipal(ppMixte2, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.CheseauxSurLausanne);
				ffpMixte2.setModeImposition(ModeImposition.MIXTE_137_2);
				final PersonnePhysique ppIndigent = addHabitant(noIndIndigent);
				final ForFiscalPrincipal ffpIndigent = addForPrincipal(ppIndigent, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ffpIndigent.setModeImposition(ModeImposition.INDIGENT);
				final PersonnePhysique ppDepense = addHabitant(noIndDepense);
				final ForFiscalPrincipal ffpDepense = addForPrincipal(ppDepense, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
				ffpDepense.setModeImposition(ModeImposition.DEPENSE);
				return null;
			}
		});

		// même pas pris en compte par la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneContribuableVaudois() throws Exception {

		final long noIndOrdinaire = 6341423L;
		final long noIndMixte1 = 6341424L;
		final long noIndMixte2 = 6341425L;
		final long noIndIndigent = 6341426L;
		final long noIndDepense = 6341427L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ord = addIndividu(noIndOrdinaire, date(1965, 2, 21), "Ordinaire", "Toto", true);
				ord.setNouveauNoAVS("7562025802593");

				final MockIndividu m1 = addIndividu(noIndMixte1, date(1965, 2, 22), "MixteUn", "Toto", true);
				m1.setNouveauNoAVS("7568935457472");

				final MockIndividu m2 = addIndividu(noIndMixte2, date(1965, 2, 23), "MixteDeux", "Toto", true);
				m2.setNouveauNoAVS("7568700351431");

				final MockIndividu ind = addIndividu(noIndIndigent, date(1965, 2, 24), "Indigent", "Toto", true);
				ind.setNouveauNoAVS("7569528331315");

				final MockIndividu dep = addIndividu(noIndDepense, date(1965, 2, 25), "Dépense", "Toto", true);
				dep.setNouveauNoAVS("7567902948722");
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
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {

				final PersonnePhysique ppOrd = addHabitant(noIndOrdinaire);
				addForPrincipal(ppOrd, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				final PersonnePhysique ppMixte1 = addHabitant(noIndMixte1);
				final ForFiscalPrincipal ffpMixte1 = addForPrincipal(ppMixte1, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Aubonne);
				ffpMixte1.setModeImposition(ModeImposition.MIXTE_137_1);
				final PersonnePhysique ppMixte2 = addHabitant(noIndMixte2);
				final ForFiscalPrincipal ffpMixte2 = addForPrincipal(ppMixte2, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.CheseauxSurLausanne);
				ffpMixte2.setModeImposition(ModeImposition.MIXTE_137_2);
				final PersonnePhysique ppIndigent = addHabitant(noIndIndigent);
				final ForFiscalPrincipal ffpIndigent = addForPrincipal(ppIndigent, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Cossonay);
				ffpIndigent.setModeImposition(ModeImposition.INDIGENT);
				final PersonnePhysique ppDepense = addHabitant(noIndDepense);
				final ForFiscalPrincipal ffpDepense = addForPrincipal(ppDepense, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Echallens);
				ffpDepense.setModeImposition(ModeImposition.DEPENSE);

				ids.idOrdinaire = ppOrd.getNumero();
				ids.idMixte1 = ppMixte1.getNumero();
				ids.idMixte2 = ppMixte2.getNumero();
				ids.idIndigent = ppIndigent.getNumero();
				ids.idDepense = ppDepense.getNumero();

				return null;
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(5, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idOrdinaire, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Ordinaire", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertFalse(elt.limite);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte1, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("MixteUn", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 22), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.MIXTE_137_1, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertFalse(elt.limite);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(2);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idMixte2, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("MixteDeux", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 23), elt.identification.dateNaissance);
			Assert.assertEquals("7568700351431", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.MIXTE_137_2, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertFalse(elt.limite);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(3);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idIndigent, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Indigent", elt.identification.nom);
			Assert.assertEquals(MockCommune.Cossonay.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 24), elt.identification.dateNaissance);
			Assert.assertEquals("7569528331315", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.INDIGENT, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertFalse(elt.limite);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(4);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ids.idDepense, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Dépense", elt.identification.nom);
			Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 25), elt.identification.dateNaissance);
			Assert.assertEquals("7567902948722", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.DEPENSE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertFalse(elt.limite);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testRevenuSourceContribuableSourcierPur() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas vu dans l'extraction initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFS(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.SOURCE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HC, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HC, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
		Assert.assertNull(elt.identification.numeroAvs);
		Assert.assertNull(elt.identification.noCtbPrincipal);
		Assert.assertNull(elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertEquals(MotifFor.DEPART_HS, elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 5, 12), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 5, 12), MotifFor.DEPART_HS, MockCommune.Bussigny);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Non-assujetti au rôle ordinaire au 31 décembre", elt.raisonIgnore);
	}

	@Test
	public void testRevenuOrdinaireContribuableSourcierPur() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas extrait par la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneContribuableSourcierPur() throws Exception {

		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);
				ffp.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas extrait par la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testRevenuHorsCanton() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Croy.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
		Assert.assertNull(elt.identification.numeroAvs);
		Assert.assertNull(elt.identification.noCtbPrincipal);
		Assert.assertNull(elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertNull(elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, elt.autoriteFiscaleForPrincipal);
	}

	@Test
	public void testFortuneHorsCanton() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Croy.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, elt.autoriteFiscaleForPrincipal);
		Assert.assertTrue(elt.limite);
	}

	@Test
	public void testRevenuHorsCantonVenteDernierImmeubleDansPeriode() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 11, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Croy.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
		Assert.assertNull(elt.identification.numeroAvs);
		Assert.assertNull(elt.identification.noCtbPrincipal);
		Assert.assertNull(elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, elt.autoriteFiscaleForPrincipal);
	}

	@Test
	public void testFortuneHorsCantonVenteDernierImmeubleDansPeriode() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 11, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Assujetti sans for vaudois au 31 décembre", elt.raisonIgnore);
	}

	@Test
	public void testRevenuHorsSuisse() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
		Assert.assertNull(elt.identification.numeroAvs);
		Assert.assertNull(elt.identification.noCtbPrincipal);
		Assert.assertNull(elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertNull(elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, elt.autoriteFiscaleForPrincipal);
	}

	@Test
	public void testFortuneHorsSuisse() throws Exception {

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1965, 2, 21), Sexe.MASCULIN);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals(ppId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);
		Assert.assertEquals("Tartempion", elt.identification.nom);
		Assert.assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
		Assert.assertNull(elt.identification.numeroAvs);
		Assert.assertNull(elt.identification.noCtbPrincipal);
		Assert.assertNull(elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertNull(elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, elt.autoriteFiscaleForPrincipal);
		Assert.assertTrue(elt.limite);
	}

	@Test
	public void testRevenuCouple() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
			}
		});

		final class Ids {
			Long mcId;
			Long mId;
			Long mmeId;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);

				final Ids ids = new Ids();
				ids.mcId = mc.getNumero();
				ids.mId = toto.getNumero();
				ids.mmeId = tata.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.mcId);
		Assert.assertNotNull(ids.mId);
		Assert.assertNotNull(ids.mmeId);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals((long) ids.mcId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);      // principal seulement
		Assert.assertEquals("Tartempion", elt.identification.nom);      // principal seulement
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);  // principal seulement
		Assert.assertEquals("7562025802593", elt.identification.numeroAvs);        // principal seulement
		Assert.assertEquals(ids.mId, elt.identification.noCtbPrincipal);
		Assert.assertEquals(ids.mmeId, elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertNull(elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
	}

	@Test
	public void testFortuneCouple() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
			}
		});

		final class Ids {
			Long mcId;
			Long mId;
			Long mmeId;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Bussigny);

				final Ids ids = new Ids();
				ids.mcId = mc.getNumero();
				ids.mId = toto.getNumero();
				ids.mmeId = tata.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.mcId);
		Assert.assertNotNull(ids.mId);
		Assert.assertNotNull(ids.mmeId);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
		Assert.assertNotNull(elt);
		Assert.assertEquals((long) ids.mcId, elt.noCtb);
		Assert.assertEquals("Toto", elt.identification.prenom);      // principal seulement
		Assert.assertEquals("Tartempion", elt.identification.nom);      // principal seulement
		Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);  // principal seulement
		Assert.assertEquals("7562025802593", elt.identification.numeroAvs);        // principal seulement
		Assert.assertEquals(ids.mId, elt.identification.noCtbPrincipal);
		Assert.assertEquals(ids.mmeId, elt.identification.noCtbConjoint);
		Assert.assertNull(elt.motifOuverture);
		Assert.assertNull(elt.motifFermeture);
		Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
		Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
		Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
		Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
		Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		Assert.assertFalse(elt.limite);
	}

	@Test
	public void testRevenuCoupleDivorceDansPeriode() throws Exception {

		final long noIndToto = 6341423L;
		final long noIndTata = 6341424L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu toto = addIndividu(noIndToto, date(1965, 2, 21), "Tartempion", "Toto", true);
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
				divorceIndividus(toto, tata, date(2008, 6, 30));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 6, 30));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 6, 30), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				addForPrincipal(toto, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
				addForPrincipal(tata, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.CheseauxSurLausanne);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(2, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idToto, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata", elt.identification.prenom);
			Assert.assertEquals("Tartempiona", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1966, 3, 12), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
				divorceIndividus(toto, tata, date(2008, 6, 30));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 6, 30));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 6, 30), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				addForPrincipal(toto, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aubonne);
				addForPrincipal(tata, date(2008, 7, 1), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.CheseauxSurLausanne);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(2, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idToto, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata", elt.identification.prenom);
			Assert.assertEquals("Tartempiona", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1966, 3, 12), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
				toto.setDateDeces(date(2008, 10, 24));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 10, 24));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 10, 24), MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				addForPrincipal(tata, date(2008, 10, 25), MotifFor.VEUVAGE_DECES, MockCommune.CheseauxSurLausanne);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(2, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata", elt.identification.prenom);
			Assert.assertEquals("Tartempiona", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1966, 3, 12), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.VEUVAGE_DECES, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 10, 25), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);      // principal seulement
			Assert.assertEquals("Tartempion", elt.identification.nom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);  // principal seulement
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);        // principal seulement
			Assert.assertEquals(ids.idToto, elt.identification.noCtbPrincipal);
			Assert.assertEquals(ids.idTata, elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertEquals(MotifFor.VEUVAGE_DECES, elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 10, 24), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
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
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2000, 4, 1));
				toto.setDateDeces(date(2008, 10, 24));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2000, 4, 1), date(2008, 10, 24));
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 10, 24), MotifFor.VEUVAGE_DECES, MockCommune.Bussigny);
				addForPrincipal(tata, date(2008, 10, 25), MotifFor.VEUVAGE_DECES, MockCommune.CheseauxSurLausanne);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(1, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Tata", elt.identification.prenom);
			Assert.assertEquals("Tartempiona", elt.identification.nom);
			Assert.assertEquals(MockCommune.CheseauxSurLausanne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1966, 3, 12), elt.identification.dateNaissance);
			Assert.assertEquals("7568935457472", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.VEUVAGE_DECES, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 10, 25), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Non-assujetti au rôle ordinaire au 31 décembre", elt.raisonIgnore);
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
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2008, 4, 1));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2008, 4, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(toto, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(tata, date(2006, 6, 15), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.CheseauxSurLausanne);
				addForPrincipal(mc, date(2008, 4, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(2, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);      // principal seulement
			Assert.assertEquals("Tartempion", elt.identification.nom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);      // principal seulement
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);            // principal seulement
			Assert.assertEquals(ids.idToto, elt.identification.noCtbPrincipal);
			Assert.assertEquals(ids.idTata, elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idToto, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
				toto.setNouveauNoAVS("7562025802593");
				final MockIndividu tata = addIndividu(noIndTata, date(1966, 3, 12), "Tartempiona", "Tata", false);
				tata.setNouveauNoAVS("7568935457472");
				marieIndividus(toto, tata, date(2008, 4, 1));
			}
		});

		final class Ids {
			Long idToto;
			Long idTata;
			Long idMenage;
		}

		// mise en place
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique toto = addHabitant(noIndToto);
				final PersonnePhysique tata = addHabitant(noIndTata);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(toto, tata, date(2008, 4, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(toto, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				addForPrincipal(tata, date(2006, 6, 15), MotifFor.ARRIVEE_HC, date(2008, 3, 31), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.CheseauxSurLausanne);
				addForPrincipal(mc, date(2008, 4, 1), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);

				final Ids ids = new Ids();
				ids.idToto = toto.getNumero();
				ids.idTata = tata.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		Assert.assertNotNull(ids);
		Assert.assertNotNull(ids.idToto);
		Assert.assertNotNull(ids.idTata);
		Assert.assertNotNull(ids.idMenage);

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(2, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idMenage, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);      // principal seulement
			Assert.assertEquals("Tartempion", elt.identification.nom);      // principal seulement
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);  // principal seulement
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);        // principal seulement
			Assert.assertEquals(ids.idToto, elt.identification.noCtbPrincipal);
			Assert.assertEquals(ids.idTata, elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idToto, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
		}
		{
			final ExtractionDonneesRptResults.InfoCtbIgnore elt = res.getListeCtbsIgnores().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals((long) ids.idTata, elt.noCtb);
			Assert.assertEquals("Non assujetti sur la période fiscale", elt.raisonIgnore);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Fraction.LesCharbonnieres);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
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
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Fraction.LesCharbonnieres);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.LeLieu.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
		}
	}

	@Test
	public void testRevenuOrdinaireContribuablePassageSourceVersOrdinaire() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 5, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testRevenuSourcePureContribuablePassageSourceVersOrdinaire() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 4, 30), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.SOURCE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testFortuneContribuablePassageSourceVersOrdinaire() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.CHGT_MODE_IMPOSITION, MockCommune.Aigle);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 5, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
	}

	@Test
	public void testRevenuOrdinaireContribuableSourcePartiHC() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HC, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				final ForFiscalPrincipal ffpHc = addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HC, MockCommune.Bern);
				ffpHc.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas vu dans la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testRevenuSourcePureContribuableSourcePartiHC() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HC, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				final ForFiscalPrincipal ffpHc = addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HC, MockCommune.Bern);
				ffpHc.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(2, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertEquals(MotifFor.DEPART_HC, elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 4, 12), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.SOURCE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertNull(elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.DEPART_HC, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 4, 13), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.SOURCE, elt.modeImposition);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, elt.autoriteFiscaleForPrincipal);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
		}
	}

	@Test
	public void testFortuneContribuableSourcePartiHC() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				final ForFiscalPrincipal ffpSource = addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HC, MockCommune.Aigle);
				ffpSource.setModeImposition(ModeImposition.SOURCE);
				final ForFiscalPrincipal ffpHc = addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HC, MockCommune.Bern);
				ffpHc.setModeImposition(ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// même pas vu dans la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testRevenuOrdinaireTouchAndGo() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HS, date(2008, 9, 25), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 9, 26), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(2, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aigle.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertEquals(MotifFor.DEPART_HS, elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 4, 12), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(1);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.ARRIVEE_HS, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 9, 26), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testRevenuSourcePureTouchAndGo() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HS, date(2008, 9, 25), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 9, 26), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// même pas vu dans la requête initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneTouchAndGo() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 4, 12), MotifFor.DEPART_HS, MockCommune.Aigle);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.DEPART_HS, date(2008, 9, 25), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 9, 26), MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertEquals(MotifFor.ARRIVEE_HS, elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 9, 26), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
	}

	@Test
	public void testRevenuOrdinaireCtbHorsSuisseImmeubleQuiArriveDansLeCanton() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 4, 12), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testRevenuSourcePureCtbHorsSuisseImmeubleQuiArriveDansLeCanton() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 4, 12), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		// même pas vu dans l'extraction initiale
		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneCtbHorsSuisseImmeubleQuiArriveDansLeCanton() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		// mise en place
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 4, 12), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2008, 4, 13), MotifFor.ARRIVEE_HS, MockCommune.Bussigny);
				addForSecondaire(pp, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.DOMICILE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
	}

	@Test
	public void testRevenuOrdinaireDiplomateEtrangerAvecImmeubleVaudois() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				final RegDate dateOuverture = date(2004, 2, 1);
				addForPrincipal(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockPays.France, MotifRattachement.DIPLOMATE_ETRANGER);
				addForSecondaire(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_ORDINAIRE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_ORDINAIRE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptRevenuOrdinaireResults.InfoPeriodeImposition elt = res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, elt.autoriteFiscaleForPrincipal);
		}
	}

	@Test
	public void testRevenuSourcePureDiplomateEtrangerAvecImmeubleVaudois() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				final RegDate dateOuverture = date(2004, 2, 1);
				addForPrincipal(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockPays.France, MotifRattachement.DIPLOMATE_ETRANGER);
				addForSecondaire(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.REVENU_SOURCE_PURE, res.getMode());
		Assert.assertEquals(0, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());
	}

	@Test
	public void testFortuneDiplomateEtrangerAvecImmeubleVaudois() throws Exception {
		final long noInd = 6341423L;

		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu i = addIndividu(noInd, date(1965, 2, 21), "Tartempion", "Toto", true);
				i.setNouveauNoAVS("7562025802593");
			}
		});

		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				final RegDate dateOuverture = date(2004, 2, 1);
				addForPrincipal(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockPays.France, MotifRattachement.DIPLOMATE_ETRANGER);
				addForSecondaire(pp, dateOuverture, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bussigny.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final ExtractionDonneesRptResults res = processor.run(RegDate.get(), 2008, TypeExtractionDonneesRpt.FORTUNE, 1, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(TypeExtractionDonneesRpt.FORTUNE, res.getMode());
		Assert.assertEquals(1, res.getListePeriode().size());
		Assert.assertEquals(0, res.getListeCtbsIgnores().size());
		Assert.assertEquals(0, res.getListeErreurs().size());

		{
			final ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune elt = (ExtractionDonneesRptFortuneResults.InfoPeriodeImpositionFortune) res.getListePeriode().get(0);
			Assert.assertNotNull(elt);
			Assert.assertEquals(ppId, elt.noCtb);
			Assert.assertEquals("Toto", elt.identification.prenom);
			Assert.assertEquals("Tartempion", elt.identification.nom);
			Assert.assertEquals(MockCommune.Bussigny.getNoOFSEtendu(), (int) elt.ofsCommuneForGestion);
			Assert.assertEquals(date(1965, 2, 21), elt.identification.dateNaissance);
			Assert.assertEquals("7562025802593", elt.identification.numeroAvs);
			Assert.assertNull(elt.identification.noCtbPrincipal);
			Assert.assertNull(elt.identification.noCtbConjoint);
			Assert.assertNull(elt.motifOuverture);
			Assert.assertNull(elt.motifFermeture);
			Assert.assertEquals(date(2008, 1, 1), elt.debutPeriodeImposition);
			Assert.assertEquals(date(2008, 12, 31), elt.finPeriodeImposition);
			Assert.assertEquals(ModeImposition.ORDINAIRE, elt.modeImposition);
			Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, elt.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, elt.autoriteFiscaleForPrincipal);
			Assert.assertFalse(elt.limite);
		}
	}
}
