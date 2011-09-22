package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v1.LiabilityChangeReason;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class MotifForTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(LiabilityChangeReason.class, ch.vd.uniregctb.type.MotifFor.class);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifFor) null));
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifFor) null));
		assertEquals(LiabilityChangeReason.MOVE_VD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEMENAGEMENT_VD));
		assertEquals(LiabilityChangeReason.WIDOWHOOD_DEATH, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.VEUVAGE_DECES));
		assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION));
		assertEquals(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT));
		assertEquals(LiabilityChangeReason.C_PERMIT_SWISS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.PERMIS_C_SUISSE));
		assertEquals(LiabilityChangeReason.MAJORITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.MAJORITE));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ARRIVEE_HS));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ARRIVEE_HC));
		assertEquals(LiabilityChangeReason.MERGE_OF_MUNICIPALITIES, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FUSION_COMMUNES));
		assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER));
		assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.VENTE_IMMOBILIER));
		assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_EXPLOITATION));
		assertEquals(LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_EXPLOITATION));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEPART_HS));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEPART_HC));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.INDETERMINE));
		assertEquals(LiabilityChangeReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.SEJOUR_SAISONNIER));
		assertEquals(LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.CHGT_MODE_IMPOSITION));
		assertEquals(LiabilityChangeReason.CANCELLATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ANNULATION));
		assertEquals(LiabilityChangeReason.REACTIVATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.REACTIVATION));
		assertEquals(LiabilityChangeReason.START_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_ACTIVITE_DIPLOMATIQUE));
	}
}
