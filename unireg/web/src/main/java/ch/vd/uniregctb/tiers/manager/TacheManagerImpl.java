package ch.vd.uniregctb.tiers.manager;

import java.util.List;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;

public class TacheManagerImpl implements TacheManager {

	private TacheService tacheService;

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public SynchronizeActionsTable buildSynchronizeActionsTable(Contribuable ctb, String titre) {

		final SynchronizeActionsTable table = new SynchronizeActionsTable(titre);

		final ValidationResults vr = ctb.validate();
		if (vr.hasErrors()) {
			table.addErrors(vr.getErrors());			
		}
		else {
			Exception exception = null;
			List<SynchronizeAction> actions = null;

			try {
				actions = tacheService.determineSynchronizeActionsForDIs(ctb);
			}
			catch (AssujettissementException e) {
				exception = e;
			}

			if (exception != null) {
				table.addException(exception);
			}
			else if (actions == null || actions.isEmpty()) {
				// rien d'intéressant à montrer
				return null;
			}
			else {
				table.addActions(actions);
			}
		}

		return table;
	}
}
