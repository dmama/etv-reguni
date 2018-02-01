package ch.vd.unireg.validation.tiers;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

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

		final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, date(2003, 12, 1));
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2003, 12, 1));
			forFiscal.setDateFin(date(2004, 8, 11));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2004, 8, 12));
			forFiscal.setDateFin(date(2006, 10, 1));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Vevey.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{ // ce for intermédiaire est ouvert => il doit entrer en conflit avec le for suivant
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 3));
			forFiscal.setDateFin(date(2007, 3, 30));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2007, 3, 31));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}

		assertValidation(Collections.singletonList("Le for DPI qui commence le 03.10.2006 et se termine le 30.03.2007 chevauche le for précédent"), null, validate(debiteur));
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

	//Test ajout de LR en dehors période de for
	/**
	 *               +-----------------------+                                +-----------------------+     +---------------------+              +-----------------------+
	 *    For        |2006.10.02  2007.12.31 |                                |2010.10.02   2010.12.31|     |2011.6.24   2011.9.30|              |2011.12.1          null|
	 *               +-----------------------+                                +-----------------------+     +---------------------+              +-----------------------+
	 *               +-----------------------------+  +---------------------+                          +---------------------+  +---------------------+
	 *  Périodicité  |2006.10.02          2008.6.30|  |2008.8.1   2008.12.31|                          |2011.1.1    2011.6.30|  |2011.9.1   2011.12.31|
	 *               +-----------------------------+  +---------------------+                          +---------------------+  +---------------------+
	 *
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationLRNonCouvertesParFor() {

		final DebiteurPrestationImposable debiteur = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.SEMESTRIEL, date(2006, 10, 2));
		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setDateFin(date(2007, 12, 31));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}

		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2010, 10, 2));
			forFiscal.setDateFin(date(2010, 12, 31));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}


		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2011, 6, 24));
			forFiscal.setDateFin(date(2011, 9, 30));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setMotifFermeture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}

		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2011, 12, 1));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}

		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2006, 10, 2));
			lr.setDateFin(date(2008, 6, 30));
			debiteur.addDeclaration(lr);
		}

		//LR en dehors de toute plage de for
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2008, 8, 1));
			lr.setDateFin(date(2008, 12, 31));
			debiteur.addDeclaration(lr);
		}
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2011, 1, 1));
			lr.setDateFin(date(2011, 6, 30));
			debiteur.addDeclaration(lr);
		}
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2011, 9, 1));
			lr.setDateFin(date(2011, 12, 31));
			debiteur.addDeclaration(lr);
		}

		assertValidation(Arrays.asList("La date de fermeture du for débiteur ForDebiteurPrestationImposable (02.10.2006 - 31.12.2007) est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.",
		                               "La date de fermeture du for débiteur ForDebiteurPrestationImposable (02.10.2010 - 31.12.2010) est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.",
		                               "La date de fermeture du for débiteur ForDebiteurPrestationImposable (24.06.2011 - 30.09.2011) est incohérente avec sa date de début ainsi que les LR et périodicités du débiteur.",
		                               "La période qui débute le (01.01.2008) et se termine le (30.06.2008) contient des LRs alors qu'elle n'est couverte par aucun for valide",
		                               "La période qui débute le (01.08.2008) et se termine le (31.12.2008) contient des LRs alors qu'elle n'est couverte par aucun for valide",
		                               "La période qui débute le (01.01.2011) et se termine le (23.06.2011) contient des LRs alors qu'elle n'est couverte par aucun for valide",
		                               "La période qui débute le (01.10.2011) et se termine le (30.11.2011) contient des LRs alors qu'elle n'est couverte par aucun for valide"),
		                 null, validate(debiteur));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationLRCouvertesParFor() {


		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		debiteur.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2006, 10, 2), null));

		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2006, 10, 2));
			lr.setDateFin(date(2008, 6, 30));
			debiteur.addDeclaration(lr);
		}
		assertValidation(null,null, validate(debiteur));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationCouvertureListeAnnulee() {

		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		debiteur.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2006, 10, 2), null));

		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setMotifOuverture(MotifFor.INDETERMINE);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			debiteur.addForFiscal(forFiscal);
		}
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2006, 10, 2));
			lr.setDateFin(date(2008, 6, 30));
			debiteur.addDeclaration(lr);
		}

		// cette LR est annulée, elle ne doit pas entrer en ligne de compte pour la validation
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.ANNUEL);
			lr.setDateDebut(date(2000, 1, 1));
			lr.setDateFin(date(2000, 12, 31));
			lr.setAnnule(true);
			debiteur.addDeclaration(lr);
		}
		assertValidation(null, null, validate(debiteur));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationCouvertureListeSansFor() {

		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		debiteur.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2006, 10, 2), null));

		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2006, 10, 2));
			lr.setDateFin(date(2006, 12, 31));
			debiteur.addDeclaration(lr);
		}
		assertValidation(Collections.singletonList("La période qui débute le (02.10.2006) et se termine le (31.12.2006) contient des LRs alors qu'elle n'est couverte par aucun for valide"),
		                 null, validate(debiteur));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidationCouvertureListeForAnnule() {

		final DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		debiteur.addPeriodicite(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, date(2006, 10, 2), null));

		{
			final ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
			forFiscal.setAnnule(true);
			debiteur.addForFiscal(forFiscal);
		}
		{
			final DeclarationImpotSource lr = new DeclarationImpotSource();
			lr.setModeCommunication(ModeCommunication.SITE_WEB);
			lr.setPeriodicite(PeriodiciteDecompte.SEMESTRIEL);
			lr.setDateDebut(date(2006, 10, 2));
			lr.setDateFin(date(2006, 12, 31));
			debiteur.addDeclaration(lr);
		}
		assertValidation(Collections.singletonList("La période qui débute le (02.10.2006) et se termine le (31.12.2006) contient des LRs alors qu'elle n'est couverte par aucun for valide"),
		                 null, validate(debiteur));
	}
}
