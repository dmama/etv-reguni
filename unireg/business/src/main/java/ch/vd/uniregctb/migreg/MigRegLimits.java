package ch.vd.uniregctb.migreg;



public class MigRegLimits {

	private boolean wantCouple = false;
	private int noPassSrc = 1;
	private boolean errorCaseProcessing = false;

	public static final int MIGRE_ALL = 1 << 1;
	public static final int MIGRE_ORDINAIRE = 1 << 2;
	public static final int MIGRE_SOURCIERS = 1 << 3;
	public static final int MIGRE_DEBITEURS = 1 << 4;
	public static final int MIGRE_DROITS_ACCES = 1 << 5;
	public static final int SOURCIERS_BADAVS = 1 << 6;

	/**
	 * Les populations a migrer (ORDINAIRE, SOURCIER, DEBITEUR)
	 */
	public int populations = 0;

	public Integer ctbFirst;
	public Integer ctbEnd;
	public Integer srcFirst;
	public Integer srcEnd;
	public Integer srcFirstBadAvs;
	public Integer srcEndBadAvs;
	public Integer empFirst;
	public Integer empEnd;

	public Integer indiDroitAccesFirst;
	public Integer indiDroitAccesEnd;

	public void copyFrom(MigRegLimits other) {

		wantCouple = other.wantCouple;

		populations = other.populations;

		ctbFirst = other.ctbFirst;
		ctbEnd = other.ctbEnd;
		srcFirst = other.srcFirst;
		srcEnd = other.srcEnd;
		empFirst = other.empFirst;
		empEnd = other.empEnd;

		indiDroitAccesFirst = other.indiDroitAccesFirst;
		indiDroitAccesEnd = other.indiDroitAccesEnd;
		srcFirstBadAvs = other.srcFirstBadAvs;
		srcEndBadAvs =  other.srcEndBadAvs;
	}

	public boolean needPopulation(int flag) {
		return ((populations & MIGRE_ALL) == MIGRE_ALL) || ((populations & flag) == flag);
	}

	public void setOrdinaire(Integer first, Integer last) {
		if (first != null && last != null) {
			ctbFirst = first;
			ctbEnd = last;
			populations |= MIGRE_ORDINAIRE;
		}
	}

	public void setSourciers(Integer first, Integer last) {
		if (first != null && last != null) {
			srcFirst = first;
			srcEnd = last;
			populations |= MIGRE_SOURCIERS;
		}
	}

	public void setSourciersBadAvs(Integer first, Integer last) {
		if (first != null && last != null) {
			srcFirstBadAvs = first;
			srcEndBadAvs = last;
			populations |= SOURCIERS_BADAVS;
		}
	}

	public void setDebiteurs(Integer first, Integer last) {
		if (first != null && last != null) {
			empFirst = first;
			empEnd = last;
			populations |= MIGRE_DEBITEURS;
		}
	}

	public void setDroitAcces(Integer first, Integer last) {
		if (first != null && last != null) {
			indiDroitAccesFirst = first;
			indiDroitAccesEnd = last;
			populations |= MIGRE_DROITS_ACCES;
		}
	}

	@Override
	public String toString() {

		String pop = "(";
		if ((populations | MIGRE_ORDINAIRE) == MIGRE_ORDINAIRE) {
			pop += "ORD";
		}
		if ((populations | MIGRE_SOURCIERS) == MIGRE_SOURCIERS) {
			if (!pop.equals("(")) {
				pop += "|";
			}
			pop += "SRC";
		}

		if ((populations | SOURCIERS_BADAVS) == SOURCIERS_BADAVS) {
			if (!pop.equals("(")) {
				pop += "|";
			}
			pop += "BADAVS";
		}
		if ((populations | MIGRE_DEBITEURS) == MIGRE_DEBITEURS) {
			if (!pop.equals("(")) {
				pop += "|";
			}
			pop += "EMP";
		}
		if ((populations | MIGRE_DROITS_ACCES) == MIGRE_DROITS_ACCES) {
			if (!pop.equals("(")) {
				pop += "|";
			}
			pop += "DROITS";
		}
		pop += ")";

		String str = "";
		str += "Populations: " + pop;
		str += " C:" + wantCouple;
		str += " CTB(" + ctbFirst + ":" + ctbEnd + ") ";
		str += " SRC(" + srcFirst + ":" + srcEnd + ") ";
		str += " BADAVS(" + srcFirstBadAvs + ":" + srcEndBadAvs + ") ";
		str += " EMP(" + empFirst + ":" + empEnd + ") ";
		str += " DROITS(" + indiDroitAccesFirst + ":" + indiDroitAccesEnd + ") ";
		return str;
	}

	public boolean isWantCouple() {
		return wantCouple;
	}

	public void setWantCouple(boolean wantCouple) {
		this.wantCouple = wantCouple;
	}

	public int getNoPassSrc() {
		return noPassSrc;
	}

	public void setNoPassSrc(int noPassSrc) {
		this.noPassSrc = noPassSrc;
	}

	public boolean isErrorCaseProcessing() {
		return errorCaseProcessing;
	}

	public void setErrorCaseProcessing(boolean errorCaseProcessing) {
		this.errorCaseProcessing = errorCaseProcessing;
	}

}
