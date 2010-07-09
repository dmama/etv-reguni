package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
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

import org.apache.log4j.Logger;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;

@Entity
@Table(name = "PERIODICITE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PERIODICITE_TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Periodicite")
public abstract class Periodicite  extends HibernateEntity implements DateRange {

	public static final String MENSUEL = "Mensuel";
	public static final String TRIMESTRIEL = "Trimestriel";
	public static final String SEMESTRIEL = "Semestriel";
	public static final String ANNUEL = "Annuel";
	public static final String UNIQUE = "Unique";

	private static final Logger LOGGER = Logger.getLogger(Periodicite.class);


	private static final long serialVersionUID = 956376828051331469L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * Date de début de validité de la périodicité
	 */
	private RegDate dateDebut;

	/**
	 Date de fin de la périodicité
	 */
	private RegDate dateFin;


	/**
	 * Le debiteur
	 */
	private DebiteurPrestationImposable debiteur;



	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * @param theDateDebut the dateDebut to set

	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * @return the dateFin
	 */
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * @param theDateFin the dateFin to set
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	public Periodicite(){

	}

	public Periodicite(RegDate dateDebut, RegDate dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public Periodicite(Periodicite periodicite){
		this(periodicite.getDateDebut(),periodicite.getDateFin());
	}


	@Transient
	@Override
	public Object getKey() {
		return id;
	}
	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}
	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	/* Calcule la date de début de la période. La période est déterminée par une date de référence située n'importe quand dans la période
	 * considérée.
	 *
	 * @param reference
	 *            la date de référence contenue dans la période considérée
	 * @return le début de la période
	 */
	public abstract RegDate getDebutPeriode(RegDate reference);

	/**
	 * Calcule la date de fin de la période. La période est déterminée par une date de référence située n'importe quand dans la période
	 * considérée.
	 *
	 * @param reference
	 *            la date de référence contenue dans la période considérée
	 * @return la fin de la période
	 */
	public final RegDate getFinPeriode(RegDate reference) {
		return getDebutPeriodeSuivante(reference).addDays(-1);
	}

	/**
	 * Calcule la date de début de la période suivant la période indiquée par la date de référence (c'est le lendemain de la fin
	 * de la période indiquée)
	 * @param reference
	 * @return le début de la période suivante
	 */
	public abstract RegDate getDebutPeriodeSuivante(RegDate reference);


		/**
	 * return le nom de la classe du tiers
	 */
	@Transient
	public abstract String getTypePeriodicite();


	@Override
	public String toString() {
		final String dateDebutStr = dateDebut != null ? RegDateHelper.dateToDisplayString(dateDebut) : "?";
		final String dateFinStr = dateFin != null ? RegDateHelper.dateToDisplayString(dateFin) : "?";
		return String.format("%s (%s - %s)", getClass().getSimpleName(), dateDebutStr, dateFinStr);
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "DEBITEUR_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_P_DEBITEUR_ID", columnNames = "DEBITEUR_ID")	
	public DebiteurPrestationImposable getDebiteur() {
		return debiteur;
	}

	public void setDebiteur(DebiteurPrestationImposable debiteur) {
		this.debiteur = debiteur;
	}
}
