/**
 *
 */
package ch.vd.unireg.mouvement;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.Contribuable;

/**
 * Classe de base des mouvements de dossiers
 */
@Entity
@Table(name = "MOUVEMENT_DOSSIER", indexes = {
		@Index(name = "IDX_MOUVEMENT_DOSSIER_ETAT_CTB", columnList = "ETAT, CTB_ID"),
		@Index(name = "IDX_VISA_COLLABORATEUR", columnList = "VISA_COLLABORATEUR"),
		@Index(name = "IDX_MVT_DOSSIER_CTB_ID", columnList = "CTB_ID"),
		@Index(name = "IDX_MVT_DOSSIER_BORD_ID", columnList = "BORDEREAU_ID")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "MVT_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class MouvementDossier extends HibernateEntity {

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
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter un mouvement de dossier à un contribuable sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "CTB_ID", foreignKey = @ForeignKey(name = "FK_MOV_DOS_CTB_ID"))

	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable theContribuable) {
		contribuable = theContribuable;
	}

	@Column(name = "ETAT", length = LengthConstants.MVTDOSSIER_ETAT, nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.EtatMouvementDossierUserType")
	public EtatMouvementDossier getEtat() {
		return etat;
	}

	public void setEtat(EtatMouvementDossier etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_MOUVEMENT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateMouvement() {
		return dateMouvement;
	}

	public void setDateMouvement(RegDate dateMouvement) {
		this.dateMouvement = dateMouvement;
	}
}
