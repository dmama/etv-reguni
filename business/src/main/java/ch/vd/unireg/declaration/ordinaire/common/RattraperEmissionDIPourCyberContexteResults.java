package ch.vd.unireg.declaration.ordinaire.common;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.JobResults;

public class RattraperEmissionDIPourCyberContexteResults extends JobResults<Long, RattraperEmissionDIPourCyberContexteResults> {

	public static class Erreur {
		public final long diId;
		public final Long ctbId;
		public final Integer periode;
		public final Integer noSequence;
		public final String message;

		public Erreur(long diId, Long ctbId, Integer periode, Integer noSequence, String message) {
			this.diId = diId;
			this.ctbId = ctbId;
			this.periode = periode;
			this.noSequence = noSequence;
			this.message = message;
		}
	}

	public static class Ignoree {
		public final long diId;
		public final long ctbId;
		public final int periode;
		public final int noSequence;
		public final String message;

		public Ignoree(long diId, long ctbId, int periode, int noSequence, String message) {
			this.diId = diId;
			this.ctbId = ctbId;
			this.periode = periode;
			this.noSequence = noSequence;
			this.message = message;
		}
	}

	public static class Traite {
		public final long diId;
		public final long ctbId;
		public final int periode;
		public final int noSequence;

		public Traite(long diId, long ctbId, int periode, int noSequence) {
			this.ctbId = ctbId;
			this.diId = diId;
			this.periode = periode;
			this.noSequence = noSequence;
		}
	}

	// paramètre d'entrée
	public final RegDate dateTraitement;
	public final int nbThreads;

	// données de sortie
	public int nbDIsTotal;
	public final List<Traite> traites = new LinkedList<>();
	public final List<Ignoree> ignorees = new LinkedList<>();
	public final List<Erreur> errors = new LinkedList<>();
	public boolean interrompu;

	public RattraperEmissionDIPourCyberContexteResults(RegDate dateTraitement, int nbThreads) {
		super(null, null);
		this.dateTraitement = dateTraitement;
		this.nbThreads = nbThreads;
	}

	public void addDiTrouvee() {
		nbDIsTotal++;
	}

	public void addDiTraitee(Long diId, Long ctbId, Integer periodeFiscale, Integer numeroSequence) {
		traites.add(new Traite(diId, ctbId, periodeFiscale, numeroSequence));
	}

	public void addDiIgnoree(Long idDI, Long ctbId, Integer periodeFiscale, Integer numeroSequence, String message) {
		ignorees.add(new Ignoree(idDI, ctbId, periodeFiscale, numeroSequence, message));
	}

	@Override
	public void addErrorException(Long idDI, Exception e) {
		errors.add(new Erreur(idDI, null, null, null, e.getMessage()));
	}

	public void addErrorTraitement(Long diId, Long ctbId, Integer periodeFiscale, Integer numeroSequence, String message) {
		errors.add(new Erreur(diId, ctbId, periodeFiscale, numeroSequence, message));
	}

	@Override
	public void addAll(RattraperEmissionDIPourCyberContexteResults right) {
		this.nbDIsTotal += right.nbDIsTotal;
		this.traites.addAll(right.traites);
		this.ignorees.addAll(right.ignorees);
		this.errors.addAll(right.errors);
	}
}
