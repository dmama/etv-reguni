package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Pair;

/**
 * @author Raphaël Marmier, 2016-09-09, <raphael.marmier@vd.ch>
 */
public class ModeleAnnonceIDERCEnt implements ModeleAnnonceIDE, Serializable {

	private static final long serialVersionUID = -7064921061708627093L;

	/*
		Identification du service IDE et d'Unireg:
		- Numéro IDE du "service IDE" selon Art. 3c LIDE
		- Identification de l'application dans RCEnt
	 */
	public static final NumeroIDE NO_IDE_SERVICE_IDE = new NumeroIDE("999999996");
	public static final String NO_APPLICATION_UNIREG = "2";
	public static final String NOM_APPLICATION_UNIREG = "UNIREG";

	private TypeAnnonce type;
	private Date dateAnnonce;

	private Utilisateur utilisateur;
	private ServiceIDE serviceIDE = new ServiceIDERCEnt(NO_IDE_SERVICE_IDE, NO_APPLICATION_UNIREG, NOM_APPLICATION_UNIREG); // Identifiant de service IDE en dur.

	private Statut statut;

	/**
	 * Indique si on a affaire à un site principal ou secondaire.
	 */
	private TypeDeSite typeDeSite;

	/**
	 * Numéro IDE (temporaire ou non) de l'entreprise.
	 */
	@Nullable
	private NumeroIDE noIde;

	/**
	 * Numéro IDE de l'entreprise de remplacement lorsque la demande de création a été refusée pour cause d'entreprise déjà existante.
	 */
	@Nullable
	private NumeroIDE noIdeRemplacant;

	/**
	 * Numéro IDE de l'entreprise faîtière à laquelle l'établissement est rattaché, lorsqu'il s'agit d'un établissement secondaire.
	 */
	@Nullable
	private NumeroIDE noIdeEtablissementPrincipal;

	/**
	 * La raison pour laquelle on demande la radiation du site.
	 */
	private RaisonDeRadiationRegistreIDE raisonDeRadiation;

	/**
	 * Commentaire de l'utilisateur qui a créé l'annonce
	 */
	private String commentaire;

	/**
	 * Informations ayant trait à l'identification de l'entreprise dans le régistre cantonal (RCEnt)
	 */
	private InformationOrganisation informationOrganisation;

	/**
	 * La charge utile de l'annonce, avec les différentes propriétés applicables. Peut être nulle en cas
	 * d'événement de radiation.
	 */
	private Contenu contenu;


	public ModeleAnnonceIDERCEnt(TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeDeSite typeDeSite, Statut statut) {
		this.type = type;
		this.dateAnnonce = dateAnnonce;
		this.utilisateur = utilisateur;
		this.typeDeSite = typeDeSite;
		if (statut != null) {
			this.statut = statut;
		} else {
			new StatutRCEnt(StatutAnnonce.A_TRANSMETTRE, dateAnnonce, Collections.<Pair<String,String>>emptyList());
		}
	}

	public ModeleAnnonceIDERCEnt(ModeleAnnonceIDE modele, @Nullable Statut statut) {
		this(modele.getType(), modele.getDateAnnonce(), modele.getUtilisateur(), modele.getTypeDeSite(), statut == null ? modele.getStatut() : statut);
		this.informationOrganisation = modele.getInformationOrganisation();
		this.noIde = modele.getNoIde();
		this.noIdeRemplacant = modele.getNoIdeRemplacant();
		this.noIdeEtablissementPrincipal = modele.getNoIdeEtablissementPrincipal();
		this.raisonDeRadiation = modele.getRaisonDeRadiation();
		this.commentaire = modele.getCommentaire();
		this.contenu = modele.getContenu();
	}


	@Override
	public TypeAnnonce getType() {
		return type;
	}

	@Override
	public Date getDateAnnonce() {
		return dateAnnonce;
	}

	@Override
	public Utilisateur getUtilisateur() {
		return utilisateur;
	}

	@Override
	public ServiceIDE getServiceIDE() {
		return serviceIDE;
	}

	@Nullable
	@Override
	public Statut getStatut() {
		return statut;
	}


	@Override
	public TypeDeSite getTypeDeSite() {
		return typeDeSite;
	}

	@Override
	@Nullable
	public NumeroIDE getNoIde() {
		return noIde;
	}

	public void setNoIde(@Nullable NumeroIDE noIde) {
		this.noIde = noIde;
	}
	@Nullable
	@Override
	public NumeroIDE getNoIdeRemplacant() {
		return noIdeRemplacant;
	}

	public void setNoIdeRemplacant(@Nullable NumeroIDE noIdeRemplacant) {
		this.noIdeRemplacant = noIdeRemplacant;
	}

	@Override
	@Nullable
	public NumeroIDE getNoIdeEtablissementPrincipal() {
		return noIdeEtablissementPrincipal;
	}

	public void setNoIdeEtablissementPrincipal(@Nullable NumeroIDE noIdeEtablissementPrincipal) {
		this.noIdeEtablissementPrincipal = noIdeEtablissementPrincipal;
	}

	@Override
	@Nullable
	public RaisonDeRadiationRegistreIDE getRaisonDeRadiation() {
		return raisonDeRadiation;
	}

	public void setRaisonDeRadiation(RaisonDeRadiationRegistreIDE raisonDeRadiation) {
		this.raisonDeRadiation = raisonDeRadiation;
	}

	@Override
	@Nullable
	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	@Override
	@Nullable
	public InformationOrganisation getInformationOrganisation() {
		return informationOrganisation;
	}

	public void setInformationOrganisation(InformationOrganisation informationOrganisation) {
		this.informationOrganisation = informationOrganisation;
	}

	@Nullable
	@Override
	public Contenu getContenu() {
		return contenu;
	}

	public void setContenu(Contenu contenu) {
		this.contenu = contenu;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final ModeleAnnonceIDERCEnt that = (ModeleAnnonceIDERCEnt) o;

		if (getType() != that.getType()) return false;
		if (getDateAnnonce() != null ? !getDateAnnonce().equals(that.getDateAnnonce()) : that.getDateAnnonce() != null) return false;
		if (getUtilisateur() != null ? !getUtilisateur().equals(that.getUtilisateur()) : that.getUtilisateur() != null) return false;
		if (getServiceIDE() != null ? !getServiceIDE().equals(that.getServiceIDE()) : that.getServiceIDE() != null) return false;
		if (getStatut() != null ? !getStatut().equals(that.getStatut()) : that.getStatut() != null) return false;
		if (getTypeDeSite() != that.getTypeDeSite()) return false;
		if (getNoIde() != null ? !getNoIde().equals(that.getNoIde()) : that.getNoIde() != null) return false;
		if (getNoIdeRemplacant() != null ? !getNoIdeRemplacant().equals(that.getNoIdeRemplacant()) : that.getNoIdeRemplacant() != null) return false;
		if (getNoIdeEtablissementPrincipal() != null ? !getNoIdeEtablissementPrincipal().equals(that.getNoIdeEtablissementPrincipal()) : that.getNoIdeEtablissementPrincipal() != null) return false;
		if (getRaisonDeRadiation() != that.getRaisonDeRadiation()) return false;
		if (getCommentaire() != null ? !getCommentaire().equals(that.getCommentaire()) : that.getCommentaire() != null) return false;
		if (getInformationOrganisation() != null ? !getInformationOrganisation().equals(that.getInformationOrganisation()) : that.getInformationOrganisation() != null) return false;
		return getContenu() != null ? getContenu().equals(that.getContenu()) : that.getContenu() == null;

	}

	@Override
	public int hashCode() {
		int result = getType() != null ? getType().hashCode() : 0;
		result = 31 * result + (getDateAnnonce() != null ? getDateAnnonce().hashCode() : 0);
		result = 31 * result + (getUtilisateur() != null ? getUtilisateur().hashCode() : 0);
		result = 31 * result + (getServiceIDE() != null ? getServiceIDE().hashCode() : 0);
		result = 31 * result + (getStatut() != null ? getStatut().hashCode() : 0);
		result = 31 * result + (getTypeDeSite() != null ? getTypeDeSite().hashCode() : 0);
		result = 31 * result + (getNoIde() != null ? getNoIde().hashCode() : 0);
		result = 31 * result + (getNoIdeRemplacant() != null ? getNoIdeRemplacant().hashCode() : 0);
		result = 31 * result + (getNoIdeEtablissementPrincipal() != null ? getNoIdeEtablissementPrincipal().hashCode() : 0);
		result = 31 * result + (getRaisonDeRadiation() != null ? getRaisonDeRadiation().hashCode() : 0);
		result = 31 * result + (getCommentaire() != null ? getCommentaire().hashCode() : 0);
		result = 31 * result + (getInformationOrganisation() != null ? getInformationOrganisation().hashCode() : 0);
		result = 31 * result + (getContenu() != null ? getContenu().hashCode() : 0);
		return result;
	}

	public static class UtilisateurRCEnt implements Utilisateur, Serializable {

		private static final long serialVersionUID = 8322026940101939155L;

		private String userId;
		private String telephone;

		public UtilisateurRCEnt(String userId, @Nullable String telephone) {
			this.userId = userId;
			this.telephone = telephone;
		}

		@Override
		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		@Override
		@Nullable
		public String getTelephone() {
			return telephone;
		}

		public void setTelephone(String telephone) {
			this.telephone = telephone;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final UtilisateurRCEnt that = (UtilisateurRCEnt) o;

			if (getUserId() != null ? !getUserId().equals(that.getUserId()) : that.getUserId() != null) return false;
			return getTelephone() != null ? getTelephone().equals(that.getTelephone()) : that.getTelephone() == null;

		}

		@Override
		public int hashCode() {
			int result = getUserId() != null ? getUserId().hashCode() : 0;
			result = 31 * result + (getTelephone() != null ? getTelephone().hashCode() : 0);
			return result;
		}
	}

	public static class ServiceIDERCEnt implements ServiceIDE, Serializable {

		private static final long serialVersionUID = -1663517686746298723L;

		NumeroIDE noIdeServiceIDE;
		String applicationId;
		String applicationName;

		public ServiceIDERCEnt(NumeroIDE noIdeServiceIDE, String applicationId, String applicationName) {
			this.noIdeServiceIDE = noIdeServiceIDE;
			this.applicationId = applicationId;
			this.applicationName = applicationName;
		}

		public NumeroIDE getNoIdeServiceIDE() {
			return noIdeServiceIDE;
		}

		@Override
		public String getApplicationId() {
			return applicationId;
		}

		@Override
		public String getApplicationName() {
			return applicationName;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final ServiceIDERCEnt that = (ServiceIDERCEnt) o;

			if (getNoIdeServiceIDE() != null ? !getNoIdeServiceIDE().equals(that.getNoIdeServiceIDE()) : that.getNoIdeServiceIDE() != null) return false;
			if (getApplicationId() != null ? !getApplicationId().equals(that.getApplicationId()) : that.getApplicationId() != null) return false;
			return getApplicationName() != null ? getApplicationName().equals(that.getApplicationName()) : that.getApplicationName() == null;

		}

		@Override
		public int hashCode() {
			int result = getNoIdeServiceIDE() != null ? getNoIdeServiceIDE().hashCode() : 0;
			result = 31 * result + (getApplicationId() != null ? getApplicationId().hashCode() : 0);
			result = 31 * result + (getApplicationName() != null ? getApplicationName().hashCode() : 0);
			return result;
		}
	}

	public static class StatutRCEnt implements Statut, Serializable {

		private static final long serialVersionUID = 6395078590109220344L;

		private StatutAnnonce statut;
		private Date dateStatut;
		private List<Pair<String, String>> erreurs;

		public StatutRCEnt(StatutAnnonce statut, Date dateStatut, List<Pair<String, String>> erreurs) {
			this.statut = statut;
			this.dateStatut = dateStatut;
			this.erreurs = erreurs;
		}

		@Override
		public StatutAnnonce getStatut() {
			return statut;
		}

		@Override
		public Date getDateStatut() {
			return dateStatut;
		}

		@Override
		public List<Pair<String, String>> getErreurs() {
			return erreurs;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final StatutRCEnt that = (StatutRCEnt) o;

			if (getStatut() != that.getStatut()) return false;
			if (getDateStatut() != null ? !getDateStatut().equals(that.getDateStatut()) : that.getDateStatut() != null) return false;
			return getErreurs() != null ? getErreurs().equals(that.getErreurs()) : that.getErreurs() == null;

		}

		@Override
		public int hashCode() {
			int result = getStatut() != null ? getStatut().hashCode() : 0;
			result = 31 * result + (getDateStatut() != null ? getDateStatut().hashCode() : 0);
			result = 31 * result + (getErreurs() != null ? getErreurs().hashCode() : 0);
			return result;
		}
	}

	public static class InformationOrganisationRCEnt implements InformationOrganisation, Serializable {

		private static final long serialVersionUID = -5539575958482798119L;

		/**
		 * Numéro cantonal de l'établissement à modifier (nul en cas de création)
		 */
		private Long numeroSite;

		/**
		 * Numéro cantonal de l'entreprise faîtière à laquelle l'établissement est rattaché.
		 */
		@Nullable
		private Long numeroOrganisation;

		/**
		 * Numéro cantonal de l'établissement de remplacement (optionnel).
		 */
		@Nullable
		private Long numeroSiteRemplacant;

		public InformationOrganisationRCEnt() {}

		public InformationOrganisationRCEnt(Long numeroSite, @Nullable Long numeroOrganisation, @Nullable Long numeroSiteRemplacant) {
			this.numeroSite = numeroSite;
			this.numeroOrganisation = numeroOrganisation;
			this.numeroSiteRemplacant = numeroSiteRemplacant;
		}

		@Override
		public Long getNumeroSite() {
			return numeroSite;
		}

		@Override
		@Nullable
		public Long getNumeroOrganisation() {
			return numeroOrganisation;
		}

		@Override
		@Nullable
		public Long getNumeroSiteRemplacant() {
			return numeroSiteRemplacant;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final InformationOrganisationRCEnt that = (InformationOrganisationRCEnt) o;

			if (getNumeroSite() != null ? !getNumeroSite().equals(that.getNumeroSite()) : that.getNumeroSite() != null) return false;
			if (getNumeroOrganisation() != null ? !getNumeroOrganisation().equals(that.getNumeroOrganisation()) : that.getNumeroOrganisation() != null) return false;
			return getNumeroSiteRemplacant() != null ? getNumeroSiteRemplacant().equals(that.getNumeroSiteRemplacant()) : that.getNumeroSiteRemplacant() == null;

		}

		@Override
		public int hashCode() {
			int result = getNumeroSite() != null ? getNumeroSite().hashCode() : 0;
			result = 31 * result + (getNumeroOrganisation() != null ? getNumeroOrganisation().hashCode() : 0);
			result = 31 * result + (getNumeroSiteRemplacant() != null ? getNumeroSiteRemplacant().hashCode() : 0);
			return result;
		}
	}


	public static class ContenuRCEnt implements Contenu, Serializable {

		private static final long serialVersionUID = 369138522853706176L;

		private String nom;
		private String nomAdditionnel;
		private AdresseAnnonceIDE adresse;

		private FormeLegale formeLegale;
		private String secteurActivite;

		@Override
		public String getNom() {
			return nom;
		}

		public void setNom(String nom) {
			this.nom = nom;
		}

		@Override
		public String getNomAdditionnel() {
			return nomAdditionnel;
		}

		public void setNomAdditionnel(String nomAdditionnel) {
			this.nomAdditionnel = nomAdditionnel;
		}

		@Override
		public AdresseAnnonceIDE getAdresse() {
			return adresse;
		}

		public void setAdresse(AdresseAnnonceIDE adresse) {
			this.adresse = adresse;
		}

		@Override
		public FormeLegale getFormeLegale() {
			return formeLegale;
		}

		public void setFormeLegale(FormeLegale formeLegale) {
			this.formeLegale = formeLegale;
		}

		public String getSecteurActivite() {
			return secteurActivite;
		}

		public void setSecteurActivite(String secteurActivite) {
			this.secteurActivite = secteurActivite;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final ContenuRCEnt that = (ContenuRCEnt) o;

			if (getNom() != null ? !getNom().equals(that.getNom()) : that.getNom() != null) return false;
			if (getNomAdditionnel() != null ? !getNomAdditionnel().equals(that.getNomAdditionnel()) : that.getNomAdditionnel() != null) return false;
			if (getAdresse() != null ? !getAdresse().equals(that.getAdresse()) : that.getAdresse() != null) return false;
			if (getFormeLegale() != that.getFormeLegale()) return false;
			return getSecteurActivite() != null ? getSecteurActivite().equals(that.getSecteurActivite()) : that.getSecteurActivite() == null;

		}

		@Override
		public int hashCode() {
			int result = getNom() != null ? getNom().hashCode() : 0;
			result = 31 * result + (getNomAdditionnel() != null ? getNomAdditionnel().hashCode() : 0);
			result = 31 * result + (getAdresse() != null ? getAdresse().hashCode() : 0);
			result = 31 * result + (getFormeLegale() != null ? getFormeLegale().hashCode() : 0);
			result = 31 * result + (getSecteurActivite() != null ? getSecteurActivite().hashCode() : 0);
			return result;
		}
	}
}
