package ch.vd.uniregctb.tiers.jobs;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.tiers.Parente;

public class InitialisationParentesResults extends AbstractJobResults<Long, InitialisationParentesResults> {

	public static class InfoParente {
		public final long noCtbParent;
		public final long noCtbEnfant;
		public final RegDate dateDebut;
		public final RegDate dateFin;

		public InfoParente(Parente parente) {
			this.noCtbParent = parente.getObjetId();
			this.noCtbEnfant = parente.getSujetId();
			this.dateDebut = parente.getDateDebut();
			this.dateFin = parente.getDateFin();
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

	private final List<InfoParente> parentes = new LinkedList<>();
	private final List<InfoErreur> erreurs = new LinkedList<>();

	public InitialisationParentesResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new InfoErreur(element, buildErrorMessage(e)));
	}

	@Override
	public void addAll(InitialisationParentesResults right) {
		parentes.addAll(right.parentes);
		erreurs.addAll(right.erreurs);
	}

	public void addParente(Parente parente) {
		parentes.add(new InfoParente(parente));
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

	public List<InfoParente> getParentes() {
		return parentes;
	}

	public List<InfoErreur> getErreurs() {
		return erreurs;
	}
}
