package ch.vd.unireg.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.type.MotifFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MotifForTest extends EnumTest {

	private static MotifFor[] buildAllowedCoreMotifsFor() {
		return MotifFor.values();
	}

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(LiabilityChangeReason.class.getEnumConstants(), buildAllowedCoreMotifsFor());

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (MotifFor motif : MotifFor.values()) {
			assertNotNull(motif.name(), EnumHelper.coreToWeb(motif));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCoreToWeb() {
		assertNull(EnumHelper.coreToWeb((MotifFor) null));
		assertEquals(LiabilityChangeReason.MOVE_VD, EnumHelper.coreToWeb(MotifFor.DEMENAGEMENT_VD));
		assertEquals(LiabilityChangeReason.WIDOWHOOD_DEATH, EnumHelper.coreToWeb(MotifFor.VEUVAGE_DECES));
		assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, EnumHelper.coreToWeb(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION));
		assertEquals(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, EnumHelper.coreToWeb(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT));
		assertEquals(LiabilityChangeReason.C_PERMIT_SWISS, EnumHelper.coreToWeb(MotifFor.PERMIS_C_SUISSE));
		assertEquals(LiabilityChangeReason.MAJORITY, EnumHelper.coreToWeb(MotifFor.MAJORITE));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, EnumHelper.coreToWeb(MotifFor.ARRIVEE_HS));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, EnumHelper.coreToWeb(MotifFor.ARRIVEE_HC));
		assertEquals(LiabilityChangeReason.MERGE_OF_MUNICIPALITIES, EnumHelper.coreToWeb(MotifFor.FUSION_COMMUNES));
		assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, EnumHelper.coreToWeb(MotifFor.ACHAT_IMMOBILIER));
		assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, EnumHelper.coreToWeb(MotifFor.VENTE_IMMOBILIER));
		assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(MotifFor.DEBUT_EXPLOITATION));
		assertEquals(LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(MotifFor.FIN_EXPLOITATION));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, EnumHelper.coreToWeb(MotifFor.DEPART_HS));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, EnumHelper.coreToWeb(MotifFor.DEPART_HC));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(MotifFor.INDETERMINE));
		assertEquals(LiabilityChangeReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(MotifFor.SEJOUR_SAISONNIER));
		assertEquals(LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, EnumHelper.coreToWeb(MotifFor.CHGT_MODE_IMPOSITION));
		assertEquals(LiabilityChangeReason.CANCELLATION, EnumHelper.coreToWeb(MotifFor.ANNULATION));
		assertEquals(LiabilityChangeReason.REACTIVATION, EnumHelper.coreToWeb(MotifFor.REACTIVATION));
		assertEquals(LiabilityChangeReason.START_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, EnumHelper.coreToWeb(MotifFor.DEBUT_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.END_WITHHOLDING_ACTIVITY, EnumHelper.coreToWeb(MotifFor.FIN_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.END_ACTIVITY_MERGER_BANKRUPTCY, EnumHelper.coreToWeb(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE));
		assertEquals(LiabilityChangeReason.MOVE_HEADQUARTERS, EnumHelper.coreToWeb(MotifFor.DEMENAGEMENT_SIEGE));
		assertEquals(LiabilityChangeReason.CORPORATION_MERGER, EnumHelper.coreToWeb(MotifFor.FUSION_ENTREPRISES));
		assertEquals(LiabilityChangeReason.BANKRUPTCY, EnumHelper.coreToWeb(MotifFor.FAILLITE));
	}
}
