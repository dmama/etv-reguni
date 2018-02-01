package ch.vd.unireg.regimefiscal.extraction;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.Sexe;

public class ExtractionRegimesFiscauxProcessorTest extends BusinessTest {

	private ExtractionRegimesFiscauxProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		processor = new ExtractionRegimesFiscauxProcessor(hibernateTemplate, transactionManager, serviceInfra, tiersService);
	}

	@Test
	public void testBaseVide() throws Exception {
		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on ne fait rien...
			}
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(0, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(0, histo.getPlagesRegimeFiscal().size());

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(0, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(0, dated.getPlagesRegimeFiscal().size());
	}

	@Test
	public void testBaseSansEntreprise() throws Exception {
		// mise en place fiscale
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on va juste créer une personne physique
				addNonHabitant("Alfred", "Balthazar", date(1943, 2, 1), Sexe.MASCULIN);
			}
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(0, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(0, histo.getPlagesRegimeFiscal().size());

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(0, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(0, dated.getPlagesRegimeFiscal().size());
	}

	@Test
	public void testEntrepriseSansAucunRegimeFiscal() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SA");
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(2, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(0, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(1);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(2, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(0, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = dated.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = dated.getSansRegimeFiscal().get(1);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
	}

	@Test
	public void testEntrepriseSansRegimeFiscalCH() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SARL");
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(1, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(1, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SARL", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SARL", prf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(1, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(1, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = dated.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SARL", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SARL", prf.raisonSociale);
		}
	}

	@Test
	public void testEntrepriseSansRegimeFiscalVD() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SA");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addIdentificationEntreprise(entreprise, "CHE123456789");
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(1, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(1, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertEquals("CHE123456789", srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertEquals("CHE123456789", prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(1, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(1, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = dated.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertEquals("CHE123456789", srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertEquals("CHE123456789", prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
	}

	@Test
	public void testEntrepriseAvecPlusieursRegimesFiscaux() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);
		final RegDate dateChangementRegimeVD = date(2014, 8, 31);
		final RegDate dateChangementRegimeCH = date(2012, 7, 2);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SA");
			addRegimeFiscalCH(entreprise, dateDebut, dateChangementRegimeCH.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateChangementRegimeCH, null, MockTypeRegimeFiscal.EXO_90C);
			addRegimeFiscalVD(entreprise, dateDebut, dateChangementRegimeVD.getOneDayBefore(), MockTypeRegimeFiscal.EXO_90F);
			addRegimeFiscalVD(entreprise, dateChangementRegimeVD, null, MockTypeRegimeFiscal.EXO_90E);
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, RegDate.get(), null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(0, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(4, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertEquals(dateChangementRegimeVD.getOneDayBefore(), prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(1);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateChangementRegimeVD, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90E.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90E.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(2);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertEquals(dateChangementRegimeCH.getOneDayBefore(), prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(3);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateChangementRegimeCH, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, RegDate.get(), null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(0, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(2, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateChangementRegimeVD, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90E.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90E.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(1);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateChangementRegimeCH, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
	}

	@Test
	public void testDateTraitementAvantDerniereValeurRegime() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);
		final RegDate dateChangementRegimeVD = date(2014, 8, 31);
		final RegDate dateChangementRegimeCH = date(2012, 7, 2);
		final RegDate dateTraitement = dateChangementRegimeVD.addYears(-1);         // entre les deux dates de changement de régime

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SA");
			addRegimeFiscalCH(entreprise, dateDebut, dateChangementRegimeCH.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateChangementRegimeCH, null, MockTypeRegimeFiscal.EXO_90C);
			addRegimeFiscalVD(entreprise, dateDebut, dateChangementRegimeVD.getOneDayBefore(), MockTypeRegimeFiscal.EXO_90F);
			addRegimeFiscalVD(entreprise, dateChangementRegimeVD, null, MockTypeRegimeFiscal.EXO_90E);
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, dateTraitement, null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(0, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(3, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertEquals(dateChangementRegimeVD.getOneDayBefore(), prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(1);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertEquals(dateChangementRegimeCH.getOneDayBefore(), prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = histo.getPlagesRegimeFiscal().get(2);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateChangementRegimeCH, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, dateTraitement, null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(0, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(2, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(0);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, prf.portee);
			Assert.assertEquals(dateDebut, prf.getDateDebut());
			Assert.assertEquals(dateChangementRegimeVD.getOneDayBefore(), prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90F.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.PlageRegimeFiscal prf = dated.getPlagesRegimeFiscal().get(1);
			Assert.assertNotNull(prf);
			Assert.assertEquals(pmId, prf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, prf.portee);
			Assert.assertEquals(dateChangementRegimeCH, prf.getDateDebut());
			Assert.assertNull(prf.getDateFin());
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getCode(), prf.code);
			Assert.assertEquals(MockTypeRegimeFiscal.EXO_90C.getLibelle(), prf.libelle);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, prf.formeLegale);
			Assert.assertNull(prf.ide);
			Assert.assertEquals("Tralala outi SA", prf.raisonSociale);
		}
	}

	@Test
	public void testDateTraitementAvantDebutEntreprise() throws Exception {
		final RegDate dateDebut = date(2009, 1, 5);
		final RegDate dateChangementRegimeVD = date(2014, 8, 31);
		final RegDate dateChangementRegimeCH = date(2012, 7, 2);
		final RegDate dateTraitement = dateDebut.addYears(-10);

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala outi SA");
			addRegimeFiscalCH(entreprise, dateDebut, dateChangementRegimeCH.getOneDayBefore(), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateChangementRegimeCH, null, MockTypeRegimeFiscal.EXO_90C);
			addRegimeFiscalVD(entreprise, dateDebut, dateChangementRegimeVD.getOneDayBefore(), MockTypeRegimeFiscal.EXO_90F);
			addRegimeFiscalVD(entreprise, dateChangementRegimeVD, null, MockTypeRegimeFiscal.EXO_90E);
			return entreprise.getNumero();
		});

		// lancement du processeur (avec historique)
		final ExtractionRegimesFiscauxResults histo = processor.run(true, 1, dateTraitement, null);
		Assert.assertNotNull(histo);
		Assert.assertEquals(0, histo.getErreurs().size());
		Assert.assertEquals(2, histo.getSansRegimeFiscal().size());
		Assert.assertEquals(0, histo.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(1);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}

		// lancement du processeur (sans historique)
		final ExtractionRegimesFiscauxResults dated = processor.run(false, 1, dateTraitement, null);
		Assert.assertNotNull(dated);
		Assert.assertEquals(0, dated.getErreurs().size());
		Assert.assertEquals(2, dated.getSansRegimeFiscal().size());
		Assert.assertEquals(0, dated.getPlagesRegimeFiscal().size());
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(0);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.VD, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
		{
			final ExtractionRegimesFiscauxResults.SansRegimeFiscal srf = histo.getSansRegimeFiscal().get(1);
			Assert.assertNotNull(srf);
			Assert.assertEquals(pmId, srf.idEntreprise);
			Assert.assertEquals(RegimeFiscal.Portee.CH, srf.portee);
			Assert.assertEquals(FormeLegale.N_0106_SOCIETE_ANONYME, srf.formeLegale);
			Assert.assertNull(srf.ide);
			Assert.assertEquals("Tralala outi SA", srf.raisonSociale);
		}
	}
}
