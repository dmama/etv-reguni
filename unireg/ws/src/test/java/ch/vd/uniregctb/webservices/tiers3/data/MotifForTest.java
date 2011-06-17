package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.MotifFor;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class MotifForTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(MotifFor.class, ch.vd.uniregctb.type.MotifFor.class);
		assertEnumConstantsEqual(MotifFor.class, ch.vd.uniregctb.type.MotifFor.class);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifFor) null));
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.MotifFor) null));
		assertEquals(MotifFor.DEMENAGEMENT_VD, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEMENAGEMENT_VD));
		assertEquals(MotifFor.VEUVAGE_DECES, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.VEUVAGE_DECES));
		assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION));
		assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT));
		assertEquals(MotifFor.PERMIS_C_SUISSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.PERMIS_C_SUISSE));
		assertEquals(MotifFor.MAJORITE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.MAJORITE));
		assertEquals(MotifFor.ARRIVEE_HS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ARRIVEE_HS));
		assertEquals(MotifFor.ARRIVEE_HC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ARRIVEE_HC));
		assertEquals(MotifFor.FUSION_COMMUNES, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FUSION_COMMUNES));
		assertEquals(MotifFor.ACHAT_IMMOBILIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ACHAT_IMMOBILIER));
		assertEquals(MotifFor.VENTE_IMMOBILIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.VENTE_IMMOBILIER));
		assertEquals(MotifFor.DEBUT_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_EXPLOITATION));
		assertEquals(MotifFor.FIN_EXPLOITATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_EXPLOITATION));
		assertEquals(MotifFor.DEPART_HS, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEPART_HS));
		assertEquals(MotifFor.DEPART_HC, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEPART_HC));
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.INDETERMINE));
		assertEquals(MotifFor.SEJOUR_SAISONNIER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.SEJOUR_SAISONNIER));
		assertEquals(MotifFor.CHGT_MODE_IMPOSITION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.CHGT_MODE_IMPOSITION));
		assertEquals(MotifFor.ANNULATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.ANNULATION));
		assertEquals(MotifFor.REACTIVATION, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.REACTIVATION));
		assertEquals(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_ACTIVITE_DIPLOMATIQUE));
	}
}
