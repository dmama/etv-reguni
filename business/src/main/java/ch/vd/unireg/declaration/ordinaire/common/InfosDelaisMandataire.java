package ch.vd.unireg.declaration.ordinaire.common;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;

public class InfosDelaisMandataire {
	private Long numeroTiers;
	private int periodeFiscale;
	private StatutDemandeType statut;
	private String ideMandataire;
	private String raisonSocialeMandataire;
	private String identifiantDemandeMandataire;
	private RegDate dateSoumission;

	enum StatutDemandeType {
		ACCEPTE("Accepté"),
		REFUSE("Refusé");

		private String libelle;

		StatutDemandeType(String libelle) {
			this.libelle = libelle;
		}

		public String getLibelle() {
			return libelle;
		}

		private static final Map<String, StatutDemandeType> byLibelle = buildByLibelleMap();

		private static Map<String, StatutDemandeType> buildByLibelleMap() {
			return Stream.of(StatutDemandeType.values())
					.collect(Collectors.toMap(StatutDemandeType::getLibelle, Function.identity()));        // ça pête si le même code est utilisé plusieurs fois...
		}

		public static StatutDemandeType valueOfLibelle(String libelle) {
			return byLibelle.get(libelle);
		}

	}

	public InfosDelaisMandataire(long numeroTiers, int periodeFiscale, StatutDemandeType statut, String ideMandataire, String raisonSocialeMandataire, String identifiantDemandeMandataire,
	                             RegDate dateSoumission) {
		this.numeroTiers = numeroTiers;
		this.periodeFiscale = periodeFiscale;
		this.statut = statut;
		this.ideMandataire = ideMandataire;
		this.raisonSocialeMandataire = raisonSocialeMandataire;
		this.identifiantDemandeMandataire = identifiantDemandeMandataire;
		this.dateSoumission = dateSoumission;
	}

	public Long getNumeroTiers() {
		return numeroTiers;
	}

	public void setNumeroTiers(long numeroTiers) {
		this.numeroTiers = numeroTiers;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public StatutDemandeType getStatut() {
		return statut;
	}

	public void setStatut(StatutDemandeType statut) {
		this.statut = statut;
	}

	public String getIdeMandataire() {
		return ideMandataire;
	}

	public void setIdeMandataire(String ideMandataire) {
		this.ideMandataire = ideMandataire;
	}

	public String getRaisonSocialeMandataire() {
		return raisonSocialeMandataire;
	}

	public void setRaisonSocialeMandataire(String raisonSocialeMandataire) {
		this.raisonSocialeMandataire = raisonSocialeMandataire;
	}

	public String getIdentifiantDemandeMandataire() {
		return identifiantDemandeMandataire;
	}

	public void setIdentifiantDemandeMandataire(String identifiantDemandeMandataire) {
		this.identifiantDemandeMandataire = identifiantDemandeMandataire;
	}

	public RegDate getDateSoumission() {
		return dateSoumission;
	}

	public void setDateSoumission(RegDate dateSoumission) {
		this.dateSoumission = dateSoumission;
	}


	@Override
	public String toString() {
		return "InfosDelaisMandataire{" +
				"numeroTiers=" + numeroTiers +
				", periodeFiscale=" + periodeFiscale +
				", statut=" + statut +
				", ideMandataire='" + ideMandataire + '\'' +
				", raisonSocialeMandataire='" + raisonSocialeMandataire + '\'' +
				", identifiantDemandeMandataire='" + identifiantDemandeMandataire + '\'' +
				", dateSoumission=" + dateSoumission +
				'}';
	}
}

