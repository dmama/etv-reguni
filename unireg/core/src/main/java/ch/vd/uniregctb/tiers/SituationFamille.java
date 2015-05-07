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

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.EtatCivil;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DU1DQOxIEdycMumkNMs2uQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DU1DQOxIEdycMumkNMs2uQ"
 */
@Entity
@Table(name = "SITUATION_FAMILLE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "SITUATION_FAMILLE_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class SituationFamille extends HibernateEntity implements DateRange, Duplicable<SituationFamille>, LinkedEntity {

	/**
	 * La primary key
	 */
	private Long id;

	/**
	 * Le contribuable associé
	 */
	private ContribuableImpositionPersonnesPhysiques contribuable;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date de début de la validité de l'adresse postale du tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAPY0Edyw0I40oDFBsg"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date de fin de la validité de l'adresse postale du tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAvY0Edyw0I40oDFBsg"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0TCpoOxIEdycMumkNMs2uQ"
	 */
	private int nombreEnfants;

	private EtatCivil etatCivil;

	public SituationFamille() {
	}

	public SituationFamille(SituationFamille situationFamille) {
		this.contribuable = situationFamille.contribuable;
		this.dateDebut = situationFamille.dateDebut;
		this.dateFin = situationFamille.dateFin;
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAPY0Edyw0I40oDFBsg?GETTER"
	 */
	@Override
	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAPY0Edyw0I40oDFBsg?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAvY0Edyw0I40oDFBsg?GETTER"
	 */
	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vf0EAvY0Edyw0I40oDFBsg?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the nombreEnfants
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0TCpoOxIEdycMumkNMs2uQ?GETTER"
	 */
	@Column(name = "NOMBRE_ENFANTS", nullable = false)
	public int getNombreEnfants() {
		// begin-user-code
		return nombreEnfants;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNombreEnfants the nombreEnfants to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0TCpoOxIEdycMumkNMs2uQ?SETTER"
	 */
	public void setNombreEnfants(int theNombreEnfants) {
		// begin-user-code
		nombreEnfants = theNombreEnfants;
		// end-user-code
	}

	@Column(name = "ETAT_CIVIL", length = LengthConstants.SITUATIONFAMILLE_ETATCIVIL)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatCivilUserType")
	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public void setEtatCivil(EtatCivil etatCivil) {
		this.etatCivil = etatCivil;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return contribuable == null ? null : Collections.singletonList(contribuable);
	}
}
