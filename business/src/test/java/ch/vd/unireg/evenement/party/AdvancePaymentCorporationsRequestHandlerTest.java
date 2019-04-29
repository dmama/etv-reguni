package ch.vd.unireg.evenement.party;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;

public class AdvancePaymentCorporationsRequestHandlerTest extends BusinessTest {

	private AdvancePaymentCorporationsRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = new AdvancePaymentCorporationsRequestHandler();
		handler.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setTiersService(tiersService);
		handler.setTransactionManager(transactionManager);
		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));
	}

	@Test
	public void testBaseVide() throws Exception {
		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(RegDate.get(), 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(0, result.extraits.size());
	}

	@Test
	public void testPlusAssujettie() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateFin = RegDate.get().addMonths(-1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(RegDate.get(), 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(1, result.ignores.size());
		Assert.assertEquals(0, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Ignore ignoree = result.ignores.get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.noCtb);
			Assert.assertEquals(AdvancePaymentCorporationsRequestHandler.ExtractionResult.RaisonIgnore.NON_ASSUJETTI, ignoree.raison);
		}
	}

	@Test
	public void testAssujettie() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Extrait extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.noCtb);
			Assert.assertEquals("Ma petite entreprise", extraite.raisonSociale);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateDenierBouclement);
			Assert.assertEquals(date(2016, 6, 30), extraite.dateBouclementFutur);

			Assert.assertEquals(dateDebut, extraite.dateDebutIcc);
			Assert.assertNull(extraite.dateFinIcc);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalVD);

			Assert.assertEquals(dateDebut, extraite.dateDebutIfd);
			Assert.assertNull(extraite.dateFinIfd);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalCH);
		}
	}

	@Test
	public void testDepartHCDansExerciceDeDateReference() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateDepartHC = date(2015, 7, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Renens);
			addForPrincipal(e, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Sierre);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Extrait extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.noCtb);
			Assert.assertEquals("Ma petite entreprise", extraite.raisonSociale);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateDenierBouclement);
			Assert.assertEquals(date(2016, 6, 30), extraite.dateBouclementFutur);

			Assert.assertEquals(dateDebut, extraite.dateDebutIcc);
			Assert.assertEquals(dateDepartHC, extraite.dateFinIcc);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalVD);

			Assert.assertEquals(dateDebut, extraite.dateDebutIfd);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateFinIfd);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalCH);
		}
	}

	@Test
	public void testFailliteAvecDateReferenceEntreFinForEtFinExercice() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateFaillite = date(2015, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(1, result.ignores.size());
		Assert.assertEquals(0, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Ignore ignoree = result.ignores.get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.noCtb);
			Assert.assertEquals(AdvancePaymentCorporationsRequestHandler.ExtractionResult.RaisonIgnore.FAILLITE, ignoree.raison);
		}
	}

	@Test
	public void testFailliteAvecDateReferenceApresFinExercice() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateFaillite = date(2014, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(1, result.ignores.size());
		Assert.assertEquals(0, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Ignore ignoree = result.ignores.get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.noCtb);
			Assert.assertEquals(AdvancePaymentCorporationsRequestHandler.ExtractionResult.RaisonIgnore.NON_ASSUJETTI, ignoree.raison);
		}
	}

	@Test
	public void testFailliteAvecDateReferenceAvantFinFor() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateFaillite = date(2015, 10, 17);
		final RegDate dateReference = dateFaillite.addMonths(-1);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Extrait extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.noCtb);
			Assert.assertEquals("Ma petite entreprise", extraite.raisonSociale);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateDenierBouclement);
			Assert.assertEquals(date(2016, 6, 30), extraite.dateBouclementFutur);

			Assert.assertEquals(dateDebut, extraite.dateDebutIcc);
			Assert.assertNull(extraite.dateFinIcc);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalVD);

			Assert.assertEquals(dateDebut, extraite.dateDebutIfd);
			Assert.assertNull(extraite.dateFinIfd);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalCH);
		}
	}

	@Test
	public void testCessationActivite() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateCessationActivite = date(2015, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateCessationActivite, MotifFor.FIN_EXPLOITATION, MockCommune.Renens);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(1, result.ignores.size());
		Assert.assertEquals(0, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Ignore ignoree = result.ignores.get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals(pmId, ignoree.noCtb);
			Assert.assertEquals(AdvancePaymentCorporationsRequestHandler.ExtractionResult.RaisonIgnore.NON_ASSUJETTI, ignoree.raison);
		}
	}

	@Test
	public void testCalculDatesIfdAvecFinExercicesHC() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateDepartHC = date(2015, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			//
			// FP     |-------------------VD-------------------|-----HC-----|----VD----|--------------HC---------------------
			// ExComm.                             ... ---|---------|-------------------------|------------ ...
			// DateRef                                                                     ^
			//

			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, date(2015, 6, 20), MotifFor.DEPART_HC, MockCommune.Renens);
			addForPrincipal(e, date(2015, 6, 21), MotifFor.DEPART_HC, date(2015, 7, 3), MotifFor.ARRIVEE_HC, MockCommune.Sierre);
			addForPrincipal(e, date(2015, 7, 4), MotifFor.ARRIVEE_HC, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Renens);
			addForPrincipal(e, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Sierre);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Extrait extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.noCtb);
			Assert.assertEquals("Ma petite entreprise", extraite.raisonSociale);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateDenierBouclement);
			Assert.assertEquals(date(2016, 6, 30), extraite.dateBouclementFutur);

			Assert.assertEquals(date(2015, 7, 4), extraite.dateDebutIcc);
			Assert.assertEquals(dateDepartHC, extraite.dateFinIcc);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalVD);

			Assert.assertEquals(dateDebut, extraite.dateDebutIfd);
			Assert.assertEquals(date(2014, 6, 30), extraite.dateFinIfd);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalCH);
		}
	}

	@Test
	public void testCalculDatesIfdAvecHCpur() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addForPrincipal(e, dateDebut, null, MockCommune.Sierre);
			addForSecondaire(e, date(2015, 6, 21), MotifFor.ACHAT_IMMOBILIER, date(2015, 12, 3), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
			addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001;
			return e.getNumero();
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final AdvancePaymentCorporationsRequestHandler.ExtractionResult.Extrait extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.noCtb);
			Assert.assertEquals("Ma petite entreprise", extraite.raisonSociale);
			Assert.assertEquals(date(2015, 6, 30), extraite.dateDenierBouclement);
			Assert.assertEquals(date(2016, 6, 30), extraite.dateBouclementFutur);

			Assert.assertEquals(date(2015, 6, 21), extraite.dateDebutIcc);
			Assert.assertEquals(date(2015, 12, 3), extraite.dateFinIcc);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalVD);

			Assert.assertNull(extraite.dateDebutIfd);
			Assert.assertNull(extraite.dateFinIfd);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.codeRegimeFiscalCH);
		}
	}
}
