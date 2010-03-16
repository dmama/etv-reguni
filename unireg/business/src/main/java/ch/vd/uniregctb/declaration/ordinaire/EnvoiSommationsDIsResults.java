package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

public class EnvoiSommationsDIsResults extends JobResults {
	
	public static class Info {
		protected static final String COMMA = ";";
		private DeclarationImpotOrdinaire di;
		private Long noTiers;
		private Integer anneePeriode;
		private RegDate diDateDebut;
		private RegDate diDateFin;
		
		private Info(DeclarationImpotOrdinaire di) {
			this.di = di;
			noTiers = di.getTiers().getNumero();
			anneePeriode = di.getPeriode().getAnnee();
			diDateDebut = di.getDateDebut();
			diDateFin = di.getDateFin();
		}

		public DeclarationImpotOrdinaire getDi() {
			return di;
		}

		public Long getNumeroTiers() {
			return noTiers;
		}

		public Integer getAnneePeriode() {
			return anneePeriode;
		}

		public RegDate getDiDateDebut() {
			return diDateDebut;
		}

		public RegDate getDiDateFin() {
			return diDateFin;
		}

		public String getCVSEntete() {
			return "Numéro de contribuable" + COMMA + "Période DI" + COMMA + "Date de début DI" + COMMA + "Date de fin DI" + appendMeEntete();
		}
		
		protected String appendMeEntete() {
			return "";
		}

		public String getCVS() {
			StringBuilder cvs =  new StringBuilder();
			cvs.append(getNumeroTiers()).append(COMMA);
			cvs.append(getAnneePeriode()).append(COMMA);
			cvs.append(RegDateHelper.dateToDisplayString(getDiDateDebut())).append(COMMA);
			cvs.append(RegDateHelper.dateToDisplayString(getDiDateFin()));
			cvs.append(appendMe());
			return cvs.toString();
		}
		
		protected String appendMe() {
			return "";
		}
		
	}
	
	public static class ErrorInfo extends Info {
		private String cause;
		
		private ErrorInfo(DeclarationImpotOrdinaire di, String cause) {
			super(di);
			this.cause = cause;
		}
		
		public String getCause() {
			return cause;
		}
		
		
		@Override
		protected String appendMeEntete() {
			return COMMA + "Cause";
		}
		
		@Override
		protected String appendMe() {
			return COMMA + getCause();
		}

	}
	
	private RegDate dateTraitement;
	private boolean miseSousPliImpossible;
	private boolean interrompu;
	private List<ErrorInfo> sommationsEnErreur = new ArrayList<ErrorInfo>();
	private Map<Integer, List<Info>> sommationsParPeriode = new HashMap<Integer, List<Info>>();
	private List<Info> disContribuablesNonAssujettis = new ArrayList<Info>();
	private List<Info> disContribuablesIndigents = new ArrayList<Info>();

	public void add(EnvoiSommationsDIsResults right) {
		this.sommationsEnErreur.addAll(right.sommationsEnErreur);
		this.disContribuablesNonAssujettis.addAll(right.disContribuablesNonAssujettis);
		this.disContribuablesIndigents.addAll(right.disContribuablesIndigents);
		List<Integer> annees = new ArrayList<Integer>(sommationsParPeriode.keySet());
		for (Integer annee : annees) {
			if (right.sommationsParPeriode.containsKey(annee)) {
				List<Info> list = sommationsParPeriode.get(annee);
				list.addAll(right.sommationsParPeriode.get(annee));
				sommationsParPeriode.put(annee, list);
			}
		}
		annees = new ArrayList<Integer>(right.sommationsParPeriode.keySet());
		for (Integer annee : annees) {
			if (!sommationsParPeriode.containsKey(annee)) {
				sommationsParPeriode.put(annee, right.sommationsParPeriode.get(annee));
			}
		}
	}
	
	public void addError(DeclarationImpotOrdinaire di, String cause ) {
		sommationsEnErreur.add(new ErrorInfo(di, cause));
	}
	
	public void addDiSommee(Integer periode, DeclarationImpotOrdinaire di) {
		List<Info> dis = sommationsParPeriode.get(periode);
		if (dis == null ) {
			dis = new ArrayList<Info>();
			sommationsParPeriode.put(periode, dis);			
		}
		dis.add(new Info(di));
	}
	
	public int getTotalDisSommees() {
		int total = 0;
		for(List<Info> list : sommationsParPeriode.values()) {
			total += list.size();
		}
		return total;
	}
	
	public int getTotalSommations(Integer periode) {
		List<Info> list = sommationsParPeriode.get(periode);
		if (list == null) {
			return 0;
		}
		return list.size();
	}
	
	public List<Integer> getListeAnnees() {
		List<Integer> annees = new ArrayList<Integer>(sommationsParPeriode.keySet());
		Collections.sort(annees);
		return annees;
	}
	
	public int getTotalSommationsEnErreur() {
		return sommationsEnErreur.size();
	}
	
	public List<ErrorInfo> getListeSommationsEnErreur() {
		return Collections.unmodifiableList(sommationsEnErreur);
	}
	
	public int getTotalDisTraitees() {
		return getTotalDisSommees() + getTotalSommationsEnErreur() + getTotalNonAssujettissement();
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public boolean isMiseSousPliImpossible() {
		return miseSousPliImpossible;
	}

	public void setMiseSousPliImpossible(boolean miseSousPliImpossible) {
		this.miseSousPliImpossible = miseSousPliImpossible;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	public void addNonAssujettissement(DeclarationImpotOrdinaire di) {
		disContribuablesNonAssujettis.add(new Info(di));
	}
	
	public int getTotalNonAssujettissement() {
		return disContribuablesNonAssujettis.size();
	}
	
	public List<Info> getListeNonAssujettissement() {
		return Collections.unmodifiableList(disContribuablesNonAssujettis);
	}
	
	public void addIndigent(DeclarationImpotOrdinaire di) {
		disContribuablesIndigents.add(new Info(di));
	}
	
	public int getTotalIndigent() {
		return disContribuablesIndigents.size();
	}

	public List<Info> getListeIndigent() {
		return Collections.unmodifiableList(disContribuablesIndigents);
	}

	public List<Info> getSommations() {
		List<Info> ret = new ArrayList<Info>(getTotalDisSommees());
		for(List<Info> list : sommationsParPeriode.values()) {
			ret.addAll(list);
		}
		return Collections.unmodifiableList(ret);
	}
}
