package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Entity
@Table(name = "TACHE")
@org.hibernate.annotations.Table(appliesTo = "TACHE", indexes = {
	@Index(name = "IDX_TACHE_ANNULATION_DATE", columnNames = {
		"ANNULATION_DATE"
	})
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TACHE_TYPE", discriminatorType = DiscriminatorType.STRING, length = LengthConstants.TACHE_TYPE)
public abstract class Tache extends HibernateEntity {

	private Long id;
	private RegDate dateEcheance;
	private TypeEtatTache etat;
	private Contribuable contribuable;
	private CollectiviteAdministrative collectiviteAdministrativeAssignee;
	private String commentaire;

	// Ce constructeur est requis par Hibernate
	protected Tache() {
	}

	/**
	 * Contructeur de tâche
	 * @param etat etat de la tâche à la construction
	 * @param dateEcheance date à partir de laquelle les OID voient cette tâche (si null -> dimanche prochain)
	 * @param contribuable contribuable à associer à la tâche
	 * @param collectivite la collectivité administrative (généralement un OID) à qui la tâche est assignée
	 */
	public Tache(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative collectivite) {
		this.etat = etat;
		this.contribuable = contribuable;
		this.collectiviteAdministrativeAssignee = collectivite;

		if (dateEcheance == null) {
			dateEcheance = getDefaultEcheance(RegDate.get());
		}
		this.dateEcheance = dateEcheance;
	}

	/**
	 * @param today date de référence
	 * @return le dimanche qui suit la date passée en paramètre (ou cette date si c'est déjà un dimanche)
	 */
	public static RegDate getDefaultEcheance(RegDate today) {
		// [UNIREG-1987] on place l'échéance de la tâche à dimanche prochain
		final RegDate.WeekDay jour = today.getWeekDay();
		return today.addDays(RegDate.WeekDay.SUNDAY.ordinal() - jour.ordinal());
	}

	public Tache(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable) {
		this(etat, dateEcheance, contribuable,null);
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

	@Column(name = "DATE_ECHEANCE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	@Index(name = "IDX_TACHE_DATE_ECH")
	public RegDate getDateEcheance() {
		return dateEcheance;
	}

	public void setDateEcheance(RegDate theDateEcheance) {
		dateEcheance = theDateEcheance;
	}

	@Column(name = "ETAT", length = LengthConstants.TACHE_ETAT, nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.TypeEtatTacheUserType")
	@Index(name = "IDX_TACHE_ETAT")
	public TypeEtatTache getEtat() {
		return etat;
	}

	public void setEtat(TypeEtatTache theEtat) {
		etat = theEtat;
	}

	@ManyToOne
	// msi: pas de cascade, parce qu'on veut pouvoir ajouter une tâche à un tiers sans automatiquement modifier celui-ci (perfs)
	@JoinColumn(name = "CTB_ID", foreignKey = @ForeignKey(name = "FK_TACH_CTB_ID"))
	@Index(name = "IDX_TACHE_CTB_ID", columnNames = "CTB_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable theContribuable) {
		contribuable = theContribuable;
	}

    @ManyToOne
	// msi-bnm: pas de cascade, parce qu'on veut pouvoir ajouter une tâche à une collectivitée sans automatiquement modifier celle-ci (perfs)
	@JoinColumn(name = "CA_ID", foreignKey = @ForeignKey(name = "FK_TACH_CA_ID"))
	public CollectiviteAdministrative getCollectiviteAdministrativeAssignee() {
		return collectiviteAdministrativeAssignee;
	}

	public void setCollectiviteAdministrativeAssignee(CollectiviteAdministrative theCollectiviteAdministrativeAssignee) {
		collectiviteAdministrativeAssignee = theCollectiviteAdministrativeAssignee;
	}

	@Column(name = "COMMENTAIRE", length = LengthConstants.TACHE_COMMENTAIRE)
	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	/**
	 * @return le type de tâche de l'instance concrète.
	 */
	@Transient
	public abstract TypeTache getTypeTache();
}
