package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception lancée lors d'un problème de cohérence dans les données récupérées depuis le registre civil
 */
public class DonneesCivilesException extends RuntimeException {

	private static final long serialVersionUID = 3301152111456034197L;

	private final List<String> errors;

	private final String descriptionContexte;

	private final Long numeroCtb;

	public DonneesCivilesException(String error) {
		this.descriptionContexte = null;
		this.errors = new ArrayList<String>(1);
		this.errors.add(error);
		this.numeroCtb = null;
	}

	public DonneesCivilesException(List<String> errors) {
		this(null, errors, null);
	}

	public DonneesCivilesException(String descriptionContexte, List<String> errors) {
		this(descriptionContexte, errors, null);
	}

	public DonneesCivilesException(String descriptionContexte, List<String> errors, Long numeroCtb) {
		this.descriptionContexte = descriptionContexte;
		this.errors = errors;
		this.numeroCtb = numeroCtb;
	}

	public List<String> getErrors() {
		return errors;
	}

	public String getDescriptionContexte() {
		return descriptionContexte;
	}

	public Long getNumeroCtb() {
		return numeroCtb;
	}

	@Override
	public String getMessage() {
		final StringBuilder builder = new StringBuilder();
		if (numeroCtb != null) {
			builder.append("Pour le tiers ").append(FormatNumeroHelper.numeroCTBToDisplay(numeroCtb));
		}
		if (descriptionContexte != null) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(descriptionContexte);
		}
		if (builder.length() > 0) {
			builder.append("\n");
		}

		final int size = errors.size();
		for (int i = 0; i < size; ++i) {
			final String error = errors.get(i);
			builder.append(error);
			if (i < size - 1) {
				builder.append("\n");
			}
		}
		return builder.toString();
	}
}
