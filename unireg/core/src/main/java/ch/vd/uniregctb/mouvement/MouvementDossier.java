/**
 *
 */
package ch.vd.uniregctb.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Classe de base des mouvements de dossiers
 */
@Entity
@Table(name = "MOUVEMENT_DOSSIER")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "MVT_TYPE", discriminatorType = DiscriminatorType.STRING)
@org.hibernate.annotations.Table(appliesTo = "MOUVEMENT_DOSSIER", indexes = { @Index(name = "IDX_MOUVEMENT_DOSSIER_ETAT_CTB", columnNames = {"ETAT", "CTB_ID"})})
public abstract class MouvementDossier extends HibernateEntity {

	private static final long serialVersionUID = -2711289657913151774L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * Contribuable = dossier
	 */
	private Contribuable contribuable;

	/**
	 * Etat courant du mouvement
	 */
	private EtatMouvementDossier etat;

	/**
	 * Date de passage à l'état "traité" du mouvement
	 */
	private RegDate dateMouvement;

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
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter un mouvement de dossier à un contribuable sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "CTB_ID")
	@Index(name = "IDX_MVT_DOSSIER_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable theContribuable) {
		contribuable = theContribuable;
	}

	@Column(name = "ETAT", length = LengthConstants.MVTDOSSIER_ETAT, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatMouvementDossierUserType")
	public EtatMouvementDossier getEtat() {
		return etat;
	}

	public void setEtat(EtatMouvementDossier etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_MOUVEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateMouvement() {
		return dateMouvement;
	}

	public void setDateMouvement(RegDate dateMouvement) {
		this.dateMouvement = dateMouvement;
	}
}
