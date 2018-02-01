package ch.vd.uniregctb.evenement.ide;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.uniregctb.common.AbstractJobResults;

public class AnnonceIDEJobResults extends AbstractJobResults<Long, AnnonceIDEJobResults> {

	public abstract static class Info {
		public final long entrepriseId;
		protected Info(long entrepriseId) {
			this.entrepriseId = entrepriseId;
		}
	}

	public static class AnnonceInfo extends Info {
		public final BaseAnnonceIDE annonceIDE;
		public AnnonceInfo(long entrepriseId, @NotNull BaseAnnonceIDE annonceIDE) {
			super(entrepriseId);
			this.annonceIDE = Objects.requireNonNull(annonceIDE);
		}
	}

	public static class ExceptionInfo extends Info {
		public final String exceptionMsg;
		public ExceptionInfo(long entrepriseId, Exception e) {
			super(entrepriseId);
			this.exceptionMsg = String.format("%s: %s", e.getClass().getName(), e.getMessage());
		}
	}

	private final List<AnnonceInfo> annoncesIDE = new LinkedList<>();
	private final List<ExceptionInfo> exceptions = new LinkedList<>();
	private final boolean simulation;
	private boolean interrupted = false;

	public AnnonceIDEJobResults(boolean simulation) {
		this.simulation = simulation;
	}

	public void addAnnonceIDE(long entrepriseId, @NotNull BaseAnnonceIDE annonceIDE) {
		annoncesIDE.add(new AnnonceInfo(entrepriseId, annonceIDE));
	}

	@Override
	public void addErrorException(Long entrepriseId, Exception e) {
		exceptions.add(new ExceptionInfo(entrepriseId, e));
	}

	@Override
	public void addAll(AnnonceIDEJobResults right) {
		annoncesIDE.addAll(right.annoncesIDE);
		exceptions.addAll(right.exceptions);
	}

	public boolean isSimulation() {
		return simulation;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public List<AnnonceInfo> getAnnoncesIDE() {
		return annoncesIDE;
	}

	public List<ExceptionInfo> getExceptions() {
		return exceptions;
	}
}
