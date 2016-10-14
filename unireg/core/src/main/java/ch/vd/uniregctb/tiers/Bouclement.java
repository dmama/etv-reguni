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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.type.DayMonth;

@Entity
@Table(name = "BOUCLEMENT")
public class Bouclement extends HibernateEntity implements LinkedEntity {

	private Long id;
	private RegDate dateDebut;
	private DayMonth ancrage;
	private int periodeMois;
	private Entreprise entreprise;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "ANCRAGE", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.DayMonthUserType")
	public DayMonth getAncrage() {
		return ancrage;
	}

	public void setAncrage(DayMonth ancrage) {
		this.ancrage = ancrage;
	}

	@Column(name = "PERIODE_MOIS", length = 2, nullable = false)
	public int getPeriodeMois() {
		return periodeMois;
	}

	public void setPeriodeMois(int periodeMois) {
		this.periodeMois = periodeMois;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false)
	@Index(name = "IDX_BOUCLEMENT_ENTR_ID", columnNames = "ENTREPRISE_ID")
	@ForeignKey(name = "FK_BOUCLEMENT_ENTR_ID")
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
