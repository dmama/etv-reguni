package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.GenreImpot;
import ch.vd.uniregctb.webservices.tiers3.ModeImposition;
import ch.vd.uniregctb.webservices.tiers3.MotifRattachement;
import ch.vd.uniregctb.webservices.tiers3.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class ForFiscalTest extends EnumTest {

	@Test
	public void testGenreImpotCoherence() {
		assertEnumLengthEquals(GenreImpot.class, ch.vd.uniregctb.type.GenreImpot.class);
		assertEnumConstantsEqual(GenreImpot.class, ch.vd.uniregctb.type.GenreImpot.class);
	}

	@Test
	public void testGenreImpotFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.GenreImpot) null));
		assertEquals(GenreImpot.REVENU_FORTUNE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.REVENU_FORTUNE));
		assertEquals(GenreImpot.GAIN_IMMOBILIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.GAIN_IMMOBILIER));
		assertEquals(GenreImpot.DROIT_MUTATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DROIT_MUTATION));
		assertEquals(GenreImpot.PRESTATION_CAPITAL, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PRESTATION_CAPITAL));
		assertEquals(GenreImpot.SUCCESSION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.SUCCESSION));
		assertEquals(GenreImpot.DONATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DONATION));
		assertEquals(GenreImpot.FONCIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.FONCIER));
		assertEquals(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE));
		assertEquals(GenreImpot.CHIENS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.CHIENS));
		assertEquals(GenreImpot.PATENTE_TABAC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PATENTE_TABAC));
	}

	@Test
	public void testRattachementCoherence() {
		assertEnumLengthEquals(MotifRattachement.class, ch.vd.uniregctb.type.MotifRattachement.class);
		assertEnumConstantsEqual(MotifRattachement.class, ch.vd.uniregctb.type.MotifRattachement.class);
	}

	@Test
	public void testRattachementFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifRattachement) null));
		assertEquals(MotifRattachement.DOMICILE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DOMICILE));
		assertEquals(MotifRattachement.IMMEUBLE_PRIVE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE));
		assertEquals(MotifRattachement.DIPLOMATE_SUISSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_SUISSE));
		assertEquals(MotifRattachement.DIPLOMATE_ETRANGER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_ETRANGER));
		assertEquals(MotifRattachement.ACTIVITE_INDEPENDANTE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_INDEPENDANTE));
		assertEquals(MotifRattachement.SEJOUR_SAISONNIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.SEJOUR_SAISONNIER));
		assertEquals(MotifRattachement.DIRIGEANT_SOCIETE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIRIGEANT_SOCIETE));
		assertEquals(MotifRattachement.ACTIVITE_LUCRATIVE_CAS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_LUCRATIVE_CAS));
		assertEquals(MotifRattachement.ADMINISTRATEUR, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ADMINISTRATEUR));
		assertEquals(MotifRattachement.CREANCIER_HYPOTHECAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.CREANCIER_HYPOTHECAIRE));
		assertEquals(MotifRattachement.PRESTATION_PREVOYANCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.PRESTATION_PREVOYANCE));
	}

	@Test
	public void testTypeAutoriteFiscaleCoherence() {
		assertEnumLengthEquals(TypeAutoriteFiscale.class, ch.vd.uniregctb.type.TypeAutoriteFiscale.class);
		assertEnumConstantsEqual(TypeAutoriteFiscale.class, ch.vd.uniregctb.type.TypeAutoriteFiscale.class);
	}

	@Test
	public void testTypeAutoriteFiscaleFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeAutoriteFiscale) null));
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD));
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_HC));
		assertEquals(TypeAutoriteFiscale.PAYS_HS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.PAYS_HS));
	}

	@Test
	public void testModeImpositionCoherence() {
		assertEnumLengthEquals(ModeImposition.class, ch.vd.uniregctb.type.ModeImposition.class);
		assertEnumConstantsEqual(ModeImposition.class, ch.vd.uniregctb.type.ModeImposition.class);
	}

	@Test
	public void testModeImpositionFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.ModeImposition) null));
		assertEquals(ModeImposition.ORDINAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.ORDINAIRE));
		assertEquals(ModeImposition.SOURCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.SOURCE));
		assertEquals(ModeImposition.DEPENSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.DEPENSE));
		assertEquals(ModeImposition.MIXTE_137_1, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_1));
		assertEquals(ModeImposition.MIXTE_137_2, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_2));
		assertEquals(ModeImposition.INDIGENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.INDIGENT));
	}

}
