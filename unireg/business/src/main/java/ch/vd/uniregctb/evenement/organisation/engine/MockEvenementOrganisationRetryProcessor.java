package ch.vd.uniregctb.evenement.organisation.engine;

import org.jetbrains.annotations.Nullable;

import ch.vd.shared.batchtemplate.StatusManager;

public class MockEvenementOrganisationRetryProcessor implements EvenementOrganisationRetryProcessor {

	@Override
	public void retraiteEvenements(@Nullable StatusManager status) {
		// rien à faire...
	}
}
