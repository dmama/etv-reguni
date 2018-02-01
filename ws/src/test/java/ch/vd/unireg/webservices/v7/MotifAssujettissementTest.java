package ch.vd.unireg.webservices.v7;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.metier.assujettissement.MotifAssujettissement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MotifAssujettissementTest extends EnumTest {

	private static MotifAssujettissement[] buildAllowedCoreMotifsAssujettissement() {
		// tous les motifs actuels n'existent pas forcément dans le mapping de la version 7
		final Set<MotifAssujettissement> set = EnumSet.complementOf(EnumSet.of(MotifAssujettissement.EXONERATION));
		return set.toArray(new MotifAssujettissement[set.size()]);
	}

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(LiabilityChangeReason.class.getEnumConstants(), buildAllowedCoreMotifsAssujettissement());

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (MotifAssujettissement motif : MotifAssujettissement.values()) {
			assertNotNull(motif.name(), EnumHelper.coreToWeb(motif));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testCoreToWeb() {
		assertNull(EnumHelper.coreToWeb((MotifAssujettissement) null));
		assertEquals(LiabilityChangeReason.MOVE_VD, EnumHelper.coreToWeb(MotifAssujettissement.DEMENAGEMENT_VD));
		assertEquals(LiabilityChangeReason.WIDOWHOOD_DEATH, EnumHelper.coreToWeb(MotifAssujettissement.VEUVAGE_DECES));
		assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, EnumHelper.coreToWeb(MotifAssujettissement.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION));
		assertEquals(LiabilityChangeReason.SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION, EnumHelper.coreToWeb(MotifAssujettissement.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT));
		assertEquals(LiabilityChangeReason.C_PERMIT_SWISS, EnumHelper.coreToWeb(MotifAssujettissement.PERMIS_C_SUISSE));
		assertEquals(LiabilityChangeReason.MAJORITY, EnumHelper.coreToWeb(MotifAssujettissement.MAJORITE));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, EnumHelper.coreToWeb(MotifAssujettissement.ARRIVEE_HS));
		assertEquals(LiabilityChangeReason.MOVE_IN_FROM_OTHER_CANTON, EnumHelper.coreToWeb(MotifAssujettissement.ARRIVEE_HC));
		assertEquals(LiabilityChangeReason.MERGE_OF_MUNICIPALITIES, EnumHelper.coreToWeb(MotifAssujettissement.FUSION_COMMUNES));
		assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, EnumHelper.coreToWeb(MotifAssujettissement.ACHAT_IMMOBILIER));
		assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, EnumHelper.coreToWeb(MotifAssujettissement.VENTE_IMMOBILIER));
		assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(MotifAssujettissement.DEBUT_EXPLOITATION));
		assertEquals(LiabilityChangeReason.END_COMMERCIAL_EXPLOITATION, EnumHelper.coreToWeb(MotifAssujettissement.FIN_EXPLOITATION));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, EnumHelper.coreToWeb(MotifAssujettissement.DEPART_HS));
		assertEquals(LiabilityChangeReason.DEPARTURE_TO_OTHER_CANTON, EnumHelper.coreToWeb(MotifAssujettissement.DEPART_HC));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(MotifAssujettissement.INDETERMINE));
		assertEquals(LiabilityChangeReason.SEASONAL_JOURNEY, EnumHelper.coreToWeb(MotifAssujettissement.SEJOUR_SAISONNIER));
		assertEquals(LiabilityChangeReason.CHANGE_OF_TAXATION_METHOD, EnumHelper.coreToWeb(MotifAssujettissement.CHGT_MODE_IMPOSITION));
		assertEquals(LiabilityChangeReason.CANCELLATION, EnumHelper.coreToWeb(MotifAssujettissement.ANNULATION));
		assertEquals(LiabilityChangeReason.REACTIVATION, EnumHelper.coreToWeb(MotifAssujettissement.REACTIVATION));
		assertEquals(LiabilityChangeReason.START_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(MotifAssujettissement.DEBUT_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.END_DIPLOMATIC_ACTVITY, EnumHelper.coreToWeb(MotifAssujettissement.FIN_ACTIVITE_DIPLOMATIQUE));
		assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, EnumHelper.coreToWeb(MotifAssujettissement.DEBUT_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.END_WITHHOLDING_ACTIVITY, EnumHelper.coreToWeb(MotifAssujettissement.FIN_PRESTATION_IS));
		assertEquals(LiabilityChangeReason.END_ACTIVITY_MERGER_BANKRUPTCY, EnumHelper.coreToWeb(MotifAssujettissement.CESSATION_ACTIVITE_FUSION_FAILLITE));
		assertEquals(LiabilityChangeReason.MOVE_HEADQUARTERS, EnumHelper.coreToWeb(MotifAssujettissement.DEMENAGEMENT_SIEGE));
		assertEquals(LiabilityChangeReason.CORPORATION_MERGER, EnumHelper.coreToWeb(MotifAssujettissement.FUSION_ENTREPRISES));
		assertEquals(LiabilityChangeReason.BANKRUPTCY, EnumHelper.coreToWeb(MotifAssujettissement.FAILLITE));
		assertEquals(LiabilityChangeReason.UNDETERMINED, EnumHelper.coreToWeb(MotifAssujettissement.EXONERATION));
	}
}
