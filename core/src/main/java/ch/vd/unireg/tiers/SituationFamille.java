package ch.vd.unireg.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.type.EtatCivil;

@Entity
@Table(name = "SITUATION_FAMILLE", indexes = {
		@Index(name = "IDX_SIT_FAM_CTB_ID", columnList = "CTB_ID"),
		@Index(name = "IDX_SIT_FAM_MC_CTB_ID", columnList = "TIERS_PRINCIPAL_ID")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "SITUATION_FAMILLE_TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("AbstractSituationFamille")
public abstract class SituationFamille extends HibernateDateRangeEntity implements Duplicable<SituationFamille>, LinkedEntity {

	/**
	 * La primary key
	 */
	private Long id;

	/**
	 * Le contribuable associé
	 */
	private ContribuableImpositionPersonnesPhysiques contribuable;
	private int nombreEnfants;
	private EtatCivil etatCivil;

	public SituationFamille() {
	}

	public SituationFamille(SituationFamille situationFamille) {
		super(situationFamille);
		this.contribuable = situationFamille.contribuable;
		this.etatCivil = situationFamille.etatCivil;
		this.nombreEnfants = situationFamille.nombreEnfants;
	}

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

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "CTB_ID", insertable = false, updatable = false, nullable = false)
	public ContribuableImpositionPersonnesPhysiques getContribuable() {
		return contribuable;
	}

	public void setContribuable(ContribuableImpositionPersonnesPhysiques contribuable) {
		this.contribuable = contribuable;
	}

	@Column(name = "NOMBRE_ENFANTS", nullable = false)
	public int getNombreEnfants() {
		return nombreEnfants;
	}

	public void setNombreEnfants(int theNombreEnfants) {
		nombreEnfants = theNombreEnfants;
	}

	@Column(name = "ETAT_CIVIL", length = LengthConstants.SITUATIONFAMILLE_ETATCIVIL)
	@Type(type = "ch.vd.unireg.hibernate.EtatCivilUserType")
	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return contribuable == null ? null : Collections.singletonList(contribuable);
	}
}
