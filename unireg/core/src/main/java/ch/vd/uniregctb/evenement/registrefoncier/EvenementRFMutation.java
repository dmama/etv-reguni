package ch.vd.uniregctb.evenement.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;

@Entity
@Table(name = "EVENEMENT_RF_MUTATION")
public class EvenementRFMutation extends HibernateEntity {

	private Long id;

	/**
	 * L'import d'où provient la mutation
	 */
	private EvenementRFImport parentImport;

	/**
	 * L'état courant de l'événement.
	 */
	private EtatEvenementRF etat;

	public enum TypeEntite {
		AYANT_DROIT,
		DROIT,
		IMMEUBLE,
		SURFACE_AU_SOL,
		BATIMENT
	}

	/**
	 * Le type d'entité concernée par la mutation.
	 */
	private TypeEntite typeEntite;

	public enum TypeMutation {
		/**
		 * La mutation est une création d'une nouvelle entité.
		 */
		CREATION,
		/**
		 * La mutation est une modification d'une entité existante.
		 */
		MODIFICATION
	}

	/**
	 * Le type de mutation considérée.
	 */
	private TypeMutation typeMutation;

	/**
	 * Le représentation XML de l'entité.
	 */
	private String xmlContent;

	/**
	 * Un message d'erreur en cas d'erreur de traitement.
	 */
	@Nullable
	private String errorMessage;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "IMPORT_ID", nullable = false)
	@Index(name = "IDX_EV_RF_IMP_ID")
	public EvenementRFImport getParentImport() {
		return parentImport;
	}

	public void setParentImport(EvenementRFImport parentImport) {
		this.parentImport = parentImport;
	}

	@Column(name = "ETAT", length = 9)
	@Enumerated(EnumType.STRING)
	@Index(name="IDX_EV_RF_MUT_ETAT")
	public EtatEvenementRF getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementRF etat) {
		this.etat = etat;
	}

	@Column(name = "TYPE_ENTITE", length = 14, nullable = false)
	@Enumerated(EnumType.STRING)
	@Index(name="IDX_EV_RF_MUT_TYPE_ENTITE")
	public TypeEntite getTypeEntite() {
		return typeEntite;
	}

	public void setTypeEntite(TypeEntite typeEntite) {
		this.typeEntite = typeEntite;
	}

	@Column(name = "TYPE_MUTATION", length = 13, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeMutation getTypeMutation() {
		return typeMutation;
	}

	public void setTypeMutation(TypeMutation typeMutation) {
		this.typeMutation = typeMutation;
	}

	@Column(name = "XML_CONTENT")   // la colonne doit être nullable car - techniquement - Hibernate fait un insert avec le blob nul puis un update pour insérer le contenu.
	@Type(type = "ch.vd.uniregctb.hibernate.StringAsClobUserType")
	public String getXmlContent() {
		return xmlContent;
	}

	public void setXmlContent(@Nullable String xmlContent) {
		this.xmlContent = xmlContent;
	}

	@Column(name = "ERROR_MESSAGE")
	@Type(type = "ch.vd.uniregctb.hibernate.StringAsClobUserType")
	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
