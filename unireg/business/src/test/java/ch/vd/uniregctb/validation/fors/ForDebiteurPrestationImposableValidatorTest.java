package ch.vd.uniregctb.validation.fors;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForDebiteurPrestationImposableValidatorTest extends AbstractValidatorTest<ForDebiteurPrestationImposable> {

	@Override
	protected String getValidatorBeanName() {
		return "forDebiteurPrestationImposableValidator";
	}

	private static ForDebiteurPrestationImposable buildFor(CategorieImpotSource cisDebiteurAssocie) {
		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setCategorieImpotSource(cisDebiteurAssocie);
		dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, date(2000, 1, 1), null));

		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setTiers(dpi);
		return ff;
	}

	@Test
	public void testValidateForAnnule() {

		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable forFiscal = buildFor(cis);

			// For invalide (type d'autorité fiscale incorrect) mais annulé => pas d'erreur
			{
				forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
				forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
				forFiscal.setAnnule(true);
				assertFalse(cis.name(), validate(forFiscal).hasErrors());
			}

			// For valide et annulé => pas d'erreur
			{
				forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
				forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
				forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				forFiscal.setAnnule(true);
				assertFalse(cis.name(), validate(forFiscal).hasErrors());
			}
		}
	}

	@Test
	public void testPresenceDateFermetureSiMotifFermeturePresent() throws Exception {

		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ffp = buildFor(cis);
			ffp.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ffp.setDateDebut(RegDate.get(2000, 1, 1));
			ffp.setMotifOuverture(MotifFor.INDETERMINE);
			ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ffp.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			ffp.setMotifFermeture(MotifFor.INDETERMINE);
			{
				ffp.setDateFin(null);
				final ValidationResults vr = validate(ffp);
				Assert.assertTrue(cis.name(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(cis.name(), 1, errors.size());
				assertEquals(cis.name(), "Une date de fermeture doit être indiquée si un motif de fermeture l'est.", errors.get(0));
			}
			{
				ffp.setDateFin(date(2005, 3, 31));
				final ValidationResults vr = validate(ffp);
				if (vr.hasErrors()) {
					throw new ValidationException(cis, vr);
				}
			}
		}
	}

	@Test
	public void testValiditeMotifOuverture() throws Exception {

		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setDateDebut(RegDate.get(2000, 1, 1));
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

			final Set<MotifFor> autorises = EnumSet.of(MotifFor.INDETERMINE, MotifFor.DEBUT_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION, MotifFor.DEMENAGEMENT_SIEGE);
			for (MotifFor motif : MotifFor.values()) {
				ff.setMotifOuverture(motif);
				final ValidationResults vr = validate(ff);
				if (autorises.contains(motif)) {
					Assert.assertFalse(cis.name() + '/' + motif.name(), vr.hasErrors());
				}
				else {
					Assert.assertTrue(motif.name(), vr.hasErrors());
					final List<String> errors = vr.getErrors();
					assertEquals(cis.name() + '/' + motif.name(), 1, errors.size());
					assertEquals(cis.name() + '/' + motif.name(), "Le motif d'ouverture '" + motif.getDescription(true) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
				}
			}
		}
	}

	@Test
	public void testValiditeMotifFermerture() throws Exception {

		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setDateDebut(RegDate.get(2000, 1, 1));
			ff.setMotifOuverture(MotifFor.INDETERMINE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			ff.setDateFin(RegDate.get(2010, 12, 31));

			final Set<MotifFor> autorises = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FIN_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.ANNULATION, MotifFor.DEMENAGEMENT_SIEGE);
			for (MotifFor motif : MotifFor.values()) {
				ff.setMotifFermeture(motif);
				final ValidationResults vr = validate(ff);
				if (autorises.contains(motif)) {
					Assert.assertFalse(cis.name() + '/' + motif.name(), vr.hasErrors());
				}
				else {
					Assert.assertTrue(cis.name() + '/' + motif.name(), vr.hasErrors());
					final List<String> errors = vr.getErrors();
					assertEquals(cis.name() + '/' + motif.name(), 1, errors.size());
					assertEquals(cis.name() + '/' + motif.name(), "Le motif de fermeture '" + motif.getDescription(false) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
				}
			}
		}
	}

	@Test
	public void testPresenceMotif() throws Exception {

		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setDateDebut(RegDate.get(2000, 1, 1));
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

			// pas de motif de début
			{
				final ValidationResults vr = validate(ff);
				assertTrue(vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(cis.name(), 1, errors.size());
				assertEquals(cis.name(), "Le motif d'ouverture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
			}

			// tout va bien maintenant
			ff.setMotifOuverture(MotifFor.INDETERMINE);
			{
				final ValidationResults vr = validate(ff);
				assertFalse(cis.name(), vr.hasErrors());
			}

			// pas de motif de fin
			ff.setDateFin(date(2010, 12, 31));
			{
				final ValidationResults vr = validate(ff);
				assertTrue(cis.name(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(cis.name(), 1, errors.size());
				assertEquals(cis.name(), "Le motif de fermeture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable' fermés.", errors.get(0));
			}

			// tout va bien maintenant
			ff.setMotifFermeture(MotifFor.INDETERMINE);
			{
				final ValidationResults vr = validate(ff);
				assertFalse(cis.name(), vr.hasErrors());
			}
		}
	}

	@Test
	public void testDateOuvertureMotifSansContrainte() throws Exception {
		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

			final Set<MotifFor> sansContrainte = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION);
			for (MotifFor motif : sansContrainte) {
				ff.setMotifOuverture(motif);
				for (RegDate ouverture = RegDate.get(2000, 1, 1) ; ouverture.year() == 2000 ; ouverture = ouverture.getOneDayAfter()) {
					ff.setDateDebut(ouverture);
					final ValidationResults vr = validate(ff);
					assertFalse(cis.name() + '/' + motif.name() + "-" + ouverture.toString(), vr.hasErrors());
				}
			}
		}
	}

	@Test
	public void testDateOuvertureDebutPrestationIS() throws Exception {
		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
			ff.setMotifOuverture(MotifFor.DEBUT_PRESTATION_IS);

			for (RegDate ouverture = RegDate.get(2000, 1, 1) ; ouverture.year() == 2000 ; ouverture = ouverture.getOneDayAfter()) {
				ff.setDateDebut(ouverture);
				final ValidationResults vr = validate(ff);
				if (ouverture.day() == 1) {
					assertFalse(cis.name() + '/' + ouverture.toString(), vr.hasErrors());
				}
				else {
					assertTrue(ouverture.toString(), vr.hasErrors());
					final List<String> errors = vr.getErrors();
					assertEquals(cis.name() + '/' + ouverture.toString(), 1, errors.size());
					assertEquals(cis.name() + '/' + ouverture.toString(), "Les fors ouverts avec le motif '" + MotifFor.DEBUT_PRESTATION_IS.getDescription(true) + "' doivent commencer un premier jour du mois.", errors.get(0));
				}
			}
		}
	}

	@Test
	public void testDateFermeture() throws Exception {
		for (CategorieImpotSource cis : CategorieImpotSource.values()) {
			final ForDebiteurPrestationImposable ff = buildFor(cis);
			ff.setDateDebut(date(2000, 1, 1));
			ff.setMotifOuverture(MotifFor.INDETERMINE);
			ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

			final Set<MotifFor> motifs = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FIN_PRESTATION_IS, MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, MotifFor.FUSION_COMMUNES, MotifFor.ANNULATION, MotifFor.DEMENAGEMENT_SIEGE);
			for (MotifFor motif : motifs) {
				ff.setMotifFermeture(motif);

				// dans le passé
				{
					final Set<RegDate> finsTrimestre = new HashSet<>(Arrays.asList(date(2000, 3, 31), date(2000, 6, 30), date(2000, 9, 30), date(2000, 12, 31)));
					for (RegDate fermeture = RegDate.get(2000, 1, 1); fermeture.year() == 2000; fermeture = fermeture.getOneDayAfter()) {
						ff.setDateFin(fermeture);
						final ValidationResults vr = validate(ff);
						if (finsTrimestre.contains(fermeture)) {
							if (vr.hasErrors()) {
								throw new ValidationException(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr);
							}
						}
						else {
							assertTrue(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr.hasErrors());
							final List<String> errors = vr.getErrors();
							if (errors.size() != 1) {
								throw new ValidationException(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr);
							}
							assertEquals(cis.name() + '/' + motif.name() + "-" + fermeture.toString(),
							             String.format("La date de fermeture du for débiteur ForDebiteurPrestationImposable (01.01.2000 - %s) est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.",
							                           RegDateHelper.dateToDisplayString(fermeture)),
							             errors.get(0));
						}
					}
				}

				// dans le futur
				{
					final RegDate today = RegDate.get();
					final Set<RegDate> finsTrimestre = new HashSet<>();
					for (int year = today.year() ; year <= today.year() + 1 ; ++ year) {
						for (int month = 3 ; month <= 12 ; month += 3) {
							finsTrimestre.add(date(year, 1, 1).addMonths(month).getOneDayBefore());
						}
					}

					for (RegDate fermeture = RegDate.get(today.year(), 1, 1); fermeture.year() <= today.year() + 1; fermeture = fermeture.getOneDayAfter()) {
						ff.setDateFin(fermeture);
						final ValidationResults vr = validate(ff);
						if (finsTrimestre.contains(fermeture)) {
							if (vr.hasErrors()) {
								throw new ValidationException(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr);
							}
						}
						else {
							assertTrue(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr.hasErrors());
							final List<String> errors = vr.getErrors();
							if (errors.size() != 1) {
								throw new ValidationException(cis.name() + '/' + motif.name() + "-" + fermeture.toString(), vr);
							}
							assertEquals(cis.name() + '/' + motif.name() + "-" + fermeture.toString(),
							             String.format("La date de fermeture du for débiteur ForDebiteurPrestationImposable (01.01.2000 - %s) est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.",
							                           RegDateHelper.dateToDisplayString(fermeture)),
						                 errors.get(0));
						}
					}
				}
			}
		}
	}

	@SafeVarargs
	private static <T> Set<T> buildSet(T... elements) {
		final Set<T> set = new HashSet<>();
		if (elements != null) {
			Collections.addAll(set, elements);
		}
		return set;
	}

	/**
	 * Avec un for déjà fermé, la seule date proposée est toujours la date actuelle de fermeture
	 */
	@Test
	public void testGetDatesFermetureAutoriseesForDejaFerme() throws Exception {
		final RegDate dateDebutFor = date(2013, 4, 1);
		final RegDate dateFinFor = date(2013, 10, 31);

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebutFor);
				addForDebiteur(dpi, dateDebutFor, MotifFor.DEBUT_PRESTATION_IS, dateFinFor, MotifFor.DEMENAGEMENT_SIEGE, MockCommune.Aigle);
				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getDernierForDebiteur();
				Assert.assertNotNull(ff);
				Assert.assertEquals(dateFinFor, ff.getDateFin());

				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(max), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateDebutFor.addMonths(1).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(1).getOneDayBefore(),
					                             dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(3).getOneDayBefore(),
					                             dateDebutFor.addMonths(4).getOneDayBefore()), dates);
				}
				// date max après la date de fin du for
				{
					final RegDate max = dateFinFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateFinFor), dates);
				}
				// date max après la date de fin du for
				{
					final RegDate max = dateFinFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);

					final Set<RegDate> expected = new HashSet<>();
					for (RegDate cursor = dateDebutFor.addMonths(1).getOneDayBefore() ; cursor.isBeforeOrEqual(max) ; cursor = cursor.getOneDayAfter().addMonths(1).getOneDayBefore()) {
						expected.add(cursor);
					}
					Assert.assertEquals(expected, dates);
				}
			}
		});
	}

	@Test
	public void testGetDatesFermetureAutoriseesSansListe() throws Exception {

		final RegDate dateDebutFor = date(2013, 4, 1);

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebutFor);
				addForDebiteur(dpi, dateDebutFor, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);
				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getDernierForDebiteur();
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getDateFin());

				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(max), dates);
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(max), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateDebutFor.addMonths(1).getOneDayBefore()), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateDebutFor.addMonths(1).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(1).getOneDayBefore(),
					                             dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(3).getOneDayBefore(),
					                             dateDebutFor.addMonths(4).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(1).getOneDayBefore(),
					                             dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(3).getOneDayBefore(),
					                             dateDebutFor.addMonths(4).getOneDayBefore()), dates);
				}
			}
		});
	}

	@Test
	public void testGetDatesFermetureAutoriseesAvecListeEtUneSeulePeriodicite() throws Exception {

		final RegDate dateDebutFor = date(2013, 4, 1);

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebutFor);
				addForDebiteur(dpi, dateDebutFor, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebutFor.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, dateDebutFor, dateDebutFor.addMonths(1).getOneDayBefore(), md);
				addListeRecapitulative(dpi, pf, dateDebutFor.addMonths(1), dateDebutFor.addMonths(2).getOneDayBefore(), md);

				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getDernierForDebiteur();
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getDateFin());

				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(3).getOneDayBefore(),
					                             dateDebutFor.addMonths(4).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(4);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(3).getOneDayBefore(),
					                             dateDebutFor.addMonths(4).getOneDayBefore()), dates);
				}
			}
		});
	}

	@Test
	public void testGetDatesFermetureAutoriseesAvecListeEtPlusieursPeriodicites() throws Exception {

		final RegDate dateDebutFor = date(2013, 2, 1);
		final RegDate dateDebutTrimestriel = date(2013, 4, 1);
		final RegDate dateDebutSemestriel = date(2013, 7, 1);

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, dateDebutFor, dateDebutTrimestriel.getOneDayBefore()));
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebutTrimestriel, dateDebutSemestriel.getOneDayBefore()));
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, dateDebutSemestriel, null));
				addForDebiteur(dpi, dateDebutFor, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebutFor.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, dateDebutFor, dateDebutFor.addMonths(1).getOneDayBefore(), md);

				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final ForDebiteurPrestationImposable ff = dpi.getDernierForDebiteur();
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getDateFin());

				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la date de début du for
				{
					final RegDate max = dateDebutFor.addMonths(-1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la date de début du for
				{
					final RegDate max = dateDebutFor;
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max avant la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).addDays(-2);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(0, dates.size());
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(max), dates);
				}
				// date max à la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1).getOneDayBefore();
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(max), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateDebutFor.addMonths(1).getOneDayBefore()), dates);
				}
				// date max au lendemain de la première échéance mensuelle depuis le début du for
				{
					final RegDate max = dateDebutFor.addMonths(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(Collections.singleton(dateDebutFor.addMonths(1).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addYears(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, false);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(1).getOneDayBefore(),
					                             dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(5).getOneDayBefore(),
					                             dateDebutFor.addMonths(11).getOneDayBefore()), dates);
				}
				// date max quelques mois après la date de début du for
				{
					final RegDate max = dateDebutFor.addYears(1);
					final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, ff, max, true);
					Assert.assertNotNull(dates);
					Assert.assertEquals(buildSet(dateDebutFor.addMonths(1).getOneDayBefore(),
					                             dateDebutFor.addMonths(2).getOneDayBefore(),
					                             dateDebutFor.addMonths(5).getOneDayBefore(),
					                             dateDebutFor.addMonths(11).getOneDayBefore()), dates);
				}
			}
		});
	}

	@Test
	public void testGetDatesFermetureAutoriseesSurForIntermediaire() throws Exception {

		final RegDate dateDebutForExistantPasse = date(2009, 1, 1);
		final RegDate dateDebutTrou = dateDebutForExistantPasse.addMonths(3);
		final RegDate dateFinForExistantPasse = dateDebutTrou.getOneDayBefore();
		final RegDate dateDebutForFutur = date(2009, 9, 1);

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.MENSUEL, null, dateDebutForExistantPasse, null));
				addForDebiteur(dpi, dateDebutForExistantPasse, MotifFor.DEBUT_PRESTATION_IS, dateFinForExistantPasse, MotifFor.FIN_PRESTATION_IS, MockCommune.Aigle);
				addForDebiteur(dpi, dateDebutForFutur, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebutForExistantPasse.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, dateDebutForExistantPasse, dateDebutForExistantPasse.addMonths(1).getOneDayBefore(), md);

				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final RegDate max = dateDebutForExistantPasse.addYears(1);
				final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, dateDebutTrou, max, dpi.getPeriodicitesNonAnnulees(true));
				Assert.assertNotNull(dates);

				Assert.assertEquals(buildSet(date(2009, 4, 30), date(2009, 5, 31), date(2009, 6, 30), date(2009, 7, 31), date(2009, 8, 31)), dates);
			}
		});
	}

	/**
	 * Le "trou" ne s'arrête pas sur une date qui colle avec les périodicités
	 */
	@Test
	public void testGetDatesFermetureAutoriseesSurForIntermediaireAvecDebutForFuturNonAligne() throws Exception {

		final RegDate dateDebutForExistantPasse = date(2009, 1, 1);
		final RegDate dateDebutTrou = dateDebutForExistantPasse.addMonths(6);
		final RegDate dateFinForExistantPasse = dateDebutTrou.getOneDayBefore();
		final RegDate dateDebutForFutur = date(2009, 11, 1);     // 4 mois de trou, alors que la périodicité est trimestrielle

		// mise en place du débiteur
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
				dpi.addPeriodicite(new Periodicite(PeriodiciteDecompte.TRIMESTRIEL, null, dateDebutForExistantPasse, null));
				addForDebiteur(dpi, dateDebutForExistantPasse, MotifFor.DEBUT_PRESTATION_IS, dateFinForExistantPasse, MotifFor.FIN_PRESTATION_IS, MockCommune.Aigle);
				addForDebiteur(dpi, dateDebutForFutur, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aigle);

				final PeriodeFiscale pf = addPeriodeFiscale(dateDebutForExistantPasse.year());
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, dateDebutForExistantPasse, dateDebutForExistantPasse.addMonths(3).getOneDayBefore(), md);

				return dpi.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(id);
				Assert.assertNotNull(dpi);

				final RegDate max = dateDebutForExistantPasse.addYears(1);
				final Set<RegDate> dates = ForDebiteurPrestationImposableValidator.getDatesFermetureAutorisees(dpi, dateDebutTrou, max, dpi.getPeriodicitesNonAnnulees(true));
				Assert.assertNotNull(dates);

				// une fin qui colle aux périodicités, et l'autre qui ferme le trou
				Assert.assertEquals(buildSet(date(2009, 9, 30), date(2009, 10, 31)), dates);
			}
		});
	}
}