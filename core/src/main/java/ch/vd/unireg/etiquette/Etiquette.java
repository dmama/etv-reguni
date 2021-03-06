package ch.vd.unireg.etiquette;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.hibernate.ActionAutoEtiquetteUserType;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.type.TypeTiersEtiquette;

@Entity
@Table(name = "ETIQUETTE", indexes = @Index(name = "IDX_ETIQ_CA_ID", columnList = "COLADM_ID"))
@TypeDef(name = "ActionAutoEtiquette", typeClass = ActionAutoEtiquetteUserType.class)
public class Etiquette extends HibernateEntity {

	private Long id;
	private String code;
	private String libelle;
	private TypeTiersEtiquette typeTiers;
	private boolean active;
	private CollectiviteAdministrative collectiviteAdministrative;
	private boolean expediteurDocuments;
	private ActionAutoEtiquette actionSurDeces;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	public Etiquette() {
	}

	public Etiquette(String code, String libelle, boolean active, TypeTiersEtiquette typeTiers, @Nullable CollectiviteAdministrative collectiviteAdministrative) {
		this.code = code;
		this.libelle = libelle;
		this.active = active;
		this.typeTiers = typeTiers;
		this.collectiviteAdministrative = collectiviteAdministrative;
		this.expediteurDocuments = collectiviteAdministrative != null;      // par défaut
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * format "[A-Z][A-Z0-9_]*"
	 */
	@Column(name = "CODE", nullable = false, unique = true, length = LengthConstants.ETIQUETTE_CODE)
//	@Index(name = "IDX_ETIQ_CODE", columnNames = "CODE")        --> impliqué par la contrainte "unique = true"
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "LIBELLE", nullable = false, length = LengthConstants.ETIQUETTE_LIBELLE)
	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	@ManyToOne
	@JoinColumn(name = "COLADM_ID", nullable = true, foreignKey = @ForeignKey(name = "FK_ETIQ_CA_ID"))
	public CollectiviteAdministrative getCollectiviteAdministrative() {
		return collectiviteAdministrative;
	}

	public void setCollectiviteAdministrative(CollectiviteAdministrative collectiviteAdministrative) {
		this.collectiviteAdministrative = collectiviteAdministrative;
	}

	@Column(name = "ACTIVE")
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Column(name = "EXPEDITEUR_DOCS")
	public boolean isExpediteurDocuments() {
		return expediteurDocuments;
	}

	public void setExpediteurDocuments(boolean expediteurDocuments) {
		this.expediteurDocuments = expediteurDocuments;
	}

	@Column(name = "TYPE_TIERS", length = LengthConstants.ETIQUETTE_TYPE_TIERS, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeTiersEtiquette getTypeTiers() {
		return typeTiers;
	}

	public void setTypeTiers(TypeTiersEtiquette typeTiers) {
		this.typeTiers = typeTiers;
	}

	@Column(name = "AUTO_DECES", length = LengthConstants.ETIQUETTE_AUTO_DECES)
	@Type(type = "ActionAutoEtiquette")
	public ActionAutoEtiquette getActionSurDeces() {
		return actionSurDeces;
	}

	public void setActionSurDeces(ActionAutoEtiquette actionSurDeces) {
		this.actionSurDeces = actionSurDeces;
	}
}
