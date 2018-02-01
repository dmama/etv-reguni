package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.tiers.TiersService;

public class EnvoiSommationsDIsPPResults extends JobResults<IdentifiantDeclaration, EnvoiSommationsDIsPPResults> {

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

	public static class DelaiEffectifNonEchuInfo extends Info {

		private final RegDate delaiEffectif;

		public DelaiEffectifNonEchuInfo(DeclarationImpotOrdinaire di, RegDate delaiEffectif) {
			super(di);
			this.delaiEffectif = delaiEffectif;
		}

		@Override
		protected String appendMeEntete() {
			return COMMA + "Délai effectif";
		}

		@Override
		protected String appendMe() {
			return String.format("%s%s", COMMA, RegDateHelper.dateToDisplayString(delaiEffectif));
		}
	}
	
	private RegDate dateTraitement;
	private boolean miseSousPliImpossible;
	private int nombreMaxSommations;
	private boolean interrompu;
	private final List<ErrorInfo> sommationsEnErreur = new LinkedList<>();
	private final Map<Integer, List<Info>> sommationsParPeriode = new HashMap<>();
	private final List<Info> disContribuablesNonAssujettis = new LinkedList<>();
	private final List<Info> disContribuablesSourcierPur = new LinkedList<>();
	private final List<Info> disContribuablesIndigents = new LinkedList<>();
	private final List<Info> disOptionnelles = new LinkedList<>();
	private final List<DelaiEffectifNonEchuInfo> disDelaiEffectifNonEchu = new LinkedList<>();

	public EnvoiSommationsDIsPPResults(TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
	}

	@Override
	public void addAll(EnvoiSommationsDIsPPResults right) {
		this.sommationsEnErreur.addAll(right.sommationsEnErreur);
		this.disContribuablesNonAssujettis.addAll(right.disContribuablesNonAssujettis);
		this.disContribuablesIndigents.addAll(right.disContribuablesIndigents);
		this.disContribuablesSourcierPur.addAll(right.disContribuablesSourcierPur);
		this.disOptionnelles.addAll(right.disOptionnelles);
		this.disDelaiEffectifNonEchu.addAll(right.disDelaiEffectifNonEchu);
		List<Integer> annees = new ArrayList<>(sommationsParPeriode.keySet());
		for (Integer annee : annees) {
			if (right.sommationsParPeriode.containsKey(annee)) {
				List<Info> list = sommationsParPeriode.get(annee);
				list.addAll(right.sommationsParPeriode.get(annee));
				sommationsParPeriode.put(annee, list);
			}
		}
		annees = new ArrayList<>(right.sommationsParPeriode.keySet());
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
		final String message = buildErrorMessage(e);
		sommationsEnErreur.add(new ErrorInfo(element.getNumeroTiers(), message));
	}

	public void addDelaiEffectifNonEchu(DeclarationImpotOrdinaire di, RegDate delaiEffectif) {
		disDelaiEffectifNonEchu.add(new DelaiEffectifNonEchuInfo(di, delaiEffectif));
	}

	public int getTotalDelaisEffectifsNonEchus() {
		return disDelaiEffectifNonEchu.size();
	}

	public List<DelaiEffectifNonEchuInfo> getListeDisDelaiEffectifNonEchu() {
		return Collections.unmodifiableList(disDelaiEffectifNonEchu);
	}

	public void addDiSommee(Integer periode, DeclarationImpotOrdinaire di) {
		final List<Info> dis = sommationsParPeriode.computeIfAbsent(periode, k -> new LinkedList<>());
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
		List<Integer> annees = new ArrayList<>(sommationsParPeriode.keySet());
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
		return getTotalDisSommees()
				+ getTotalSommationsEnErreur()
				+ getTotalDelaisEffectifsNonEchus()
				+ getTotalNonAssujettissement()
				+ getTotalIndigent()
				+ getTotalSourcierPur()
				+ getTotalDisOptionnelles();
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

	public int getNombreMaxSommations() {
		return nombreMaxSommations;
	}

	public void setNombreMaxSommations(int nombreMaxSommations) {
		this.nombreMaxSommations = nombreMaxSommations;
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
		List<Info> ret = new ArrayList<>(getTotalDisSommees());
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
