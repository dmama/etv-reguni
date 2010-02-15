package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;

public class MigregDataTest extends MigregTest {

	@Test
	@NotTransactional
	public void testMigrerDPI() throws Exception {
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, addPeriodeFiscale(2009));
				return null;
			}
		});

		executeMigration(LimitsConfigurator.cfg(LimitsConfigurator.DPI, null), 100);
		}

	@Test
	@NotTransactional
	public void testMigrerSourcier() throws Exception {
		executeMigration(LimitsConfigurator.cfg(LimitsConfigurator.SOURCIER, null), 100);
	}

	@Test
	public void testCalculForCasStandard() throws Exception {

		class Ids {
			Long ericId;
			Long amandaId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				return null;
			}
		});

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique amanda = addNonHabitant("Amanda", "Schults", date(1978, 4, 13), Sexe.FEMININ);
				ids.amandaId = amanda.getNumero();
				return null;
			}
		});

		// Création d'un contribuable couple
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.ericId);
				PersonnePhysique amanda = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.amandaId);
				EnsembleTiersCouple ensemble = createEnsembleTiersCouple(eric, amanda, RegDate.get(2000, RegDate.MAI, 12));
				addForPrincipal(ensemble.getMenage(), RegDate.get(2009, RegDate.MAI, 5), MotifFor.CHGT_MODE_IMPOSITION,null,null,
						MockCommune.Zurich);
				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut(RegDate.get(2000, RegDate.MAI, 12));
				situation.setEtatCivil(EtatCivil.MARIE);
				situation.setNombreEnfants(0);
				situation.setContribuablePrincipal(eric);
				eric.addSituationFamille(situation);

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				HostSourcierMigratorHelper sourcierHelper = new HostSourcierMigratorHelper();
				sourcierHelper.setHelper(hostMigrationManager.getHelper());

				PersonnePhysique eric = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.ericId);

				List<ForFiscalPrincipal> allFors = sourcierHelper.removeForSameDate(createListeForAdresse(), createListeForSituation(),
						createListeFoTypeImposition());
				allFors = sourcierHelper.adaptListFor(allFors);
				Range periodeCouple = new Range(RegDate.get(2009, RegDate.MAI, 5), null);
				List<DateRange> listePeriodeCouple = new ArrayList<DateRange>();
				listePeriodeCouple.add(periodeCouple);
				sourcierHelper.mergeAndSaveFor(eric, allFors, true, listePeriodeCouple);
				Assert.notEmpty(eric.getForsFiscaux());

				MenageCommun couple = hostMigrationManager.getHelper().tiersService.findMenageCommun(eric, null);
				Assert.notNull(couple);
				Assert.notEmpty(couple.getForsFiscaux());
				Assert.isTrue(RegDate.get(2000, RegDate.MAI, 11).equals(eric.getDernierForFiscalPrincipal().getDateFin()));
				Assert.isTrue(RegDate.get(2009, RegDate.MAI, 5).equals(couple.getDernierForFiscalPrincipal().getDateDebut()));
				Assert.isTrue(ModeImposition.ORDINAIRE.equals(couple.getDernierForFiscalPrincipal().getModeImposition()));
				return null;
			}
		});
	}

	@Test
	public void testCalculForCasMemeDates() throws Exception {

		class Ids {
			Long ericId;
			Long amandaId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				return null;
			}
		});

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique amanda = addNonHabitant("Amanda", "Schults", date(1978, 4, 13), Sexe.FEMININ);
				ids.amandaId = amanda.getNumero();
				return null;
			}
		});

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.ericId);
				PersonnePhysique amanda = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.amandaId);
				createEnsembleTiersCouple(eric, amanda, RegDate.get(1995, RegDate.JANVIER, 1));
				SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
				situation.setDateDebut( RegDate.get(1995, RegDate.JANVIER, 1));
				situation.setEtatCivil(EtatCivil.MARIE);
				situation.setNombreEnfants(2);
				situation.setContribuablePrincipal(eric);
				MenageCommun couple = hostMigrationManager.getHelper().tiersService.findMenageCommun(eric, null);
				couple.addSituationFamille(situation);

				return null;
			}
		});

		HostSourcierMigratorHelper sourcierHelper = new HostSourcierMigratorHelper();
		sourcierHelper.setHelper(hostMigrationManager.getHelper());

		PersonnePhysique eric = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.ericId);

		List<ForFiscalPrincipal> allFors = new ArrayList<ForFiscalPrincipal>();
		allFors.addAll(createListeForAdresseCasMemeDate());
		// allFors.addAll(createListeForSituationMemeDate());
		allFors.addAll(createListeFoTypeImposition());

		Collections.sort(allFors, new DateRangeComparator<ForFiscal>());

		allFors = sourcierHelper.adaptListFor(allFors);
		Range periodeCouple = new Range(RegDate.get(2007, RegDate.SEPTEMBRE, 13), null);
		List<DateRange> listePeriodeCouple = new ArrayList<DateRange>();
		listePeriodeCouple.add(periodeCouple);
		sourcierHelper.mergeAndSaveFor(eric, allFors, true, listePeriodeCouple);

		eric = (PersonnePhysique) hostMigrationManager.getHelper().tiersService.getTiers(ids.ericId);
		Assert.isTrue(eric.getForsFiscaux().isEmpty());
		MenageCommun couple = hostMigrationManager.getHelper().tiersService.findMenageCommun(eric, null);
		Assert.notNull(couple);
		Assert.notEmpty(couple.getForsFiscaux());
		Assert.isTrue(RegDate.get(2007, RegDate.SEPTEMBRE, 13).equals(couple.getDernierForFiscalPrincipal().getDateDebut()));

	}

	private List<ForFiscalPrincipal> createListeForAdresse() {
		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		ForFiscalPrincipal for1 = new ForFiscalPrincipal();
		for1.setMotifRattachement(MotifRattachement.DOMICILE);
		for1.setDateDebut(RegDate.get(1995, 1, 1));
		for1.setDateFin(RegDate.get(1996, 12, 31));
		for1.setMotifOuverture(MotifFor.ARRIVEE_HS);
		for1.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		for1.setModeImposition(ModeImposition.SOURCE);
		for1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(5586);

		ForFiscalPrincipal for2 = new ForFiscalPrincipal();
		for2.setMotifRattachement(MotifRattachement.DOMICILE);
		for2.setDateDebut(RegDate.get(1996, 1, 1));
		for2.setDateFin(RegDate.get(1999, 12, 31));
		for2.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		for2.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		for2.setModeImposition(ModeImposition.SOURCE);
		for2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for2.setNumeroOfsAutoriteFiscale(5586);

		ForFiscalPrincipal for3 = new ForFiscalPrincipal();
		for3.setMotifRattachement(MotifRattachement.DOMICILE);
		for3.setDateDebut(RegDate.get(2000, 1, 1));
		for3.setDateFin(RegDate.get(2002, 12, 31));
		for3.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		for3.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		for3.setModeImposition(ModeImposition.SOURCE);
		for3.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for3.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for3.setNumeroOfsAutoriteFiscale(5586);

		ForFiscalPrincipal for4 = new ForFiscalPrincipal();
		for4.setMotifRattachement(MotifRattachement.DOMICILE);
		for4.setDateDebut(RegDate.get(2003, 1, 1));
		for4.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		for4.setModeImposition(ModeImposition.SOURCE);
		for4.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for4.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for4.setNumeroOfsAutoriteFiscale(5586);

		listeFor.add(for1);
		listeFor.add(for2);
		listeFor.add(for3);
		listeFor.add(for4);

		return listeFor;
	}

	private List<ForFiscalPrincipal> createListeForAdresseCasMemeDate() {
		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		ForFiscalPrincipal for1 = new ForFiscalPrincipal();
		for1.setMotifRattachement(MotifRattachement.DOMICILE);
		for1.setDateDebut(RegDate.get(1995, 1, 1));
		for1.setDateFin(RegDate.get(1996, 12, 31));
		for1.setMotifOuverture(MotifFor.ARRIVEE_HS);
		for1.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		for1.setModeImposition(ModeImposition.SOURCE);
		for1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(5586);

		listeFor.add(for1);

		return listeFor;
	}

	private List<ForFiscalPrincipal> createListeForSituation() {

		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		ForFiscalPrincipal for1 = new ForFiscalPrincipal();
		for1.setMotifRattachement(MotifRattachement.DOMICILE);
		for1.setDateDebut(RegDate.get(2000, 5, 12));
		for1.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		for1.setModeImposition(ModeImposition.SOURCE);
		for1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(5586);

		listeFor.add(for1);
		return listeFor;
	}

	private List<ForFiscalPrincipal> createListeFoTypeImposition() {

		List<ForFiscalPrincipal> listeFor = new ArrayList<ForFiscalPrincipal>();
		ForFiscalPrincipal for1 = new ForFiscalPrincipal();
		for1.setMotifRattachement(MotifRattachement.DOMICILE);
		for1.setDateDebut(RegDate.get(2007, RegDate.SEPTEMBRE, 13));
		for1.setMotifOuverture(MotifFor.CHGT_MODE_IMPOSITION);
		for1.setModeImposition(ModeImposition.MIXTE_137_2);
		for1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		for1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		for1.setNumeroOfsAutoriteFiscale(5586);
		listeFor.add(for1);
		return listeFor;
	}
}
