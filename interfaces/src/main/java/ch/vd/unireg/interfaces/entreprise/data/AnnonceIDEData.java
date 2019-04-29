package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * @author Raphaël Marmier, 2016-09-09, <raphael.marmier@vd.ch>
 */
public abstract class AnnonceIDEData implements BaseAnnonceIDE, Serializable {

	private static final long serialVersionUID = -7064921061708627093L;

	private final TypeAnnonce type;
	private final Date dateAnnonce;

	private final Utilisateur utilisateur;
	private final InfoServiceIDEObligEtendues infoServiceIDEObligEtendues;

	private final Statut statut;

	/**
	 * Indique si on a affaire à un établissement civil principal ou secondaire.
	 */
	private final TypeEtablissementCivil typeEtablissementCivil;

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
	 * La raison pour laquelle on demande la radiation de l'établissement civil.
	 */
	private RaisonDeRadiationRegistreIDE raisonDeRadiation;

	/**
	 * Commentaire de l'utilisateur qui a créé l'annonce
	 */
	private String commentaire;

	/**
	 * Informations ayant trait à l'identification de l'entreprise dans le régistre cantonal
	 */
	private InformationEntreprise informationEntreprise;

	/**
	 * La charge utile de l'annonce, avec les différentes propriétés applicables. Peut être nulle en cas
	 * d'événement de radiation.
	 */
	private Contenu contenu;


	public AnnonceIDEData(TypeAnnonce type, Date dateAnnonce, Utilisateur utilisateur, TypeEtablissementCivil typeEtablissementCivil, Statut statut, InfoServiceIDEObligEtendues infoServiceIDEObligEtendues) {
		this.type = type;
		this.dateAnnonce = dateAnnonce;
		this.utilisateur = utilisateur;
		this.typeEtablissementCivil = typeEtablissementCivil;
		this.statut = statut;
		this.infoServiceIDEObligEtendues = infoServiceIDEObligEtendues;
	}

	public AnnonceIDEData(BaseAnnonceIDE modele, @Nullable Statut statut) {
		this(modele.getType(), modele.getDateAnnonce(), modele.getUtilisateur(), modele.getTypeEtablissementCivil(), statut == null ? modele.getStatut() : statut, modele.getInfoServiceIDEObligEtendues());
		this.informationEntreprise = modele.getInformationEntreprise();
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

	public InfoServiceIDEObligEtendues getInfoServiceIDEObligEtendues() {
		return infoServiceIDEObligEtendues;
	}

	@Nullable
	@Override
	public Statut getStatut() {
		return statut;
	}


	@Override
	public TypeEtablissementCivil getTypeEtablissementCivil() {
		return typeEtablissementCivil;
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
	public BaseAnnonceIDE.InformationEntreprise getInformationEntreprise() {
		return informationEntreprise;
	}

	public void setInformationEntreprise(InformationEntreprise informationEntreprise) {
		this.informationEntreprise = informationEntreprise;
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

		final AnnonceIDEData that = (AnnonceIDEData) o;

		if (getType() != that.getType()) return false;
		if (getDateAnnonce() != null ? !getDateAnnonce().equals(that.getDateAnnonce()) : that.getDateAnnonce() != null) return false;
		if (getUtilisateur() != null ? !getUtilisateur().equals(that.getUtilisateur()) : that.getUtilisateur() != null) return false;
		if (getInfoServiceIDEObligEtendues() != null ? !getInfoServiceIDEObligEtendues().equals(that.getInfoServiceIDEObligEtendues()) : that.getInfoServiceIDEObligEtendues() != null) return false;
		if (getStatut() != null ? !getStatut().equals(that.getStatut()) : that.getStatut() != null) return false;
		if (getTypeEtablissementCivil() != that.getTypeEtablissementCivil()) return false;
		if (getNoIde() != null ? !getNoIde().equals(that.getNoIde()) : that.getNoIde() != null) return false;
		if (getNoIdeRemplacant() != null ? !getNoIdeRemplacant().equals(that.getNoIdeRemplacant()) : that.getNoIdeRemplacant() != null) return false;
		if (getNoIdeEtablissementPrincipal() != null ? !getNoIdeEtablissementPrincipal().equals(that.getNoIdeEtablissementPrincipal()) : that.getNoIdeEtablissementPrincipal() != null) return false;
		if (getRaisonDeRadiation() != that.getRaisonDeRadiation()) return false;
		if (getCommentaire() != null ? !getCommentaire().equals(that.getCommentaire()) : that.getCommentaire() != null) return false;
		if (getInformationEntreprise() != null ? !getInformationEntreprise().equals(that.getInformationEntreprise()) : that.getInformationEntreprise() != null) return false;
		return getContenu() != null ? getContenu().equals(that.getContenu()) : that.getContenu() == null;

	}

	@Override
	public int hashCode() {
		int result = getType() != null ? getType().hashCode() : 0;
		result = 31 * result + (getDateAnnonce() != null ? getDateAnnonce().hashCode() : 0);
		result = 31 * result + (getUtilisateur() != null ? getUtilisateur().hashCode() : 0);
		result = 31 * result + (getInfoServiceIDEObligEtendues() != null ? getInfoServiceIDEObligEtendues().hashCode() : 0);
		result = 31 * result + (getStatut() != null ? getStatut().hashCode() : 0);
		result = 31 * result + (getTypeEtablissementCivil() != null ? getTypeEtablissementCivil().hashCode() : 0);
		result = 31 * result + (getNoIde() != null ? getNoIde().hashCode() : 0);
		result = 31 * result + (getNoIdeRemplacant() != null ? getNoIdeRemplacant().hashCode() : 0);
		result = 31 * result + (getNoIdeEtablissementPrincipal() != null ? getNoIdeEtablissementPrincipal().hashCode() : 0);
		result = 31 * result + (getRaisonDeRadiation() != null ? getRaisonDeRadiation().hashCode() : 0);
		result = 31 * result + (getCommentaire() != null ? getCommentaire().hashCode() : 0);
		result = 31 * result + (getInformationEntreprise() != null ? getInformationEntreprise().hashCode() : 0);
		result = 31 * result + (getContenu() != null ? getContenu().hashCode() : 0);
		return result;
	}

	public static class UtilisateurImpl implements Utilisateur, Serializable {

		private static final long serialVersionUID = 8322026940101939155L;

		private String userId;
		private String telephone;

		public UtilisateurImpl(String userId, @Nullable String telephone) {
			this.userId = userId;
			if (userId == null || "".equals(userId)) {
				throw new NullPointerException("Un userId est nécessaire pour constituer un utilisateur d'annonce à l'IDE.");
			}
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

			final UtilisateurImpl that = (UtilisateurImpl) o;

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

	public static class InfoServiceIDEObligEtenduesImpl implements InfoServiceIDEObligEtendues, Serializable {

		private static final long serialVersionUID = -1663517686746298723L;

		// msi : à quoi sert ce numéro ? Il n'est pas retourné par RCEnt...
		private final NumeroIDE noIdeServiceIDEObligEtendues;
		private final String applicationId;
		private final String applicationName;

		public InfoServiceIDEObligEtenduesImpl(NumeroIDE noIdeServiceIDEObligEtendues, String applicationId, String applicationName) {
			this.noIdeServiceIDEObligEtendues = noIdeServiceIDEObligEtendues;
			this.applicationId = applicationId;
			this.applicationName = applicationName;
		}

		public NumeroIDE getNoIdeServiceIDEObligEtendues() {
			return noIdeServiceIDEObligEtendues;
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

			final InfoServiceIDEObligEtenduesImpl that = (InfoServiceIDEObligEtenduesImpl) o;

			if (getNoIdeServiceIDEObligEtendues() != null ? !getNoIdeServiceIDEObligEtendues().equals(that.getNoIdeServiceIDEObligEtendues()) : that.getNoIdeServiceIDEObligEtendues() != null) return false;
			if (getApplicationId() != null ? !getApplicationId().equals(that.getApplicationId()) : that.getApplicationId() != null) return false;
			return getApplicationName() != null ? getApplicationName().equals(that.getApplicationName()) : that.getApplicationName() == null;

		}

		@Override
		public int hashCode() {
			int result = getNoIdeServiceIDEObligEtendues() != null ? getNoIdeServiceIDEObligEtendues().hashCode() : 0;
			result = 31 * result + (getApplicationId() != null ? getApplicationId().hashCode() : 0);
			result = 31 * result + (getApplicationName() != null ? getApplicationName().hashCode() : 0);
			return result;
		}
	}

	public static class StatutImpl implements Statut, Serializable {

		private static final long serialVersionUID = 6395078590109220344L;

		private final StatutAnnonce statut;
		private final Date dateStatut;
		private final List<Pair<String, String>> erreurs;

		public StatutImpl(StatutAnnonce statut, Date dateStatut, List<Pair<String, String>> erreurs) {
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
		public String getTexteErreurs() {
			if (erreurs == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			for (Pair<String, String> erreur : erreurs) {
				sb.append(erreur.toString());
			}
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final StatutImpl that = (StatutImpl) o;

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

	public static class InformationEntrepriseImpl implements InformationEntreprise, Serializable {

		private static final long serialVersionUID = -5539575958482798119L;

		/**
		 * Numéro cantonal de l'établissement à modifier (nul en cas de création)
		 */
		private Long numeroEtablissement;

		/**
		 * Numéro cantonal de l'entreprise faîtière à laquelle l'établissement est rattaché.
		 */
		@Nullable
		private Long numeroEntreprise;

		/**
		 * Numéro cantonal de l'établissement de remplacement (optionnel).
		 */
		@Nullable
		private Long numeroEtablissementRemplacant;

		public InformationEntrepriseImpl() {}

		public InformationEntrepriseImpl(Long numeroEtablissement, @Nullable Long numeroEntreprise, @Nullable Long numeroEtablissementRemplacant) {
			this.numeroEtablissement = numeroEtablissement;
			this.numeroEntreprise = numeroEntreprise;
			this.numeroEtablissementRemplacant = numeroEtablissementRemplacant;
		}

		@Override
		public Long getNumeroEtablissement() {
			return numeroEtablissement;
		}

		@Override
		@Nullable
		public Long getNumeroEntreprise() {
			return numeroEntreprise;
		}

		@Override
		@Nullable
		public Long getNumeroEtablissementRemplacant() {
			return numeroEtablissementRemplacant;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final InformationEntrepriseImpl that = (InformationEntrepriseImpl) o;

			if (getNumeroEtablissement() != null ? !getNumeroEtablissement().equals(that.getNumeroEtablissement()) : that.getNumeroEtablissement() != null) return false;
			if (getNumeroEntreprise() != null ? !getNumeroEntreprise().equals(that.getNumeroEntreprise()) : that.getNumeroEntreprise() != null) return false;
			return getNumeroEtablissementRemplacant() != null ? getNumeroEtablissementRemplacant().equals(that.getNumeroEtablissementRemplacant()) : that.getNumeroEtablissementRemplacant() == null;

		}

		@Override
		public int hashCode() {
			int result = getNumeroEtablissement() != null ? getNumeroEtablissement().hashCode() : 0;
			result = 31 * result + (getNumeroEntreprise() != null ? getNumeroEntreprise().hashCode() : 0);
			result = 31 * result + (getNumeroEtablissementRemplacant() != null ? getNumeroEtablissementRemplacant().hashCode() : 0);
			return result;
		}
	}


	public static class ContenuImpl implements Contenu, Serializable {

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

			final ContenuImpl that = (ContenuImpl) o;

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
