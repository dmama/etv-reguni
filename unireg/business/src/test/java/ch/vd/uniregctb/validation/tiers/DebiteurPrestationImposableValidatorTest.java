package ch.vd.uniregctb.validation.tiers;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class DebiteurPrestationImposableValidatorTest extends AbstractValidatorTest<DebiteurPrestationImposable> {

	@Override
	protected String getValidatorBeanName() {
		return "debiteurPrestationImposableValidator";
	}

	/**
	 * Cas où un for intermédiaire est ouvert (date de fin = null).
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDetectionChevauchementForsDebiteurForIntermediateOuvert() {

		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2003, 12, 1));
			forFiscal.setDateFin(date(2004, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFSEtendu());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2004, 8, 12));
			forFiscal.setDateFin(date(2006, 10, 1));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Vevey.getNoOFSEtendu());
			debiteur.addForFiscal(forFiscal);
		}
		{ // ce for intermédiaire est ouvert => il doit entrer en conflit avec le for suivant
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFSEtendu());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 3));
			forFiscal.setDateFin(date(2007, 3, 30));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFSEtendu());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2007, 3, 31));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFSEtendu());
			debiteur.addForFiscal(forFiscal);
		}

		assertValidation(Arrays.asList("Le for DPI qui commence le 03.10.2006 et se termine le 30.03.2007 chevauche le for précédent"), null, validate(debiteur));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateTiersAnnule() {

		final DebiteurPrestationImposable tiers = new DebiteurPrestationImposable();

		// Tiers invalide (périodicité avec date de début nulle) mais annulé => pas d'erreur
		{
			tiers.addPeriodicite(new Periodicite());
			tiers.setAnnule(true);
			Assert.assertFalse(validate(tiers).hasErrors());
		}

		// Tiers valide et annulée => pas d'erreur
		{
			tiers.getPeriodicites().clear();
			tiers.setAnnule(true);
			Assert.assertFalse(validate(tiers).hasErrors());
		}
	}
}
