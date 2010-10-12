package ch.vd.uniregctb.stats.evenements;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class StatsEvenementsCivilsResults {

	public static class EvenementCivilInfo {
		public final long id;
		public final TypeEvenementCivil type;
		public final RegDate dateEvenement;
		public final Date dateTraitement;
		public final EtatEvenementCivil etat;
		public final long individuPrincipal;
		public final Long individuConjoint;
		public final Integer ofsAnnonce;

		public EvenementCivilInfo(long id, TypeEvenementCivil type, RegDate dateEvenement, Date dateTraitement,
		                          EtatEvenementCivil etat, long individuPrincipal, Long individuConjoint,
		                          Integer ofsAnnonce) {
			this.id = id;
			this.type = type;
			this.dateEvenement = dateEvenement;
			this.dateTraitement = dateTraitement;
			this.etat = etat;
			this.individuPrincipal = individuPrincipal;
			this.individuConjoint = individuConjoint;
			this.ofsAnnonce = ofsAnnonce;
		}
	}

	public static final class EvenementCivilEnErreurInfo extends EvenementCivilInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "TYPE", "DATE_EVENEMENT", "DATE_TRAITEMENT", "ETAT", "INVIDIVU_PRINCIPAL", "INDIVIDU_CONJOINT", "OFS_ANNONCE", "ERREUR" };

		public final String message;

		public EvenementCivilEnErreurInfo(long id, TypeEvenementCivil type, RegDate dateEvenement, Date dateTraitement, EtatEvenementCivil etat,
		                                  long individuPrincipal, Long individuConjoint, Integer ofsAnnonce,
		                                  String message) {
			super(id, type, dateEvenement, dateTraitement, etat, individuPrincipal, individuConjoint, ofsAnnonce);
			this.message = message;
		}

		public String[] getNomsColonnes() {
			return COLONNES;
		}

		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), type.getName(), RegDateHelper.dateToDashString(dateEvenement), dateTraitement.toString(), etat.name(),
								  Long.toString(individuPrincipal), individuConjoint != null ? Long.toString(individuConjoint) : null,
								  ofsAnnonce != null ? Integer.toString(ofsAnnonce) : null, message };
		}
	}

	public static final class EvenementCivilTraiteManuellementInfo extends EvenementCivilInfo  implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "TYPE", "DATE_EVENEMENT", "DATE_RECEPTION", "DATE_MODIFICATION", "VISA_OPERATEUR", "ETAT", "INVIDIVU_PRINCIPAL", "INDIVIDU_CONJOINT", "OFS_ANNONCE" };

		public final String visaOperateur;
		public final Date dateReception;

		public EvenementCivilTraiteManuellementInfo(long id, TypeEvenementCivil type, RegDate dateEvenement, EtatEvenementCivil etat,
		                                            long individuPrincipal, Long individuConjoint, Integer ofsAnnonce,
		                                            String visaOperateur, Date dateReception, Date dateModification) {
			super(id, type, dateEvenement, dateModification, etat, individuPrincipal, individuConjoint, ofsAnnonce);
			this.visaOperateur = visaOperateur;
			this.dateReception = dateReception;
		}

		public String[] getNomsColonnes() {
			return COLONNES;
		}

		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), type.getName(), RegDateHelper.dateToDashString(dateEvenement), dateReception.toString(),
								  dateTraitement.toString(), visaOperateur, etat.name(),
								  Long.toString(individuPrincipal), individuConjoint != null ? Long.toString(individuConjoint) : null,
								  ofsAnnonce != null ? Integer.toString(ofsAnnonce) : null };
		}
	}

	private final Map<EtatEvenementCivil, Integer> etats;
	private final Map<EtatEvenementCivil, Integer> etatsNouveaux;
	private final Map<TypeEvenementCivil, Integer> erreursParType;
	private final Map<TypeEvenementCivil, Integer> erreursParTypeNouveaux;
	private final List<EvenementCivilEnErreurInfo> toutesErreurs;
	private final List<EvenementCivilTraiteManuellementInfo> manipulationsManuelles;
	private final Map<Integer, Integer> ignores;

	public StatsEvenementsCivilsResults(Map<EtatEvenementCivil, Integer> etats,
	                                    Map<EtatEvenementCivil, Integer> etatsNouveaux,
	                                    Map<TypeEvenementCivil, Integer> erreursParType,
	                                    Map<TypeEvenementCivil, Integer> erreursParTypeNouveaux,
	                                    List<EvenementCivilEnErreurInfo> toutesErreurs,
	                                    List<EvenementCivilTraiteManuellementInfo> manipulationsManuelles,
	                                    Map<Integer, Integer> ignores) {
		this.etats = etats != null ? Collections.unmodifiableMap(etats) : Collections.<EtatEvenementCivil, Integer>emptyMap();
		this.etatsNouveaux = etatsNouveaux != null ? Collections.unmodifiableMap(etatsNouveaux) : Collections.<EtatEvenementCivil, Integer>emptyMap();
		this.erreursParType = erreursParType != null ? Collections.unmodifiableMap(erreursParType) : Collections.<TypeEvenementCivil, Integer>emptyMap();
		this.erreursParTypeNouveaux = erreursParTypeNouveaux != null ? Collections.unmodifiableMap(erreursParTypeNouveaux) : Collections.<TypeEvenementCivil, Integer>emptyMap();
		this.toutesErreurs = toutesErreurs != null ? Collections.unmodifiableList(toutesErreurs) : Collections.<EvenementCivilEnErreurInfo>emptyList();
		this.manipulationsManuelles = manipulationsManuelles != null ? Collections.unmodifiableList(manipulationsManuelles) : Collections.<EvenementCivilTraiteManuellementInfo>emptyList();
		this.ignores = ignores != null ? Collections.unmodifiableMap(ignores) : Collections.<Integer, Integer>emptyMap();
	}

	public Map<EtatEvenementCivil, Integer> getEtats() {
		return etats;
	}

	public Map<EtatEvenementCivil, Integer> getEtatsNouveaux() {
		return etatsNouveaux;
	}

	public Map<TypeEvenementCivil, Integer> getErreursParType() {
		return erreursParType;
	}

	public Map<TypeEvenementCivil, Integer> getErreursParTypeNouveaux() {
		return erreursParTypeNouveaux;
	}

	public List<EvenementCivilEnErreurInfo> getToutesErreurs() {
		return toutesErreurs;
	}

	public List<EvenementCivilTraiteManuellementInfo> getManipulationsManuelles() {
		return manipulationsManuelles;
	}

	public Map<Integer, Integer> getIgnores() {
		return ignores;
	}
}
