package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;
import ch.vd.uniregctb.type.CategorieIdentifiant;

/**
 * Identification d'une personne physique dans un registre fédéral (RCE, InfoStar...), cantonal, communal ou autre, à l'exclusion du nouveau numéro d'assuré social.
 * Voir norme eCH-0044.
 */
@Entity
@Table(name = "IDENTIFICATION_PERSONNE")
public class IdentificationPersonne extends HibernateEntity implements LinkedEntity {

	private Long id;
	private PersonnePhysique personnePhysique;

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


	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "NON_HABITANT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ID_PERS_TIERS_ID", columnNames = "NON_HABITANT_ID")
	public PersonnePhysique getPersonnePhysique() {
		return personnePhysique;
	}

	public void setPersonnePhysique(PersonnePhysique PersonnePhysique) {
		this.personnePhysique = PersonnePhysique;
	}


	/**
	 * Système fédéral, cantonal, communal ou autre ayant attribué l'identifiant
	 */
	private CategorieIdentifiant categorieIdentifiant;

	@Column(name = "CATEGORIE", length = LengthConstants.IDENTPERSONNE_CATEGORIE)
	@Type(type = "ch.vd.uniregctb.hibernate.CategorieIdentifiantUserType")
	public CategorieIdentifiant getCategorieIdentifiant() {
		return categorieIdentifiant;
	}

	public void setCategorieIdentifiant(CategorieIdentifiant theSource) {
		categorieIdentifiant = theSource;
	}

	/**
	 * Identifiant de la personne physique dans le système source
	 */
	private String identifiant;

	@Column(name = "IDENTIFIANT", length = LengthConstants.IDENTPERSONNE_IDENTIFIANT)
	public String getIdentifiant() {
		return identifiant;
	}

	public void setIdentifiant(String theIdentifiant) {
		identifiant = theIdentifiant;
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return personnePhysique == null ? null : Collections.singletonList(personnePhysique);
	}
}
