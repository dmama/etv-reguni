package ch.vd.uniregctb.evenement.reqdes.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.StatusManager;

public class MockEvenementReqDesRetryProcessor implements EvenementReqDesRetryProcessor {

	@Override
	public void relancerEvenementsReqDesNonTraites(@Nullable StatusManager statusManager) {
		// rien à faire... nous sommes dans un mock
	}
}
