package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AppariementEtablissementsSecondairesProcessorTest extends BusinessTest {

	private AppariementEtablissementsSecondairesProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final Dialect dialect = getBean(Dialect.class, "hibernateDialect");
		final AppariementService appariementService = getBean(AppariementService.class, "appariementService");
		processor = new AppariementEtablissementsSecondairesProcessor(transactionManager, hibernateTemplate, appariementService, tiersService, dialect);
	}

	@Test
	public void testEntrepriseNonAppariee() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire = 43278L;
		final RegDate dateDebut = date(2010, 12, 1);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999995", null, null);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire, ent, dateDebut, null, "Toto Lausanne", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999996", null, null);
			}
		});

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Toto et compagnie");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Etablissement prn = addEtablissement();
			addDomicileEtablissement(prn, dateDebut, null, MockCommune.Echallens);
			addActiviteEconomique(e, prn, dateDebut, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec, dateDebut, null, false);
			sec.setRaisonSociale("Toto Lausanne");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// tentative d'appariement
		final AppariementEtablissementsSecondairesResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getAppariements().size());
		Assert.assertEquals(0, results.getErreurs().size());

		// vérification des données en base
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertNull(e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertNull(prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertNull(sec.getNumeroEtablissement());

			final List<DomicileEtablissement> domiciles = hibernateTemplate.find("from DomicileEtablissement", null);
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(2, domiciles.size());
			for (DomicileEtablissement domicile : domiciles) {
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertNull(domicile.getDateFin());
			}
			return null;
		});
	}

	@Test
	public void testEntrepriseNonApparieeExplicitementDemandee() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire = 43278L;
		final RegDate dateDebut = date(2010, 12, 1);

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire, ent, dateDebut, null, "Toto Lausanne", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999997", null, null);
			}
		});

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Toto et compagnie");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Etablissement prn = addEtablissement();
			addDomicileEtablissement(prn, dateDebut, null, MockCommune.Echallens);
			addActiviteEconomique(e, prn, dateDebut, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec, dateDebut, null, false);
			sec.setRaisonSociale("Toto Lausanne");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// tentative d'appariement explicite pour cette entreprise
		final AppariementEtablissementsSecondairesResults results = processor.run(Collections.singletonList(ids.idEntreprise), 1, false, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getAppariements().size());
		Assert.assertEquals(0, results.getErreurs().size());

		// vérification des données en base
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertNull(e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertNull(prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertNull(sec.getNumeroEtablissement());

			final List<DomicileEtablissement> domiciles = hibernateTemplate.find("from DomicileEtablissement", null);
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(2, domiciles.size());
			for (DomicileEtablissement domicile : domiciles) {
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertNull(domicile.getDateFin());
			}
			return null;
		});
	}

	@Test
	public void testAppariementAnnulationDomicileFiscal() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56795846L;
		final RegDate dateDebut = date(2010, 12, 1);
		final String ide = "CHE101390939";

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				final MockEtablissementCivil sec = MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                                                                  false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                  MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                                                                  null, null, ide, null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebut, null, "Toto Lausanne 2", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999998", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseConnueAuCivil(noCantonalEntreprise);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Etablissement prn = addEtablissement();
			prn.setNumeroEtablissement(noCantonalEtablissementPrincipal);
			addActiviteEconomique(e, prn, dateDebut, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec, dateDebut, null, false);
			sec.setRaisonSociale("Toto et compagnie Lausanne");
			sec.setEnseigne("Toto & cie");
			addIdentificationEntreprise(sec, ide);

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// lancement de l'appariement (SIMULATION)

		final AppariementEtablissementsSecondairesResults simulation = processor.run(1, true, null);
		Assert.assertNotNull(simulation);
		Assert.assertEquals(0, simulation.getErreurs().size());
		Assert.assertEquals(1, simulation.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = simulation.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.IDE_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base (SIMULATION -> rien)
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertNull(sec.getNumeroEtablissement());
			Assert.assertEquals("Toto et compagnie Lausanne", sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			final List<DomicileEtablissement> domiciles = hibernateTemplate.find("from DomicileEtablissement", null);
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());
			for (DomicileEtablissement domicile : domiciles) {
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertNull(domicile.getDateFin());
			}
			return null;
		});

		// lancement de l'appariement (REEL)

		final AppariementEtablissementsSecondairesResults reel = processor.run(1, false, null);
		Assert.assertNotNull(reel);
		Assert.assertEquals(0, reel.getErreurs().size());
		Assert.assertEquals(1, reel.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = reel.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.IDE_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertEquals((Long) noCantonalEtablissementSecondaire1, sec.getNumeroEtablissement());
			Assert.assertNull(sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			// le domicile fiscal a été carrément annulé car la date de début de l'établissement civil est la même que celle du domicile fiscal actuel
			final List<DomicileEtablissement> secDomiciles = sec.getSortedDomiciles(true);
			Assert.assertNotNull(secDomiciles);
			Assert.assertEquals(1, secDomiciles.size());
			{
				final DomicileEtablissement domicile = secDomiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertTrue(domicile.isAnnule());
				Assert.assertEquals(dateDebut, domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			return null;
		});
	}

	@Test
	public void testAppariementFermetureDomicileFiscal() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56795846L;
		final RegDate dateDebutCivile = date(2015, 8, 1);
		final RegDate dateDebutFiscale = date(2010, 12, 1);
		final RegDate dateDemenagementFiscal = dateDebutCivile.addMonths(-2);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebutCivile, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebutCivile.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebutCivile, null, "Toto Lausanne", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, null,
				                                               null, null, "CHE999999997", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebutCivile, null, "Toto Renens", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Renens.getNoOFS(), StatusInscriptionRC.INCONNU, null,
				                                               null, null, "CHE999999997", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseConnueAuCivil(noCantonalEntreprise);
			addRegimeFiscalVD(e, dateDebutFiscale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutFiscale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutFiscale, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Etablissement prn = addEtablissement();
			prn.setNumeroEtablissement(noCantonalEtablissementPrincipal);
			addActiviteEconomique(e, prn, dateDebutFiscale, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebutFiscale, dateDemenagementFiscal.getOneDayBefore(), MockCommune.Prilly);
			addDomicileEtablissement(sec, dateDemenagementFiscal, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec, dateDebutFiscale, null, false);
			sec.setRaisonSociale("Toto et compagnie Lausanne");
			sec.setEnseigne("Toto & cie");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// lancement de l'appariement (SIMULATION)

		final AppariementEtablissementsSecondairesResults simulation = processor.run(1, true, null);
		Assert.assertNotNull(simulation);
		Assert.assertEquals(0, simulation.getErreurs().size());
		Assert.assertEquals(1, simulation.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = simulation.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.SEULS_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base (SIMULATION -> rien)
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertNull(sec.getNumeroEtablissement());
			Assert.assertEquals("Toto et compagnie Lausanne", sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			final List<DomicileEtablissement> domiciles = hibernateTemplate.find("FROM DomicileEtablissement de ORDER BY de.dateDebut ASC", null);
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(2, domiciles.size());
			{
				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDebutFiscale, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = domiciles.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal, domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			return null;
		});

		// lancement de l'appariement (REEL)

		final AppariementEtablissementsSecondairesResults reel = processor.run(1, false, null);
		Assert.assertNotNull(reel);
		Assert.assertEquals(0, reel.getErreurs().size());
		Assert.assertEquals(1, reel.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = reel.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.SEULS_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertEquals((Long) noCantonalEtablissementSecondaire1, sec.getNumeroEtablissement());
			Assert.assertNull(sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			// le domicile fiscal actif au moment de la date de début civile a été fermé à la veille
			final List<DomicileEtablissement> secDomiciles = sec.getSortedDomiciles(true);
			Assert.assertNotNull(secDomiciles);
			Assert.assertEquals(2, secDomiciles.size());
			{
				final DomicileEtablissement domicile = secDomiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDebutFiscale, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = secDomiciles.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal, domicile.getDateDebut());
				Assert.assertEquals(dateDebutCivile.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			return null;
		});
	}

	@Test
	public void testAppariementDeplacementFermetureDomicileFiscal() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56795846L;
		final RegDate dateDebutCivile = date(2015, 8, 1);
		final RegDate dateDebutFiscale = date(2010, 12, 1);
		final RegDate dateDemenagementFiscal1 = dateDebutCivile.addMonths(-2);
		final RegDate dateDemenagementFiscal2 = dateDebutCivile.addMonths(2);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebutCivile, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebutCivile.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebutCivile, null, "Toto Lausanne", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, null,
				                                               null, null, "CHE999999997", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseConnueAuCivil(noCantonalEntreprise);
			addRegimeFiscalVD(e, dateDebutFiscale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebutFiscale, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebutFiscale, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Etablissement prn = addEtablissement();
			prn.setNumeroEtablissement(noCantonalEtablissementPrincipal);
			addActiviteEconomique(e, prn, dateDebutFiscale, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebutFiscale, dateDemenagementFiscal1.getOneDayBefore(), MockCommune.Prilly);
			addDomicileEtablissement(sec, dateDemenagementFiscal1, dateDemenagementFiscal2.getOneDayBefore(), MockCommune.Renens);
			addDomicileEtablissement(sec, dateDemenagementFiscal2, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec, dateDebutFiscale, null, false);
			sec.setRaisonSociale("Toto Lausanne");
			sec.setEnseigne("Toto & cie");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// lancement de l'appariement (SIMULATION)

		final AppariementEtablissementsSecondairesResults simulation = processor.run(1, true, null);
		Assert.assertNotNull(simulation);
		Assert.assertEquals(0, simulation.getErreurs().size());
		Assert.assertEquals(1, simulation.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = simulation.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.SEULS_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base (SIMULATION -> rien)
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertNull(sec.getNumeroEtablissement());
			Assert.assertEquals("Toto Lausanne", sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			final List<DomicileEtablissement> domiciles = hibernateTemplate.find("FROM DomicileEtablissement de ORDER BY de.dateDebut ASC", null);
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(3, domiciles.size());
			{
				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDebutFiscale, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal1.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = domiciles.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal1, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal2.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Renens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = domiciles.get(2);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal2, domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			return null;
		});

		// lancement de l'appariement (REEL)

		final AppariementEtablissementsSecondairesResults reel = processor.run(1, false, null);
		Assert.assertNotNull(reel);
		Assert.assertEquals(0, reel.getErreurs().size());
		Assert.assertEquals(1, reel.getAppariements().size());
		{
			final AppariementEtablissementsSecondairesResults.AppariementEtablissement appariement = reel.getAppariements().get(0);
			Assert.assertNotNull(appariement);
			Assert.assertEquals(ids.idEtablissementSecondaire, appariement.idEtablissement);
			Assert.assertEquals(noCantonalEtablissementSecondaire1, appariement.idEtablissementCivil);
			Assert.assertEquals(AppariementEtablissementsSecondairesResults.RaisonAppariement.SEULS_MEME_ENDROIT, appariement.raison);
		}

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			Assert.assertNotNull(e);
			Assert.assertEquals((Long) noCantonalEntreprise, e.getNumeroEntreprise());

			final Etablissement prn = (Etablissement) tiersService.getTiers(ids.idEtablissementPrincipal);
			Assert.assertNotNull(prn);
			Assert.assertEquals((Long) noCantonalEtablissementPrincipal, prn.getNumeroEtablissement());

			final Etablissement sec = (Etablissement) tiersService.getTiers(ids.idEtablissementSecondaire);
			Assert.assertNotNull(sec);
			Assert.assertEquals((Long) noCantonalEtablissementSecondaire1, sec.getNumeroEtablissement());
			Assert.assertNull(sec.getRaisonSociale());
			Assert.assertEquals("Toto & cie", sec.getEnseigne());

			// le domicile fiscal actif au moment de la date de début civile a été fermé à la veille
			final List<DomicileEtablissement> secDomiciles = sec.getSortedDomiciles(true);
			Assert.assertNotNull(secDomiciles);
			Assert.assertEquals(4, secDomiciles.size());
			{
				final DomicileEtablissement domicile = secDomiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDebutFiscale, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal1.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = secDomiciles.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal1, domicile.getDateDebut());
				Assert.assertEquals(dateDebutCivile.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Renens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = secDomiciles.get(2);
				Assert.assertNotNull(domicile);
				Assert.assertTrue(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal1, domicile.getDateDebut());
				Assert.assertEquals(dateDemenagementFiscal2.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Renens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			{
				final DomicileEtablissement domicile = secDomiciles.get(3);
				Assert.assertNotNull(domicile);
				Assert.assertTrue(domicile.isAnnule());
				Assert.assertSame(sec, domicile.getEtablissement());
				Assert.assertEquals(dateDemenagementFiscal2, domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}
			return null;
		});
	}
}
