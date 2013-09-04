package ch.vd.uniregctb.validation.fors;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForDebiteurPrestationImposableValidatorTest extends AbstractValidatorTest<ForDebiteurPrestationImposable> {

	@Override
	protected String getValidatorBeanName() {
		return "forDebiteurPrestationImposableValidator";
	}

	@Test
	public void testValidateForAnnule() {

		final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();

		// For invalide (type d'autorité fiscale incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	@Test
	public void testPresenceDateFermetureSiMotifFermeturePresent() throws Exception {

		final ForDebiteurPrestationImposable ffp = new ForDebiteurPrestationImposable();
		ffp.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ffp.setDateDebut(RegDate.get(2000, 1, 1));
		ffp.setMotifOuverture(MotifFor.INDETERMINE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ffp.setMotifFermeture(MotifFor.INDETERMINE);
		{
			ffp.setDateFin(null);
			final ValidationResults vr = validate(ffp);
			Assert.assertTrue(vr.hasErrors());
			final List<String> errors = vr.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Une date de fermeture doit être indiquée si un motif de fermeture l'est.", errors.get(0));
		}
		{
			ffp.setDateFin(date(2005, 5, 23));
			final ValidationResults vr = validate(ffp);
			Assert.assertFalse(vr.hasErrors());
		}
	}

	@Test
	public void testValiditeMotifOuverture() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setDateDebut(RegDate.get(2000, 1, 1));
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

		final Set<MotifFor> autorises = EnumSet.of(MotifFor.INDETERMINE, MotifFor.DEBUT_PRESTATION_IS, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION, MotifFor.DEMENAGEMENT_SIEGE);
		for (MotifFor motif : MotifFor.values()) {
			ff.setMotifOuverture(motif);
			final ValidationResults vr = validate(ff);
			if (autorises.contains(motif)) {
				Assert.assertFalse(motif.name(), vr.hasErrors());
			}
			else {
				Assert.assertTrue(motif.name(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(motif.name(), 1, errors.size());
				assertEquals(motif.name(), "Le motif d'ouverture '" + motif.getDescription(true) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
			}
		}
	}

	@Test
	public void testValiditeMotifFermerture() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
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
				Assert.assertFalse(motif.name(), vr.hasErrors());
			}
			else {
				Assert.assertTrue(motif.name(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(motif.name(), 1, errors.size());
				assertEquals(motif.name(), "Le motif de fermeture '" + motif.getDescription(false) + "' n'est pas autorisé sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
			}
		}
	}

	@Test
	public void testPresenceMotif() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setDateDebut(RegDate.get(2000, 1, 1));
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

		// pas de motif de début
		{
			final ValidationResults vr = validate(ff);
			assertTrue(vr.hasErrors());
			final List<String> errors = vr.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le motif d'ouverture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable'.", errors.get(0));
		}

		// tout va bien maintenant
		ff.setMotifOuverture(MotifFor.INDETERMINE);
		{
			final ValidationResults vr = validate(ff);
			assertFalse(vr.hasErrors());
		}

		// pas de motif de fin
		ff.setDateFin(date(2010, 12, 31));
		{
			final ValidationResults vr = validate(ff);
			assertTrue(vr.hasErrors());
			final List<String> errors = vr.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Le motif de fermeture est une donnée obligatoire sur les fors fiscaux 'débiteur prestation imposable' fermés.", errors.get(0));
		}

		// tout va bien maintenant
		ff.setMotifFermeture(MotifFor.INDETERMINE);
		{
			final ValidationResults vr = validate(ff);
			assertFalse(vr.hasErrors());
		}
	}

	@Test
	public void testDateOuvertureMotifSansContrainte() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

		final Set<MotifFor> sansContrainte = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FUSION_COMMUNES, MotifFor.REACTIVATION);
		for (MotifFor motif : sansContrainte) {
			ff.setMotifOuverture(motif);
			for (RegDate ouverture = RegDate.get(2000, 1, 1) ; ouverture.year() == 2000 ; ouverture = ouverture.getOneDayAfter()) {
				ff.setDateDebut(ouverture);
				final ValidationResults vr = validate(ff);
				assertFalse(motif.name() + "-" + ouverture.toString(), vr.hasErrors());
			}
		}
	}

	@Test
	public void testDateOuvertureDebutPrestationIS() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ff.setMotifOuverture(MotifFor.DEBUT_PRESTATION_IS);

		for (RegDate ouverture = RegDate.get(2000, 1, 1) ; ouverture.year() == 2000 ; ouverture = ouverture.getOneDayAfter()) {
			ff.setDateDebut(ouverture);
			final ValidationResults vr = validate(ff);
			if (ouverture.day() == 1) {
				assertFalse(ouverture.toString(), vr.hasErrors());
			}
			else {
				assertTrue(ouverture.toString(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(ouverture.toString(), 1, errors.size());
				assertEquals(ouverture.toString(), "Les fors ouverts avec le motif '" + MotifFor.DEBUT_PRESTATION_IS.getDescription(true) + "' doivent commencer un premier jour du mois.", errors.get(0));
			}
		}
	}

	@Test
	public void testDateFermetureMotifSansContrainte() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setDateDebut(date(2000, 1, 1));
		ff.setMotifOuverture(MotifFor.INDETERMINE);
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());

		final Set<MotifFor> sansContrainte = EnumSet.of(MotifFor.INDETERMINE, MotifFor.FUSION_COMMUNES, MotifFor.ANNULATION);
		for (MotifFor motif : sansContrainte) {
			ff.setMotifFermeture(motif);
			for (RegDate fermeture = RegDate.get(2000, 1, 1) ; fermeture.year() == 2000 ; fermeture = fermeture.getOneDayAfter()) {
				ff.setDateFin(fermeture);
				final ValidationResults vr = validate(ff);
				assertFalse(motif.name() + "-" + fermeture.toString(), vr.hasErrors());
			}
		}
	}

	@Test
	public void testDateFermetureCessassionActivite() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setDateDebut(date(2000, 1, 1));
		ff.setMotifOuverture(MotifFor.INDETERMINE);
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ff.setMotifFermeture(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE);

		for (RegDate fermeture = RegDate.get(2000, 1, 1) ; fermeture.year() == 2000 ; fermeture = fermeture.getOneDayAfter()) {
			ff.setDateFin(fermeture);
			final ValidationResults vr = validate(ff);
			if (fermeture == fermeture.getLastDayOfTheMonth()) {
				assertFalse(fermeture.toString(), vr.hasErrors());
			}
			else {
				assertTrue(fermeture.toString(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(fermeture.toString(), 1, errors.size());
				assertEquals(fermeture.toString(), "Les fors fermés avec le motif '" + MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE.getDescription(false) + "' doivent être fermés à une fin de mois.", errors.get(0));
			}
		}
	}

	@Test
	public void testDateFermetureFinPrestationIS() throws Exception {
		final ForDebiteurPrestationImposable ff = new ForDebiteurPrestationImposable();
		ff.setDateDebut(date(2000, 1, 1));
		ff.setMotifOuverture(MotifFor.INDETERMINE);
		ff.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ff.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ff.setMotifFermeture(MotifFor.FIN_PRESTATION_IS);

		for (RegDate fermeture = RegDate.get(2000, 1, 1) ; fermeture.year() == 2000 ; fermeture = fermeture.getOneDayAfter()) {
			ff.setDateFin(fermeture);
			final ValidationResults vr = validate(ff);
			if (fermeture == date(2000, 12, 31)) {
				assertFalse(fermeture.toString(), vr.hasErrors());
			}
			else {
				assertTrue(fermeture.toString(), vr.hasErrors());
				final List<String> errors = vr.getErrors();
				assertEquals(fermeture.toString(), 1, errors.size());
				assertEquals(fermeture.toString(), "Les fors fermés avec le motif '" + MotifFor.FIN_PRESTATION_IS.getDescription(false) + "' doivent être fermés à une fin d'année.", errors.get(0));
			}
		}
	}
}