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
import ch.vd.uniregctb.common.LengthConstants;

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

	/**
	 * Le type d'entité concernée par la mutation.
	 */
	private TypeEntiteRF typeEntite;

	/**
	 * Le type de mutation considérée.
	 */
	private TypeMutationRF typeMutation;

	/**
	 * L'id de l'immeuble associé à la mutation (pas toujours renseigné)
	 */
	private String idRF;

	/**
	 * Le représentation XML de l'entité.
	 */
	private String xmlContent;

	/**
	 * Un message d'erreur en cas d'erreur de traitement.
	 */
	@Nullable
	private String errorMessage;

	/**
	 * La callstack complète en cas d'erreur de traitement.
	 */
	@Nullable
	private String callstack;

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

	@Column(name = "ETAT", length = LengthConstants.RF_ETAT_EVENEMENT)
	@Enumerated(EnumType.STRING)
	@Index(name="IDX_EV_RF_MUT_ETAT")
	public EtatEvenementRF getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementRF etat) {
		this.etat = etat;
	}

	@Column(name = "TYPE_ENTITE", length = LengthConstants.RF_TYPE_ENTITE, nullable = false)
	@Enumerated(EnumType.STRING)
	@Index(name="IDX_EV_RF_MUT_TYPE_ENTITE")
	public TypeEntiteRF getTypeEntite() {
		return typeEntite;
	}

	public void setTypeEntite(TypeEntiteRF typeEntite) {
		this.typeEntite = typeEntite;
	}

	@Column(name = "TYPE_MUTATION", length = LengthConstants.RF_TYPE_MUTATION, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeMutationRF getTypeMutation() {
		return typeMutation;
	}

	public void setTypeMutation(TypeMutationRF typeMutation) {
		this.typeMutation = typeMutation;
	}

	/**
	 * Valeur de l'id RF en fonction du type d'entité :
	 * <ul>
	 *     <li>AYANT_DROIT : l'idRF de l'ayant-droit lui-même</li>
	 *     <li>DROIT : idRF de l'ayant-droit qui possède le droit</li>
	 *     <li>IMMEUBLE : idRF de l'immeuble lui-même</li>
	 *     <li>SURFACE_AU_SOL : idRF de l'immeuble qui possède les surfaces au sol</li>
	 *     <li>BATIMENT : masterIdRF du bâtiment</li>
	 *     <li>COMMUNE : numéro RF de la commune</li>
	 * </ul>
	 * @return l'idRF de l'entité liée à la mutation.
	 */
	@Index(name = "IDX_EV_RF_ID_RF")
	@Column(name = "ID_RF", length = LengthConstants.RF_ID_RF)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "XML_CONTENT")   // la colonne doit être nullable car - techniquement - Hibernate fait un insert avec le blob nul puis un update pour insérer le contenu.
	@Type(type = "ch.vd.uniregctb.hibernate.StringAsClobUserType")
	public String getXmlContent() {
		return xmlContent;
	}

	public void setXmlContent(@Nullable String xmlContent) {
		this.xmlContent = xmlContent;
	}

	@Column(name = "ERROR_MESSAGE", length = LengthConstants.RF_ERROR_MESSAGE)
	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Column(name = "CALLSTACK")
	@Type(type = "ch.vd.uniregctb.hibernate.StringAsClobUserType")
	@Nullable
	public String getCallstack() {
		return callstack;
	}

	public void setCallstack(@Nullable String callstack) {
		this.callstack = callstack;
	}
}
