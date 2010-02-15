package ch.vd.uniregctb.webservices.tiers2;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;


public class ForFiscalTest extends EnumTest {

	@Ignore // car BENEFICE_CAPITAL n'existe pas dans core, mais est déjà exposé dans le ws pour des raisons de compatibilité future
	@Test
	public void testGenreImpotCoherence() {
		assertEnumLengthEquals(ForFiscal.GenreImpot.class, ch.vd.uniregctb.type.GenreImpot.class);
		assertEnumConstantsEqual(ForFiscal.GenreImpot.class, ch.vd.uniregctb.type.GenreImpot.class);
	}

	@Test
	public void testGenreImpotFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.GenreImpot) null));
		assertEquals(ForFiscal.GenreImpot.REVENU_FORTUNE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.REVENU_FORTUNE));
		assertEquals(ForFiscal.GenreImpot.GAIN_IMMOBILIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.GAIN_IMMOBILIER));
		assertEquals(ForFiscal.GenreImpot.DROIT_MUTATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DROIT_MUTATION));
		assertEquals(ForFiscal.GenreImpot.PRESTATION_CAPITAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PRESTATION_CAPITAL));
		assertEquals(ForFiscal.GenreImpot.SUCCESSION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.SUCCESSION));
		assertEquals(ForFiscal.GenreImpot.DONATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DONATION));
		assertEquals(ForFiscal.GenreImpot.FONCIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.FONCIER));
		assertEquals(ForFiscal.GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE));
		assertEquals(ForFiscal.GenreImpot.CHIENS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.CHIENS));
		assertEquals(ForFiscal.GenreImpot.PATENTE_TABAC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PATENTE_TABAC));
	}

	@Ignore // car ETABLISSEMENT_STABLE n'existe pas dans core, mais est déjà exposé dans le ws pour des raisons de compatibilité future
	@Test
	public void testRattachementCoherence() {
		assertEnumLengthEquals(ForFiscal.MotifRattachement.class, ch.vd.uniregctb.type.MotifRattachement.class);
		assertEnumConstantsEqual(ForFiscal.MotifRattachement.class, ch.vd.uniregctb.type.MotifRattachement.class);
	}

	@Test
	public void testRattachementFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifRattachement) null));
		assertEquals(ForFiscal.MotifRattachement.DOMICILE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DOMICILE));
		assertEquals(ForFiscal.MotifRattachement.IMMEUBLE_PRIVE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE));
		assertEquals(ForFiscal.MotifRattachement.DIPLOMATE_SUISSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_SUISSE));
		assertEquals(ForFiscal.MotifRattachement.DIPLOMATE_ETRANGER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_ETRANGER));
		assertEquals(ForFiscal.MotifRattachement.ACTIVITE_INDEPENDANTE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_INDEPENDANTE));
		assertEquals(ForFiscal.MotifRattachement.SEJOUR_SAISONNIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.SEJOUR_SAISONNIER));
		assertEquals(ForFiscal.MotifRattachement.DIRIGEANT_SOCIETE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIRIGEANT_SOCIETE));
		assertEquals(ForFiscal.MotifRattachement.ACTIVITE_LUCRATIVE_CAS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_LUCRATIVE_CAS));
		assertEquals(ForFiscal.MotifRattachement.ADMINISTRATEUR, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ADMINISTRATEUR));
		assertEquals(ForFiscal.MotifRattachement.CREANCIER_HYPOTHECAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.CREANCIER_HYPOTHECAIRE));
		assertEquals(ForFiscal.MotifRattachement.PRESTATION_PREVOYANCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.PRESTATION_PREVOYANCE));
	}

	@Test
	public void testTypeAutoriteFiscaleCoherence() {
		assertEnumLengthEquals(ForFiscal.TypeAutoriteFiscale.class, ch.vd.uniregctb.type.TypeAutoriteFiscale.class);
		assertEnumConstantsEqual(ForFiscal.TypeAutoriteFiscale.class, ch.vd.uniregctb.type.TypeAutoriteFiscale.class);
	}

	@Test
	public void testTypeAutoriteFiscaleFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeAutoriteFiscale) null));
		assertEquals(ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD));
		assertEquals(ForFiscal.TypeAutoriteFiscale.COMMUNE_HC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_HC));
		assertEquals(ForFiscal.TypeAutoriteFiscale.PAYS_HS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.PAYS_HS));
	}

	@Test
	public void testModeImpositionCoherence() {
		assertEnumLengthEquals(ForFiscal.ModeImposition.class, ch.vd.uniregctb.type.ModeImposition.class);
		assertEnumConstantsEqual(ForFiscal.ModeImposition.class, ch.vd.uniregctb.type.ModeImposition.class);
	}

	@Test
	public void testModeImpositionFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.ModeImposition) null));
		assertEquals(ForFiscal.ModeImposition.ORDINAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.ORDINAIRE));
		assertEquals(ForFiscal.ModeImposition.SOURCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.SOURCE));
		assertEquals(ForFiscal.ModeImposition.DEPENSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.DEPENSE));
		assertEquals(ForFiscal.ModeImposition.MIXTE_137_1, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_1));
		assertEquals(ForFiscal.ModeImposition.MIXTE_137_2, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_2));
		assertEquals(ForFiscal.ModeImposition.INDIGENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.INDIGENT));
	}

}
