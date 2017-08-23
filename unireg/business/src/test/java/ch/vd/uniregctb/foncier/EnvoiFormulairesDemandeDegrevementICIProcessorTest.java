package ch.vd.uniregctb.foncier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
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
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static ch.vd.uniregctb.foncier.EnvoiFormulairesDemandeDegrevementICIResults.RaisonIgnorance;

@SuppressWarnings("Duplicates")
public class EnvoiFormulairesDemandeDegrevementICIProcessorTest extends BusinessTest {

	private EnvoiFormulairesDemandeDegrevementICIProcessor processor;
	private DelaisService delaisService;
	private ParametreAppService parametreAppService;

	private Integer[] oldValueDateDebutPriseEnCompteMutationRF = null;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		final AutreDocumentFiscalService autreDocumentFiscalService = getBean(AutreDocumentFiscalService.class, "autreDocumentFiscalService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final RegistreFoncierService registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		final RegimeFiscalService regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
		processor = new EnvoiFormulairesDemandeDegrevementICIProcessor(parametreAppService, transactionManager, autreDocumentFiscalService, hibernateTemplate, registreFoncierService, regimeFiscalService);
		delaisService = getBean(DelaisService.class, "delaisService");

		oldValueDateDebutPriseEnCompteMutationRF = parametreAppService.getDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI();
		parametreAppService.setDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI(new Integer[] {1, 1, 1994});
	}

	@Override
	public void onTearDown() throws Exception {
		if (oldValueDateDebutPriseEnCompteMutationRF != null) {
			parametreAppService.setDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI(oldValueDateDebutPriseEnCompteMutationRF);
		}
		super.onTearDown();
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
			final BienFondsRF imm1 = addImmeubleRF("jkbgbjfwbfhqlb");
			final BienFondsRF imm2 = addImmeubleRF("fjewh32z35g2");

			// des droits sur les immeubles
			addDroitPersonnePhysiqueRF(date(2010, 1, 1), date(2010, 1, 1), date(2013, 5, 2), date(2013, 5, 2), "Achat", "Vente", "5413fshsguhsghs", "5413fshsguhsghr", null, new Fraction(1, 1000), GenrePropriete.COPROPRIETE, rfPP, imm1, null);
			addDroitPersonnePhysiqueRF(date(2010, 1, 1), date(2010, 1, 1), null, null, "Achat", null, "481347sshsfga", "481347sshsfgz", null, new Fraction(8, 1000), GenrePropriete.COPROPRIETE, rfPP, imm2, null);
			addDroitPersonneMoraleRF(date(2013, 1, 1), date(2013, 1, 1), null, null, "Achat", null, "456156fnjsdgfseewgbf", "456156fnjsdgfseewgbe", null, new Fraction(354, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm1, null);
			addDroitPersonneMoraleRF(date(2010, 1, 1), date(2010, 1, 1), date(2013, 5, 2), date(2013, 5, 2), "Achat", "Vente", "45815348481hdfktgerfw", "45815348481hdfktgerfv", null, new Fraction(142, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm2, null);
			addDroitPersonneMoraleRF(date(2014, 1, 1), date(2014, 1, 1), null, null, "Achat", null, "jhbfshsbhsb", "jhbfshsbhsa", null, new Fraction(12, 1000), GenrePropriete.COPROPRIETE, rfPM1, imm2, null);
			addDroitPersonneMoraleRF(date(2015, 1, 1), date(2015, 1, 1), date(2016, 5, 2), date(2016, 5, 2), "Achat", "Vente", "41534154sdfjkgfsadg", "41534154sdfjkgfsadf", null, new Fraction(157, 1000), GenrePropriete.COPROPRIETE, rfPM2, imm1, null);
			addDroitPersonneMoraleRF(date(2010, 1, 1), date(2010, 1, 1), null, null, "Achat", null, "18451438fkjfgdsj", "18451438fkjfgdsi", null, new Fraction(1, 1000), GenrePropriete.COPROPRIETE, rfPM2, imm2, null);

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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testNonEnvoiSimpleCarAvantSeuil() throws Exception {

		// le seuil est fixé au 1.1.1994 dans ce test (voir runOnSetup())

		final RegDate dateDebutEntreprise = date(1990, 4, 1);
		final RegDate dateDebutDroit = date(1993, 7, 12);
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(1993, 12, 1), null, null, false, 424242L, "1993", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DATE_MUTATION_AVANT_SEUIL,
			                   "Estimation fiscale (01.12.1993)",
			                   immeubleInfos.get(0));
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final DemandeDegrevementICI demandeExistante = addDemandeDegrevementICI(entreprise, dateDebutDroit.addDays(10), dateDebutDroit.addMonths(2), null, null, dateDebutDroit.year() + 1, immeuble);
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.ESTIMATION_FISCALE_ABSENTE_OU_ZERO,
			                   null,
			                   immeubleInfos.get(0));
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 0L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.ESTIMATION_FISCALE_ABSENTE_OU_ZERO,
			                   null,
			                   immeubleInfos.get(0));
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			addDegrevementICI(entreprise,
			                  immeuble,
			                  dateDebutDroit.year(),
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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.DEGREVEMENT_DEJA_ACTIF_ANNEE_SUIVANT_DEBUT_DROIT,
			                   "Année suivant début de droit : " + (dateDebutDroit.year() + 1),
			                   immeubleInfos.get(0));
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2013, 12, 1), null, null, false, 484541745L, "2013", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT,
			                   "Demande émise le " + RegDateHelper.dateToDisplayString(dateDebutDroit.addDays(10)) + " pour la PF " + (dateDebutDroit.year() + 1),
			                   immeubleInfos.get(0));
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, date(2016, 12, 31), false, 484541745L, "RG2015", immeuble);
			addEstimationFiscale(date(2017, 1, 6), date(2017, 1, 1), null, false, 46512165L, "2017", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2010, 12, 1), null, date(2014, 12, 31), false, 484541745L, "2010", immeuble);
			addEstimationFiscale(date(2015, 12, 1), date(2015, 1, 1), null, false, 454545445L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_ESTIMATION_FISCALE,
			                   "Demande émise le 25.01.2015 pour la PF 2016",
			                   immeubleInfos.get(0));
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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.EXO_ICI_TOTALE);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.CONTRIBUABLE_TOTALEMENT_EXONERE,
			                   "Exonération totale ICI valable sur la période 2016",
			                   immeubleInfos.get(0));
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
	public void testTraiteCompletementExonereMaisMauvaisePeriode() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateChangementRegime = date(2016, 1, 1);

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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateChangementRegime.getOneDayBefore(), MockTypeRegimeFiscal.EXO_ICI_TOTALE);
			addRegimeFiscalVD(entreprise, dateChangementRegime, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final RegDate dateTraitement = RegDate.get();
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testTraiteCompletementExonereMaisPasICI() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateChangementRegime = date(2016, 1, 1);

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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.EXO_IBC_TOTALE);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final RegDate dateTraitement = RegDate.get();
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
	}

	@Test
	public void testTraiteSeulementExonereDeFait() throws Exception {

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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.EXO_ICI_FAIT);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final RegDate dateTraitement = RegDate.get();
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
				}
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
			final BienFondsRF immeuble1 = addBienFondsRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondsRF immeuble2 = addBienFondsRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "sdfe7to34z57", "sdfe7to34z56", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble2, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Yverdon-les-Bains", envoi.nomCommune);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
			final BienFondsRF immeuble1 = addBienFondsRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondsRF immeuble2 = addBienFondsRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "sdfe7to34z57", "sdfe7to34z56", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeuble2, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Yverdon-les-Bains", envoi.nomCommune);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
			final BienFondsRF immeuble1 = addBienFondsRF("4545841dfsshdas", null, commune1, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble1);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble1, null);

			final CommuneRF commune2 = addCommuneRF(56251, "Yverdon", MockCommune.YverdonLesBains.getNoOFS());
			final BienFondsRF immeuble2 = addBienFondsRF("hjsgfsgfsle4753", null, commune2, 142);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 87454L, "2015", immeuble2);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "sdfe7to34z57", "sdfe7to34z56", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble2, null);

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
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addUsufruitRF(null, dateDebutDroit, null, null, "Achat", null, "74i6783", "74i6782", new IdentifiantAffaireRF(51, null, null, null), new IdentifiantDroitRF(41, 2001, 4), rf, immeuble);
			addDroitHabitationRF(null, dateDebutDroit, null, null, "Achat", null, "gfjk34z78", "gfjk34z77", new IdentifiantAffaireRF(51, null, null, null), new IdentifiantDroitRF(41, 2001, 4), rf, immeuble);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION,
			                   DroitHabitationRF.class.getSimpleName(),
			                   immeubleInfos.get(0));
		}
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(1);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION,
			                   UsufruitRF.class.getSimpleName(),
			                   immeubleInfos.get(0));
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

	/**
	 * [SIFISC-25330] Ce test vérifie qu'un contribuable qui possède un droit de propriété sur l'immeuble A et un usufruit
	 * sur l'immeuble B voit bien une demande d'éxonération envoyée sur l'immeuble A (et rien sur l'immeuble B).
	 */
	@Test
	public void testDroitProprieteEtUsufruit() throws Exception {

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
			long idImmeubleA;
			long idImmeubleB;
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

			// le droit de propriété sur l'immeuble A
			final BienFondsRF immeubleA = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeubleA);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeubleA, null);

			// l'usufruit sur l'immeuble B
			final BienFondsRF immeubleB = addBienFondsRF("3727717711", null, commune, 113);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 388282L, "2015", immeubleB);
			addUsufruitRF(null, dateDebutDroit, null, null, "Achat", null, "74i6783", "74i6782", new IdentifiantAffaireRF(51, null, null, null), new IdentifiantDroitRF(41, 2001, 4), rf, immeubleB);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeubleA = immeubleA.getId();
			identifiants.idImmeubleB = immeubleB.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getEnvois().size());     // le droit de propriété
		Assert.assertEquals(1, results.getIgnores().size());    // l'usufruit

		// le droit de propriété
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = results.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
			Assert.assertEquals((Long) ids.idImmeubleA, envoi.idImmeuble);
			Assert.assertEquals(ids.idContribuable, envoi.noContribuable);
			Assert.assertEquals("Lausanne", envoi.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), envoi.noOfsCommune);
			Assert.assertEquals((Integer) 112, envoi.noParcelle);
			Assert.assertNull(envoi.index1);
			Assert.assertNull(envoi.index2);
			Assert.assertNull(envoi.index3);
		}

		// l'usufruit
		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeubleB,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   113,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION,
			                   UsufruitRF.class.getSimpleName(),
			                   immeubleInfos.get(0));
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());
				}
			}
		});
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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2010, 12, 1), null, null, false, 424242L, "2010", immeuble);
			addEstimationFiscale(date(2014, 12, 1), null, null, false, 424242L, "2014", immeuble);      // même montant 4 ans plus tard

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble, null);

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
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT,
			                   "Demande émise le 22.07.2010 pour la PF 2011",
			                   immeubleInfos.get(0));
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

	/**
	 * [SIFISC-23163] Une émission n'empêchait pas une autre émission si le début du droit ou de la dernière
	 * estimation fiscale étaient suffisamment loin dans le passé
	 */
	@Test
	public void testDeuxRunsSuccessifs() throws Exception {

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
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 424242L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults resultsFirstRun = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(resultsFirstRun);
		Assert.assertEquals(1, resultsFirstRun.getNbDroitsInspectes());
		Assert.assertEquals(0, resultsFirstRun.getNbDroitsIgnores());
		Assert.assertEquals(0, resultsFirstRun.getErreurs().size());
		Assert.assertEquals(1, resultsFirstRun.getEnvois().size());
		Assert.assertEquals(0, resultsFirstRun.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementEnvoyee envoi = resultsFirstRun.getEnvois().get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(dateDebutDroit.year() + 1, envoi.periodeFiscale);
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
		final Mutable<RegDate> dateEmission = new MutableObject<>();
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
					Assert.assertEquals((Integer) (dateDebutDroit.year() + 1), demande.getPeriodeFiscale());

					Assert.assertNotNull(demande.getDateEnvoi());
					dateEmission.setValue(demande.getDateEnvoi());
				}
			}
		});

		// et rebelotte !!
		final EnvoiFormulairesDemandeDegrevementICIResults resultSecondRun = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(resultSecondRun);
		Assert.assertEquals(1, resultSecondRun.getNbDroitsInspectes());
		Assert.assertEquals(1, resultSecondRun.getNbDroitsIgnores());
		Assert.assertEquals(0, resultSecondRun.getErreurs().size());
		Assert.assertEquals(0, resultSecondRun.getEnvois().size());
		Assert.assertEquals(1, resultSecondRun.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = resultSecondRun.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT,
			                   String.format("Demande émise le %s pour la PF %d",
			                                 RegDateHelper.dateToDisplayString(dateEmission.getValue()),
			                                 dateDebutDroit.year() + 1),
			                   immeubleInfos.get(0));
		}
	}

	/**
	 * [SIFISC-23412] un dégrèvement migré pour 2013 pour un achat en 1985 devrait être suffisant
	 * de formulaire de demande de dégrèvement tant que rien ne change...
	 */
	@Test
	public void testNonEmissionSiDegrevementUlterieur() throws Exception {

		final RegDate dateDebutEntreprise = date(1956, 3, 1);
		final RegDate dateDebutDroit = date(1985, 11, 29);
		final int pfDebutDegrevement = 2013;
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile -> rien
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien...
			}
		});

		final class Ids {
			final long pm;
			final long immeuble;

			public Ids(long pm, long immeuble) {
				this.pm = pm;
				this.immeuble = immeuble;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Propriétaire terrien");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Propriétaire terrien", null, "23672526423ljbhv", 42, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble = addBienFondsRF("46782362387i", null, commune, 579);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "b,deb4z754", "b,deb4z753", new IdentifiantAffaireRF(74, dateDebutDroit.year(), 78, 4), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);
			addEstimationFiscale(null, null, null, false, 285000L, "RG94", immeuble);

			addDegrevementICI(entreprise, immeuble, pfDebutDegrevement, null, null, new DonneesUtilisation(null, null, null, BigDecimal.valueOf(100L), BigDecimal.valueOf(100L)), null);

			return new Ids(entreprise.getNumero(), immeuble.getId());
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.pm, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.immeuble,
			                   ids.pm,
			                   "La Sarraz",
			                   5498,
			                   579,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DEGREVEMENT_ULTERIEUR_DEJA_PRESENT,
			                   "Dégrèvement (01.01.2013 - ?) (période visée : 1995)",   // 1995 à cause de l'estimation fiscale RG94
			                   immeubleInfos.get(0));
		}
	}

	/**
	 * [SIFISC-23397] un dégrèvement migré pour 2013 mais plus actif ne doit pas empêcher un nouvel envoi
	 * de formulaire de demande de dégrèvement même si rien n'a changé...
	 * [SIFISC-23412] en fait, si, il doit l'empêcher aussi (le cas d'un dégrèvement cloturé sans successeur ne devrait pas exister)
	 */
	@Test
	public void testEmissionSiDernierDegrevementCloture() throws Exception {

		final RegDate dateDebutEntreprise = date(1956, 3, 1);
		final RegDate dateDebutDroit = date(1985, 11, 29);
		final int pfDebutDegrevement = 2013;
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile -> rien
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien...
			}
		});

		final class Ids {
			final long pm;
			final long immeuble;

			public Ids(long pm, long immeuble) {
				this.pm = pm;
				this.immeuble = immeuble;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Propriétaire terrien");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SARL);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebutEntreprise, DayMonth.get(12, 31), 12);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Propriétaire terrien", null, "23672526423ljbhv", 42, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("46782362387i", null, commune, 579);
			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "b,deb4z754", "b,deb4z753", new IdentifiantAffaireRF(74, dateDebutDroit.year(), 78, 4), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);
			addEstimationFiscale(null, null, null, false, 285000L, "RG94", immeuble);

			addDegrevementICI(entreprise, immeuble, pfDebutDegrevement, dateTraitement.year() - 1, null, new DonneesUtilisation(null, null, null, BigDecimal.valueOf(100L), BigDecimal.valueOf(100L)), null);

			return new Ids(entreprise.getNumero(), immeuble.getId());
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.pm, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.immeuble,
			                   ids.pm,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   579,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DEGREVEMENT_ULTERIEUR_DEJA_PRESENT,
			                   String.format("Dégrèvement (01.01.2013 - 31.12.%d) (période visée : 1995)", dateTraitement.year() - 1),
			                   immeubleInfos.get(0));
		}

		// vérification en base...
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise e = (Entreprise) tiersDAO.get(ids.pm);
				Assert.assertNotNull(e);

				final List<DemandeDegrevementICI> demandes = e.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, true, true);
				Assert.assertNotNull(demandes);
				Assert.assertEquals(0, demandes.size());
			}
		});
	}

	@Test
	public void testEmissionIgnoreeSiFinExonerationLointaine() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();
		final RegDate dateFinExoneration = dateTraitement.addYears(2);

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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateFinExoneration, MockTypeRegimeFiscal.EXO_ICI_TOTALE);
			addRegimeFiscalVD(entreprise, dateFinExoneration.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, 54, 12, 53);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   54,
			                   12,
			                   53,
			                   RaisonIgnorance.CONTRIBUABLE_TOTALEMENT_EXONERE,
			                   "Exonération totale ICI valable sur la période 2016",
			                   immeubleInfos.get(0));
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
	public void testEmissionSiFinExonerationProche() throws Exception {

		final RegDate dateDebutEntreprise = date(2009, 4, 1);
		final RegDate dateDebutDroit = date(2015, 7, 12);
		final RegDate dateTraitement = RegDate.get();
		final RegDate dateFinExoneration = dateTraitement.addDays(1);

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
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, dateFinExoneration, MockTypeRegimeFiscal.EXO_ICI_TOTALE);
			addRegimeFiscalVD(entreprise, dateFinExoneration.getOneDayAfter(), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Acheteuse", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112, null, null, null);
			addEstimationFiscale(date(2015, 12, 1), null, null, false, 484541745L, "2015", immeuble);

			addDroitPersonneMoraleRF(null, dateDebutDroit, null, null, "Achat", null, "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

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
			Assert.assertEquals(2018, envoi.periodeFiscale);
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
					Assert.assertEquals((Integer) 2018, demande.getPeriodeFiscale());
				}
			}
		});
	}

	/**
	 * [SIFISC-25066] Apparemment un formulaire a été envoyé en juin 2017 alors que le droit a été clôturé en décembre 2016
	 */
	@Test
	public void testNonEmissionSiDroitCloture() throws Exception {

		final RegDate dateDebutEntreprise = date(1990, 4, 1);
		final RegDate dateDebutDroit = date(2010, 1, 4);
		final RegDate dateClotureDroit = date(2016, 12, 23);
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
			addRaisonSociale(entreprise, dateDebutEntreprise, null, "Mon entreprise");
			addFormeJuridique(entreprise, dateDebutEntreprise, null, FormeJuridiqueEntreprise.SA);
			addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

			final PersonneMoraleRF rf = addPersonneMoraleRF("Mon entreprise", null, "48514s66fss", 445198L, null);
			addRapprochementRF(entreprise, rf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(15451, "Lausanne", MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("4545841dfsshdas", null, commune, 112);
			addEstimationFiscale(date(2016, 12, 1), null, null, false, 93000L, "2016", immeuble);

			addDroitPersonneMoraleRF(date(2017, 3, 15), dateDebutDroit, date(2017, 3, 22), dateClotureDroit, "Achat", "Revente", "1555sfsgbsfhd", "1555sfsgbsfhc", new IdentifiantAffaireRF(51, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, rf, immeuble, null);

			final Ids identifiants = new Ids();
			identifiants.idContribuable = entreprise.getNumero();
			identifiants.idImmeuble = immeuble.getId();
			return identifiants;
		});

		// lancement du processus
		final EnvoiFormulairesDemandeDegrevementICIResults results = processor.run(1, null, dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDroitsInspectes());
		Assert.assertEquals(1, results.getNbDroitsIgnores());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getEnvois().size());
		Assert.assertEquals(1, results.getIgnores().size());

		{
			final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee ignore = results.getIgnores().get(0);
			Assert.assertNotNull(ignore);
			Assert.assertEquals(ids.idContribuable, ignore.noContribuable);
			final List<EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo> immeubleInfos = ignore.getImmeubleInfos();
			Assert.assertEquals(1, immeubleInfos.size());
			assertImmeubleInfo(ids.idImmeuble,
			                   ids.idContribuable,
			                   "Lausanne",
			                   MockCommune.Lausanne.getNoOFS(),
			                   112,
			                   null,
			                   null,
			                   null,
			                   RaisonIgnorance.DROIT_CLOTURE,
			                   "Droit clôturé au 23.12.2016, avant le début de la PF 2017",
			                   immeubleInfos.get(0));
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

	private static void assertImmeubleInfo(Long idImmeuble,
	                                       long idContribuable,
	                                       String nomCommune,
	                                       Integer noOfsCommune,
	                                       Integer noParcelle,
	                                       Integer index1,
	                                       Integer index2,
	                                       Integer index3,
	                                       RaisonIgnorance raison,
	                                       String messageAdditionnel,
	                                       EnvoiFormulairesDemandeDegrevementICIResults.ImmeubleInfo immeubleInfo) {
		Assert.assertNotNull(immeubleInfo);
		final EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee parent = (EnvoiFormulairesDemandeDegrevementICIResults.DemandeDegrevementNonEnvoyee) immeubleInfo.getParent();
		Assert.assertEquals(idImmeuble, immeubleInfo.idImmeuble);
		Assert.assertEquals(idContribuable, parent.noContribuable);
		Assert.assertEquals(nomCommune, immeubleInfo.nomCommune);
		Assert.assertEquals(noOfsCommune, immeubleInfo.noOfsCommune);
		Assert.assertEquals(noParcelle, immeubleInfo.noParcelle);
		Assert.assertEquals(index1, immeubleInfo.index1);
		Assert.assertEquals(index2, immeubleInfo.index2);
		Assert.assertEquals(index3, immeubleInfo.index3);
		Assert.assertEquals(raison, parent.raison);
		Assert.assertEquals(messageAdditionnel, parent.messageAdditionnel);
	}
}
