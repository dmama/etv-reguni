package ch.vd.uniregctb.validation;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.validation.ValidationResults;

public class ValidationResultsView {

	private List<String> errors;
	private List<String> warnings;

	public ValidationResultsView(ValidationResults res) {
		if (res.hasErrors()) {
			this.errors = new ArrayList<String>();
			this.errors.addAll(res.getErrors());
		}
		if (res.hasWarnings()) {
			this.warnings = new ArrayList<String>();
			this.warnings.addAll(res.getWarnings());
		}
	}

	public List<String> getErrors() {
		return errors;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<String> getWarnings() {
		return warnings;
	}
}
