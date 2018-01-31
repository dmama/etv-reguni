package ch.vd.uniregctb.stats.evenements;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;

public class StatsEvenementsNotairesResults {

	public abstract static class UniteTraitementInfo {

		public final long id;
		public final EtatTraitement etat;
		public final String visaNotaire;
		public final String numeroMinute;
		public final RegDate dateActe;
		public final @Nullable Date dateTraitement;
		public final NomPrenom nomPrenomPartiePrenante1;
		public final @Nullable NomPrenom nomPrenomPartiePrenante2;

		protected UniteTraitementInfo(long id, EtatTraitement etat, String visaNotaire, String numeroMinute, RegDate dateActe, @Nullable Date dateTraitement,
		                              NomPrenom nomPrenomPartiePrenante1, @Nullable NomPrenom nomPrenomPartiePrenante2) {
			this.id = id;
			this.etat = etat;
			this.visaNotaire = visaNotaire;
			this.numeroMinute = numeroMinute;
			this.dateActe = dateActe;
			this.dateTraitement = dateTraitement;
			this.nomPrenomPartiePrenante1 = nomPrenomPartiePrenante1;
			this.nomPrenomPartiePrenante2 = nomPrenomPartiePrenante2;
		}
	}

	public static final class UniteTraitementEnErreurInfo extends UniteTraitementInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "VISA_NOTAIRE", "NUMERO_MINUTE", "DATE_ACTE", "DATE_TRAITEMENT", "ETAT", "PARTIE_PRENANTE_1", "PARTIE_PRENANTE_2", "TYPE_MESSAGE", "MESSAGE" };

		public final ErreurTraitement.TypeErreur typeMessage;
		public final String message;

		public UniteTraitementEnErreurInfo(long id, EtatTraitement etat, String visaNotaire, String numeroMinute, RegDate dateActe, @Nullable Date dateTraitement,
		                                   NomPrenom nomPrenomPartiePrenante1, @Nullable NomPrenom nomPrenomPartiePrenante2, ErreurTraitement.TypeErreur typeMessage, String message) {
			super(id, etat, visaNotaire, numeroMinute, dateActe, dateTraitement, nomPrenomPartiePrenante1, nomPrenomPartiePrenante2);
			this.typeMessage = typeMessage;
			this.message = message;
		}

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), visaNotaire, numeroMinute, RegDateHelper.dateToDashString(dateActe),
					DateHelper.dateTimeToDisplayString(dateTraitement), etat.name(),
					nomPrenomPartiePrenante1 != null ? nomPrenomPartiePrenante1.getNomPrenom() : null,
					nomPrenomPartiePrenante2 != null ? nomPrenomPartiePrenante2.getNomPrenom() : null,
					typeMessage.name(), message
			};
		}
	}

	public static final class UniteTraitementForceesInfo extends UniteTraitementInfo implements StatistiqueEvenementInfo {

		private static final String[] COLONNES = { "ID", "VISA_NOTAIRE", "NUMERO_MINUTE", "DATE_ACTE", "DATE_RECEPTION", "DATE_MODIFICATION", "VISA_FORCAGE", "ETAT", "PARTIE_PRENANTE_1", "PARTIE_PRENANTE_2" };

		public final String visaForcage;
		public final Date dateReception;
		public final Date dateModification;

		public UniteTraitementForceesInfo(long id, EtatTraitement etat, String visaNotaire, String numeroMinute, RegDate dateActe,
		                                  NomPrenom nomPrenomPartiePrenante1, @Nullable NomPrenom nomPrenomPartiePrenante2, String visaForcage, Date dateReception, Date dateModification) {
			super(id, etat, visaNotaire, numeroMinute, dateActe, null, nomPrenomPartiePrenante1, nomPrenomPartiePrenante2);
			this.visaForcage = visaForcage;
			this.dateReception = dateReception;
			this.dateModification = dateModification;
		}

		@Override
		public String[] getNomsColonnes() {
			return COLONNES;
		}

		@Override
		public String[] getValeursColonnes() {
			return new String[] { Long.toString(id), visaNotaire, numeroMinute, RegDateHelper.dateToDashString(dateActe),
					DateHelper.dateToDisplayString(dateReception), DateHelper.dateTimeToDisplayString(dateModification),
					visaForcage, etat.name(),
					nomPrenomPartiePrenante1 != null ? nomPrenomPartiePrenante1.getNomPrenom() : null,
					nomPrenomPartiePrenante2 != null ? nomPrenomPartiePrenante2.getNomPrenom() : null };
		}
	}

	private final Map<EtatTraitement, Integer> etats;
	private final Map<EtatTraitement, Integer> etatsNouveaux;   // <-- sur les événements reçus récemment
	private final List<UniteTraitementEnErreurInfo> toutesErreurs;
	private final List<UniteTraitementForceesInfo> manipulationsManuelles;

	public StatsEvenementsNotairesResults(Map<EtatTraitement, Integer> etats, Map<EtatTraitement, Integer> etatsNouveaux,
	                                      List<UniteTraitementEnErreurInfo> toutesErreurs,
	                                      List<UniteTraitementForceesInfo> manipulationsManuelles) {


		this.etats = etats != null ? Collections.unmodifiableMap(etats) : Collections.emptyMap();
		this.etatsNouveaux = etatsNouveaux != null ? Collections.unmodifiableMap(etatsNouveaux) : Collections.emptyMap();
		this.toutesErreurs = toutesErreurs != null ? Collections.unmodifiableList(toutesErreurs) : Collections.emptyList();
		this.manipulationsManuelles = manipulationsManuelles != null ? Collections.unmodifiableList(manipulationsManuelles) : Collections.emptyList();
	}

	public Map<EtatTraitement, Integer> getEtats() {
		return etats;
	}

	public Map<EtatTraitement, Integer> getEtatsNouveaux() {
		return etatsNouveaux;
	}

	public List<UniteTraitementEnErreurInfo> getToutesErreurs() {
		return toutesErreurs;
	}

	public List<UniteTraitementForceesInfo> getManipulationsManuelles() {
		return manipulationsManuelles;
	}
}
