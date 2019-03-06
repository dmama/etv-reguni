package ch.vd.unireg.tiers;

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

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

/**
 * Représente l'autorisation ou l'interdiction d'accès entre un opérateur (représenté par son numéro d'individu) et un dossier (représenté
 * par son contribuable personne physique ou d'entreprise).
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
public class DroitAcces extends HibernateDateRangeEntity implements Duplicable<DroitAcces> {

	private Long id;
	private String visaOperateur;
	private TypeDroitAcces type;
	private Niveau niveau;
	private Contribuable tiers;

	public DroitAcces() {
		// pour hibernate
	}

	private DroitAcces(DroitAcces right) {
		super(right);
		this.visaOperateur = right.visaOperateur;
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

	@Column(name = "VISA_OPERATEUR", length = 25)
	@Index(name = "IDX_VISA_OPERATEUR")
	public String getVisaOperateur() {
		return visaOperateur;
	}

	public void setVisaOperateur(String visaOperateur) {
		this.visaOperateur = (visaOperateur == null ? null : visaOperateur.toLowerCase());  // le visa est toujours stocké en minuscules
	}

	@Column(name = "TYPE", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.TypeDroitAccesUserType")
	public TypeDroitAcces getType() {
		return type;
	}

	public void setType(TypeDroitAcces type) {
		this.type = type;
	}

	@Column(name = "NIVEAU", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.NiveauUserType")
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
	public Contribuable getTiers() {
		return tiers;
	}

	public void setTiers(Contribuable tiers) {
		this.tiers = tiers;
	}

	@Override
	public String toString() {
		return "DroitAcces{" +
				"id=" + id +
				", dateDebut=" + RegDateHelper.dateToDisplayString(getDateDebut()) +
				", dateFin=" + RegDateHelper.dateToDisplayString(getDateFin()) +
				", visaOperateur=" + visaOperateur +
				", type=" + type +
				", niveau=" + niveau +
				", tiers=" + tiers +
				'}';
	}

	@Override
	public DroitAcces duplicate() {
		return new DroitAcces(this);
	}
}
