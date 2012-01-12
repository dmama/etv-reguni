package ch.vd.uniregctb.tiers.manager;

import java.util.List;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.validation.ValidationService;

public class TacheManagerImpl implements TacheManager {

	private TacheService tacheService;

	private ValidationService validationService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@Override
	public SynchronizeActionsTable buildSynchronizeActionsTable(Contribuable ctb, String titre, String titreErreurValidation) {

		final SynchronizeActionsTable table;

		final ValidationResults vr = validationService.validate(ctb);
		if (vr.hasErrors()) {
			table = new SynchronizeActionsTable(titreErreurValidation);
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

			table = new SynchronizeActionsTable(titre);
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
