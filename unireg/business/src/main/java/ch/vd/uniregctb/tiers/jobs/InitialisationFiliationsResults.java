package ch.vd.uniregctb.tiers.jobs;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tiers.Filiation;

public class InitialisationFiliationsResults extends AbstractJobResults<Long, InitialisationFiliationsResults> {

	public static class InfoFiliation {
		public final long noCtbParent;
		public final long noCtbEnfant;
		public final RegDate dateDebut;
		public final RegDate dateFin;

		public InfoFiliation(Filiation filiation) {
			this.noCtbParent = filiation.getSujetId();
			this.noCtbEnfant = filiation.getObjetId();
			this.dateDebut = filiation.getDateDebut();
			this.dateFin = filiation.getDateFin();
		}
	}

	public static class InfoErreur {
		public final long noCtbEnfant;
		public final String msg;

		public InfoErreur(long noCtbEnfant, String msg) {
			this.noCtbEnfant = noCtbEnfant;
			this.msg = msg;
		}
	}

	public final int nbThreads;
	public boolean interrupted;

	private final List<InfoFiliation> filiations = new LinkedList<>();
	private final List<InfoErreur> erreurs = new LinkedList<>();

	public InitialisationFiliationsResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new InfoErreur(element, buildErrorMessage(e)));
	}

	@Override
	public void addAll(InitialisationFiliationsResults right) {
		filiations.addAll(right.filiations);
		erreurs.addAll(right.erreurs);
	}

	public void addFiliation(Filiation filiation) {
		filiations.add(new InfoFiliation(filiation));
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

	public List<InfoFiliation> getFiliations() {
		return filiations;
	}

	public List<InfoErreur> getErreurs() {
		return erreurs;
	}
}
