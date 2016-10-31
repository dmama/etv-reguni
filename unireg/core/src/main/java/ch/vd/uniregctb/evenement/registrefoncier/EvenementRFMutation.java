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
		SURFACE,
		BATIMENT
	}

	/**
	 * Le type d'entité concernée par la mutation.
	 */
	private TypeEntite typeEntite;

	public enum TypeMutation {
		CREATION,
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

	@Column(name = "TYPE_ENTITE", length = 11, nullable = false)
	@Enumerated(EnumType.STRING)
	@Index(name="IDX_EV_RF_MUT_TYPE_ENTITE")
	public TypeEntite getTypeEntite() {
		return typeEntite;
	}

	public void setTypeEntite(TypeEntite typeEntite) {
		this.typeEntite = typeEntite;
	}

	@Column(name = "TYPE_MUTATION", length = 12, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeMutation getTypeMutation() {
		return typeMutation;
	}

	public void setTypeMutation(TypeMutation typeMutation) {
		this.typeMutation = typeMutation;
	}

	@Column(name = "XML_CONTENT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.StringAsClobUserType")
	public String getXmlContent() {
		return xmlContent;
	}

	public void setXmlContent(@Nullable String xmlContent) {
		this.xmlContent = xmlContent;
	}
}
