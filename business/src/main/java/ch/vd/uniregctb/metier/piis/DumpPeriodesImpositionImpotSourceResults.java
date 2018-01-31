package ch.vd.uniregctb.metier.piis;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DumpPeriodesImpositionImpotSourceResults extends AbstractJobResults<Long, DumpPeriodesImpositionImpotSourceResults> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DumpPeriodesImpositionImpotSourceResults.class);

	public static class Info implements Comparable<Info>, DateRange {

		public final long noCtb;
		public final PeriodeImpositionImpotSource.Type type;
		public final RegDate dateDebut;
		public final RegDate dateFin;
		public final Localisation localisation;
		@Nullable
		public final TypeAutoriteFiscale typeAutoriteFiscale;
		@Nullable
		public final Integer noOfs;

		public Info(PeriodeImpositionImpotSource piis) {
			this.noCtb = piis.getContribuable().getNumero();
			this.type = piis.getType();
			this.dateDebut = piis.getDateDebut();
			this.dateFin = piis.getDateFin();
			this.localisation = piis.getLocalisation();
			this.typeAutoriteFiscale = piis.getTypeAutoriteFiscale();
			this.noOfs = piis.getNoOfs();
		}

		@Override
		public int compareTo(Info o) {
			int comparison = Long.compare(noCtb, o.noCtb);
			if (comparison == 0) {
				comparison = DateRangeComparator.compareRanges(this, o);
			}
			return comparison;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}
	}

	public static final class Error implements Comparable<Error> {
		public final long noCtb;
		public final String message;

		public Error(long noCtb, Exception e) {
			this.noCtb = noCtb;
			this.message = buildErrorMessage(e);
		}

		@Override
		public int compareTo(Error o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	public enum CauseIgnorance {
		ANNULE("La personne physique est annulée"),
		AUCUNE_PIIS("Aucune période d'imposition IS calculée");

		public final String msg;

		CauseIgnorance(String msg) {
			this.msg = msg;
		}
	}

	public static final class Ignore implements Comparable<Ignore> {
		public final long noCtb;
		public final CauseIgnorance cause;

		public Ignore(long noCtb, CauseIgnorance cause) {
			this.noCtb = noCtb;
			this.cause = cause;
		}

		@Override
		public int compareTo(Ignore o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}

	private static String buildErrorMessage(Exception e) {
		if (StringUtils.isNotBlank(e.getMessage())) {
			return e.getMessage();
		}
		else {
			return e.getClass().getName();
		}
	}

	private final int nbThreads;
	private final List<Info> infos = new LinkedList<>();
	private final List<Error> errors = new LinkedList<>();
	private final List<Ignore> ignores = new LinkedList<>();
	private boolean interrupted = false;
	private int nbPersonnesPhysiquesAnalysees = 0;

	public DumpPeriodesImpositionImpotSourceResults(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	public void addIgnore(long noCtb, CauseIgnorance cause) {
		ignores.add(new Ignore(noCtb, cause));
		++ nbPersonnesPhysiquesAnalysees;
	}

	public void addPeriodes(long noCtb, List<PeriodeImpositionImpotSource> piis) {
		if (piis.isEmpty()) {
			addIgnore(noCtb, CauseIgnorance.AUCUNE_PIIS);
		}
		else {
			for (PeriodeImpositionImpotSource pi : piis) {
				infos.add(new Info(pi));
			}
			++ nbPersonnesPhysiquesAnalysees;
		}
	}

	@Override
	public void addErrorException(Long aLong, Exception e) {
		LOGGER.error("Erreur levée sur le contribuable " + aLong, e);
		errors.add(new Error(aLong, e));
		++ nbPersonnesPhysiquesAnalysees;
	}

	@Override
	public void addAll(DumpPeriodesImpositionImpotSourceResults other) {
		infos.addAll(other.infos);
		errors.addAll(other.errors);
		ignores.addAll(other.ignores);
		nbPersonnesPhysiquesAnalysees += other.nbPersonnesPhysiquesAnalysees;
	}

	@Override
	public void end() {
		Collections.sort(infos);
		Collections.sort(errors);
		Collections.sort(ignores);
		super.end();
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public List<Info> getInfos() {
		return infos;
	}

	public List<Error> getErrors() {
		return errors;
	}

	public List<Ignore> getIgnores() {
		return ignores;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public int getNbPersonnesPhysiquesAnalysees() {
		return nbPersonnesPhysiquesAnalysees;
	}
}
