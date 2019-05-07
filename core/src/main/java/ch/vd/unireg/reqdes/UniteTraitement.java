package ch.vd.unireg.reqdes;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "REQDES_UNITE_TRAITEMENT")
public class UniteTraitement extends HibernateEntity {

	private Long id;
	private EtatTraitement etat;
	private Date dateTraitement;
	private Set<PartiePrenante> partiesPrenantes;
	private Set<ErreurTraitement> erreurs;
	private EvenementReqDes evenement;

	@Transient
	@Override
	public Object getKey() {
		return getId();
	}

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ETAT", length = LengthConstants.REQDES_ETAT_TRAITEMENT, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public EtatTraitement getEtat() {
		return etat;
	}

	public void setEtat(EtatTraitement etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_TRAITEMENT")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "uniteTraitement")
	public Set<PartiePrenante> getPartiesPrenantes() {
		return partiesPrenantes;
	}

	public void setPartiesPrenantes(Set<PartiePrenante> partiesPrenantes) {
		this.partiesPrenantes = partiesPrenantes;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "UNITE_TRAITEMENT_ID", nullable = false)
	@ForeignKey(name = "FK_REQDES_UT_ERR_UT_ID")
	public Set<ErreurTraitement> getErreurs() {
		return erreurs;
	}

	public void setErreurs(Set<ErreurTraitement> erreurs) {
		this.erreurs = erreurs;
	}

	@ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
	@JoinColumn(name = "EVENEMENT_ID", nullable = false)
	@ForeignKey(name = "FK_REQDES_UT_EVT_ID")
	public EvenementReqDes getEvenement() {
		return evenement;
	}

	public void setEvenement(EvenementReqDes evenement) {
		this.evenement = evenement;
	}
}
