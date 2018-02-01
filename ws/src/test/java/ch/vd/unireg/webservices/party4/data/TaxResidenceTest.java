package ch.vd.unireg.webservices.party4.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationMethod;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class TaxResidenceTest extends EnumTest {

	@Test
	public void testGenreImpotCoherence() {
 		assertEnumLengthEquals(TaxType.class, GenreImpot.class);
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
		// les EFF et les PHS ne sont pas mappables séparément dans le type de sortie en v4
		final Set<MotifRattachement> motifsMappables = EnumSet.complementOf(EnumSet.of(MotifRattachement.EFFEUILLEUSES, MotifRattachement.PARTICIPATIONS_HORS_SUISSE));
		assertEquals(TaxLiabilityReason.values().length, motifsMappables.size());
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

		// non mappables séparément -> réutilisation d'autres constantes les plus proches...
		assertEquals(TaxLiabilityReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(MotifRattachement.EFFEUILLEUSES));
		assertEquals(TaxLiabilityReason.ADMINISTRATOR, EnumHelper.coreToWeb(MotifRattachement.PARTICIPATIONS_HORS_SUISSE));
	}

	@Test
	public void testTypeAutoriteFiscaleCoherence() {
		assertEnumLengthEquals(TaxationAuthorityType.class, TypeAutoriteFiscale.class);
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
