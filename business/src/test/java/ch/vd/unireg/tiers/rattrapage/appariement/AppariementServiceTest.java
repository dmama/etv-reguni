package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999995", null, null);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire, org, dateDebut, null, "Toto Lausanne", null,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                    null, null, "CHE999999996", null, null);
			}
		});

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
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
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(pmId);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(0, candidats.size());
			}
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				final MockSiteOrganisation sec = MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                                                     false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                     MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                                                     null, null, ide, null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 2", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire = sec.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(1, candidats.size());

				{
					final CandidatAppariement candidat = candidats.get(0);
					Assert.assertNotNull(candidat);
					Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getSite().getNumeroSite());
					Assert.assertEquals((Long) ids.idEtablissementSecondaire, candidat.getEtablissement().getNumero());
					Assert.assertEquals(CandidatAppariement.CritereDecisif.IDE, candidat.getCritere());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
				}
			}
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				final MockSiteOrganisation sec = MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                                                     false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                     MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                                                     null, null, ide, null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 2", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire = sec.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(0, candidats.size());
			}
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                    null, null, "CHE999999997", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 2", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire = sec.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(1, candidats.size());

				{
					final CandidatAppariement candidat = candidats.get(0);
					Assert.assertNotNull(candidat);
					Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getSite().getNumeroSite());
					Assert.assertEquals((Long) ids.idEtablissementSecondaire, candidat.getEtablissement().getNumero());
					Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
				}
			}
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                    null, null, "CHE999999997", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 2", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire1 = sec1.getNumero();
				ids.idEtablissementSecondaire2 = sec2.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(2, candidats.size());

				final List<CandidatAppariement> candidatsTries = new ArrayList<>(candidats);
				Collections.sort(candidatsTries, new Comparator<CandidatAppariement>() {
					@Override
					public int compare(CandidatAppariement o1, CandidatAppariement o2) {
						return Long.compare(o1.getEtablissement().getNumero(), o2.getEtablissement().getNumero());
					}
				});

				{
					final CandidatAppariement candidat = candidatsTries.get(0);
					Assert.assertNotNull(candidat);
					Assert.assertEquals(noCantonalEtablissementSecondaire2, candidat.getSite().getNumeroSite());
					Assert.assertEquals((Long) ids.idEtablissementSecondaire1, candidat.getEtablissement().getNumero());
					Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
					Assert.assertEquals((Integer) MockCommune.Prilly.getNoOFS(), candidat.getOfsSiege());
				}
				{
					final CandidatAppariement candidat = candidatsTries.get(1);
					Assert.assertNotNull(candidat);
					Assert.assertEquals(noCantonalEtablissementSecondaire1, candidat.getSite().getNumeroSite());
					Assert.assertEquals((Long) ids.idEtablissementSecondaire2, candidat.getEtablissement().getNumero());
					Assert.assertEquals(CandidatAppariement.CritereDecisif.LOCALISATION, candidat.getCritere());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, candidat.getTypeAutoriteFiscaleSiege());
					Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), candidat.getOfsSiege());
				}
			}
		});
	}

	/**
	 * [SIFISC-19770] Au final, on ne fait plus l'appariement sur les raisons sociales identiques quand on a plusieurs
	 * établissements/sites sur une commune donnée avec un même flag d'activité
	 */
	@Test
	public void testAppariementPlusieursEtablissementsCivilsSurMemeCommuneDifferenciesParRaisonSociale() throws Exception {

		final long noCantonalEntreprise = 23563127543L;
		final long noCantonalEtablissementPrincipal = 32782537L;
		final long noCantonalEtablissementSecondaire1 = 43278L;
		final long noCantonalEtablissementSecondaire2 = 56783L;
		final RegDate dateDebut = date(2010, 12, 1);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                    null, null, "CHE999999997", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 2", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire1 = sec1.getNumero();
				ids.idEtablissementSecondaire2 = sec2.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(0, candidats.size());
			}
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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				MockSiteOrganisationFactory.addSite(noCantonalEtablissementPrincipal, org, dateDebut, null, "Toto Echallens", FormeLegale.N_0106_SOCIETE_ANONYME,
				                                    true, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Echallens.getNoOFS(), StatusInscriptionRC.ACTIF, dateDebut.addDays(-3),
				                                    StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.SITE, "CHE999999996", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire1, org, dateDebut, null, "Toto Lausanne 1", null,
				                                    false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                    MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.INCONNU, dateDebut.addDays(-3),
				                                    null, null, "CHE999999997", null, null);

				MockSiteOrganisationFactory.addSite(noCantonalEtablissementSecondaire2, org, dateDebut, null, "Toto Lausanne 1", null,
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
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

				final Ids ids = new Ids();
				ids.idEntreprise = e.getNumero();
				ids.idEtablissementPrincipal = prn.getNumero();
				ids.idEtablissementSecondaire = sec.getNumero();
				return ids;
			}
		});

		// calcul des candidats à l'appariement
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersService.getTiers(ids.idEntreprise);
				final List<CandidatAppariement> candidats = appariementService.rechercheAppariementsEtablissementsSecondaires(entreprise);
				Assert.assertNotNull(candidats);
				Assert.assertEquals(0, candidats.size());
			}
		});
	}
}
