package ch.vd.uniregctb.stats.evenements;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class StatsEvenementsCivilsEchResults {
	
	public static abstract class EvenementCivilInfo {

		public final long id;
		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;
		public final RegDate dateEvenement;
		public final Date dateTraitement;
		public final EtatEvenementCivil etat;
		public final Long noIndividu;       // nullable, dans le cas où l'erreur est justement que l'on a pas trouvé l'individu

		public EvenementCivilInfo(long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate dateEvenement, Date dateTraitement, EtatEvenementCivil etat, Long noIndividu) {
			this.id = id;
			this.type = type;
			this.action = action;
			this.dateEvenement = dateEvenement;
			this.dateTraitement = dateTraitement;
			this.etat = etat;
			this.noIndividu = noIndividu;
		}
	}
	
	public static final class EvenementCivilEnErreurInfo extends EvenementCivilInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "TYPE", "ACTION", "DATE_EVENEMENT", "DATE_TRAITEMENT", "ETAT", "INVIDIVU", "ERREUR" };

		public final String message;

		public EvenementCivilEnErreurInfo(long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate dateEvenement,
		                                  Date dateTraitement, EtatEvenementCivil etat, Long noIndividu, String message) {
			super(id, type, action, dateEvenement, dateTraitement, etat, noIndividu);
			this.message = message;
		}

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), type.name(), action.name(), RegDateHelper.dateToDashString(dateEvenement),
								  dateTraitement.toString(), etat.name(), noIndividu != null ? Long.toString(noIndividu) : null, message };
		}
	}
	
	public static final class EvenementCivilTraiteManuellementInfo extends EvenementCivilInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "TYPE", "ACTION", "DATE_EVENEMENT", "DATE_RECEPTION", "DATE_MODIFICATION", "VISA_OPERATEUR", "ETAT", "INVIDIVU" };

		public final String visaOperateur;
		public final Date dateReception;

		public EvenementCivilTraiteManuellementInfo(long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate dateEvenement, EtatEvenementCivil etat,
		                                            Long noIndividu, String visaOperateur, Date dateReception, Date dateModification) {
			super(id, type, action, dateEvenement, dateModification, etat, noIndividu);
			this.visaOperateur = visaOperateur;
			this.dateReception = dateReception;
		}

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), type.name(), action.name(), RegDateHelper.dateToDashString(dateEvenement), dateReception.toString(),
								  dateTraitement.toString(), visaOperateur, etat.name(), noIndividu != null ? Long.toString(noIndividu) : null };
		}
	}
	
	public static final class QueueAttenteInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "NO_INDIVIDU", "PLUS_ANCIEN", "PLUS_RECENT", "TAILLE_QUEUE" };
		
		private final long noIndividu;
		private final RegDate dateEvtPlusAncien;
		private final RegDate dateEvtPlusRecent;
		private final int tailleQueue;

		public QueueAttenteInfo(long noIndividu, RegDate dateEvtPlusAncien, RegDate dateEvtPlusRecent, int tailleQueue) {
			this.noIndividu = noIndividu;
			this.dateEvtPlusAncien = dateEvtPlusAncien;
			this.dateEvtPlusRecent = dateEvtPlusRecent;
			this.tailleQueue = tailleQueue;
		}

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] { Long.toString(noIndividu), RegDateHelper.dateToDashString(dateEvtPlusAncien), RegDateHelper.dateToDashString(dateEvtPlusRecent), Integer.toString(tailleQueue) };
		}
	}
	
	private final Map<EtatEvenementCivil, Integer> etats;
	private final Map<EtatEvenementCivil, Integer> etatsNouveaux;
	private final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParType;
	private final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParTypeNouveaux;
	private final List<EvenementCivilEnErreurInfo> toutesErreurs;
	private final List<EvenementCivilTraiteManuellementInfo> manipulationsManuelles;
	private final List<QueueAttenteInfo> queuesAttente;

	public StatsEvenementsCivilsEchResults(Map<EtatEvenementCivil, Integer> etats, Map<EtatEvenementCivil, Integer> etatsNouveaux,
	                                       Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParType,
	                                       Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> erreursParTypeNouveaux, List<EvenementCivilEnErreurInfo> toutesErreurs,
	                                       List<EvenementCivilTraiteManuellementInfo> manipulationsManuelles, List<QueueAttenteInfo> queuesAttente) {
		this.etats = etats != null ? Collections.unmodifiableMap(etats) : Collections.<EtatEvenementCivil, Integer>emptyMap();
		this.etatsNouveaux = etatsNouveaux != null ? Collections.unmodifiableMap(etatsNouveaux) : Collections.<EtatEvenementCivil, Integer>emptyMap();
		this.erreursParType = erreursParType != null ? Collections.unmodifiableMap(erreursParType) : Collections.<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer>emptyMap();
		this.erreursParTypeNouveaux = erreursParTypeNouveaux != null ? Collections.unmodifiableMap(erreursParTypeNouveaux) : Collections.<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer>emptyMap();
		this.toutesErreurs = toutesErreurs != null ? Collections.unmodifiableList(toutesErreurs) : Collections.<EvenementCivilEnErreurInfo>emptyList();
		this.manipulationsManuelles = manipulationsManuelles != null ? Collections.unmodifiableList(manipulationsManuelles) : Collections.<EvenementCivilTraiteManuellementInfo>emptyList();
		this.queuesAttente = queuesAttente != null ? Collections.unmodifiableList(queuesAttente) : Collections.<QueueAttenteInfo>emptyList();
	}

	public Map<EtatEvenementCivil, Integer> getEtats() {
		return etats;
	}

	public Map<EtatEvenementCivil, Integer> getEtatsNouveaux() {
		return etatsNouveaux;
	}

	public Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> getErreursParType() {
		return erreursParType;
	}

	public Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> getErreursParTypeNouveaux() {
		return erreursParTypeNouveaux;
	}

	public List<EvenementCivilEnErreurInfo> getToutesErreurs() {
		return toutesErreurs;
	}

	public List<EvenementCivilTraiteManuellementInfo> getManipulationsManuelles() {
		return manipulationsManuelles;
	}

	public List<QueueAttenteInfo> getQueuesAttente() {
		return queuesAttente;
	}
}
