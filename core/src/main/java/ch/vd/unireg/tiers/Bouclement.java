package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.type.DayMonth;

@Entity
@Table(name = "BOUCLEMENT", indexes = @Index(name = "IDX_BOUCLEMENT_ENTR_ID", columnList = "ENTREPRISE_ID"))
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
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "ANCRAGE", nullable = false)
	@Type(type = "ch.vd.unireg.hibernate.DayMonthUserType")
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
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_BOUCLEMENT_ENTR_ID"))
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}
}
