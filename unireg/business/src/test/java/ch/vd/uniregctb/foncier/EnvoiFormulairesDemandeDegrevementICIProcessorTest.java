package ch.vd.uniregctb.foncier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscalService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class EnvoiFormulairesDemandeDegrevementICIProcessorTest extends BusinessTest {

	private EnvoiFormulairesDemandeDegrevementICIProcessor processor;
	private DelaisService delaisService;
	private ParametreAppService parametreAppService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		final AutreDocumentFiscalService autreDocumentFiscalService = getBean(AutreDocumentFiscalService.class, "autreDocumentFiscalService");
		processor = new EnvoiFormulairesDemandeDegrevementICIProcessor(transactionManager, autreDocumentFiscalService, hibernateTemplate, tiersService);
		delaisService = getBean(DelaisService.class, "delaisService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
	}

	/**
	 * Développe les couples (clé, valeur1) (clé, valeur2)...
	 * @param src map contenant les valeurs associées à chaque clé
	 * @return une liste de couples (clé, valeur)
	 */
	private static List<Pair<Long, Long>> develop(List<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable> src) {
		if (src == null) {
			return null;
		}
		return src.stream()
				.map(entry -> entry.getIdsDroitsImmeubles().stream().map(immeuble -> Pair.of(entry.getNoContribuable(), immeuble.getIdImmeuble())))
				.flatMap(Function.identity())
				.collect(Collectors.toList());
	}

	/**
	 * Simple vérification syntaxique de la requête...
	 */
	@Test
	public void testPopulationInspecteeSurBaseVide() throws Exception {

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale -> rien...

		// lancement de la requête (simple vérification syntaxique, du coup)
		final List<Pair<Long, Long>> couples = develop(processor.findCouples(RegDate.get()));
		Assert.assertNotNull(couples);
		Assert.assertEquals(0, couples.size());
	}

	@Test
	public void testPopulationInspectee() throws Exception {

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final class Ids {
			long pp;
			long pm1;
			long pm2;
			long imm1;
			long imm2;
		}
		final Ids ids = doInNewTransactionAndSession(status -> {

			// une personne physique et deux entreprises
			final PersonnePhysique pp = addNonHabitant("Francis", "Lulune", null, Sexe.MASCULIN);
			final Entreprise pm1 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm1, date(2009, 4,1), null, "Première PM");
			addFormeJuridique(pm1, date(2009, 4,1), null, FormeJuridiqueEntreprise.SA);
			addBouclement(pm1, date(2009, 4, 1), DayMonth.get(12, 31), 12);
			final Entreprise pm2 = addEntrepriseInconnueAuCivil();
			addRaisonSociale(pm2, date(2009, 4,1), null, "Seconde PM");
			addFormeJuridique(pm2, date(2009, 4,1), null, FormeJuridiqueEntreprise.SA);
			addBouclement(pm2, date(2009, 4, 1), DayMonth.get(12, 31), 12);

			// les tiers RF correspondants
			final PersonnePhysiqueRF rfPP = addPersonnePhysiqueRF("Francis", "Lulune", null, "4801784ldshsf", 6526485L, null);
			final PersonneMoraleRF rfPM1 = addPersonneMoraleRF("Première PM", null, "ffe47853vbf", 8178451L, null);
			final PersonneMoraleRF rfPM2 = addPersonneMoraleRF("Seconde PM", null, "15102dfmjsgfs", 55264121L, null);

			// rapprochements contribuable <-> tiers RF
			addRapprochementRF(pp, rfPP, null, null, TypeRapprochementRF.AUTO);
			addRapprochementRF(pm1, rfPM1, date(2012, 1, 1), null, TypeRapprochementRF.MANUEL);
			addRapprochementRF(pm2, rfPM2, null, null, TypeRapprochementRF.AUTO);

			// deux immeubles
			final BienFondRF imm1 = addImmeubleRF("jkbgbjfwbfhqlb");
			final BienFondRF imm2 = addImmeubleRF("fjewh32z35g2");

			// des droits sur les immeubles
			addDroitPersonnePhysiqueRF(date(2010, 1, 1), date(2010, 1, 1), date(2013, 5, 2), "Achat", "Vente", "5413fshsguhsghs", null, new Fraction(1, 1000), GenrePropriete.COPROPRIETE, rfPP, imm1, null);
			addDroitPersonnePhysiqueRF(date(2010, 1, 1), date(2010, 1, 1), null, "Achat", null, "481347sshsfga", null, new Fraction(8, 1000), GenrePropriete.COPROPRIETE, rfPP, imm2, null);
			addDroitPersonneMoraleRF(date(2013, 1, 1), date(2013, 1, 1), null, "Achat", null, "456156fnjsdgfseewgbf", null, new Fraction(354, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm1, null);
			addDroitPersonneMoraleRF(date(2010, 1, 1), date(2010, 1, 1), date(2013, 5, 2), "Achat", "Vente", "45815348481hdfktgerfw", null, new Fraction(142, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm2, null);
			addDroitPersonneMoraleRF(date(2014, 1, 1), date(2014, 1, 1), null, "Achat", null, "jhbfshsbhsb", null, new Fraction(12, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm2, null);
			addDroitPersonneMoraleRF(date(2015, 1, 1), date(2015, 1, 1), date(2016, 5, 2), "Achat", "Vente", "41534154sdfjkgfsadg", null, new Fraction(157, 1000), GenrePropriete.COPROPRIETE, rfPM2, imm1, null);
			addDroitPersonneMoraleRF(date(2010, 1, 1), date(2010, 1, 1), null, "Achat", null, "18451438fkjfgdsj", null, new Fraction(1, 1000), GenrePropriete.COPROPRIETE, rfPM2, imm2, null);

			final Ids results = new Ids();
			results.pp = pp.getNumero();
			results.pm1 = pm1.getNumero();
			results.pm2 = pm2.getNumero();
			results.imm1 = imm1.getId();
			results.imm2 = imm2.getId();
			return results;
		});

		// ok, maintenant on va chercher la population à diverses dates
		{
			// date trop lointaine dans le passé -> rien (car aucun droit encore ouvert)
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2009, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(0, population.size());
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2010, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(1, population.size());
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(0));       // le lien PM1 <-> IMM2 ne sort pas car le rapprochement n'est pas valide à la date de traitement
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2012, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(2, population.size());
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm2), population.get(0));       // le rapprochement est enfin valable
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(1));
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2013, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(3, population.size());
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm1), population.get(0));
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm2), population.get(1));
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(2));
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2014, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(3, population.size());
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm1), population.get(0));
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm2), population.get(1));
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(2));
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2015, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(4, population.size());
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm1), population.get(0));
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm2), population.get(1));
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm1), population.get(2));
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(3));
		}
		{
			final List<Pair<Long, Long>> population = develop(processor.findCouples(date(2017, 1, 1)));
			Assert.assertNotNull(population);
			Assert.assertEquals(3, population.size());
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm1), population.get(0));
			Assert.assertEquals(Pair.of(ids.pm1, ids.imm2), population.get(1));
			Assert.assertEquals(Pair.of(ids.pm2, ids.imm2), population.get(2));
		}
	}

	@Test
	public void testEnvoiSimple() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testEnvoiSimpleAvecDemandeAnnuleeExistante() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final DemandeDegrevementICI demandeExistante = addDemandeDegrevementICI(entreprise, dateDebutDroit.addDays(10), dateDebutDroit.addMonths(2), null, null, dateTraitement.year() + 1, immeuble);
			demandeExistante.setAnnule(true);
			demandeExistante.setNumeroSequence(2);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, false);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 3, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testIgnoreEstimationFiscaleAbsente() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertEquals((Integer) 54, ignore.index1);
			Assert.assertEquals((Integer) 12, ignore.index2);
			Assert.assertEquals((Integer) 53, ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.ESTIMATION_FISCALE_ABSENTE_OU_ZERO, ignore.raison);
			Assert.assertEquals(null, ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testIgnoreEstimationFiscaleZero() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 0L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertEquals((Integer) 54, ignore.index1);
			Assert.assertEquals((Integer) 12, ignore.index2);
			Assert.assertEquals((Integer) 53, ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.ESTIMATION_FISCALE_ABSENTE_OU_ZERO, ignore.raison);
			Assert.assertEquals(null, ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testIgnoreDegrevementActif() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			addDegrevementICI(entreprise,
			                  immeuble,
			                  dateDebutDroit,
			                  null,
			                  new DonneesUtilisation(100000, null, 300, BigDecimal.valueOf(25), null),
			                  new DonneesUtilisation(300000, null, 900, BigDecimal.valueOf(75), null),
			                  null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertEquals((Integer) 54, ignore.index1);
			Assert.assertEquals((Integer) 12, ignore.index2);
			Assert.assertEquals((Integer) 53, ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DEGREVEMENT_DEJA_ACTIF_ANNEE_SUIVANT_DEBUT_DROIT, ignore.raison);
			Assert.assertEquals("Année suivant début de droit : " + (dateDebutDroit.year() + 1), ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testEnvoiDemandeDejaFaiteMaisPasAnneeSuivantDebutDroit() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2013, 12, 1), null, null, false, 484541745L, "2013", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			// même PF que le début du droit... ça n'empêche pas l'envoi
			addDemandeDegrevementICI(entreprise, dateDebutDroit.addDays(10), dateDebutDroit.addMonths(2), null, null, dateDebutDroit.year(), immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertEquals((Integer) 54, envoi.index1);
			Assert.assertEquals((Integer) 12, envoi.index2);
			Assert.assertEquals((Integer) 53, envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(2, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					Assert.assertEquals(dateDebutDroit.addDays(10), demande.getDateEnvoi());
					Assert.assertEquals(dateDebutDroit.addMonths(2), demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) dateDebutDroit.year(), demande.getPeriodeFiscale());
				}
				{
					final DemandeDegrevementICI demande = demandes.get(1);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testIgnoreDemandeDejaFaitePourAnneeSuivantDebutDroit() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			// PF suivante par rapport au début du droit... ça empêche l'envoi
			addDemandeDegrevementICI(entreprise, dateDebutDroit.addDays(10), dateDebutDroit.addMonths(2), null, null, dateDebutDroit.year() + 1, immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertEquals((Integer) 54, ignore.index1);
			Assert.assertEquals((Integer) 12, ignore.index2);
			Assert.assertEquals((Integer) 53, ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT, ignore.raison);
			Assert.assertEquals("Demande émise le " + RegDateHelper.dateToDisplayString(dateDebutDroit.addDays(10)) + " pour la PF " + (dateDebutDroit.year() + 1), ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());            // la demande déjà posée en entrée
			}
		});
	}

	@Test
	public void testEnvoiDemandeDejaFaiteMaisPasAnneeDerniereEstimationFiscale() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, date(2016, 12, 31), false, 484541745L, "RG2015", immeuble);
			addEstimationFiscale(date(2017, 1, 6), date(2017, 1, 1), null, false, 46512165L, "2017", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			// même PF que l'estimation fiscale... ça n'empêche pas l'envoi
			addDemandeDegrevementICI(entreprise, dateDebutDroit.addYears(1), dateDebutDroit.addMonths(14), null, null, 2015, immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertEquals((Integer) 54, envoi.index1);
			Assert.assertEquals((Integer) 12, envoi.index2);
			Assert.assertEquals((Integer) 53, envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(2, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					Assert.assertEquals(dateDebutDroit.addYears(1), demande.getDateEnvoi());
					Assert.assertEquals(dateDebutDroit.addMonths(14), demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) dateDebutDroit.year(), demande.getPeriodeFiscale());
				}
				{
					final DemandeDegrevementICI demande = demandes.get(1);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testIgnoreDemandeDejaFaitePourAnneeDerniereEstimationFiscale() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2010, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2010, 12, 1), null, date(2014, 12, 31), false, 484541745L, "2010", immeuble);
			addEstimationFiscale(date(2015, 12, 1), date(2015, 1, 1), null, false, 454545445L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			// PF suivante par rapport à la dernière estimation fiscale... ça empêche l'envoi
			addDemandeDegrevementICI(entreprise, date(2015, 1, 25), date(2015, 2, 28), null, null, 2016, immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertEquals((Integer) 54, ignore.index1);
			Assert.assertEquals((Integer) 12, ignore.index2);
			Assert.assertEquals((Integer) 53, ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_ESTIMATION_FISCALE, ignore.raison);
			Assert.assertEquals("Demande émise le 25.01.2015 pour la PF 2016", ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());        // demande déjà posée en entrée
			}
		});
	}

	@Test
	public void testIgnoreCompletementExonere() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ART90C);          // commune
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, RegDate.get(), null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertNull(ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertNull(ignore.nomCommune);
			Assert.assertNull(ignore.noOfsCommune);
			Assert.assertNull(ignore.noParcelle);
			Assert.assertNull(ignore.index1);
			Assert.assertNull(ignore.index2);
			Assert.assertNull(ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.CONTRIBUABLE_TOTALEMENT_EXONERE, ignore.raison);
			Assert.assertEquals("1 droit(s) concernés pour 1 immeuble(s)", ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testEnvoiSimpleSurDeuxImmeublesAchetesLaMemeAnnee() throws Exception {
		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble1;
			long idImmeuble2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF pmRF = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, pmRF, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune1 = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble1 = addBienFondRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondRF immeuble2 = addBienFondRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "sdfe7to34z57", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble1 = immeuble1.getId();
			identifiants.idImmeuble2 = immeuble2.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(2, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble1, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(1);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble2, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Yverdon", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.YverdonLesBains.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 142, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(2, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
				{
					final DemandeDegrevementICI demande = demandes.get(1);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 2, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testEnvoiSimpleSurDeuxImmeublesAchetesLaMemeAnneeAvecMaxEnvoisNonLimitant() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble1;
			long idImmeuble2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF pmRF = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, pmRF, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune1 = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble1 = addBienFondRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondRF immeuble2 = addBienFondRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "sdfe7to34z57", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble1 = immeuble1.getId();
			identifiants.idImmeuble2 = immeuble2.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, 2, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(2, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble1, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(1);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble2, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Yverdon", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.YverdonLesBains.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 142, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(2, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
				{
					final DemandeDegrevementICI demande = demandes.get(1);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 2, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testEnvoiSimpleSurDeuxImmeublesAchetesLaMemeAnneeAvecMaxEnvoisLimitant() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble1;
			long idImmeuble2;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF pmRF = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, pmRF, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune1 = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble1 = addBienFondRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondRF immeuble2 = addBienFondRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "sdfe7to34z57", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble1 = immeuble1.getId();
			identifiants.idImmeuble2 = immeuble2.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, 1, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(0, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());
		Assert.assertEquals(0, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateTraitement.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble1, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());
				{
					final DemandeDegrevementICI demande = demandes.get(0);
					Assert.assertNotNull(demande);
					Assert.assertFalse(demande.isAnnule());

					final RegDate dateEnvoi = delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement);
					final RegDate delaiRetour = dateEnvoi.addDays(parametreAppService.getDelaiRetourDemandeDegrevementICI());
					Assert.assertEquals(dateEnvoi, demande.getDateEnvoi());
					Assert.assertEquals(delaiRetour, demande.getDelaiRetour());
					Assert.assertNull(demande.getDateRetour());
					Assert.assertNull(demande.getDateRappel());
					Assert.assertNotNull(demande.getCodeControle());
					Assert.assertEquals((Integer) 1, demande.getNumeroSequence());
					Assert.assertEquals((Integer) (dateTraitement.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testDroitUsufruitHabitation() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addUsufruitRF(null, dateDebutDroit, null, "Achat", null, "74i6783", new IdentifiantAffaireRF(51, null, null, null), new IdentifiantDroitRF(41, 2001, 4), rf, immeuble);
			addDroitHabitationRF(null, dateDebutDroit, null, "Achat", null, "gfjk34z78", new IdentifiantAffaireRF(51, null, null, null), new IdentifiantDroitRF(41, 2001, 4), rf, immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbDroitsInspectes());
		Assert.assertEquals(2, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(2, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertNull(ignore.index1);
			Assert.assertNull(ignore.index2);
			Assert.assertNull(ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION, ignore.raison);
			Assert.assertEquals(UsufruitRF.class.getSimpleName(), ignore.messageAdditionnel);
		}
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(1);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertNull(ignore.index1);
			Assert.assertNull(ignore.index2);
			Assert.assertNull(ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION, ignore.raison);
			Assert.assertEquals(DroitHabitationRF.class.getSimpleName(), ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testAnneeDebutValiditeEstimationFiscale() throws Exception {
		// priorité à la référence
		Assert.assertEquals(Optional.of(2015), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2014", null)));
		Assert.assertEquals(Optional.of(2015), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2014", RegDate.get(1965, 2, 1))));
		Assert.assertEquals(Optional.of(2015), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2014", RegDate.get(2017, 1, 1))));
		Assert.assertEquals(Optional.of(2018), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF(null, RegDate.get(2017, 1, 1))));
		Assert.assertEquals(Optional.of(2014), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF(null, RegDate.get(2013, 1, 1))));
		Assert.assertEquals(Optional.of(2014), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF(null, RegDate.get(2013, 1, 2))));
		Assert.assertEquals(Optional.empty(), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF(null, null)));

		// interprétation de la référence
		Assert.assertEquals(Optional.of(1968), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("RG67", null)));
		Assert.assertEquals(Optional.of(1968), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("RG 67", null)));
		Assert.assertEquals(Optional.of(1968), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("Rg 67", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("RG2000", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("1.4.2000", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000enrévision", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000T.", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000rg", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000rf", null)));
		Assert.assertEquals(Optional.of(2001), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000rP", null)));
		Assert.assertEquals(Optional.of(1998), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF(" RG   97   ", null)));

		// non-interprétation de la référence
		Assert.assertEquals(Optional.empty(), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("2000EE", null)));
		Assert.assertEquals(Optional.empty(), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("20011", null)));
		Assert.assertEquals(Optional.empty(), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("64", null)));
		Assert.assertEquals(Optional.empty(), EnvoiFormulairesDemandeDegrevementICIProcessor.getAnneeDebutValiditeEstimationFiscale(buildDummyEstimationRF("4.2000", null)));
	}

	private static EstimationRF buildDummyEstimationRF(String reference, RegDate dateInscription) {
		return buildDummyEstimationRF(reference, dateInscription, null);
	}

	private static EstimationRF buildDummyEstimationRF(String reference, RegDate dateInscription, Long montant) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setReference(reference);
		estimation.setDateInscription(dateInscription);
		estimation.setMontant(montant);
		return estimation;
	}

	@Test
	public void testGetFirstEstimationWithSameAmountAsLast() throws Exception {
		{
			final NavigableMap<Integer, EstimationRF> map = new TreeMap<>();
			map.put(2005, buildDummyEstimationRF("2005", date(2005, 2, 1), 10000L));
			map.put(2007, buildDummyEstimationRF("2007", date(2007, 2, 1), 9000L));
			map.put(2009, buildDummyEstimationRF("2009", date(2009, 2, 1), 10000L));
			map.put(2015, buildDummyEstimationRF("2015", date(2015, 2, 1), 10000L));

			final EstimationRF actual = EnvoiFormulairesDemandeDegrevementICIProcessor.getFirstEstimationWithSameAmountAsLast(map);
			Assert.assertNotNull(actual);
			Assert.assertEquals("2009", actual.getReference());
			Assert.assertEquals(date(2009, 2, 1), actual.getDateInscription());
		}
		{
			final NavigableMap<Integer, EstimationRF> map = new TreeMap<>();
			map.put(2015, buildDummyEstimationRF("2015", date(2015, 2, 1), 10000L));

			final EstimationRF actual = EnvoiFormulairesDemandeDegrevementICIProcessor.getFirstEstimationWithSameAmountAsLast(map);
			Assert.assertNotNull(actual);
			Assert.assertEquals("2015", actual.getReference());
			Assert.assertEquals(date(2015, 2, 1), actual.getDateInscription());
		}
		{
			final NavigableMap<Integer, EstimationRF> map = Collections.emptyNavigableMap();
			final EstimationRF actual = EnvoiFormulairesDemandeDegrevementICIProcessor.getFirstEstimationWithSameAmountAsLast(map);
			Assert.assertNull(actual);
		}
		{
			final NavigableMap<Integer, EstimationRF> map = new TreeMap<>();
			map.put(2005, buildDummyEstimationRF("2005", date(2005, 2, 1), 10000L));
			map.put(2007, buildDummyEstimationRF("2007", date(2007, 2, 1), 9000L));
			map.put(2009, buildDummyEstimationRF("2009", date(2009, 2, 1), null));
			map.put(2015, buildDummyEstimationRF("2015", date(2015, 2, 1), 10000L));

			final EstimationRF actual = EnvoiFormulairesDemandeDegrevementICIProcessor.getFirstEstimationWithSameAmountAsLast(map);
			Assert.assertNotNull(actual);
			Assert.assertEquals("2015", actual.getReference());
			Assert.assertEquals(date(2015, 2, 1), actual.getDateInscription());
		}
		{
			final NavigableMap<Integer, EstimationRF> map = new TreeMap<>();
			map.put(2005, buildDummyEstimationRF("2005", date(2005, 2, 1), 10000L));
			map.put(2007, buildDummyEstimationRF("2007", date(2007, 2, 1), null));
			map.put(2009, buildDummyEstimationRF("2009", date(2009, 2, 1), null));
			map.put(2015, buildDummyEstimationRF("2015", date(2015, 2, 1), null));

			final EstimationRF actual = EnvoiFormulairesDemandeDegrevementICIProcessor.getFirstEstimationWithSameAmountAsLast(map);
			Assert.assertNotNull(actual);
			Assert.assertEquals("2007", actual.getReference());
			Assert.assertEquals(date(2007, 2, 1), actual.getDateInscription());
		}
	}

	@Test
	public void testNouvelleEstimationFiscaleSansChangementDuMontant() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2010, 7, 12);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// vide
			}
		});

		final class Ids {
			long idContribuable;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Acheteuse...");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF pmRF = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, pmRF, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2010, 12, 1), null, null, false, 424242L, "2010", immeuble);
			addEstimationFiscale(date(2014, 12, 1), null, null, false, 424242L, "2014", immeuble);      // même montant 4 ans plus tard

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, "Achat", null, "1555sfsgbsfhd", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble, null);

			addDemandeDegrevementICI(entreprise, dateDebutDroit.addDays(10), dateDebutDroit.addMonths(2), null, null, 2011, immeuble);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, 1, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals((Long) ids.idImmeuble, ignore.idImmeuble);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			Assert.assertEquals("Lausanne", ignore.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignore.noOfsCommune);
			Assert.assertEquals((Integer) 112, ignore.noParcelle);
			Assert.assertNull(ignore.index1);
			Assert.assertNull(ignore.index2);
			Assert.assertNull(ignore.index3);
			Assert.assertEquals(EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT, ignore.raison);
			Assert.assertEquals("Demande émise le 22.07.2010 pour la PF 2011", ignore.messageAdditionnel);
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.idContribuable);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(1, demandes.size());        // la demande précédemment présente
			}
		});
	}
}