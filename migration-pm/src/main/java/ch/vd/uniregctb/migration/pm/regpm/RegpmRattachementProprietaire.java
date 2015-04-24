package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "RATT_PROPRIETAIRE")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmRattachementProprietaire extends RegpmEntity implements WithLongId {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegpmImmeuble immeuble;

	@Id
	@Column(name = "NUMERO")
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DAD_RATTACHEMENT")
	@Type(type = "RegDate")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DAF_RATTACHEMENT")
	@Type(type = "RegDate")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_IMMNO")
	public RegpmImmeuble getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(RegpmImmeuble immeuble) {
		this.immeuble = immeuble;
	}
}
