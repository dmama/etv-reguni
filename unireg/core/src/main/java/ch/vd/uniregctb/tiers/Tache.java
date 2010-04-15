package ch.vd.uniregctb.tiers;

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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_O-fRgFjGEd2uSoZKEkgcsw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_O-fRgFjGEd2uSoZKEkgcsw"
 */
@Entity
@Table(name = "TACHE")
@org.hibernate.annotations.Table(appliesTo = "TACHE", indexes = {
	@Index(name = "IDX_TACHE_ANNULATION_DATE", columnNames = {
		"ANNULATION_DATE"
	})
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TACHE_TYPE", discriminatorType = DiscriminatorType.STRING, length = LengthConstants.TACHE_TYPE)
public abstract class Tache extends HibernateEntity implements Validateable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_a3vXcFjGEd2uSoZKEkgcsw"
	 */
	private RegDate dateEcheance;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZlMKcFjGEd2uSoZKEkgcsw"
	 */
	private TypeEtatTache etat;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LbWQAFjIEd2uSoZKEkgcsw"
	 */
	private Contribuable contribuable;

    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Y1A-0HtaEd6oM4x3TyifiA"
	 */
	private CollectiviteAdministrative collectiviteAdministrativeAssignee;

	// Ce constructeur est requis par Hibernate
	protected Tache() {
	}

	/**
	 * Contructeur de tâche
	 * @param etat etat de la tâche à la construction
	 * @param dateEcheance date à partir de laquelle les OID voient cette tâche (si null -> dimanche prochain)
	 * @param contribuable contribuable à associer à la tâche
	 * @param collectiviteAdministrativeAssignee la collectivité administrative (généralement un OID) à qui la tâche est assignée
	 */
	public Tache(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		this.etat = etat;
		this.contribuable = contribuable;
		this.collectiviteAdministrativeAssignee = collectiviteAdministrativeAssignee;

		if (dateEcheance == null) {
			// [UNIREG-1987] on place l'échéance de la tâche à dimanche prochain
			final RegDate aujourdhui = RegDate.get();
			final RegDate.WeekDay jour = aujourdhui.getWeekDay();
			dateEcheance = aujourdhui.addDays(RegDate.WeekDay.SUNDAY.ordinal() - jour.ordinal());
		}
		this.dateEcheance = dateEcheance;
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateEcheance
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_a3vXcFjGEd2uSoZKEkgcsw?GETTER"
	 */
	@Column(name = "DATE_ECHEANCE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	@Index(name = "IDX_TACHE_DATE_ECH")
	public RegDate getDateEcheance() {
		// begin-user-code
		return dateEcheance;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateEcheance the dateEcheance to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_a3vXcFjGEd2uSoZKEkgcsw?SETTER"
	 */
	public void setDateEcheance(RegDate theDateEcheance) {
		// begin-user-code
		dateEcheance = theDateEcheance;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the etat
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZlMKcFjGEd2uSoZKEkgcsw?GETTER"
	 */
	@Column(name = "ETAT", length = LengthConstants.TACHE_ETAT, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEtatTacheUserType")
	@Index(name = "IDX_TACHE_ETAT")
	public TypeEtatTache getEtat() {
		// begin-user-code
		return etat;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theEtat the etat to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ZlMKcFjGEd2uSoZKEkgcsw?SETTER"
	 */
	public void setEtat(TypeEtatTache theEtat) {
		// begin-user-code
		etat = theEtat;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the contribuable
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LbWQAFjIEd2uSoZKEkgcsw?GETTER"
	 */
	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter une tâche à un tiers sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "CTB_ID")
	@ForeignKey(name = "FK_TACH_CTB_ID")
	@Index(name = "IDX_TACHE_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getContribuable() {
		// begin-user-code
		return contribuable;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theContribuable the contribuable to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LbWQAFjIEd2uSoZKEkgcsw?SETTER"
	 */
	public void setContribuable(Contribuable theContribuable) {
		// begin-user-code
		contribuable = theContribuable;
		// end-user-code
	}


    /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the collectiviteAdministrativeAssignee
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Y1A-0HtaEd6oM4x3TyifiA?GETTER"
	 */
    @ManyToOne
	// msi-bnm: pas de cascade, parce qu'on veut pouvoir ajouter une tâche à une collectivitée sans automatiquement modifier celle-ci (perfs)
	@JoinColumn(name = "CA_ID")
	@ForeignKey(name = "FK_TACH_CA_ID")
	@Index(name = "IDX_TACHE_CA_ID", columnNames = "CA_ID")
	public CollectiviteAdministrative getCollectiviteAdministrativeAssignee() {
		// begin-user-code
		return collectiviteAdministrativeAssignee;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theCollectiviteAdministrativeAssignee the collectiviteAdministrativeAssignee to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Y1A-0HtaEd6oM4x3TyifiA?SETTER"
	 */
	public void setCollectiviteAdministrativeAssignee(
			CollectiviteAdministrative theCollectiviteAdministrativeAssignee) {
		// begin-user-code
		collectiviteAdministrativeAssignee = theCollectiviteAdministrativeAssignee;
		// end-user-code
	}
	/**
	 * @return le type de tâche de l'instance concrète.
	 */
	@Transient
	public abstract TypeTache getTypeTache();

	public ValidationResults validate() {

		ValidationResults results = new ValidationResults();

		if (collectiviteAdministrativeAssignee == null) {
			results.addError("La collectivité assignée doit être renseignée.");
		}

		return results;
	}
}
