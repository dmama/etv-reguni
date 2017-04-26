package ch.vd.uniregctb.regimefiscal.rattrapage;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalConsolide;

public class RattrapageRegimesFiscauxJobResults extends AbstractJobResults<Long, RattrapageRegimesFiscauxJobResults> {

	public abstract static class Info {
		public final long entrepriseId;
		protected Info(long entrepriseId) {
			this.entrepriseId = entrepriseId;
		}
	}

	public static class RegimeFiscalInfo extends Info {

		public final String derniereRaisonSociale;
		public final FormeLegale formeLegale;
		public final RegDate dateDeCreation;
		public final List<RegimeFiscalConsolide> regimesFiscauxConsolides;
		public RegimeFiscalInfo(long entrepriseId, String derniereRaisonSociale, @NotNull FormeLegale formeLegale, RegDate dateDeCreation, @NotNull List<RegimeFiscalConsolide> regimesFiscauxConsolides) {
			super(entrepriseId);
			this.derniereRaisonSociale = derniereRaisonSociale;
			this.regimesFiscauxConsolides = Objects.requireNonNull(regimesFiscauxConsolides);
			this.formeLegale = formeLegale;
			this.dateDeCreation = dateDeCreation;
		}
	}

	public static class ExceptionInfo extends Info {
		public final String exceptionMsg;
		public ExceptionInfo(long entrepriseId, Exception e) {
			super(entrepriseId);
			this.exceptionMsg = String.format("%s: %s", e.getClass().getName(), e.getMessage());
		}
	}

	private final List<RegimeFiscalInfo> regimeFiscalInfos = new LinkedList<>();
	private final List<ExceptionInfo> exceptions = new LinkedList<>();
	private final boolean simulation;
	private boolean interrupted = false;

	public RattrapageRegimesFiscauxJobResults(boolean simulation) {
		this.simulation = simulation;
	}

	public void addRegimeFiscalInfo(long entrepriseId, String derniereRaisonSociale, @NotNull FormeLegale formeLegale, RegDate dateDeCreation, @NotNull List<RegimeFiscalConsolide> regimesFiscauxConsolides) {
		regimeFiscalInfos.add(new RegimeFiscalInfo(entrepriseId, derniereRaisonSociale, formeLegale, dateDeCreation, regimesFiscauxConsolides));
	}

	@Override
	public void addErrorException(Long entrepriseId, Exception e) {
		exceptions.add(new ExceptionInfo(entrepriseId, e));
	}

	@Override
	public void addAll(RattrapageRegimesFiscauxJobResults right) {
		regimeFiscalInfos.addAll(right.regimeFiscalInfos);
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

	public List<RegimeFiscalInfo> getRegimeFiscalInfos() {
		return regimeFiscalInfos;
	}

	public List<ExceptionInfo> getExceptions() {
		return exceptions;
	}
}
