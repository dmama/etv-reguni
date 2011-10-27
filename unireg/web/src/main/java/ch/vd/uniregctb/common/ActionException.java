package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;

/**
 * Exception qui contient les erreurs (éventuellement les warnings) découlant d'une action impossible dans un contrôleur web (ou un
 * manager).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ActionException extends RuntimeException {

	private static final long serialVersionUID = -4038209694889119288L;

	private final List<String> errors;
	private final List<String> warnings;

	public ActionException(String error) {
		this(error, null);
	}

	public ActionException(String error, @Nullable Throwable cause) {
		super(cause);
		this.errors = new ArrayList<String>(1);
		this.errors.add(error);
		this.warnings = Collections.emptyList();
	}

	public ActionException(List<String> errors, List<String> warnings) {
		this(errors, warnings, null);
	}

	public ActionException(List<String> errors, List<String> warnings, @Nullable Throwable cause) {
		super(cause);
		Assert.notEmpty(errors);
		this.errors = errors;
		this.warnings = warnings;
	}

	public List<String> getErrors() {
		return errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public String getMessage() {

		StringBuilder message = new StringBuilder();
		message.append(errors.size()).append(" erreur(s) - ").append(warnings.size()).append(" warning(s):\n");

		for (String e : errors) {
			message.append(" [E] ").append(e).append('\n');
		}

		for (String w : warnings) {
			message.append(" [W] ").append(w).append('\n');
		}

		return message.toString();
	}
}
