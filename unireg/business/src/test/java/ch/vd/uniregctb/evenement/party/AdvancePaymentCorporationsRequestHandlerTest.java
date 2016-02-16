package ch.vd.uniregctb.evenement.party;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.xml.event.party.advancepayment.corporation.v1.Taxpayer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.xml.DataHelper;

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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.CESSATION_ACTIVITE, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final Taxpayer extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.getNumber());
			Assert.assertEquals("Ma petite entreprise", extraite.getName());
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getPastEndOfBusinessYear()));
			Assert.assertEquals(date(2016, 6, 30), DataHelper.xmlToCore(extraite.getFutureEndOfBusinessYear()));

			Assert.assertNotNull(extraite.getVdTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateFrom()));
			Assert.assertNull(extraite.getVdTaxLiability().getDateTo());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getVdTaxSystemType());

			Assert.assertNotNull(extraite.getChTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getChTaxLiability().getDateFrom()));
			Assert.assertNull(extraite.getChTaxLiability().getDateTo());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getChTaxSystemType());
		}
	}

	@Test
	public void testDepartHCDansExerciceDeDateReference() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateDepartHC = date(2015, 7, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateDepartHC, MotifFor.DEPART_HC, MockCommune.Renens);
				addForPrincipal(e, dateDepartHC.getOneDayAfter(), MotifFor.DEPART_HC, MockCommune.Sierre);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final Taxpayer extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.getNumber());
			Assert.assertEquals("Ma petite entreprise", extraite.getName());
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getPastEndOfBusinessYear()));
			Assert.assertEquals(date(2016, 6, 30), DataHelper.xmlToCore(extraite.getFutureEndOfBusinessYear()));

			Assert.assertNotNull(extraite.getVdTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateFrom()));
			Assert.assertEquals(dateDepartHC, DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateTo()));
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getVdTaxSystemType());

			Assert.assertNotNull(extraite.getChTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getChTaxLiability().getDateFrom()));
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getChTaxLiability().getDateTo()));
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getChTaxSystemType());
		}
	}

	@Test
	public void testFailliteAvecDateReferenceEntreFinForEtFinExercice() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateFaillite = date(2015, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFaillite, MotifFor.FAILLITE, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final Taxpayer extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.getNumber());
			Assert.assertEquals("Ma petite entreprise", extraite.getName());
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getPastEndOfBusinessYear()));
			Assert.assertEquals(date(2016, 6, 30), DataHelper.xmlToCore(extraite.getFutureEndOfBusinessYear()));

			Assert.assertNotNull(extraite.getVdTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateFrom()));
			Assert.assertNull(extraite.getVdTaxLiability().getDateTo());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getVdTaxSystemType());

			Assert.assertNotNull(extraite.getChTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getChTaxLiability().getDateFrom()));
			Assert.assertNull(extraite.getChTaxLiability().getDateTo());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getChTaxSystemType());
		}
	}

	@Test
	public void testCessationActivite() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateCessationActivite = date(2015, 10, 17);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateCessationActivite, MotifFor.CESSATION_ACTIVITE, MockCommune.Renens);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
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
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

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
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final Taxpayer extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.getNumber());
			Assert.assertEquals("Ma petite entreprise", extraite.getName());
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getPastEndOfBusinessYear()));
			Assert.assertEquals(date(2016, 6, 30), DataHelper.xmlToCore(extraite.getFutureEndOfBusinessYear()));

			Assert.assertNotNull(extraite.getVdTaxLiability());
			Assert.assertEquals(date(2015, 7, 4), DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateFrom()));
			Assert.assertEquals(dateDepartHC, DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateTo()));
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getVdTaxSystemType());

			Assert.assertNotNull(extraite.getChTaxLiability());
			Assert.assertEquals(dateDebut, DataHelper.xmlToCore(extraite.getChTaxLiability().getDateFrom()));
			Assert.assertEquals(date(2014, 6, 30), DataHelper.xmlToCore(extraite.getChTaxLiability().getDateTo()));
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getChTaxSystemType());
		}
	}

	@Test
	public void testCalculDatesIfdAvecHCpur() throws Exception {

		final RegDate dateDebut = date(2000, 3, 1);
		final RegDate dateReference = date(2016, 1, 4);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRaisonSociale(e, dateDebut, null, "Ma petite entreprise");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addForPrincipal(e, dateDebut, null, MockCommune.Sierre);
				addForSecondaire(e, date(2015, 6, 21), MotifFor.ACHAT_IMMOBILIER, date(2015, 12, 3), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
				addBouclement(e, dateDebut.addYears(1), DayMonth.get(6, 30), 12);       // tous les 30.06 depuis 2001
				return e.getNumero();
			}
		});

		final AdvancePaymentCorporationsRequestHandler.ExtractionResult result = handler.buildPopulation(dateReference, 1);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.erreurs.size());
		Assert.assertEquals(0, result.ignores.size());
		Assert.assertEquals(1, result.extraits.size());

		{
			final Taxpayer extraite = result.extraits.get(0);
			Assert.assertNotNull(extraite);
			Assert.assertEquals(pmId, extraite.getNumber());
			Assert.assertEquals("Ma petite entreprise", extraite.getName());
			Assert.assertEquals(date(2015, 6, 30), DataHelper.xmlToCore(extraite.getPastEndOfBusinessYear()));
			Assert.assertEquals(date(2016, 6, 30), DataHelper.xmlToCore(extraite.getFutureEndOfBusinessYear()));

			Assert.assertNotNull(extraite.getVdTaxLiability());
			Assert.assertEquals(date(2015, 6, 21), DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateFrom()));
			Assert.assertEquals(date(2015, 12, 3), DataHelper.xmlToCore(extraite.getVdTaxLiability().getDateTo()));
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getVdTaxSystemType());

			Assert.assertNull(extraite.getChTaxLiability());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), extraite.getChTaxSystemType());
		}
	}
}
