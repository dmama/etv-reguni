package ch.vd.unireg.webservices.party3.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MotifForTest extends EnumTest {

	private static ch.vd.unireg.type.MotifFor[] buildAllowedCoreMotifsFor() {
		// tous les motifs actuels n'existent pas forcément dans le mapping de la version 3
		final Set<ch.vd.unireg.type.MotifFor> set = EnumSet.complementOf(EnumSet.of(ch.vd.unireg.type.MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE,
							                                                           ch.vd.unireg.type.MotifFor.DEBUT_PRESTATION_IS,
							                                                           ch.vd.unireg.type.MotifFor.FIN_PRESTATION_IS,
							                                                           ch.vd.unireg.type.MotifFor.DEMENAGEMENT_SIEGE,
							                                                           ch.vd.unireg.type.MotifFor.FUSION_ENTREPRISES,
							                                                           ch.vd.unireg.type.MotifFor.FAILLITE));
		return set.toArray(new ch.vd.unireg.type.MotifFor[set.size()]);
	}

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(LiabilityChangeReason.class.getEnumConstants(), buildAllowedCoreMotifsFor());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCoreToWeb() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.MotifFor) null));
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.MotifFor) null));
		assertEquals(LiabilityChangeReason.MOVE_VD, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEMENAGEMENT_VD));
		assertEquals(LiabilityChangeReason.WIDOWHOOD_DEATH, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.VEUVAGE_DECES));
		assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION));
		assertEquals(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT));
		assertEquals(LiabilityChangeReason.C_PERMIT_SWISS, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.PERMIS_C_SUISSE));
		assertEquals(LiabilityChangeReason.MAJORITY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.MAJORITE));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.ARRIVEE_HS));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.ARRIVEE_HC));
		assertEquals(LiabilityChangeReason.MERGE_OF_MUNICIPALITIES, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FUSION_COMMUNES));
		assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.ACHAT_IMMOBILIER));
		assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.VENTE_IMMOBILIER));
		assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEBUT_EXPLOITATION));
		assertEquals(LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FIN_EXPLOITATION));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEPART_HS));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEPART_HC));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.INDETERMINE));
		assertEquals(LiabilityChangeReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.SEJOUR_SAISONNIER));
		assertEquals(LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.CHGT_MODE_IMPOSITION));
		assertEquals(LiabilityChangeReason.CANCELLATION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.ANNULATION));
		assertEquals(LiabilityChangeReason.REACTIVATION, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.REACTIVATION));
		assertEquals(LiabilityChangeReason.START_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FIN_ACTIVITE_DIPLOMATIQUE));

		// [SIFISC-8712] Pour des raisons de compatibilité ascendante, les nouveaux motifs sont traduits en "UNDETERMINED"
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEBUT_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FIN_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.DEMENAGEMENT_SIEGE));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FUSION_ENTREPRISES));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.unireg.type.MotifFor.FAILLITE));
	}
}
