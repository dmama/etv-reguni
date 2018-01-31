package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;
import ch.vd.uniregctb.type.EtatCivil;

@Entity
@Table(name = "SITUATION_FAMILLE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "SITUATION_FAMILLE_TYPE", discriminatorType = DiscriminatorType.STRING)
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
	@JoinColumn(name = "CTB_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_SIT_FAM_CTB_ID", columnNames = "CTB_ID")
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
	@Type(type = "ch.vd.uniregctb.hibernate.EtatCivilUserType")
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
