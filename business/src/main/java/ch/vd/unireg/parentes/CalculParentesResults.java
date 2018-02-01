package ch.vd.unireg.parentes;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.AbstractJobResults;

public class CalculParentesResults extends AbstractJobResults<Long, CalculParentesResults> {

	public static class InfoErreur implements Comparable<InfoErreur> {
		public final long noCtbEnfant;
		public final String msg;

		public InfoErreur(long noCtbEnfant, String msg) {
			this.noCtbEnfant = noCtbEnfant;
			this.msg = msg;
		}

		@Override
		public int compareTo(InfoErreur o) {
			return Long.compare(noCtbEnfant, o.noCtbEnfant);
		}
	}

	public final CalculParentesMode mode;
	public final int nbThreads;
	public boolean interrupted;

	private final List<ParenteUpdateInfo> updates = new LinkedList<>();
	private final List<InfoErreur> erreurs = new LinkedList<>();

	public CalculParentesResults(int nbThreads, CalculParentesMode mode) {
		this.nbThreads = nbThreads;
		this.mode = mode;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		addError(element, buildErrorMessage(e));
	}

	public void addError(Long element, String errorMessage) {
		erreurs.add(new InfoErreur(element, errorMessage));
	}

	@Override
	public void addAll(CalculParentesResults right) {
		updates.addAll(right.updates);
		erreurs.addAll(right.erreurs);
	}

	public void addParenteUpdate(ParenteUpdateInfo update) {
		updates.add(update);
	}

	private static String buildErrorMessage(Exception e) {
		final String msg = e.getMessage();
		if (StringUtils.isBlank(msg)) {
			return e.getClass().getName();
		}
		else {
			return String.format("%s: %s", e.getClass().getName(), e.getMessage());
		}
	}

	public List<ParenteUpdateInfo> getUpdates() {
		return updates;
	}

	public List<InfoErreur> getErreurs() {
		return erreurs;
	}

	@Override
	public void end() {
		Collections.sort(updates);
		Collections.sort(erreurs);
		super.end();
	}
}
