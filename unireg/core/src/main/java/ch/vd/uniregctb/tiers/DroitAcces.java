package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

/**
 * Représente l'autorisation ou l'interdiction d'accès entre un opérateur (représenté par son numéro d'individu) et un dossier (représenté
 * par son contribuable personne physique).
 * <p>
 * La sécurité des dossiers est une couche spécialisée qui s'ajoute au contrôle d'accès fait par IFOSEC. IFOSEC gère le contrôle d'accès de
 * manière global sur l'application en fonction de l'opérateur connecté, alors que le contrôle des dossiers gère l'accès particuliers à
 * certains dossiers sensibles (personnes célèbres, politiciens, forfaits fiscaux, ...). Il est recommendé de lire les spécifications
 * "Vérifier l'accès d'un dossier", "Gérer l'accès à un dossier" et "Tenir le registre des tiers" avant toutes choses.
 * <p>
 * Le principe base est que pour une personne physique :
 * <ul>
 * <li><b>sans autorisation ni interdiction</b> : tous les opérateurs peuvent y accèder en lecture et en écriture (pour autant que les
 * droits IFOSEC le permettent)</li>
 * <li><b>avec des droits d'accès de type 'autorisation'</b> : seuls les opérateurs autorisés peuvent y accéder. Les autres opérateurs
 * perdent implicitement le droit d'accès.</li>
 * <li><b>avec des droits d'accès de type 'interdiction'</b> : les opérateurs spécifiés perdent le droit d'accès. Les autres opérateurs ne
 * sont pas impactés.</li>
 * </ul>
 * <p>
 * A noter qu'il existe des passes-droits pour les membres de la direction de l'ACI.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@Table(name = "DROIT_ACCES")
public class DroitAcces extends HibernateEntity implements DateRange, Duplicable<DroitAcces>, Validateable {

	private static final long serialVersionUID = 5947849643843925215L;

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private long noIndividuOperateur;
	private TypeDroitAcces type;
	private Niveau niveau;
	private PersonnePhysique tiers;

	public DroitAcces() {
		// pour hibernate
	}

	private DroitAcces(DroitAcces right) {
		this.dateDebut = right.dateDebut;
		this.dateFin = right.dateFin;
		this.noIndividuOperateur = right.noIndividuOperateur;
		this.type = right.type;
		this.niveau = right.niveau;
		this.tiers = right.tiers;
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

	@Column(name = "DATE_DEBUT", nullable = false)
	@org.hibernate.annotations.Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DATE_FIN", nullable = true)
	@org.hibernate.annotations.Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "NUMERO_IND_OPER", nullable = false)
	@Index(name = "IDX_NUMERO_IND_OPER")
	public long getNoIndividuOperateur() {
		return noIndividuOperateur;
	}

	public void setNoIndividuOperateur(long noIndividuOperateur) {
		this.noIndividuOperateur = noIndividuOperateur;
	}

	@Column(name = "TYPE", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeDroitAccesUserType")
	public TypeDroitAcces getType() {
		return type;
	}

	public void setType(TypeDroitAcces type) {
		this.type = type;
	}

	@Column(name = "NIVEAU", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.NiveauUserType")
	public Niveau getNiveau() {
		return niveau;
	}

	public void setNiveau(Niveau niveau) {
		this.niveau = niveau;
	}

	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter un droit d'accès à un tiers sans automatiquement modifier celui-ci (perfs + audit)
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@Index(name = "IDX_DA_TIERS_ID", columnNames = "TIERS_ID")
	public PersonnePhysique getTiers() {
		return tiers;
	}

	public void setTiers(PersonnePhysique tiers) {
		this.tiers = tiers;
	}

	/**
	 * {@inheritDoc}
	 */
	public ValidationResults validate() {

		ValidationResults results = new ValidationResults();

		if (isAnnule()) {
			return results;
		}

		// La date de début doit être renseignée
		if (dateDebut == null) {
			results.addError("Le droit d'accès " + toString() + " possède une date de début nulle");
		}

		// Date de début doit être avant ou égale à la date de fin
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin) && !isAnnule()) {
			results.addError("Le droit d'accès " + toString() + " possède une date de début qui est après la date de fin: début = " + dateDebut + " fin = " + dateFin + "");
		}

		return results;
	}

	@Override
	public String toString() {
		return "DroitAcces{" +
				"id=" + id +
				", dateDebut=" + dateDebut +
				", dateFin=" + dateFin +
				", noIndividuOperateur=" + noIndividuOperateur +
				", type=" + type +
				", niveau=" + niveau +
				", tiers=" + tiers +
				'}';
	}

	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public DroitAcces duplicate() {
		return new DroitAcces(this);
	}
}
