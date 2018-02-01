package ch.vd.unireg.registrefoncier;

import ch.vd.unireg.tiers.Tiers;

public class MockRapprochementManuelTiersRFService implements RapprochementManuelTiersRFService {

	@Override
	public void genererDemandeIdentificationManuelle(TiersRF tiersRF) {
		// on ne fait rien, c'est un mock...
	}

	@Override
	public void marquerDemandesIdentificationManuelleEventuelles(TiersRF tiersRF, Tiers tiersUnireg) {
		// on ne fait rien c'est un mock...
	}
}
