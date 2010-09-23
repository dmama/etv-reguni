package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
//TODO(BNM) Reflechir a une implementation plus simple:
// ajouter une propriété PeriodicitéDecompte et supprimer toutes la hierarchie de classe

@Entity
@Table(name = "PERIODICITE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Periodicite extends HibernateEntity implements CollatableDateRange, Validateable, LinkedEntity {

	//private static final Logger LOGGER = Logger.getLogger(Periodicite.class);

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
	 * Date de fin de la périodicité
	 */
	private RegDate dateFin;


	private PeriodiciteDecompte periodiciteDecompte;


	private PeriodeDecompte periodeDecompte;

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

	public Periodicite(PeriodiciteDecompte periodiciteDecompte) {
		this.periodiciteDecompte = periodiciteDecompte;
	}

	public Periodicite(PeriodiciteDecompte periodiciteDecompte, PeriodeDecompte periodeDecompte, RegDate dateDebut, RegDate dateFin) {
		this.periodiciteDecompte = periodiciteDecompte;
		this.periodeDecompte = periodeDecompte;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public Periodicite() {
	}

	public Periodicite(Periodicite periodicite) {
		this(periodicite.getPeriodiciteDecompte(), periodicite.getPeriodeDecompte(), periodicite.getDateDebut(), periodicite.getDateFin());
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
	 * @param id the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	/**
	 * Calcule la date de début de la période. La période est déterminée par une date de référence située n'importe quand dans la période considérée.
	 *
	 * @param reference la date de référence contenue dans la période considérée
	 * @return le début de la période
	 */
	public RegDate getDebutPeriode(RegDate reference) {
		return periodiciteDecompte.getDebutPeriode(reference);
	}

	/**
	 * Calcule la date de fin de la période. La période est déterminée par une date de référence située n'importe quand dans la période considérée.
	 *
	 * @param reference la date de référence contenue dans la période considérée
	 * @return la fin de la période
	 */
	public final RegDate getFinPeriode(RegDate reference) {
		return periodiciteDecompte.getFinPeriode(reference);
	}


	/**
	 * Calcule la date de début de la période suivant la période indiquée par la date de référence (c'est le lendemain de la fin de la période indiquée)
	 *
	 * @param reference
	 * @return le début de la période suivante
	 */
	public RegDate getDebutPeriodeSuivante(RegDate reference) {
		return periodiciteDecompte.getDebutPeriodeSuivante(reference);
	}

	@Column(name = "PERIODICITE_TYPE", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodiciteDecompte() {
		return periodiciteDecompte;
	}

	public void setPeriodiciteDecompte(PeriodiciteDecompte periodiciteDecompte) {
		this.periodiciteDecompte = periodiciteDecompte;
	}

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

	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next) && periodiciteDecompte.equals(((Periodicite) next).getPeriodiciteDecompte());
	}

	public DateRange collate(DateRange next) {
		return new Periodicite(periodiciteDecompte, periodeDecompte, dateDebut, next.getDateFin());
	}

	public static List<Periodicite> comblerVidesPeriodicites(List<Periodicite> periodicites) {
		// la périodicité actuelle doit avoir une date de fin null
		final int lastIndex = periodicites.size() - 1;
		if (lastIndex >= 0) {
			Periodicite periodiciteActive = periodicites.get(lastIndex);
			periodiciteActive.setDateFin(null);
		}

		for (int i = periodicites.size() - 1; i > 0; --i) {
			Periodicite periodiciteCourante = periodicites.get(i);
			Periodicite periodicitePrecedente = periodicites.get(i - 1);
			final RegDate debut = periodiciteCourante.getDateDebut();
			final RegDate finPrecedent = periodicitePrecedente.getDateFin();
			if (debut != null && NullDateBehavior.EARLIEST.compare(debut.getOneDayBefore(), finPrecedent) > 0) {
				periodicitePrecedente.setDateFin(debut.getOneDayBefore());
			}
		}

		return periodicites;
	}

	@Column(name = "PERIODE_DECOMPTE", length = LengthConstants.DPI_PERIODE_DECOMPTE)
	@Type(type = "ch.vd.uniregctb.hibernate.PeriodeDecompteUserType")
	public PeriodeDecompte getPeriodeDecompte() {
		return periodeDecompte;
	}

	public void setPeriodeDecompte(PeriodeDecompte periodeDecompte) {
		this.periodeDecompte = periodeDecompte;
	}

	public ValidationResults validate() {
		final ValidationResults results = new ValidationResults();

		if (isAnnule()) {
			return results;
		}

		DateRangeHelper.validate(this, false, true, results);

		if (periodiciteDecompte == null) {
			results.addError("La périodicité de décompte doit être renseignée.");
		}
		if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE && periodeDecompte == null) {
			results.addError("La période de décompte doit être renseignée lorsque la périodicité de décompte est UNIQUE.");
		}
		return results;
	}

	@Transient
	public List<?> getLinkedEntities() {
		return debiteur == null ? null : Arrays.asList(debiteur);
	}
}
