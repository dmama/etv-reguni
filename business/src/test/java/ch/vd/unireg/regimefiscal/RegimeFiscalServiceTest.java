package ch.vd.unireg.regimefiscal;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static ch.vd.unireg.type.FormeJuridiqueEntreprise.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2017-03-30, <raphael.marmier@vd.ch>
 */
public class RegimeFiscalServiceTest extends BusinessTest {

	private RegimeFiscalService regimeFiscalService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
	}

	@Test
	public void testGetTypeRegimeFiscal() {
		final TypeRegimeFiscal typeRegimeFiscal = regimeFiscalService.getTypeRegimeFiscal("01");
		assertEquals("01", typeRegimeFiscal.getCode());
		assertEquals("Ordinaire", typeRegimeFiscal.getLibelle());
		assertEquals(CategorieEntreprise.PM, typeRegimeFiscal.getCategorie());
	}

	@Test
	public void testGetTypeRegimeFiscalMauvaisCode() {
		try {
			regimeFiscalService.getTypeRegimeFiscal("00001");
			fail();
		}
		catch (RegimeFiscalServiceException e) {
			assertEquals("Aucun type de régime fiscal ne correspond au code fourni '00001'. Soit le code est erroné, soit il manque des données dans FiDoR.", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void testGetFormeJuridiqueMapping() throws Exception {

		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(EI, null));
		assertMapping("80", null, null, regimeFiscalService.getFormeJuridiqueMapping(SNC, null));
		assertMapping("80", null, null, regimeFiscalService.getFormeJuridiqueMapping(SC, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(SCA, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(SA, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(SARL, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(SCOOP, null));
		assertMapping("70", null, RegDate.get(2017, 12, 31), regimeFiscalService.getFormeJuridiqueMapping(ASSOCIATION, RegDate.get(2017, 1, 1)));
		assertMapping("703", RegDate.get(2018, 1, 1), null, regimeFiscalService.getFormeJuridiqueMapping(ASSOCIATION, null));
		assertMapping("70", null, RegDate.get(2017, 12, 31), regimeFiscalService.getFormeJuridiqueMapping(FONDATION, RegDate.get(2017, 1, 1)));
		assertMapping("703", RegDate.get(2018, 1, 1), null, regimeFiscalService.getFormeJuridiqueMapping(FONDATION, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(FILIALE_HS_RC, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(PARTICULIER, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(SCPC, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(SICAV, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(SICAF, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(IDP, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(PNC, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(INDIVISION, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(FILIALE_CH_RC, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ADM_CH, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ADM_CT, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ADM_DI, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ADM_CO, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(CORP_DP_ADM, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_CH, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_CT, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_DI, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_CO, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(CORP_DP_ENT, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(SS, null));
		assertMapping("01", null, null, regimeFiscalService.getFormeJuridiqueMapping(FILIALE_HS_NIRC, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_PUBLIQUE_HS, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ADM_PUBLIQUE_HS, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ORG_INTERNAT, null));
		assertMapping("00", null, null, regimeFiscalService.getFormeJuridiqueMapping(ENT_HS, null));
	}

	private static void assertMapping(String code, RegDate dateDebut, RegDate dateFin, FormeJuridiqueVersTypeRegimeFiscalMapping mapping) {
		final TypeRegimeFiscal type = mapping.getTypeRegimeFiscal();
		assertNotNull(type);
		assertEquals(code, type.getCode());
		assertEquals(dateDebut, mapping.getDateDebut());
		assertEquals(dateFin, mapping.getDateFin());
	}

	@Test
	public void testGetCategoryEntreprisePasDeRegime() throws Exception {
		Long noEntrepriseCivile = 1000L;
		Long noEtablissement = 1001L;

		final MockEntrepriseCivile entrepriseCivile =
				MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                       StatusRegistreIDE.DEFINITIF,
				                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entrepriseCivile);
			}
		});

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				status -> {
					final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

					final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

					Assert.assertEquals(CategorieEntreprise.INDET, currentCategorie);
					return null;
				}
		);
	}

	@Test
	public void testGetCategoryEntrepriseAvecSurchargeEtRegime() throws Exception {
		Long noEntrepriseCivile = 1000L;
		Long noEtablissement = 1001L;

		final MockEntrepriseCivile entrepriseCivile =
				MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                       StatusRegistreIDE.DEFINITIF,
				                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entrepriseCivile);
			}
		});

		// Création de l'entreprise
		Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			addFormeJuridique(entreprise, date(2015, 6, 25), null, FormeJuridiqueEntreprise.ASSOCIATION);

			addRegimeFiscalVD(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);

			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				status -> {
					final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

					final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

					Assert.assertEquals(CategorieEntreprise.APM, currentCategorie);
					return null;
				}
		);
	}

	@Test
	public void testGetCategoryEntrepriseRegimesFors() throws Exception {
		Long noEntrepriseCivile = 1000L;
		Long noEtablissement = 1001L;

		final MockEntrepriseCivile entrepriseCivile =
				MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                       StatusRegistreIDE.DEFINITIF,
				                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(entrepriseCivile);
			}
		});

		// Création de l'entreprise
		Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2015, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				status -> {
					final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

					final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

					Assert.assertEquals(CategorieEntreprise.PM, currentCategorie);
					return null;
				}
		);
	}

	@Test
	public void testGetExonerations() throws Exception {

		final RegDate dateDebut = date(2010, 3, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// vide
			}
		});

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.CORP_DP_ENT);
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, dateDebut.addYears(1).getOneDayBefore(), MockTypeRegimeFiscal.EXO_IBC_FAIT);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(1), dateDebut.addYears(2).getOneDayBefore(), MockTypeRegimeFiscal.EXO_IBC_TOTALE);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(2), dateDebut.addYears(3).getOneDayBefore(), MockTypeRegimeFiscal.EXO_IBC_TOTALE);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(3), dateDebut.addYears(4).getOneDayBefore(), MockTypeRegimeFiscal.EXO_ICI_FAIT);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(4), dateDebut.addYears(5).getOneDayBefore(), MockTypeRegimeFiscal.EXO_ICI_TOTALE);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(5), dateDebut.addYears(6).getOneDayBefore(), MockTypeRegimeFiscal.EXO_IFONC_FAIT);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(6), dateDebut.addYears(7).getOneDayBefore(), MockTypeRegimeFiscal.EXO_IFONC_TOTALE);
			addRegimeFiscalVD(entreprise, dateDebut.addYears(7), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala");
			return entreprise.getNumero();
		});

		// test du service
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
			Assert.assertNotNull(entreprise);

			// null -> vide
			{
				final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, null);
				Assert.assertNotNull(exonerations);
				Assert.assertEquals(0, exonerations.size());
			}

			// IBC
			{
				final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, GenreImpotExoneration.IBC);
				Assert.assertNotNull(exonerations);
				Assert.assertEquals(2, exonerations.size());
				{
					final ModeExonerationHisto exo = exonerations.get(0);
					Assert.assertEquals(dateDebut, exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(1).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.DE_FAIT, exo.getModeExoneration());
				}
				{
					final ModeExonerationHisto exo = exonerations.get(1);
					Assert.assertEquals(dateDebut.addYears(1), exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(3).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.TOTALE, exo.getModeExoneration());
				}
			}

			// ICI
			{
				final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, GenreImpotExoneration.ICI);
				Assert.assertNotNull(exonerations);
				Assert.assertEquals(2, exonerations.size());
				{
					final ModeExonerationHisto exo = exonerations.get(0);
					Assert.assertEquals(dateDebut.addYears(3), exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(4).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.DE_FAIT, exo.getModeExoneration());
				}
				{
					final ModeExonerationHisto exo = exonerations.get(1);
					Assert.assertEquals(dateDebut.addYears(4), exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(5).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.TOTALE, exo.getModeExoneration());
				}
			}

			// IFONC
			{
				final List<ModeExonerationHisto> exonerations = regimeFiscalService.getExonerations(entreprise, GenreImpotExoneration.IFONC);
				Assert.assertNotNull(exonerations);
				Assert.assertEquals(2, exonerations.size());
				{
					final ModeExonerationHisto exo = exonerations.get(0);
					Assert.assertEquals(dateDebut.addYears(5), exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(6).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.DE_FAIT, exo.getModeExoneration());
				}
				{
					final ModeExonerationHisto exo = exonerations.get(1);
					Assert.assertEquals(dateDebut.addYears(6), exo.getDateDebut());
					Assert.assertEquals(dateDebut.addYears(7).getOneDayBefore(), exo.getDateFin());
					Assert.assertEquals(ModeExoneration.TOTALE, exo.getModeExoneration());
				}
			}
			return null;
		});
	}
}