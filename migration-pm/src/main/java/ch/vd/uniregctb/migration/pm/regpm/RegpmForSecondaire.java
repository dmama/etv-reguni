package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "FOR_SECONDAIRE_PM")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
          })
public class RegpmForSecondaire extends RegpmEntity implements DateRange, WithLongId {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegpmCommune commune;

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Id
	@Column(name = "NO_TECHNIQUE")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DAD_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DAF_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@ManyToOne
	@JoinColumn(name = "FK_COMMUNENO")
	public RegpmCommune getCommune() {
		return commune;
	}

	public void setCommune(RegpmCommune commune) {
		this.commune = commune;
	}
}
