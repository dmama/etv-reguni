package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Iterator;

import ch.vd.uniregctb.migreg.HostMigratorHelper.IndexationMode;

public class MigRegLimitsList implements Iterable<MigRegLimits> {

	private final ArrayList<MigRegLimits> limits = new ArrayList<MigRegLimits>();

	private final String name;
	private IndexationMode indexationMode = IndexationMode.NONE;
//	private int nbThreads = 1;
	private boolean wantTruncate = false;
	private boolean wantTutelles = false;
	private boolean wantMariesSeuls = false;
//	private boolean errorCasesProcessing = false;

	public MigRegLimitsList(String name) {
		this.name = name;
	}

	public void append(MigRegLimits lim) {
		limits.add(lim);
	}
	public void prepend(MigRegLimits lim) {
		limits.add(0, lim);
	}

	public Iterator<MigRegLimits> iterator() {
		return limits.iterator();
	}


	public IndexationMode getIndexationMode() {
		return indexationMode;
	}
	public void setIndexationMode(IndexationMode mode) {
		indexationMode = mode;
	}

	public boolean isWantTruncate() {
		return wantTruncate;
	}
	public void setWantTruncate(boolean wantTruncate) {
		this.wantTruncate = wantTruncate;
	}

	public boolean isWantTutelles() {
		return wantTutelles;
	}
	public void setWantTutelles(boolean wantTutelles) {
		this.wantTutelles = wantTutelles;
	}

//	public boolean isErrorCasesProcessing() {
//		return errorCasesProcessing;
//	}
//
//	public void setErrorCasesProcessing(boolean errorCasesProcessing) {
//		this.errorCasesProcessing = errorCasesProcessing;
//	}

	public String getName() {
		return name;
	}

	/**
	 * On ne peut faire du multi-thread que si on choist pas de Range
	 *
	 * @return
	 */
//	public int getNbThreads() {
//		// Assert.isTrue(nbThreads == 1 || populations == 0);
//		return nbThreads;
//	}
//
//	public void setNbThreads(int n) {
//		nbThreads = n;
//	}

	public boolean isWantMariesSeuls() {
		return wantMariesSeuls;
	}

	public void setWantMariesSeuls(boolean wantMariesSeuls) {
		this.wantMariesSeuls = wantMariesSeuls;
	}

}
