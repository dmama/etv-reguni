package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;


public class TaxResidenceTest extends EnumTest {

	@Test
	public void testGenreImpotCoherence() {
		assertEnumLengthEquals(TaxType.class, GenreImpot.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (GenreImpot genre : GenreImpot.values()) {
			assertNotNull(genre.name(), EnumHelper.coreToWeb(genre));
		}
	}

	@Test
	public void testGenreImpotFromValue() {
		assertNull(EnumHelper.coreToWeb((GenreImpot) null));
		assertEquals(TaxType.INCOME_WEALTH, EnumHelper.coreToWeb(GenreImpot.REVENU_FORTUNE));
		assertEquals(TaxType.IMMOVABLE_PROPERTY_GAINS, EnumHelper.coreToWeb(GenreImpot.GAIN_IMMOBILIER));
		assertEquals(TaxType.REAL_ESTATE_TRANSFER, EnumHelper.coreToWeb(GenreImpot.DROIT_MUTATION));
		assertEquals(TaxType.CAPITAL_INCOME, EnumHelper.coreToWeb(GenreImpot.PRESTATION_CAPITAL));
		assertEquals(TaxType.INHERITANCE, EnumHelper.coreToWeb(GenreImpot.SUCCESSION));
		assertEquals(TaxType.GIFTS, EnumHelper.coreToWeb(GenreImpot.DONATION));
		assertEquals(TaxType.REAL_ESTATE, EnumHelper.coreToWeb(GenreImpot.FONCIER));
		assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, EnumHelper.coreToWeb(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE));
		assertEquals(TaxType.DOGS, EnumHelper.coreToWeb(GenreImpot.CHIENS));
		assertEquals(TaxType.TOBACCO_PATENT, EnumHelper.coreToWeb(GenreImpot.PATENTE_TABAC));
	}

	@Test
	public void testRattachementCoherence() {
		assertEnumLengthEquals(TaxLiabilityReason.class, MotifRattachement.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (MotifRattachement motif : MotifRattachement.values()) {
			assertNotNull(motif.name(), EnumHelper.coreToWeb(motif));
		}
	}

	@Test
	public void testRattachementFromValue() {
		assertNull(EnumHelper.coreToWeb((MotifRattachement) null));
		assertEquals(TaxLiabilityReason.RESIDENCE, EnumHelper.coreToWeb(MotifRattachement.DOMICILE));
		assertEquals(TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY, EnumHelper.coreToWeb(MotifRattachement.IMMEUBLE_PRIVE));
		assertEquals(TaxLiabilityReason.SWISS_DIPLOMAT, EnumHelper.coreToWeb(MotifRattachement.DIPLOMATE_SUISSE));
		assertEquals(TaxLiabilityReason.FOREIGN_DIPLOMAT, EnumHelper.coreToWeb(MotifRattachement.DIPLOMATE_ETRANGER));
		assertEquals(TaxLiabilityReason.INDEPENDANT_ACTIVITY, EnumHelper.coreToWeb(MotifRattachement.ACTIVITE_INDEPENDANTE));
		assertEquals(TaxLiabilityReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(MotifRattachement.SEJOUR_SAISONNIER));
		assertEquals(TaxLiabilityReason.COMPANY_LEADER, EnumHelper.coreToWeb(MotifRattachement.DIRIGEANT_SOCIETE));
		assertEquals(TaxLiabilityReason.GAINFUL_ACTIVITY_SAS, EnumHelper.coreToWeb(MotifRattachement.ACTIVITE_LUCRATIVE_CAS));
		assertEquals(TaxLiabilityReason.ADMINISTRATOR, EnumHelper.coreToWeb(MotifRattachement.ADMINISTRATEUR));
		assertEquals(TaxLiabilityReason.MORTGAGE_CREDITORS, EnumHelper.coreToWeb(MotifRattachement.CREANCIER_HYPOTHECAIRE));
		assertEquals(TaxLiabilityReason.PENSION, EnumHelper.coreToWeb(MotifRattachement.PRESTATION_PREVOYANCE));
		assertEquals(TaxLiabilityReason.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYER, EnumHelper.coreToWeb(MotifRattachement.PARTICIPATIONS_HORS_SUISSE));
		assertEquals(TaxLiabilityReason.WINE_FARM_SEASONAL_WORKER, EnumHelper.coreToWeb(MotifRattachement.EFFEUILLEUSES));
	}

	@Test
	public void testTypeAutoriteFiscaleCoherence() {
		assertEnumLengthEquals(TaxationAuthorityType.class, TypeAutoriteFiscale.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeAutoriteFiscale taf : TypeAutoriteFiscale.values()) {
			assertNotNull(taf.name(), EnumHelper.coreToWeb(taf));
		}
	}

	@Test
	public void testTypeAutoriteFiscaleFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeAutoriteFiscale) null));
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, EnumHelper.coreToWeb(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD));
		assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, EnumHelper.coreToWeb(TypeAutoriteFiscale.COMMUNE_HC));
		assertEquals(TaxationAuthorityType.FOREIGN_COUNTRY, EnumHelper.coreToWeb(TypeAutoriteFiscale.PAYS_HS));
	}

	@Test
	public void testModeImpositionCoherence() {
		assertEnumLengthEquals(TaxationMethod.class, ModeImposition.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (ModeImposition mode : ModeImposition.values()) {
			assertNotNull(mode.name(), EnumHelper.coreToWeb(mode));
		}
	}

	@Test
	public void testModeImpositionFromValue() {
		assertNull(EnumHelper.coreToWeb((ModeImposition) null));
		assertEquals(TaxationMethod.ORDINARY, EnumHelper.coreToWeb(ModeImposition.ORDINAIRE));
		assertEquals(TaxationMethod.WITHHOLDING, EnumHelper.coreToWeb(ModeImposition.SOURCE));
		assertEquals(TaxationMethod.EXPENDITURE_BASED, EnumHelper.coreToWeb(ModeImposition.DEPENSE));
		assertEquals(TaxationMethod.MIXED_137_1, EnumHelper.coreToWeb(ModeImposition.MIXTE_137_1));
		assertEquals(TaxationMethod.MIXED_137_2, EnumHelper.coreToWeb(ModeImposition.MIXTE_137_2));
		assertEquals(TaxationMethod.INDIGENT, EnumHelper.coreToWeb(ModeImposition.INDIGENT));
	}

}
