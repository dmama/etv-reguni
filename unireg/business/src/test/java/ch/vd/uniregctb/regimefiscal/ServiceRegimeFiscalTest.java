package ch.vd.uniregctb.regimefiscal;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.GenreImpotExoneration;
import ch.vd.unireg.interfaces.infra.data.ModeExoneration;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CH;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CO;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_CT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_DI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ADM_PUBLIQUE_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ASSOCIATION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.CORP_DP_ADM;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.CORP_DP_ENT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.EI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CH;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CO;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_CT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_DI;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ENT_PUBLIQUE_HS;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_CH_RC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_HS_NIRC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FILIALE_HS_RC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.FONDATION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.IDP;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.INDIVISION;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.ORG_INTERNAT;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.PARTICULIER;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.PNC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SA;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SARL;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCA;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCOOP;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SCPC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SICAF;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SICAV;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SNC;
import static ch.vd.uniregctb.type.FormeJuridiqueEntreprise.SS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2017-03-30, <raphael.marmier@vd.ch>
 */
public class ServiceRegimeFiscalTest extends BusinessTest {

	private ServiceRegimeFiscal serviceRegimeFiscal;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		serviceRegimeFiscal = getBean(ServiceRegimeFiscal.class, "serviceRegimeFiscal");
	}

	@Test
	public void testGetTypeRegimeFiscal() {
		final TypeRegimeFiscal typeRegimeFiscal = serviceRegimeFiscal.getTypeRegimeFiscal("01");
		assertEquals("01", typeRegimeFiscal.getCode());
		assertEquals("Ordinaire", typeRegimeFiscal.getLibelle());
		assertEquals(CategorieEntreprise.PM, typeRegimeFiscal.getCategorie());
	}

	@Test
	public void testGetTypeRegimeFiscalMauvaisCode() {
		try {
			serviceRegimeFiscal.getTypeRegimeFiscal("00001");
			fail();
		}
		catch (ServiceRegimeFiscalException e) {
			assertEquals("Aucun type de régime fiscal ne correspond au code fourni '00001'. Soit le code est erroné, soit il manque des données dans FiDoR.", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void testGetTypeRegimeFiscalParDefaut() throws Exception {

		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(EI).getCode());

		assertEquals("80", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SNC).getCode());
		assertEquals("80", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SC).getCode());

		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SCA).getCode());
		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SA).getCode());
		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SARL).getCode());
		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SCOOP).getCode());

		assertEquals("70", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ASSOCIATION).getCode());
		assertEquals("70", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(FONDATION).getCode());

		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(FILIALE_HS_RC).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(PARTICULIER).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SCPC).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SICAV).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SICAF).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(IDP).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(PNC).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(INDIVISION).getCode());

		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(FILIALE_CH_RC).getCode());

		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ADM_CH).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ADM_CT).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ADM_DI).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ADM_CO).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(CORP_DP_ADM).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_CH).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_CT).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_DI).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_CO).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(CORP_DP_ENT).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(SS).getCode());

		assertEquals("01", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(FILIALE_HS_NIRC).getCode());

		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_PUBLIQUE_HS).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ADM_PUBLIQUE_HS).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ORG_INTERNAT).getCode());
		assertEquals("00", serviceRegimeFiscal.getTypeRegimeFiscalParDefaut(ENT_HS).getCode());
	}

	@Test
	public void testGetCategoryOrganisationEntreprisePasDeRegime() throws Exception {
		Long noOrganisation = 1000L;
		Long noSite = 1001L;

		final MockOrganisation organisation =
				MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                           StatusRegistreIDE.DEFINITIF,
				                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noSite);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

						final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

						Assert.assertEquals(CategorieEntreprise.INDET, currentCategorie);
					}
				}
		);
	}

	@Test
	public void testGetCategoryOrganisationEntrepriseAvecSurchargeEtRegime() throws Exception {
		Long noOrganisation = 1000L;
		Long noSite = 1001L;

		final MockOrganisation organisation =
				MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                           StatusRegistreIDE.DEFINITIF,
				                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise
		Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noSite);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			addFormeJuridique(entreprise, date(2015, 6, 25), null, FormeJuridiqueEntreprise.ASSOCIATION);

			addRegimeFiscalVD(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalCH(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_APM);

			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

						final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

						Assert.assertEquals(CategorieEntreprise.APM, currentCategorie);
					}
				}
		);
	}

	@Test
	public void testGetCategoryOrganisationEntrepriseRegimesFors() throws Exception {
		Long noOrganisation = 1000L;
		Long noSite = 1001L;

		final MockOrganisation organisation =
				MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2015, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2015, 6, 24),
				                                           StatusRegistreIDE.DEFINITIF,
				                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise
		Long noEntreprise = doInNewTransactionAndSession(transactionStatus -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noSite);

			addActiviteEconomique(entreprise, etablissement, date(2015, 6, 24), null, true);

			addRegimeFiscalVD(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2015, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2015, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			return entreprise.getNumero();
		});

		doInNewTransactionAndSession(
				new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

						final CategorieEntreprise currentCategorie = tiersService.getCategorieEntreprise(entreprise, date(2015, 6, 27));

						Assert.assertEquals(CategorieEntreprise.PM, currentCategorie);
					}
				}
		);
	}

	@Test
	public void testGetExonerations() throws Exception {

		final RegDate dateDebut = date(2010, 3, 1);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
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
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				// null -> vide
				{
					final List<ModeExonerationHisto> exonerations = serviceRegimeFiscal.getExonerations(entreprise, null);
					Assert.assertNotNull(exonerations);
					Assert.assertEquals(0, exonerations.size());
				}

				// IBC
				{
					final List<ModeExonerationHisto> exonerations = serviceRegimeFiscal.getExonerations(entreprise, GenreImpotExoneration.IBC);
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
					final List<ModeExonerationHisto> exonerations = serviceRegimeFiscal.getExonerations(entreprise, GenreImpotExoneration.ICI);
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
					final List<ModeExonerationHisto> exonerations = serviceRegimeFiscal.getExonerations(entreprise, GenreImpotExoneration.IFONC);
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
			}
		});
	}
}