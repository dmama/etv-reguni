package ch.vd.unireg.evenement.organisation.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.StatusManager;

public class MockEvenementOrganisationRetryProcessor implements EvenementOrganisationRetryProcessor {

	@Override
	public void retraiteEvenements(@Nullable StatusManager status) {
		// rien à faire...
	}
}
