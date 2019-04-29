package ch.vd.unireg.metier;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.AllegementFiscalConfederation;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FusionDeCommunesProcessorTest extends BusinessTest {

	private ValidationService validationService;
	private FusionDeCommunesProcessor processor;
	private AdresseService adresseService;
	private Set<Integer> anciensNoOfs;
	private int nouveauNoOfs;
	private RegDate dateFusion;
	private RegDate dateTraitement = RegDate.get();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		validationService = getBean(ValidationService.class, "validationService");
		adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new FusionDeCommunesProcessor(transactionManager, hibernateTemplate, tiersService, serviceInfra, validationService, validationInterceptor, adresseService);

		// Annexion de Croy et Vaulion par Romainmôtier (scénario prophétique et stimulant)
		anciensNoOfs = new HashSet<>();
		anciensNoOfs.add(MockCommune.RomainmotierEnvy.getNoOFS());
		anciensNoOfs.add(MockCommune.Croy.getNoOFS());
		anciensNoOfs.add(MockCommune.Vaulion.getNoOFS());
		nouveauNoOfs = MockCommune.RomainmotierEnvy.getNoOFS();
		dateFusion = date(2000, 1, 1);
	}

	@Test
	public void testTraiteContribuableInvalideDansLesDatesDeFors() throws Exception {

		final long ppId = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);

			addForPrincipal(bruno, date(1984, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);

			// un second for principal qui chevauche le premier -> invalide
			final ForFiscalPrincipalPP f = new ForFiscalPrincipalPP();
			f.setDateDebut(date(1997, 10, 29));
			f.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			f.setDateFin(null);
			f.setMotifFermeture(null);
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Croy.getNoOFS());
			f.setMotifRattachement(MotifRattachement.DOMICILE);
			f.setModeImposition(ModeImposition.ORDINAIRE);
			bruno.addForFiscal(f);

			final ValidationResults validationResults = validationService.validate(bruno);
			assertTrue(validationResults.hasErrors());
			return bruno.getNumero();
		});

		final FusionDeCommunesResults rapport = processor.run(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, null);
		assertNotNull(rapport);

		assertEquals(1, rapport.nbTiersExamines);
		assertEquals(0, rapport.tiersTraitesPourFors.size());
		assertEquals(0, rapport.tiersIgnoresPourFors.size());
		assertEquals(1, rapport.tiersEnErreur.size());

		final FusionDeCommunesResults.Erreur error = rapport.tiersEnErreur.get(0);
		assertNotNull(error);
		assertEquals(FusionDeCommunesResults.ErreurType.VALIDATION, error.raison);

		doInNewTransaction(status -> {
			// Le contribuable ne valide pas -> il ne devrait pas être traité et apparaître en erreur
			final PersonnePhysique bruno = (PersonnePhysique) tiersDAO.get(ppId);
			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, fors.principauxPP.size());
			assertEmpty(fors.secondaires);

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(date(1984, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

			final ForFiscalPrincipalPP ffp1 = fors.principauxPP.get(1);
			assertNotNull(ffp1);
			assertForPrincipal(date(1997, 10, 29), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp1);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableInvalideHorsFor() throws Exception {

		final long ppId = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
			addForPrincipal(bruno, date(1984, 8, 1), MotifFor.MAJORITE, MockCommune.Croy);
			bruno.setNom(null);     // c'est ça qui devrait poser problème

			final ValidationResults validationResults = validationService.validate(bruno);
			assertTrue(validationResults.hasErrors());
			return bruno.getNumero();
		});

		final FusionDeCommunesResults rapport = processor.run(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, null);
		assertNotNull(rapport);

		assertEquals(1, rapport.nbTiersExamines);
		assertEquals(0, rapport.tiersTraitesPourFors.size());
		assertEquals(0, rapport.tiersIgnoresPourFors.size());
		assertEquals(1, rapport.tiersEnErreur.size());

		final FusionDeCommunesResults.Erreur error = rapport.tiersEnErreur.get(0);
		assertNotNull(error);
		assertEquals(FusionDeCommunesResults.ErreurType.VALIDATION, error.raison);
		assertEquals(ppId, error.noTiers);

		doInNewTransaction(status -> {
			// Le contribuable ne valide pas -> il ne devrait pas être traité et apparaître en erreur
			final PersonnePhysique bruno = (PersonnePhysique) tiersDAO.get(ppId);
			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principauxPP.size());
			assertEmpty(fors.secondaires);

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(date(1984, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableInvalideMaisCorrigeParFusion() throws Exception {

		final RegDate dateDebut = date(2001, 10, 29);
		final RegDate dateFusion = MockCommune.BourgEnLavaux.getDateDebutValidite();

		final long ppId = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
			addForPrincipal(bruno, dateDebut, MotifFor.INDETERMINE, MockCommune.Villette);      // le problème de validation est que le for sur Villette est encore ouvert alors que la fusion est passée

			final ValidationResults validationResults = validationService.validate(bruno);
			assertTrue(validationResults.hasErrors());
			return bruno.getNumero();
		});

		final Set<Integer> anciennesCommunes = new HashSet<>(Collections.singletonList(MockCommune.Villette.getNoOFS()));
		final FusionDeCommunesResults rapport = processor.run(anciennesCommunes, MockCommune.BourgEnLavaux.getNoOFS(), dateFusion, dateTraitement, null);
		assertNotNull(rapport);

		assertEquals(1, rapport.nbTiersExamines);
		assertEquals(1, rapport.tiersTraitesPourFors.size());
		assertEquals(0, rapport.tiersIgnoresPourFors.size());
		assertEquals(0, rapport.tiersEnErreur.size());

		final long traite = rapport.tiersTraitesPourFors.get(0);
		assertEquals(ppId, traite);

		doInNewTransaction(status -> {
			final PersonnePhysique bruno = (PersonnePhysique) tiersDAO.get(ppId);
			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, fors.principauxPP.size());
			assertEmpty(fors.secondaires);

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(dateDebut, MotifFor.INDETERMINE, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Villette.getNoOFS(),
			                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

			final ForFiscalPrincipalPP ffp1 = fors.principauxPP.get(1);
			assertNotNull(ffp1);
			assertForPrincipal(dateFusion, MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.BourgEnLavaux.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp1);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableSansFor() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable ne possède pas de for -> il ne devrait pas être impacté
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);
			assertEmpty(bruno.getForsFiscaux());

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersTraitesPourFors.size());
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForsSurAutresCommunes() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Quelquechose", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex, MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable possède des fors sur des communes non-concernées pas la fusion -> il ne devrait pas être impacté
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principauxPP.size());
			assertEquals(1, fors.secondaires.size());

			final ForFiscalPrincipalPP ffp = fors.principauxPP.get(0);
			assertNotNull(ffp);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

			final ForFiscalSecondaire ffs = fors.secondaires.get(0);
			assertNotNull(ffs);
			assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(0, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecAnciensFors() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Quelquechose", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, date(1990, 4, 22), MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
				addForPrincipal(bruno, date(1990, 4, 23), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, date(1999, 6, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Croy, MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable possède des fors sur les communes concernées pas la fusion, mais ils sont tous fermés -> il ne devrait pas être impacté
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, fors.principauxPP.size());
			assertEquals(1, fors.secondaires.size());

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, date(1990, 4, 22), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
			                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

			final ForFiscalPrincipalPP ffp1 = fors.principauxPP.get(1);
			assertNotNull(ffp1);
			assertForPrincipal(date(1990, 4, 23), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
			                   ffp1);

			final ForFiscalSecondaire ffs = fors.secondaires.get(0);
			assertNotNull(ffs);
			assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, date(1999, 6, 30), MotifFor.VENTE_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
			                    MotifRattachement.IMMEUBLE_PRIVE, ffs);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(0, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForSecondaire() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Propriétaire", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Renens);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy, MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable possède un immeuble sur une des communes concernées pas la fusion -> ce for devrait être mis-à-jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principauxPP.size());
			assertEquals(2, fors.secondaires.size());

			final ForFiscalPrincipalPP ffp = fors.principauxPP.get(0);
			assertNotNull(ffp);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

			final ForFiscalSecondaire ffs0 = fors.secondaires.get(0);
			assertNotNull(ffs0);
			assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
			                    MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs0);

			final ForFiscalSecondaire ffs1 = fors.secondaires.get(1);
			assertNotNull(ffs1);
			assertForSecondaire(dateFusion, MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs1);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(1, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForPrincipal() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Croy);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable habite sur une des communes concernées pas la fusion -> son for principal devrait être mis-à-jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, fors.principauxPP.size());
			assertEmpty(fors.secondaires);

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
			                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

			final ForFiscalPrincipalPP ffp1 = fors.principauxPP.get(1);
			assertNotNull(ffp1);
			assertForPrincipal(dateFusion, MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE,
			                   ModeImposition.ORDINAIRE,
			                   ffp1);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(1, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForsExotiques() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForAutreImpot(bruno, date(1983, 4, 6), null, MockCommune.Vaulion, GenreImpot.CHIENS);
				addForAutreElementImposable(bruno, date(1992, 4, 6), null, MockCommune.Vaulion, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.PRESTATION_PREVOYANCE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransaction(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable possèdes plusieurs fors sur des communes concernées pas la fusion -> ils devraient être mis à jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principauxPP.size());
			assertEmpty(fors.secondaires);
			assertEquals(1, fors.autresImpots.size()); // les fors autres impôts représentent des impositions ponctuelles valable une seule journée
			assertEquals(2, fors.autreElementImpot.size());

			final ForFiscalAutreImpot fai0 = fors.autresImpots.get(0);
			assertNotNull(fai0);
			assertForAutreImpot(date(1983, 4, 6), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vaulion, GenreImpot.CHIENS, fai0);

			final ForFiscalAutreElementImposable faei0 = fors.autreElementImpot.get(0);
			assertNotNull(faei0);
			assertForAutreElementImposable(date(1992, 4, 6), dateFusion.getOneDayBefore(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vaulion, MotifRattachement.PRESTATION_PREVOYANCE,
			                               faei0);

			final ForFiscalAutreElementImposable faei1 = fors.autreElementImpot.get(1);
			assertNotNull(faei1);
			assertForAutreElementImposable(dateFusion, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, MotifRattachement.PRESTATION_PREVOYANCE, faei1);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(1, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForsDansLeFutur() throws Exception {

		final RegDate dateFutur = date(2006, 1, 1);
		final RegDate veilleDateFutur = dateFutur.getOneDayBefore();

		// Un contribuable avec tout pleins de fors exotiques dans le futur (= cas à priori pas autorisé aujourd'hui, mais soyons prévoyant)
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, veilleDateFutur, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(bruno, dateFutur, MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
				addForSecondaire(bruno, dateFutur, MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy, MotifRattachement.IMMEUBLE_PRIVE);
				addForAutreImpot(bruno, dateFutur, null, MockCommune.Croy, GenreImpot.CHIENS);
				addForAutreElementImposable(bruno, dateFutur, null, MockCommune.Croy, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.PRESTATION_PREVOYANCE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransaction(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		// Le contribuable possède plusieurs fors *dans le futurs* sur des communes concernées pas la fusion -> leurs numéro OFs devraient être mis à jour sans changement de dates
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, fors.principauxPP.size());
			assertEquals(1, fors.secondaires.size());
			assertEquals(1, fors.autresImpots.size());
			assertEquals(1, fors.autreElementImpot.size());

			final ForFiscalPrincipalPP ffp0 = fors.principauxPP.get(0);
			assertNotNull(ffp0);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, veilleDateFutur, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
			                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

			final ForFiscalPrincipalPP ffp1 = fors.principauxPP.get(1);
			assertNotNull(ffp1);
			assertForPrincipal(dateFutur, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE,
			                   ModeImposition.ORDINAIRE,
			                   ffp1);

			final ForFiscalSecondaire ffs = fors.secondaires.get(0);
			assertNotNull(ffs);
			assertForSecondaire(dateFutur, MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs);

			final ForFiscalAutreImpot fai = fors.autresImpots.get(0);
			assertNotNull(fai);
			assertForAutreImpot(dateFutur, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, GenreImpot.CHIENS, fai);

			final ForFiscalAutreElementImposable faei = fors.autreElementImpot.get(0);
			assertNotNull(faei);
			assertForAutreElementImposable(dateFutur, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, MotifRattachement.PRESTATION_PREVOYANCE, faei);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(1, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecForDejaSurNouvelleCommune() throws Exception {

		// Le contribuable habite déjà sur la commune résultant de la fusion
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Majoritaire", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.RomainmotierEnvy);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable habite déjà sur la commune résultant de la fusion -> son for principal ne doit pas être mis-à-jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principauxPP.size());

			final ForFiscalPrincipalPP ffp = fors.principauxPP.get(0);
			assertNotNull(ffp);
			assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(1, rapport.tiersIgnoresPourFors.size());
			assertEquals(0, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteDebiteurAvecFor() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(1990, 5, 23));
				addForDebiteur(dpi, date(1990, 5, 23), MotifFor.INDETERMINE, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy);
				return dpi.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, true, false, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le débiteur habite sur une des communes concernées pas la fusion -> son for principal devrait être mis-à-jour
			final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, id);
			assertNotNull(dpi);

			final List<ForFiscal> fors = dpi.getForsFiscauxSorted();
			assertNotNull(fors);
			assertEquals(2, fors.size());

			final ForDebiteurPrestationImposable ffp0 = (ForDebiteurPrestationImposable) fors.get(0);
			assertNotNull(ffp0);
			assertForDebiteur(date(1990, 5, 23), dateFusion.getOneDayBefore(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), ffp0);

			final ForDebiteurPrestationImposable ffp1 = (ForDebiteurPrestationImposable) fors.get(1);
			assertNotNull(ffp1);
			assertForDebiteur(dateFusion, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), ffp1);

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourFors.size());
			assertEquals(1, rapport.tiersTraitesPourFors.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testControleCantonsDeCommunes() throws Exception {
		// Communes vaudoises
		{
			final Set<Integer> anciennesCommunes = new HashSet<>(Arrays.asList(MockCommune.Aubonne.getNoOFS(), MockCommune.Bex.getNoOFS()));
			final int nouvelleCommune = MockCommune.Lausanne.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2010, 1, 1), RegDate.get(), null);
			assertNotNull(results);
			assertEquals(0, results.nbTiersExamines);
		}

		// Communes neuchâteloises
		{
			final Set<Integer> anciennesCommunes = new HashSet<>(Arrays.asList(MockCommune.Neuchatel.getNoOFS(), MockCommune.Peseux.getNoOFS()));
			final int nouvelleCommune = MockCommune.Neuchatel.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2010, 1, 1), RegDate.get(), null);
			assertNotNull(results);
			assertEquals(0, results.nbTiersExamines);
		}

		// Communes non-vaudoises réparties sur plusieurs cantons
		try {
			final Set<Integer> anciennesCommunes = new HashSet<>(Arrays.asList(MockCommune.Neuchatel.getNoOFS(), MockCommune.Bern.getNoOFS()));
			final int nouvelleCommune = MockCommune.Bern.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2010, 1, 1), RegDate.get(), null);
			fail("Une exception aurait dû être levée, puisque Berne n'a pas encore annexé Neuchâtel...");
		}
		catch (FusionDeCommunesProcessor.MauvaiseCommuneException e) {
			final String expectedMessage = String.format("L'ancienne commune %s (%d) est dans le canton %s, alors que la nouvelle commune %s (%d) est dans le canton %s",
														 MockCommune.Neuchatel.getNomOfficiel(), MockCommune.Neuchatel.getNoOFS(), MockCommune.Neuchatel.getSigleCanton(),
														 MockCommune.Bern.getNomOfficiel(), MockCommune.Bern.getNoOFS(), MockCommune.Bern.getSigleCanton());
			assertEquals(expectedMessage, e.getMessage());
		}

		// Commune vaudoise annexée par un autre canton
		try {
			final Set<Integer> anciennesCommunes = new HashSet<>(Arrays.asList(MockCommune.Prilly.getNoOFS(), MockCommune.Bern.getNoOFS()));
			final int nouvelleCommune = MockCommune.Bern.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2010, 1, 1), RegDate.get(), null);
			fail("Une exception aurait dû être levée, puisque Berne n'a pas encore annexé Prilly...");
		}
		catch (FusionDeCommunesProcessor.MauvaiseCommuneException e) {
			final String expectedMessage = String.format("L'ancienne commune %s (%d) est dans le canton %s, alors que la nouvelle commune %s (%d) est dans le canton %s",
			                                             MockCommune.Prilly.getNomOfficiel(), MockCommune.Prilly.getNoOFS(), MockCommune.Prilly.getSigleCanton(),
			                                             MockCommune.Bern.getNomOfficiel(), MockCommune.Bern.getNoOFS(), MockCommune.Bern.getSigleCanton());
			assertEquals(expectedMessage, e.getMessage());
		}

		// Commune hors-canton annexée par Vaud
		try {
			final Set<Integer> anciennesCommunes = new HashSet<>(Arrays.asList(MockCommune.Prilly.getNoOFS(), MockCommune.Bern.getNoOFS()));
			final int nouvelleCommune = MockCommune.Lausanne.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2010, 1, 1), RegDate.get(), null);
			fail("Une exception aurait dû être levée, puisque Vaud n'a pas encore annexé Berne...");
		}
		catch (FusionDeCommunesProcessor.MauvaiseCommuneException e) {
			final String expectedMessage = String.format("L'ancienne commune %s (%d) est dans le canton %s, alors que la nouvelle commune %s (%d) est dans le canton %s",
			                                             MockCommune.Bern.getNomOfficiel(), MockCommune.Bern.getNoOFS(), MockCommune.Bern.getSigleCanton(),
			                                             MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS(), MockCommune.Lausanne.getSigleCanton());
			assertEquals(expectedMessage, e.getMessage());
		}
	}

	/**
	 * SIFISC-7313
	 * @throws Exception
	 */
	@Test
	public void testTiersAvec2ForsConcernesParLaFusion() throws Exception {

		// Le contribuable à son for principal et un for secondaire sur la commune à fusionner
		final Long id = doInNewTransactionAndSessionWithoutValidation(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Majoritaire", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Cully);
				addForSecondaire(bruno, date(1984, 8, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cully, MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		doInNewTransaction(status -> {
			final Set<Integer> anciennesCommunes = new HashSet<>(Collections.singletonList(MockCommune.Cully.getNoOFS()));
			final int nouvelleCommune = MockCommune.BourgEnLavaux.getNoOFS();
			final FusionDeCommunesResults results = processor.run(anciennesCommunes, nouvelleCommune, date(2011, 1, 1), RegDate.get(), null);
			assertNotNull(results);
			assertEquals(1, results.nbTiersExamines);
			PersonnePhysique bruno = (PersonnePhysique) tiersService.getTiers(id);
			assertEquals("Bruno doit avoir 2 nouveaux fors ouverts, donc 4 au total", 4, bruno.getForsFiscaux().size());
			assertEquals("Bruno doit avoir 2 fors ouverts", 2, bruno.getForsFiscauxValidAt(date(2011, 1, 1)).size());
			boolean forFiscalPrincipalFound = false;
			boolean forFiscalSecondaireFound = false;
			for (ForFiscal f : bruno.getForsFiscauxValidAt(date(2011, 1, 1))) {
				assertEquals(date(2011, 1, 1), f.getDateDebut());
				assertNull(f.getDateFin());
				assertEquals(f.getNumeroOfsAutoriteFiscale().longValue(), MockCommune.BourgEnLavaux.getNoOFS());
				if (f instanceof ForFiscalSecondaire) {
					forFiscalSecondaireFound = true;
				}
				else if (f instanceof ForFiscalPrincipal) {
					forFiscalPrincipalFound = true;
				}
			}
			assertTrue(forFiscalPrincipalFound && forFiscalSecondaireFound);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecDecisionAci() throws Exception {

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addDecisionAci(bruno,date(1988,1,2),null,MockCommune.Croy.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, false, true, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable habite sur une des communes concernées pas la fusion -> son for principal devrait être mis-à-jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final ForsParType fors = bruno.getForsParType(true);
			assertNotNull(fors);
			assertEquals(2, bruno.getDecisionsSorted().size());

			final DecisionAci d0 = bruno.getDecisionsSorted().get(0);
			assertNotNull(d0);
			assertEquals(d0.getDateFin(), dateFusion.getOneDayBefore());
			assertEquals(d0.getNumeroOfsAutoriteFiscale().intValue(), MockCommune.Croy.getNoOFS());
			final DecisionAci d1 = bruno.getDecisionsSorted().get(1);
			assertEquals(d1.getDateDebut(), dateFusion);
			assertEquals(d1.getNumeroOfsAutoriteFiscale().intValue(), MockCommune.RomainmotierEnvy.getNoOFS());

			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(0, rapport.tiersIgnoresPourDecisions.size());
			assertEquals(1, rapport.tiersTraitesPourDecisions.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableAvecDecisionDejaSurNouvelleCommune() throws Exception {

		// Le contribuable habite déjà sur la commune résultant de la fusion
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Majoritaire", date(1966, 8, 1), Sexe.MASCULIN);
				addDecisionAci(bruno,date(1988,1,2),null,MockCommune.RomainmotierEnvy.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, false, true, false, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		doInNewTransactionAndSession(status -> {
			// Le contribuable habite déjà sur la commune résultant de la fusion -> sa décision ne doit pas être mis-à-jour
			final PersonnePhysique bruno = hibernateTemplate.get(PersonnePhysique.class, id);
			assertNotNull(bruno);

			final List<DecisionAci> decisions = bruno.getDecisionsSorted();
			assertNotNull(decisions);
			assertEquals(1, decisions.size());

			final DecisionAci decisionAci = decisions.get(0);
			assertNotNull(decisionAci);

			assertEquals(decisionAci.getNumeroOfsAutoriteFiscale().intValue(), MockCommune.RomainmotierEnvy.getNoOFS());
			assertEquals(1, rapport.nbTiersExamines);
			assertEquals(1, rapport.tiersIgnoresPourDecisions.size());
			assertEquals(0, rapport.tiersTraitesPourDecisions.size());
			assertEmpty(rapport.tiersEnErreur);
			return null;
		});
	}

	@Test
	public void testTraiteContribuableInvalideHorsDecision() throws Exception {

		final long ppId = doInNewTransactionAndSessionWithoutValidation(status -> {
			final PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
			addDecisionAci(bruno, date(1988, 1, 2), null, MockCommune.Croy.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			bruno.setNom(null);     // c'est ça qui devrait poser problème

			final ValidationResults validationResults = validationService.validate(bruno);
			assertTrue(validationResults.hasErrors());
			return bruno.getNumero();
		});

		final FusionDeCommunesResults rapport = processor.run(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, null);
		assertNotNull(rapport);

		assertEquals(1, rapport.nbTiersExamines);

		assertEquals(0, rapport.tiersTraitesPourFors.size());
		assertEquals(0, rapport.tiersIgnoresPourFors.size());
		assertEquals(1, rapport.tiersEnErreur.size());

		assertEquals(0, rapport.tiersIgnoresPourDecisions.size());
		assertEquals(0, rapport.tiersTraitesPourDecisions.size());

		final FusionDeCommunesResults.Erreur error = rapport.tiersEnErreur.get(0);
		assertNotNull(error);
		assertEquals(FusionDeCommunesResults.ErreurType.VALIDATION, error.raison);
		assertEquals(ppId, error.noTiers);

		doInNewTransaction(status -> {
			// Le contribuable ne valide pas -> il ne devrait pas être traité et apparaître en erreur
			final PersonnePhysique bruno = (PersonnePhysique) tiersDAO.get(ppId);
			final List<DecisionAci> decisions = bruno.getDecisionsSorted();
			assertNotNull(decisions);
			assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			assertNotNull(decision);
			assertEquals(decision.getNumeroOfsAutoriteFiscale().intValue(), MockCommune.Croy.getNoOFS());
			return null;
		});
	}

	@Test
	public void testSimpleDomicilesEtablissement() throws Exception {

		final long id = doInNewTransactionAndSession(status -> {
			final Etablissement etb = (Etablissement) getCurrentSession().merge(new Etablissement());
			etb.addDomicile(new DomicileEtablissement(date(1995, 1, 1), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), etb));
			return etb.getNumero();
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, false, false, true, false), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		assertEquals(1, rapport.nbTiersExamines);
		assertEquals(1, rapport.tiersTraitesPourDomicilesEtablissement.size());
		assertEquals(0, rapport.tiersIgnoresPourDomicilesEtablissement.size());
		assertEmpty(rapport.tiersEnErreur);

		doInNewTransactionAndSession(status -> {
			// Le domicile de l'établissement doit avoir été mis à jour
			final Etablissement etb = hibernateTemplate.get(Etablissement.class, id);
			assertNotNull(etb);

			final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
			assertNotNull(domiciles);
			assertEquals(2, domiciles.size());

			{
				final DomicileEtablissement domicile = domiciles.get(0);
				assertNotNull(domicile);
				assertEquals(date(1995, 1, 1), domicile.getDateDebut());
				assertEquals(dateFusion.getOneDayBefore(), domicile.getDateFin());
				assertEquals((Integer) MockCommune.Croy.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				assertFalse(domicile.isAnnule());
			}
			{
				final DomicileEtablissement domicile = domiciles.get(1);
				assertNotNull(domicile);
				assertEquals(dateFusion, domicile.getDateDebut());
				assertNull(domicile.getDateFin());
				assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				assertFalse(domicile.isAnnule());
			}
			;
			return null;
		});
	}

	@Test
	public void testAllegementFiscal() throws Exception {

		final long id = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) getCurrentSession().merge(new Entreprise(12L));
			addAllegementFiscalFederal(entreprise, date(1995, 1, 1), null, AllegementFiscal.TypeImpot.CAPITAL, BigDecimal.TEN, AllegementFiscalConfederation.Type.EXONERATION_SPECIALE);
			addAllegementFiscalCommunal(entreprise, date(1997, 1, 1), null, AllegementFiscal.TypeImpot.BENEFICE, BigDecimal.ONE, MockCommune.Croy, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI);
			return entreprise.getNumero();
		});

		final FusionDeCommunesResults rapport = doInNewTransactionAndSession(new TxCallback<FusionDeCommunesResults>() {
			@Override
			public FusionDeCommunesResults execute(TransactionStatus status) throws Exception {
				final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement, tiersService, adresseService);
				processor.traiteTiers(new FusionDeCommunesProcessor.TiersATraiter(id, false, false, false, true), anciensNoOfs, nouveauNoOfs, dateFusion, rapport);
				return rapport;
			}
		});

		assertEquals(1, rapport.nbTiersExamines);
		assertEquals(1, rapport.tiersTraitesPourAllegementsFiscaux.size());
		assertEquals(0, rapport.tiersIgnoresPourAllegementsFiscaux.size());
		assertEmpty(rapport.tiersEnErreur);

		doInNewTransactionAndSession(status -> {
			// l'un des allègements fiscaux de l'entreprise (celui qui est posé sur la commune) doit avoir été mis à jour
			final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
			assertNotNull(entreprise);

			final List<AllegementFiscal> allegements = new ArrayList<>(entreprise.getAllegementsFiscaux());
			assertNotNull(allegements);
			assertEquals(3, allegements.size());        // les deux pré-existants + le nouveau

			Collections.sort(allegements, new DateRangeComparator<>());
			{
				final AllegementFiscal af = allegements.get(0);
				assertNotNull(af);
				assertEquals(date(1995, 1, 1), af.getDateDebut());
				assertNull(af.getDateFin());
				assertEquals(0, BigDecimal.TEN.compareTo(af.getPourcentageAllegement()));
				assertEquals(AllegementFiscal.TypeImpot.CAPITAL, af.getTypeImpot());
				assertEquals(AllegementFiscal.TypeCollectivite.CONFEDERATION, af.getTypeCollectivite());
				assertFalse(af.isAnnule());
			}
			{
				final AllegementFiscal af = allegements.get(1);
				assertNotNull(af);
				assertEquals(date(1997, 1, 1), af.getDateDebut());
				assertEquals(dateFusion.getOneDayBefore(), af.getDateFin());
				assertEquals(0, BigDecimal.ONE.compareTo(af.getPourcentageAllegement()));
				assertEquals(AllegementFiscal.TypeImpot.BENEFICE, af.getTypeImpot());
				assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, af.getTypeCollectivite());
				assertEquals((Integer) MockCommune.Croy.getNoOFS(), ((AllegementFiscalCommune) af).getNoOfsCommune());
				assertFalse(af.isAnnule());
			}
			{
				final AllegementFiscal af = allegements.get(2);
				assertNotNull(af);
				assertEquals(dateFusion, af.getDateDebut());
				assertNull(af.getDateFin());
				assertEquals(0, BigDecimal.ONE.compareTo(af.getPourcentageAllegement()));
				assertEquals(AllegementFiscal.TypeImpot.BENEFICE, af.getTypeImpot());
				assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, af.getTypeCollectivite());
				assertEquals((Integer) MockCommune.RomainmotierEnvy.getNoOFS(), ((AllegementFiscalCommune) af).getNoOfsCommune());
				assertFalse(af.isAnnule());
			}
			;
			return null;
		});
	}
}
