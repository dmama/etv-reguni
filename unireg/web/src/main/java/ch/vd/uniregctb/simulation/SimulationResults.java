package ch.vd.uniregctb.simulation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;

@SuppressWarnings("UnusedDeclaration")
public class SimulationResults {

	private String exception;
	private final List<String> errors = new ArrayList<String>();
	private final List<String> actions = new ArrayList<String>();

	public void addErrors(List<String> errors) {
		this.errors.addAll(errors);
	}

	public void addError(String message) {
		this.errors.add(message);
	}

	public void setException(Exception exception) {
		this.exception = ExceptionUtils.extractCallStack(exception);
	}

	public void addActions(List<SynchronizeAction> actions) {
		for (SynchronizeAction action : actions) {
			this.actions.add(action.toString());
		}
	}

	public String getException() {
		return exception;
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<String> getActions() {
		return actions;
	}

	public boolean isEmpty() {
		return exception == null && errors.isEmpty() && actions.isEmpty();
	}
}
