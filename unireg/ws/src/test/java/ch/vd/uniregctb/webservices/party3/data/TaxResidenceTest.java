package ch.vd.uniregctb.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.uniregctb.webservices.party3.EnumTest;
import ch.vd.uniregctb.webservices.party3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class TaxResidenceTest extends EnumTest {

	@Test
	public void testGenreImpotCoherence() {
		assertEnumLengthEquals(TaxType.class, ch.vd.uniregctb.type.GenreImpot.class);
	}

	@Test
	public void testGenreImpotFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.GenreImpot) null));
		assertEquals(TaxType.INCOME_WEALTH, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.REVENU_FORTUNE));
		assertEquals(TaxType.IMMOVABLE_PROPERTY_GAINS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.GAIN_IMMOBILIER));
		assertEquals(TaxType.REAL_ESTATE_TRANSFER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DROIT_MUTATION));
		assertEquals(TaxType.CAPITAL_INCOME, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PRESTATION_CAPITAL));
		assertEquals(TaxType.INHERITANCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.SUCCESSION));
		assertEquals(TaxType.GIFTS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DONATION));
		assertEquals(TaxType.REAL_ESTATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.FONCIER));
		assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE));
		assertEquals(TaxType.DOGS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.CHIENS));
		assertEquals(TaxType.TOBACCO_PATENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.GenreImpot.PATENTE_TABAC));
	}

	@Test
	public void testRattachementCoherence() {
		assertEnumLengthEquals(TaxLiabilityReason.class, ch.vd.uniregctb.type.MotifRattachement.class);
	}

	@Test
	public void testRattachementFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifRattachement) null));
		assertEquals(TaxLiabilityReason.RESIDENCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DOMICILE));
		assertEquals(TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.IMMEUBLE_PRIVE));
		assertEquals(TaxLiabilityReason.SWISS_DIPLOMAT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_SUISSE));
		assertEquals(TaxLiabilityReason.FOREIGN_DIPLOMAT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIPLOMATE_ETRANGER));
		assertEquals(TaxLiabilityReason.INDEPENDANT_ACTIVITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_INDEPENDANTE));
		assertEquals(TaxLiabilityReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.SEJOUR_SAISONNIER));
		assertEquals(TaxLiabilityReason.COMPANY_LEADER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.DIRIGEANT_SOCIETE));
		assertEquals(TaxLiabilityReason.GAINFUL_ACTIVITY_SAS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ACTIVITE_LUCRATIVE_CAS));
		assertEquals(TaxLiabilityReason.ADMINISTRATOR, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.ADMINISTRATEUR));
		assertEquals(TaxLiabilityReason.MORTGAGE_CREDITORS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.CREANCIER_HYPOTHECAIRE));
		assertEquals(TaxLiabilityReason.PENSION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifRattachement.PRESTATION_PREVOYANCE));
	}

	@Test
	public void testTypeAutoriteFiscaleCoherence() {
		assertEnumLengthEquals(TaxationAuthorityType.class, ch.vd.uniregctb.type.TypeAutoriteFiscale.class);
	}

	@Test
	public void testTypeAutoriteFiscaleFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeAutoriteFiscale) null));
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD));
		assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_HC));
		assertEquals(TaxationAuthorityType.FOREIGN_COUNTRY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeAutoriteFiscale.PAYS_HS));
	}

	@Test
	public void testModeImpositionCoherence() {
		assertEnumLengthEquals(TaxationMethod.class, ch.vd.uniregctb.type.ModeImposition.class);
	}

	@Test
	public void testModeImpositionFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.ModeImposition) null));
		assertEquals(TaxationMethod.ORDINARY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.ORDINAIRE));
		assertEquals(TaxationMethod.WITHHOLDING, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.SOURCE));
		assertEquals(TaxationMethod.EXPENDITURE_BASED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.DEPENSE));
		assertEquals(TaxationMethod.MIXED_137_1, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_1));
		assertEquals(TaxationMethod.MIXED_137_2, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.MIXTE_137_2));
		assertEquals(TaxationMethod.INDIGENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.ModeImposition.INDIGENT));
	}

}
