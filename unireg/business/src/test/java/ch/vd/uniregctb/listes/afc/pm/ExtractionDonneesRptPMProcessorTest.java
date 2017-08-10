package ch.vd.uniregctb.listes.afc.pm;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ExtractionDonneesRptPMProcessorTest extends BusinessTest {

	private ExtractionDonneesRptPMProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		final ServiceCivilCacheWarmer civilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		this.processor = new ExtractionDonneesRptPMProcessor(hibernateTemplate, transactionManager, tiersService, civilCacheWarmer, tiersDAO, serviceInfra, periodeImpositionService, adresseService);
	}

	/**
	 * [SIFISC-25638]
	 */
	@Test
	public void testHorsCantonForVaudoisFermeAvantFinExercice() throws Exception {

		final int anneeExtraction = 2015;
		final RegDate dateDebut = date(2009, 6, 4);
		final RegDate dateFermetureForVaudois = date(anneeExtraction - 1, 12, 4);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});
		
		// mise en place fiscale
		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Les petits lutins SARL");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut.addYears(1), DayMonth.get(6, 30), 12);      // tous les 30.06 depuis 2010
			addForPrincipal(entreprise, dateDebut, null, MockCommune.Geneve);
			addForSecondaire(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFermetureForVaudois, MotifFor.FIN_EXPLOITATION, MockCommune.Leysin.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
		});

		// lancement de l'extraction pour l'année de l'extraction
		final ExtractionDonneesRptPMResults results = processor.run(RegDate.get(), anneeExtraction, ModeExtraction.BENEFICE, VersionWS.V7, 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(ModeExtraction.BENEFICE, results.mode);
		Assert.assertEquals(VersionWS.V7, results.versionWS);
		Assert.assertEquals(anneeExtraction, results.periodeFiscale);
		Assert.assertEquals(1, results.getNombreCtbAnalyses());
		Assert.assertEquals(1, results.getListePeriodes().size());
		Assert.assertEquals(0, results.getListeCtbsIgnores().size());
		Assert.assertEquals(0, results.getListeCtbsDecisionACI().size());
		Assert.assertEquals(0, results.getListeErreurs().size());

		// vérification du contenu de la période retournée
		{
			final ExtractionDonneesRptPMResults.InfoPeriodeImposition info = results.getListePeriodes().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(idpm, info.noCtb);
			Assert.assertEquals(MotifRattachement.DOMICILE, info.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, info.autoriteFiscalePrincipale);
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.dateDebutPI);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.datefinPI);
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, info.formeJuridique);
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.debutExerciceCommercial);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.finExerciceCommercial);
			Assert.assertEquals(360, info.joursImposables);
			Assert.assertNull(info.motifOuverture);
			Assert.assertNull(info.motifFermeture);
			Assert.assertEquals("Leysin", info.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Leysin.getNoOFS(), info.noOfsCommune);
			Assert.assertEquals("Genève", info.nomForPrincipal);
			Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), info.noOfsForPrincipal);
		}
	}

	@Test
	public void testVaudoisEnFailliteAvantFinExerciceCommercial() throws Exception {

		final int anneeExtraction = 2015;
		final RegDate dateDebut = date(2009, 6, 4);
		final RegDate datePrononceFaillite = date(anneeExtraction - 1, 12, 4);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Les petits lutins SARL");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut.addYears(1), DayMonth.get(6, 30), 12);      // tous les 30.06 depuis 2010
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, datePrononceFaillite, MotifFor.FAILLITE, MockCommune.Grandson);
			return entreprise.getNumero();
		});

		// lancement de l'extraction pour l'année de l'extraction
		final ExtractionDonneesRptPMResults results = processor.run(RegDate.get(), anneeExtraction, ModeExtraction.BENEFICE, VersionWS.V7, 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(ModeExtraction.BENEFICE, results.mode);
		Assert.assertEquals(VersionWS.V7, results.versionWS);
		Assert.assertEquals(anneeExtraction, results.periodeFiscale);
		Assert.assertEquals(1, results.getNombreCtbAnalyses());
		Assert.assertEquals(1, results.getListePeriodes().size());
		Assert.assertEquals(0, results.getListeCtbsIgnores().size());
		Assert.assertEquals(0, results.getListeCtbsDecisionACI().size());
		Assert.assertEquals(0, results.getListeErreurs().size());

		// vérification du contenu de la période retournée
		{
			final ExtractionDonneesRptPMResults.InfoPeriodeImposition info = results.getListePeriodes().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(idpm, info.noCtb);
			Assert.assertEquals(MotifRattachement.DOMICILE, info.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.autoriteFiscalePrincipale);
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.dateDebutPI);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.datefinPI);
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, info.formeJuridique);
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.debutExerciceCommercial);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.finExerciceCommercial);
			Assert.assertEquals(360, info.joursImposables);
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, info.motifOuverture);
			Assert.assertEquals(MotifFor.FAILLITE, info.motifFermeture);
			Assert.assertEquals("Grandson", info.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), info.noOfsCommune);
			Assert.assertEquals("Grandson", info.nomForPrincipal);
			Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), info.noOfsForPrincipal);
		}
	}

	@Test
	public void testDepartHorsCanton() throws Exception {

		final int anneeExtraction = 2015;
		final RegDate dateDebut = date(2009, 6, 4);
		final RegDate dateDepartHorsCanton = date(anneeExtraction - 1, 12, 4);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide
			}
		});
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Les petits lutins SARL");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut.addYears(1), DayMonth.get(6, 30), 12);      // tous les 30.06 depuis 2010
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDepartHorsCanton, MotifFor.DEPART_HC, MockCommune.Grandson);
			addForPrincipal(entreprise, dateDepartHorsCanton.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Bern);
			return entreprise.getNumero();
		});

		// lancement de l'extraction pour l'année de l'extraction
		final ExtractionDonneesRptPMResults results = processor.run(RegDate.get(), anneeExtraction, ModeExtraction.BENEFICE, VersionWS.V7, 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(ModeExtraction.BENEFICE, results.mode);
		Assert.assertEquals(VersionWS.V7, results.versionWS);
		Assert.assertEquals(anneeExtraction, results.periodeFiscale);
		Assert.assertEquals(1, results.getNombreCtbAnalyses());
		Assert.assertEquals(1, results.getListePeriodes().size());
		Assert.assertEquals(0, results.getListeCtbsIgnores().size());
		Assert.assertEquals(0, results.getListeCtbsDecisionACI().size());
		Assert.assertEquals(0, results.getListeErreurs().size());

		// vérification du contenu de la période retournée
		{
			final ExtractionDonneesRptPMResults.InfoPeriodeImposition info = results.getListePeriodes().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(idpm, info.noCtb);
			Assert.assertEquals(MotifRattachement.DOMICILE, info.motifRattachement);
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, info.autoriteFiscalePrincipale);        // car la PI est vaudoise !! attention, ça peut changer à l'avenir (-> HC) !
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.dateDebutPI);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.datefinPI);
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, info.formeJuridique);
			Assert.assertEquals(date(anneeExtraction - 1, 7, 1), info.debutExerciceCommercial);
			Assert.assertEquals(date(anneeExtraction, 6, 30), info.finExerciceCommercial);
			Assert.assertEquals(360, info.joursImposables);
			Assert.assertEquals(MotifFor.DEPART_HC, info.motifOuverture);
			Assert.assertNull(info.motifFermeture);
			Assert.assertEquals("Grandson", info.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Grandson.getNoOFS(), info.noOfsCommune);
			Assert.assertEquals("Bern", info.nomForPrincipal);
			Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), info.noOfsForPrincipal);
		}
	}
}
