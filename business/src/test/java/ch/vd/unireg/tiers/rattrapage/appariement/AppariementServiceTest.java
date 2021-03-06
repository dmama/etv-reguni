package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.ArrayList;
import java.util.Collections;
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
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AppariementServiceTest extends BusinessTest {

	private AppariementService appariementService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.appariementService = getBean(AppariementService.class, "appariementService");
	}

	@Test
	public void testEntrepriseNonAppariee() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire = 43278L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
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
		final long pmId = doInNewTransactionAndSession(status -> {
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
			return e.getNumero();
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(pmId);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(0, candidats.size());
			return null;
		});
	}

	@Test
	public void testAppariementNumeroIDE() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56795846L;
		final RegDate dateDebut = date(2010, 12, 1);
		final String ide = "CHE101390939";

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
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
			addIdentificationEntreprise(sec, ide);

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(1, candidats.size());

			{
				final CandidatAppariement candidat = candidats.get(0);
				Assert.assertNotNull(candidat);
				Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getEtablissementCivil().getNumeroEtablissement());
				Assert.assertEquals((Long) ids.idEtablissementSecondaire, candidat.getEtablissement().getNumero());
				Assert.assertEquals(CandidatAppariement.CritereDecisif.IDE, candidat.getCritere());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
			}
			;
			return null;
		});
	}

	@Test
	public void testAppariementNumeroIDEMaisMauvaiseCommune() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56795846L;
		final RegDate dateDebut = date(2010, 12, 1);
		final String ide = "CHE101390939";

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
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
			addDomicileEtablissement(prn, dateDebut, null, MockCommune.Echallens);
			addActiviteEconomique(e, prn, dateDebut, null, true);

			final Etablissement sec = addEtablissement();
			addDomicileEtablissement(sec, dateDebut, null, MockCommune.Prilly);     // dans le civil, l'établissement avec ce numéro IDE est à Lausanne
			addActiviteEconomique(e, sec, dateDebut, null, false);
			sec.setRaisonSociale("Toto et compagnie Lausanne");
			addIdentificationEntreprise(sec, ide);

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(0, candidats.size());
			return null;
		});
	}

	@Test
	public void testAppariementCommune() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56783L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999997", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebut, null, "Toto Lausanne 2", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Prilly.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
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
			sec.setRaisonSociale("Toto Lausanne");      // même avec une raison sociale différente, comme c'est le seul établissement sur Lausanne

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(1, candidats.size());

			{
				final CandidatAppariement candidat = candidats.get(0);
				Assert.assertNotNull(candidat);
				Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getEtablissementCivil().getNumeroEtablissement());
				Assert.assertEquals((Long) ids.idEtablissementSecondaire, candidat.getEtablissement().getNumero());
				Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
			}
			;
			return null;
		});
	}

	@Test
	public void testAppariementPlusieursCommunes() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56783L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999997", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebut, null, "Toto Lausanne 2", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Prilly.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE99999998", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire1;
			long idEtablissementSecondaire2;
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

			final Etablissement sec1 = addEtablissement();
			addDomicileEtablissement(sec1, dateDebut, null, MockCommune.Prilly);
			addActiviteEconomique(e, sec1, dateDebut, null, false);
			sec1.setRaisonSociale("Toto Prilly");      // même avec une raison sociale différente, comme c'est le seul établissement sur Prilly

			final Etablissement sec2 = addEtablissement();
			addDomicileEtablissement(sec2, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec2, dateDebut, null, false);
			sec2.setRaisonSociale("Toto Lausanne");      // même avec une raison sociale différente, comme c'est le seul établissement sur Lausanne

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire1 = sec1.getNumero();
			ids1.idEtablissementSecondaire2 = sec2.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(2, candidats.size());

			final List<CandidatAppariement> candidatsTries = new ArrayList<>(candidats);
			Collections.sort(candidatsTries, (o1, o2) -> Long.compare(o1.getEtablissement().getNumero(), o2.getEtablissement().getNumero()));

			{
				final CandidatAppariement candidat = candidatsTries.get(0);
				Assert.assertNotNull(candidat);
				Assert.assertEquals(noCantonalEtablissementSecondaire2, candidat.getEtablissementCivil().getNumeroEtablissement());
				Assert.assertEquals((Long) ids.idEtablissementSecondaire1, candidat.getEtablissement().getNumero());
				Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
				Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), candidat.getOfsSiege());
			}
			{
				final CandidatAppariement candidat = candidatsTries.get(1);
				Assert.assertNotNull(candidat);
				Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getEtablissementCivil().getNumeroEtablissement());
				Assert.assertEquals((Long) ids.idEtablissementSecondaire2, candidat.getEtablissement().getNumero());
				Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
			}
			;
			return null;
		});
	}

	/**
	 * [SIFISC-19770] Au final, on ne fait plus l'appariement sur les raisons sociales identiques quand on a plusieurs
	 * établissements/établissements civils sur une commune donnée avec un même flag d'activité
	 */
	@Test
	public void testAppariementPlusieursEtablissementsCivilsSurMemeCommuneDifferenciesParRaisonSociale() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56783L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999997", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebut, null, "Toto Lausanne 2", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999996", null, null);
			}
		});

		final class Ids {
			long idEntreprise;
			long idEtablissementPrincipal;
			long idEtablissementSecondaire1;
			long idEtablissementSecondaire2;
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

			final Etablissement sec1 = addEtablissement();
			addDomicileEtablissement(sec1, dateDebut, null, MockCommune.Prilly);
			addActiviteEconomique(e, sec1, dateDebut, null, false);
			sec1.setRaisonSociale("Toto Prilly");

			final Etablissement sec2 = addEtablissement();
			addDomicileEtablissement(sec2, dateDebut, null, MockCommune.Lausanne);
			addActiviteEconomique(e, sec2, dateDebut, null, false);
			sec2.setRaisonSociale("Toto Lausanne 1");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire1 = sec1.getNumero();
			ids1.idEtablissementSecondaire2 = sec2.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(0, candidats.size());
			return null;
		});
	}

	@Test
	public void testAppariementMauvaiseCommune() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56783L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = addEntreprise(noCantonalEntreprise);
				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementPrincipal, ent, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                               true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                               StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.SITE, "CHE999999996", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire1, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                               null, null, "CHE999999997", null, null);

				MockEtablissementCivilFactory.addEtablissement(noCantonalEtablissementSecondaire2, ent, dateDebut, null, "Toto Lausanne 1", null,
				                                               false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                               MockCommune.Prilly.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
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
			addDomicileEtablissement(sec, dateDebut, null, MockCommune.Renens);     // on ne connait que Lausanne et Prilly dans le civil
			addActiviteEconomique(e, sec, dateDebut, null, false);
			sec.setRaisonSociale("Toto Lausanne 1");

			final Ids ids1 = new Ids();
			ids1.idEntreprise = e.getNumero();
			ids1.idEtablissementPrincipal = prn.getNumero();
			ids1.idEtablissementSecondaire = sec.getNumero();
			return ids1;
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
			final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
			Assert.assertNotNull(candidats);
			Assert.assertEquals(0, candidats.size());
			return null;
		});
	}
}
