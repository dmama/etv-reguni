package ch.vd.unireg.validation.tiers;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCanton;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.CapitalFiscalEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.GroupeFlagsEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class EntrepriseValidatorTest extends AbstractValidatorTest<Entreprise> {

	private static final String CHF = "CHF";

	@Override
	protected String getValidatorBeanName() {
		return "entrepriseValidator";
	}

	@Test
	public void testChevauchementRegimesFiscaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), date(2005, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), null, RegimeFiscal.Portee.CH, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));

		// ici, tout va bien, les différentes portées ne se marchent pas dessus
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui ne chevauche pas -> pas de souci

		entreprise.addRegimeFiscal(new RegimeFiscal(date(2007, 1, 1), date(2009, 12, 1), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons un régime fiscal VD qui chevauche -> rien ne va plus
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2005, 1, 1), date(2007, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(2, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs régimes fiscaux de portée VD", errors.get(0));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs régimes fiscaux de portée VD", errors.get(1));
		}
	}

	@Test
	public void testRegimeFiscalInvalide() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2010, 1, 1), date(2005, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le régime fiscal de portée VD (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testChevauchementDonneesCiviles() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2000, 1, 1), date(2005, 12, 31), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2000, 1, 1), date(2005, 12, 31), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2000, 1, 1), date(2005, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));

		// aucun chevauchement (= 1 seule donnée de chaque type, de toute façon...)
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui ne chevauche pas -> pas de souci
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2006, 1, 1), date(2009, 12, 1), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2006, 1, 1), date(2009, 12, 1), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2007, 1, 1), date(2009, 12, 1), new MontantMonetaire(10000L, MontantMonetaire.CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
		}

		// ajoutons une donnée qui chevauche -> rien ne va plus
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2005, 1, 1), date(2007, 12, 31), "Ma petite entreprise"));
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2005, 1, 1), date(2007, 12, 31), FormeJuridiqueEntreprise.SARL));
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2005, 1, 1), date(2007, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertEquals(4, vr.errorsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2007] est couverte par plusieurs valeurs de raison sociale", errors.get(0));
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2007] est couverte par plusieurs valeurs de forme juridique", errors.get(1));
			Assert.assertEquals("La période [01.01.2005 ; 31.12.2005] est couverte par plusieurs valeurs de capital", errors.get(2));
			Assert.assertEquals("La période [01.01.2007 ; 31.12.2007] est couverte par plusieurs valeurs de capital", errors.get(3));
		}
	}

	@Test
	public void testDonneesCivilesRaisonSocialeInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new RaisonSocialeFiscaleEntreprise(date(2010, 1, 1), date(2005, 12, 31), "Ma grande entreprise"));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("La raison sociale RaisonSocialeFiscaleEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testDonneesCivilesFormeJuridiqueInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new FormeJuridiqueFiscaleEntreprise(date(2010, 1, 1), date(2005, 12, 31), FormeJuridiqueEntreprise.SA));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("La forme juridique FormeJuridiqueFiscaleEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testDonneesCivilesCapitalInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), date(2005, 12, 31), new MontantMonetaire(10000L, MontantMonetaire.CHF)));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital CapitalFiscalEntreprise (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testChevauchementAllegementsFiscaux() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFiscal(new AllegementFiscalCanton(date(2000, 1, 1), date(2005, 12, 31), BigDecimal.valueOf(25L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI));
		entreprise.addAllegementFiscal(new AllegementFiscalCanton(date(2000, 1, 1), null, BigDecimal.TEN, AllegementFiscal.TypeImpot.CAPITAL, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI));

		// ici, tout va bien, les différents allègements ne se marchent pas dessus
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
			Assert.assertFalse(vr.toString(), vr.hasWarnings());
		}

		// ajoutons un nouvel allègement avec une cible déjà existante, sans chevauchement
		entreprise.addAllegementFiscal(new AllegementFiscalCanton(date(2006, 1, 1), date(2006, 12, 31), BigDecimal.valueOf(35L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.toString(), vr.hasErrors());
			Assert.assertFalse(vr.toString(), vr.hasWarnings());
		}

		// ajoutons maintenant un chevauchement
		entreprise.addAllegementFiscal(new AllegementFiscalCanton(date(2004, 1, 1), date(2007, 12, 31), BigDecimal.valueOf(22L), AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String error = vr.getErrors().get(0);
			Assert.assertEquals("La période [01.01.2004 ; 31.12.2006] est couverte par plusieurs allègements fiscaux de type 'allègement BENEFICE CANTON'.", error);
		}
	}

	@Test
	public void testAllegementsFiscauxInvalides() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFiscal(new AllegementFiscalCommune(date(2010, 1, 1), date(2005, 12, 31), BigDecimal.TEN, AllegementFiscal.TypeImpot.BENEFICE, AllegementFiscalCantonCommune.Type.TEMPORAIRE_91LI, null));     // les dates sont à l'envers !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("L'allègement fiscal AllegementFiscalCommune BENEFICE (01.01.2010 - 31.12.2005) possède une date de début qui est après la date de fin: début = 01.01.2010, fin = 31.12.2005", errors.get(0));
	}

	@Test
	public void testCapitalSansMonnaie() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(1000000L, null)));     // pas de monnaie !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (1000000 ici) et d'une monnaie ('' ici).", errors.get(0));
	}

	@Test
	public void testCapitalSansMontant() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, CHF)));     // pas de montant !

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (vide ici) et d'une monnaie ('CHF' ici).", errors.get(0));
	}

	@Test
	public void testCapitalSansMontantNiMonnaie() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(null, null)));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant (vide ici) et d'une monnaie ('' ici).", errors.get(0));
	}

	@Test
	public void testSansCapital() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, null));

		final ValidationResults vr = validate(entreprise);
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise doit être composé d'un montant et d'une monnaie (tous deux vides ici).", errors.get(0));
	}

	@Test
	public void testCapitalAvecMontantNegatif() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addDonneeCivile(new CapitalFiscalEntreprise(date(2010, 1, 1), null, new MontantMonetaire(-42L, CHF)));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());
		Assert.assertEquals(1, vr.errorsCount());
		Assert.assertEquals(0, vr.warningsCount());

		final List<String> errors = vr.getErrors();
		Assert.assertEquals("Le capital d'une entreprise ne peut être négatif (-42).", errors.get(0));
	}

	@Test
	public void testChevauchementFlagsExclusifs() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.UTILITE_PUBLIQUE, date(2000, 1, 1), date(2000, 12, 31)));
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES, date(2005, 1, 1), date(2010, 6, 30)));

		// aucun chevauchement
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL, date(2000, 12, 1), date(2006, 9, 12)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(2, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.12.2000 ; 31.12.2000] est couverte par plusieurs spécificités fiscales du groupe " + GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE, errors.get(0));
			Assert.assertEquals("La période [01.01.2005 ; 12.09.2006] est couverte par plusieurs spécificités fiscales du groupe " + GroupeFlagsEntreprise.SI_SERVICE_UTILITE_PUBLIQUE, errors.get(1));
		}
	}

	@Test
	public void testChevauchementFlagsNonExclusifsDifferents() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.AUDIT, date(2000, 1, 1), date(2000, 12, 31)));
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.EXPERTISE, date(2005, 1, 1), date(2010, 6, 30)));

		// aucun chevauchement
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.IMIN, date(2000, 12, 1), date(2006, 9, 12)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	public void testChevauchementFlagsNonExclusifsIdentiques() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.AUDIT, date(2000, 1, 1), date(2000, 12, 31)));
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.AUDIT, date(2005, 1, 1), date(2010, 6, 30)));

		// aucun chevauchement
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.AUDIT, date(2000, 12, 1), date(2006, 9, 12)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(2, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final List<String> errors = vr.getErrors();
			Assert.assertEquals("La période [01.12.2000 ; 31.12.2000] est couverte par plusieurs spécificités fiscales de type " + TypeFlagEntreprise.AUDIT, errors.get(0));
			Assert.assertEquals("La période [01.01.2005 ; 12.09.2006] est couverte par plusieurs spécificités fiscales de type " + TypeFlagEntreprise.AUDIT, errors.get(1));
		}
	}

	@Test
	public void testChevauchementFlagsGroupesDifferents() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES, date(2000, 1, 1), date(2000, 12, 31)));
		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.SOC_IMM_ORDINAIRE, date(2005, 1, 1), date(2010, 6, 30)));

		// aucun chevauchement
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}

		entreprise.addFlag(new FlagEntreprise(TypeFlagEntreprise.AUDIT, date(2000, 12, 1), date(2006, 9, 12)));
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasErrors());
			Assert.assertFalse(vr.hasWarnings());
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	/**
	 * [SIFISC-20655] Dans SuperGRA, la validation d'une entreprise pour laquelle on avait deux régimes fiscaux sans portée explosait avec une NPE...
	 */
	@Test
	public void testMultiplesRegimesFiscauxSansPortee() throws Exception {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(null, null, null, null));
		entreprise.addRegimeFiscal(new RegimeFiscal(null, null, null, null));

		final ValidationResults vr = validate(entreprise);
		Assert.assertTrue(vr.hasErrors());      // ok, ce n'est pas valide, mais au moins cela n'explose plus...
	}

	//[SIFISC-30422] Une entreprise sans information de bouclement en base doit lever un warning
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAbsencesBouclements() throws Exception {

		final Entreprise entreprise = addEntrepriseInconnueAuCivil();

		//Pas de warning
		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertFalse(vr.hasWarnings());

		}
		addRegimeFiscalVD(entreprise,date(2015,1,1),null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise,date(2015,1,1),null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		addForPrincipal(entreprise,date(2015,1,1), MotifFor.DEBUT_EXPLOITATION,null,null, MockCommune.Aubonne, GenreImpot.BENEFICE_CAPITAL);

		{
			final ValidationResults vr = validate(entreprise);
			Assert.assertTrue(vr.hasWarnings());
			Assert.assertEquals(1, vr.warningsCount());

			final List<String> errors = vr.getWarnings();
			Assert.assertEquals("Aucune information de bouclement n'est renseignée pour cette entreprise malgré la présence de fors fiscaux. Veuillez renseigner les informations manquantes.",
					errors.get(0));
		}
	}

	//[SIFISC-30422] Par définition les SNC n'ont pas d'information de bouclement, on ne doit pas warner dessus
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAbsencesBouclementsSurSNC() throws Exception {

		final Entreprise maPetiteSNC = addEntrepriseInconnueAuCivil();

		//Pas de warning
		{
			final ValidationResults vr = validate(maPetiteSNC);
			Assert.assertFalse(vr.hasWarnings());

		}
		addRegimeFiscalVD(maPetiteSNC,date(2015,1,1),null, MockTypeRegimeFiscal.SOCIETE_PERS);
		addRegimeFiscalCH(maPetiteSNC,date(2015,1,1),null, MockTypeRegimeFiscal.SOCIETE_PERS);

		addForPrincipal(maPetiteSNC,date(2015,1,1), MotifFor.DEBUT_EXPLOITATION,null,null, MockCommune.Aubonne, GenreImpot.REVENU_FORTUNE);

		{
			final ValidationResults vr = validate(maPetiteSNC);
			Assert.assertFalse(vr.hasWarnings());
		}
	}
}
