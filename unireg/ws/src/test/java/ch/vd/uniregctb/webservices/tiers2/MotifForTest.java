package ch.vd.uniregctb.webservices.tiers2;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal.MotifFor;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class MotifForTest extends EnumTest {

	private static ch.vd.uniregctb.type.MotifFor[] buildAllowedCoreMotifsFor() {
		// tous les motifs actuels n'existent pas forcément dans le mapping de la version 2
		final Set<ch.vd.uniregctb.type.MotifFor> set = EnumSet.complementOf(EnumSet.of(ch.vd.uniregctb.type.MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE,
		                                                                               ch.vd.uniregctb.type.MotifFor.FIN_ACTIVITE_DIPLOMATIQUE,
		                                                                               ch.vd.uniregctb.type.MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE,
		                                                                               ch.vd.uniregctb.type.MotifFor.DEBUT_PRESTATION_IS,
		                                                                               ch.vd.uniregctb.type.MotifFor.FIN_PRESTATION_IS));
		return set.toArray(new ch.vd.uniregctb.type.MotifFor[set.size()]);
	}

	@Test
	public void testCoherence() {
		final ch.vd.uniregctb.type.MotifFor[] coreMotifsFor = buildAllowedCoreMotifsFor();
		assertEnumLengthEquals(MotifFor.class.getEnumConstants(), coreMotifsFor);
		assertEnumConstantsEqual(MotifFor.class.getEnumConstants(), coreMotifsFor);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCoreToWeb() {
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
		// [UNIREG-911] pour des raisons de compatibilité ascendante, les motifs de début/fin d'activité diplomatiques sont mappés comme indéterminés
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_ACTIVITE_DIPLOMATIQUE));
		// [SIFISC-8712] Pareil pour les motifs liés spécifiquement aux débiteurs IS
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.DEBUT_PRESTATION_IS));
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.FIN_PRESTATION_IS));
		assertEquals(MotifFor.INDETERMINE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE));
	}
}
