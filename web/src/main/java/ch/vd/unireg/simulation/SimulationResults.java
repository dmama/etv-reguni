package ch.vd.unireg.simulation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import ch.vd.unireg.tache.sync.SynchronizeAction;

@SuppressWarnings("UnusedDeclaration")
public class SimulationResults {

	private String exception;
	private final List<String> errors = new ArrayList<>();
	private final List<String> actions = new ArrayList<>();

	public void addErrors(List<String> errors) {
		this.errors.addAll(errors);
	}

	public void addError(String message) {
		this.errors.add(message);
	}

	public void setException(Exception exception) {
		this.exception = ExceptionUtils.getStackTrace(exception);
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
