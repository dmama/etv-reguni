package ch.vd.uniregctb.tiers.rattrapage.appariement.sifisc24852;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.rcent.unireg.unpairingree.OrganisationLocation;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.common.AbstractJobResults;

/**
 * @author Raphaël Marmier, 2017-06-06, <raphael.marmier@vd.ch>
 */
public class DesappariementREEResults extends AbstractJobResults<OrganisationLocation, DesappariementREEResults> {

	// La date du chargement des données et jour d'appariement
	private final RegDate dateCharchementInitial;
	// La date de valeur des données civiles du fichier.
	private final RegDate dateValeurDonneesCiviles;

	private List<OrganisationLocation> etablissementsADesapparier = new ArrayList<>();

	private final List<Desappariement> desappariements = new ArrayList<>();

	private final List<ExceptionInfo> exceptions = new ArrayList<>();

	private final boolean simulation;
	private boolean interrompu = false;

	public void addDesappariement (Desappariement desappariement) {
		this.desappariements.add(desappariement);
	}

	public List<Desappariement> getDesappariements() {
		return desappariements;
	}

	/**
	 *
	 * @param etablissementsADesapparier Copie des données en entrée du job: les établissements à désapparier.
	 * @param dateCharchementInitial Copie des données en entrée du job: la date de chargement initial des établissements à désapparier.
	 * @param dateValeurDonneesCiviles Copie des données en entrée du job: la date de valeur des données civiles fournies avec les établissements à désapparier.
	 * @param simulation Mode simulation, activé (<code>true</code>) ou non (<code>false</code>)
	 */
	public DesappariementREEResults(@Nullable List<OrganisationLocation> etablissementsADesapparier, RegDate dateCharchementInitial, RegDate dateValeurDonneesCiviles, boolean simulation) {
		this.etablissementsADesapparier = etablissementsADesapparier;
		this.simulation = simulation;
		this.dateCharchementInitial = dateCharchementInitial;
		this.dateValeurDonneesCiviles = dateValeurDonneesCiviles;
	}

	public List<OrganisationLocation> getEtablissementsADesapparier() {
		return etablissementsADesapparier;
	}

	public void setEtablissementsADesapparier(List<OrganisationLocation> etablissementsADesapparier) {
		this.etablissementsADesapparier = etablissementsADesapparier;
	}

	public RegDate getDateCharchementInitial() {
		return dateCharchementInitial;
	}

	public RegDate getDateValeurDonneesCiviles() {
		return dateValeurDonneesCiviles;
	}

	public List<ExceptionInfo> getExceptions() {
		return exceptions;
	}

	public boolean isSimulation() {
		return simulation;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(OrganisationLocation element, Exception e) {
		this.exceptions.add(new ExceptionInfo(element, e));
	}

	/**
	 * Ajoute le contenu d'un résultat.
	 *
	 * Attention: la liste aDesapparier de <i>right</i> est optionnelle. On peut ainsi choisir de remplir une foispour toute
	 */
	@Override
	public void addAll(DesappariementREEResults right) {
		if (!this.dateCharchementInitial.equals(right.getDateCharchementInitial()) || this.dateValeurDonneesCiviles != right.dateValeurDonneesCiviles) {
			throw new IllegalArgumentException("Les résultats de désappariement ne peuvent être ajoutés aux résultats actuels, car ils ne partagent pas les dates de validité et de chargement initial!");
		}
		if (etablissementsADesapparier != null && right.etablissementsADesapparier != null) {
			etablissementsADesapparier.addAll(right.etablissementsADesapparier);
		}
		this.desappariements.addAll(right.getDesappariements());
		this.exceptions.addAll(right.getExceptions());
	}

	public List<Desappariement> getDesappariementsOk() {
		return desappariements.stream()
				.filter(d -> d.getTypeResultat() == TypeResultat.SUCCES).collect(Collectors.toList());
	}

	public List<Desappariement> getDesappariementsAVerifier() {
		return desappariements.stream()
				.filter(d -> d.getTypeResultat() == TypeResultat.VERIFIER).collect(Collectors.toList());
	}

	public List<Desappariement> getDesappariementsEnEchec() {
		return desappariements.stream()
				.filter(d -> d.getTypeResultat() == TypeResultat.ECHEC).collect(Collectors.toList());
	}

	public static class ExceptionInfo {
		public final String exceptionMsg;
		public final OrganisationLocation etablissementRCEnt;
		ExceptionInfo(OrganisationLocation etablissementRCEnt, Exception e) {
			this.etablissementRCEnt = etablissementRCEnt;
			this.exceptionMsg = String.format("%s: %s", e.getClass().getName(), e.getMessage());
		}
	}

	public enum TypeResultat {
		// Désapparié avec succès.
		SUCCES,
		// Désapparié avec succès mais nécessite une revue. Par ex. parce que des données civiles ont changées.
		VERIFIER,
		// Une erreur est survenue et l'appariement n'a pu être complété.
		ECHEC
	}

	public static class Desappariement {
		private final TypeResultat typeResultat;

		// L'établissement RCEnt à désapparier
		private final OrganisationLocation etablissementRCEnt;

		// Numéro de l'entreprise désappariée, si applicable
		private final Long noEntreprise;

		// Numéro de l'entreprise désappariée, si applicable
		private final Long noCantonalEntreprise;

		// Numéro de l'établissement désapparié
		private final Long noEtablissement;

		// Raison sociale fiscale de l'entreprise
		private String raisonSociale;
		// Distance de Jaro Winkler entre les deux raisons sociales fiscale et civile. On a un bon match vers 0.85.
		private double raisonsSocialesJaroWinker;

		// Forme juridique fiscale de l'entreprise
		private String formeJuridique;
		// Numéro OFS de la commune de domicile fiscal de l'entreprise
		private Integer noOFSCommune;
		// Adresse fiscale de l'entreprise
		private AdresseEnvoiDetaillee adresse;

		private String message;

		public Desappariement(@NotNull DesappariementREEResults.TypeResultat typeResultat, @NotNull OrganisationLocation etablissementRCEnt, Long noEntreprise, Long noCantonalEntreprise, Long noEtablissement) {
			this.typeResultat = typeResultat;
			this.etablissementRCEnt = etablissementRCEnt;
			this.noEntreprise = noEntreprise;
			this.noCantonalEntreprise = noCantonalEntreprise;
			this.noEtablissement = noEtablissement;
		}

		public TypeResultat getTypeResultat() {
			return typeResultat;
		}

		/**
		 * @return L'établissement RCEnt à désapparier
		 */
		public OrganisationLocation getEtablissementRCEnt() {
			return etablissementRCEnt;
		}

		/**
		 * @return Le numéro de l'entreprise désappariée, si applicable, ou <code>null</code> sinon.
		 */
		public Long getNoEntreprise() {
			return noEntreprise;
		}

		public Long getNoCantonalEntreprise() {
			return noCantonalEntreprise;
		}

		public String getRaisonSociale() {
			return raisonSociale;
		}

		public void setRaisonSociale(String raisonSociale) {
			this.raisonSociale = raisonSociale;
		}

		public double getRaisonsSocialesJaroWinker() {
			return raisonsSocialesJaroWinker;
		}

		public void setRaisonsSocialesJaroWinker(double raisonsSocialesJaroWinker) {
			this.raisonsSocialesJaroWinker = raisonsSocialesJaroWinker;
		}

		public String getFormeJuridique() {
			return formeJuridique;
		}

		public void setFormeJuridique(String formeJuridique) {
			this.formeJuridique = formeJuridique;
		}

		public Integer getNoOFSCommune() {
			return noOFSCommune;
		}

		public void setNoOFSCommune(Integer noOFSCommune) {
			this.noOFSCommune = noOFSCommune;
		}

		public AdresseEnvoiDetaillee getAdresse() {
			return adresse;
		}

		public void setAdresse(AdresseEnvoiDetaillee adresse) {
			this.adresse = adresse;
		}

		/**
		 * @return Le numéro de l'établissement désapparié.
		 */
		public Long getNoEtablissement() {
			return noEtablissement;
		}

		/**
		 * @return le message de traitement, ou <code>null</code> si aucun.
		 */
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
