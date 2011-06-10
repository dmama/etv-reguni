package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;

public class EnvoiSommationsDIsResults extends JobResults<IdentifiantDeclaration, EnvoiSommationsDIsResults> {


	public static class Info {
		protected static final String COMMA = ";";
		private final Long noTiers;
		private final Integer anneePeriode;
		private final RegDate diDateDebut;
		private final RegDate diDateFin;
		
		private Info(DeclarationImpotOrdinaire di) {
			noTiers = di.getTiers().getNumero();
			anneePeriode = di.getPeriode().getAnnee();
			diDateDebut = di.getDateDebut();
			diDateFin = di.getDateFin();
		}

		private Info(Long noTiers) {
			this.noTiers = noTiers;
			anneePeriode = null;
			diDateDebut = null;
			diDateFin = null;
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

		public String getCsvEntete() {
			return "Numéro de contribuable" + COMMA + "Période DI" + COMMA + "Date de début DI" + COMMA + "Date de fin DI" + appendMeEntete();
		}
		
		protected String appendMeEntete() {
			return "";
		}

		public String getCsv() {
			final StringBuilder cvs =  new StringBuilder();
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
		private final String cause;
		
		private ErrorInfo(DeclarationImpotOrdinaire di, String cause) {
			super(di);
			this.cause = cause;
		}

		public ErrorInfo(Long tiersId, String message) {
			super(tiersId);
			this.cause = message;
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
			return String.format("%s\"%s\"", COMMA, StringUtils.isNotBlank(cause) ? cause.replaceAll("[\";]", "_") : StringUtils.EMPTY);
		}
	}
	
	private RegDate dateTraitement;
	private boolean miseSousPliImpossible;
	private boolean interrompu;
	private final List<ErrorInfo> sommationsEnErreur = new ArrayList<ErrorInfo>();
	private final Map<Integer, List<Info>> sommationsParPeriode = new HashMap<Integer, List<Info>>();
	private final List<Info> disContribuablesNonAssujettis = new ArrayList<Info>();
	private final List<Info> disContribuablesSourcierPur = new ArrayList<Info>();
	private final List<Info> disContribuablesIndigents = new ArrayList<Info>();
	private final List<Info> disOptionnelles = new ArrayList<Info>();

	@Override
	public void addAll(EnvoiSommationsDIsResults right) {
		this.sommationsEnErreur.addAll(right.sommationsEnErreur);
		this.disContribuablesNonAssujettis.addAll(right.disContribuablesNonAssujettis);
		this.disContribuablesIndigents.addAll(right.disContribuablesIndigents);
		this.disContribuablesSourcierPur.addAll(right.disContribuablesSourcierPur);
		this.disOptionnelles.addAll(right.disOptionnelles);
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


	@Override
	public void addErrorException(IdentifiantDeclaration element, Exception e) {
		String message = buildErrorMessage(e);
		sommationsEnErreur.add(new ErrorInfo(element.getNumeroTiers(),message ));
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
		return getTotalDisSommees() + getTotalSommationsEnErreur() + getTotalNonAssujettissement() + getTotalIndigent() +getTotalSourcierPur()+ getTotalDisOptionnelles();
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

	public void addSourcierPur(DeclarationImpotOrdinaire di) {
		disContribuablesSourcierPur.add(new Info(di));
	}

	public int getTotalIndigent() {
		return disContribuablesIndigents.size();
	}

	public int getTotalSourcierPur() {
		return disContribuablesSourcierPur.size();
	}

	public List<Info> getListeIndigent() {
		return Collections.unmodifiableList(disContribuablesIndigents);
	}

	public List<Info> getListeSourcierPur() {
		return Collections.unmodifiableList(disContribuablesSourcierPur);
	}

	public void addDiOptionelle(DeclarationImpotOrdinaire di) {
		disOptionnelles.add(new Info(di));
	}

	public int getTotalDisOptionnelles() {
		return disOptionnelles.size();
	}

	public List<Info> getDisOptionnelles() {
		return Collections.unmodifiableList(disOptionnelles);
	}

	public List<Info> getSommations() {
		List<Info> ret = new ArrayList<Info>(getTotalDisSommees());
		for(List<Info> list : sommationsParPeriode.values()) {
			ret.addAll(list);
		}
		return Collections.unmodifiableList(ret);
	}

	protected final String buildErrorMessage(Exception e) {
        final String message;
        if (e.getMessage() != null && e.getMessage().trim().length() > 0) {
            message = String.format("%s - %s", e.getClass().getName(), e.getMessage().trim());
        } else {
            message = String.format("%s - %s", e.getClass().getName(), Arrays.toString(e.getStackTrace()));
        }
        return message;
    }
}
