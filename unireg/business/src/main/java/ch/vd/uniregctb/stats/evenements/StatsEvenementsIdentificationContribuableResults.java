package ch.vd.uniregctb.stats.evenements;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresAdresse;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.type.Sexe;

public class StatsEvenementsIdentificationContribuableResults {

	public static class EvenementInfo implements StatistiqueEvenementInfo {

		private static final String[] NOMS_COLONNES = { "DATE_DEMANDE", "EMETTEUR_ID", "ETAT", "MESSAGE_ID", "PERIODE_FISCALE", "TYPE_MESSAGE", "BUSINESS_ID", "NB_CTB_TROUVES", "NAVS11", "NAVS13", "ADR_CH_COMPL", "ADR_CODE_PAYS", "ADR_LIEU",
													   "ADR_LIGNE_1", "ADR_LIGNE_2", "ADR_LOCALITE", "ADR_NO_APPART", "ADR_ORDRE_POSTE", "ADR_NO_POLICE", "ADR_NPA_ETRANGER", "ADR_NPA_SUISSE", "ADR_NO_CP", "ADR_RUE", "ADR_TEXT_CP",
													   "ADR_TYPE", "DATE_NAISSANCE", "NOM", "PRENOMS", "SEXE" };

		private static final String EMPTY_STRING = "";

		private final Date dateDemande;
		private final String emetteurId;
		private final IdentificationContribuable.Etat etat;
		private final String messageId;
		private final Integer periodeFiscale;
		private final String typeMessage;
		private final String businessId;
		private final Integer nbContribuablesTrouves;
		private final String navs11;
		private final String navs13;
		private final String adresseChiffreComplementaire;
		private final String adresseCodePays;
		private final String adresseLieu;
		private final String adresseLigne1;
		private final String adresseLigne2;
		private final String adresseLocalite;
		private final String adresseNumeroAppartement;
		private final Integer adresseNumeroOrdrePoste;
		private final String adresseNumeroPolice;
		private final String adresseNpaEtranger;
		private final Integer adresseNpaSuisse;
		private final Integer adresseNumeroCasePostale;
		private final String adresseRue;
		private final String adresseTexteCasePostale;
		private final CriteresAdresse.TypeAdresse adresseType;
		private final RegDate dateNaissance;
		private final String nom;
		private final String prenoms;
		private final Sexe sexe;

		public EvenementInfo(Date dateDemande, String emetteurId, IdentificationContribuable.Etat etat, String messageId, Integer periodeFiscale, String typeMessage, String businessId,
		                     Integer nbContribuablesTrouves, String navs11, String navs13, String adresseChiffreComplementaire, String adresseCodePays, String adresseLieu, String adresseLigne1,
		                     String adresseLigne2, String adresseLocalite, String adresseNumeroAppartement, Integer adresseNumeroOrdrePoste, String adresseNumeroPolice, String adresseNpaEtranger,
		                     Integer adresseNpaSuisse, Integer adresseNumeroCasePostale, String adresseRue, String adresseTexteCasePostale, CriteresAdresse.TypeAdresse adresseType,
		                     RegDate dateNaissance,
		                     String nom, String prenoms, Sexe sexe) {
			this.dateDemande = dateDemande;
			this.emetteurId = emetteurId;
			this.etat = etat;
			this.messageId = messageId;
			this.periodeFiscale = periodeFiscale;
			this.typeMessage = typeMessage;
			this.businessId = businessId;
			this.nbContribuablesTrouves = nbContribuablesTrouves;
			this.navs11 = navs11;
			this.navs13 = navs13;
			this.adresseChiffreComplementaire = adresseChiffreComplementaire;
			this.adresseCodePays = adresseCodePays;
			this.adresseLieu = adresseLieu;
			this.adresseLigne1 = adresseLigne1;
			this.adresseLigne2 = adresseLigne2;
			this.adresseLocalite = adresseLocalite;
			this.adresseNumeroAppartement = adresseNumeroAppartement;
			this.adresseNumeroOrdrePoste = adresseNumeroOrdrePoste;
			this.adresseNumeroPolice = adresseNumeroPolice;
			this.adresseNpaEtranger = adresseNpaEtranger;
			this.adresseNpaSuisse = adresseNpaSuisse;
			this.adresseNumeroCasePostale = adresseNumeroCasePostale;
			this.adresseRue = adresseRue;
			this.adresseTexteCasePostale = adresseTexteCasePostale;
			this.adresseType = adresseType;
			this.dateNaissance = dateNaissance;
			this.nom = nom;
			this.prenoms = prenoms;
			this.sexe = sexe;
		}

		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		public String[] getValeursColonnes() {
			final String dateDemandeStr = getDatePresentation(dateDemande);
			final String etatStr = getStringValue(etat);
			final String pfStr = getStringValue(periodeFiscale);
			final String nbCtbTrouvesStr = getStringValue(nbContribuablesTrouves);
			final String adresseNumeroOrdrePosteStr = getStringValue(adresseNumeroOrdrePoste);
			final String adresseNpaSuisseStr = getStringValue(adresseNpaSuisse);
			final String adresseNumeroCasePostaleStr = getStringValue(adresseNumeroCasePostale);
			final String adresseTypeStr = getStringValue(adresseType);
			final String dateNaissanceStr = getStringValue(dateNaissance);
			final String sexeStr = getStringValue(sexe);

			return new String[] { dateDemandeStr, emetteurId, etatStr, messageId, pfStr, typeMessage, businessId, nbCtbTrouvesStr, navs11, navs13, adresseChiffreComplementaire, adresseCodePays, adresseLieu,
				   adresseLigne1, adresseLigne2, adresseLocalite, adresseNumeroAppartement, adresseNumeroOrdrePosteStr, adresseNumeroPolice, adresseNpaEtranger, adresseNpaSuisseStr, adresseNumeroCasePostaleStr, adresseRue, adresseTexteCasePostale,
				   adresseTypeStr, dateNaissanceStr, nom, prenoms, sexeStr } ;
		}

		private static String getDatePresentation(Date date) {
			if (date != null) {
				final Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);

				return String.format("%d-%02d-%02d %02d:%02d:%02d.%03d",
						calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
						calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));
			}
			else {
				return EMPTY_STRING;
			}
		}

		private static <T extends Enum<T>> String getStringValue(T modalite) {
			if (modalite == null) {
				return null;
			}
			else {
				return modalite.name();
			}
		}

		private static String getStringValue(RegDate date) {
			if (date == null) {
				return null;
			}
			else {
				return Integer.toString(date.index());
			}
		}

		private static String getStringValue(Object o) {
			if (o == null) {
				return null;
			}
			else {
				return o.toString();
			}
		}
	}

	private final Map<IdentificationContribuable.Etat, BigDecimal> etats;
	private final Map<IdentificationContribuable.Etat, BigDecimal> etatsNouveaux;
	private final List<EvenementInfo> aTraiter;

	public StatsEvenementsIdentificationContribuableResults(Map<IdentificationContribuable.Etat, BigDecimal> etats,
	                                                        Map<IdentificationContribuable.Etat, BigDecimal> etatsNouveaux,
	                                                        List<EvenementInfo> aTraiter) {
		this.etats = etats != null ? Collections.unmodifiableMap(etats) : Collections.<IdentificationContribuable.Etat, BigDecimal>emptyMap();
		this.etatsNouveaux = etatsNouveaux != null ? Collections.unmodifiableMap(etatsNouveaux) : Collections.<IdentificationContribuable.Etat, BigDecimal>emptyMap();
		this.aTraiter = aTraiter != null ? Collections.unmodifiableList(aTraiter) : Collections.<EvenementInfo>emptyList();
	}

	public Map<IdentificationContribuable.Etat, BigDecimal> getEtats() {
		return etats;
	}

	public Map<IdentificationContribuable.Etat, BigDecimal> getEtatsNouveaux() {
		return etatsNouveaux;
	}

	public List<EvenementInfo> getATraiter() {
		return aTraiter;
	}
}
