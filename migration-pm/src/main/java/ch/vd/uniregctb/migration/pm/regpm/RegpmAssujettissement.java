package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeAssujettissementUserType;

@Entity
@Table(name = "ASSUJETTIS_PM")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "TypeAssujettissement", typeClass = TypeAssujettissementUserType.class)
})
public class RegpmAssujettissement extends RegpmEntity implements Comparable<RegpmAssujettissement>, WithLongId {

	private Long id;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegDate dateEnvoiLettre;
	private RegpmTypeAssujettissement type;

	@Override
	public int compareTo(@NotNull RegpmAssujettissement o) {
		int comparison = NullDateBehavior.EARLIEST.compare(dateDebut, o.dateDebut);
		if (comparison == 0) {
			comparison = Long.compare(id, o.id);
		}
		return comparison;
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

	@Column(name = "DA_ENVOI_LETTRE")
	@Type(type = "RegDate")
	public RegDate getDateEnvoiLettre() {
		return dateEnvoiLettre;
	}

	public void setDateEnvoiLettre(RegDate dateEnvoiLettre) {
		this.dateEnvoiLettre = dateEnvoiLettre;
	}

	@Column(name = "FK_TYASSUJNO")
	@Type(type = "TypeAssujettissement")
	public RegpmTypeAssujettissement getType() {
		return type;
	}

	public void setType(RegpmTypeAssujettissement type) {
		this.type = type;
	}
}
