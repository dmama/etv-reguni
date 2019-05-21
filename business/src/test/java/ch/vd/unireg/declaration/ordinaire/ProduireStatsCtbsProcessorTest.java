package ch.vd.unireg.declaration.ordinaire;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

public class ProduireStatsCtbsProcessorTest extends BusinessTest {

	private ProduireStatsCtbsProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		final PeriodeImpositionImpotSourceService piisService = getBean(PeriodeImpositionImpotSourceService.class, "periodeImpositionImpotSourceService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		processor = new ProduireStatsCtbsProcessor(hibernateTemplate, serviceInfra, tiersService, transactionManager, assujettissementService, periodeImpositionService, adresseService, piisService);
	}

	@Test
	public void testExtractionEntreprise() throws Exception {

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idAssujettie;
			long idAssujettieMaisDiSurPFSuivante;
		}

		final int anneeExtraction = 2016;
		final RegDate dateDebut = date(2009, 5, 1);
		final RegDate dateAchat = date(anneeExtraction + 1, 3, 6);      // en tout cas avant le 30.06

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise assujettie = addEntrepriseInconnueAuCivil();
			addRaisonSociale(assujettie, dateDebut, null, "Assujettie SA");
			addFormeJuridique(assujettie, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(assujettie, dateDebut, DayMonth.get(6, 30), 12);              // tous les 30.06 depuis 2009
			addRegimeFiscalCH(assujettie, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(assujettie, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(assujettie, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens);

			final Entreprise assujettiePlusTard = addEntrepriseInconnueAuCivil();
			addRaisonSociale(assujettiePlusTard, dateAchat, null, "Assujettie Plustard SA");
			addFormeJuridique(assujettiePlusTard, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(assujettiePlusTard, dateDebut, DayMonth.get(6, 30), 12);              // tous les 30.06 depuis 2009
			addRegimeFiscalCH(assujettiePlusTard, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(assujettiePlusTard, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(assujettiePlusTard, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);
			addForSecondaire(assujettiePlusTard, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Grandson, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);

			final Ids res = new Ids();
			res.idAssujettie = assujettie.getNumero();
			res.idAssujettieMaisDiSurPFSuivante = assujettiePlusTard.getNumero();
			return res;
		});

		// lancement du job d'extraction
		final StatistiquesCtbs results = processor.runPM(anneeExtraction, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(anneeExtraction, results.annee);
		Assert.assertEquals(1, results.nbCtbsTotal);
		Assert.assertNotNull(results.stats);
		Assert.assertEquals(1, results.stats.size());
		final Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> entry = results.stats.entrySet().iterator().next();
		Assert.assertNotNull(entry);
		Assert.assertEquals(new StatistiquesCtbs.Key(ServiceInfrastructureRaw.noOIPM, MockCommune.Echallens, StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE), entry.getKey());
		Assert.assertEquals(1, entry.getValue().nombre);
	}


		@Test
	public void testExtractionCtbSourceAvecPIIS() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idAssujettie;
			long idAssujettieMaisDiSurPFSuivante;
		}

		final int anneeExtraction = 2017;
		final RegDate dateDebutVaud = date(2013, 1, 1);
		final RegDate dateFinVaud = date(2015, 4, 30);
		final RegDate dateDebut = date(2015, 5, 1);
		final RegDate dateAchat = date(anneeExtraction + 1, 3, 6);      // en tout cas avant le 30.06


			// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur();
			final PersonnePhysique assujettie = addNonHabitant("Martial","Loiseau",RegDate.get(1985,12,4), Sexe.MASCULIN);

			addForPrincipal(assujettie, dateDebutVaud, MotifFor.ARRIVEE_HS,dateFinVaud,MotifFor.DEPART_HS,MockCommune.Lausanne, ModeImposition.SOURCE );
			addForPrincipal(assujettie, dateDebut, MotifFor.DEPART_HS, MockPays.RoyaumeUni, ModeImposition.SOURCE );
			addRapportPrestationImposable(dpi,assujettie,date(2015,2,1),null,false);

			final Ids res = new Ids();
			res.idAssujettie = assujettie.getNumero();
			return res;
		});

		// lancement du job d'extraction
		final StatistiquesCtbs results = processor.runPP(anneeExtraction, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(anneeExtraction, results.annee);
		Assert.assertEquals(1, results.nbCtbsTotal);
		Assert.assertNotNull(results.stats);
		Assert.assertEquals(1, results.stats.size());
		final Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> entry = results.stats.entrySet().iterator().next();
		Assert.assertNotNull(entry);
		Assert.assertEquals(new StatistiquesCtbs.Key(7, MockCommune.Lausanne, StatistiquesCtbs.TypeContribuable.SOURCIER_PUR), entry.getKey());
		Assert.assertEquals(1, entry.getValue().nombre);
	}


	@Test
	public void testExtractionCtbSourceSansPIIS() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idAssujettie;
		}

		final int anneeExtraction = 2017;
		final RegDate dateDebutVaud = date(2013, 1, 1);
		final RegDate dateFinVaud = date(2015, 4, 30);
		final RegDate dateDebut = date(2015, 5, 1);
		final RegDate dateAchat = date(anneeExtraction + 1, 3, 6);      // en tout cas avant le 30.06


		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique assujettie = addNonHabitant("Martial","Loiseau",RegDate.get(1985,12,4), Sexe.MASCULIN);

			addForPrincipal(assujettie, dateDebutVaud, MotifFor.ARRIVEE_HS,dateFinVaud,MotifFor.DEPART_HS,MockCommune.Lausanne, ModeImposition.SOURCE );
			addForPrincipal(assujettie, dateDebut, MotifFor.DEPART_HS, MockPays.RoyaumeUni, ModeImposition.SOURCE );

			final Ids res = new Ids();
			res.idAssujettie = assujettie.getNumero();
			return res;
		});

		// lancement du job d'extraction
		final StatistiquesCtbs results = processor.runPP(anneeExtraction, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(anneeExtraction, results.annee);
		Assert.assertEquals(0, results.nbCtbsTotal);
	}

//	@Test
//	public void testExtractionCtbAvecForSurCommuneFusionnee() throws Exception {
//
//		// mise en place civile
//		serviceCivil.setUp(new MockIndividuConnector() {
//			@Override
//			protected void init() {
//				// rien
//			}
//		});
//
//		final class Ids {
//			long idAssujettie;
//			long idAssujettieMaisDiSurPFSuivante;
//		}
//
//		final int anneeExtraction = 2017;
//		final RegDate dateDebut = date(2009, 5, 1);
//		final RegDate dateAchat = date(anneeExtraction + 1, 3, 6);      // en tout cas avant le 30.06
//
//		// mise en place fiscale
//		final Ids ids = doInNewTransactionAndSession(status -> {
//			final PersonnePhysique assujettie = addNonHabitant("Martial","Loiseau",RegDate.get(1985,12,4), Sexe.MASCULIN);
//
//			addForPrincipal(assujettiePlusTard, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);
//			addForSecondaire(assujettiePlusTard, dateAchat, MotifFor.ACHAT_IMMOBILIER, MockCommune.Grandson.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);
//
//			final Ids res = new Ids();
//			res.idAssujettie = assujettie.getNumero();
//			res.idAssujettieMaisDiSurPFSuivante = assujettiePlusTard.getNumero();
//			return res;
//		});
//
//		// lancement du job d'extraction
//		final StatistiquesCtbs results = processor.runPM(anneeExtraction, RegDate.get(), null);
//		Assert.assertNotNull(results);
//		Assert.assertEquals(anneeExtraction, results.annee);
//		Assert.assertEquals(1, results.nbCtbsTotal);
//		Assert.assertNotNull(results.stats);
//		Assert.assertEquals(1, results.stats.size());
//		final Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> entry = results.stats.entrySet().iterator().next();
//		Assert.assertNotNull(entry);
//		Assert.assertEquals(new StatistiquesCtbs.Key(ServiceInfrastructureRaw.noOIPM, MockCommune.Echallens, StatistiquesCtbs.TypeContribuable.VAUDOIS_ORDINAIRE), entry.getKey());
//		Assert.assertEquals(1, entry.getValue().nombre);
//	}
}
