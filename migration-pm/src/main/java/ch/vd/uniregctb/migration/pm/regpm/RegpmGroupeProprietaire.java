package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.TypeGroupeProprietaireUserType;

@Entity
@Table(name = "GRP_PROPRIET")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "TypeGroupeProprietaire", typeClass = TypeGroupeProprietaireUserType.class)
})
public class RegpmGroupeProprietaire extends RegpmEntity implements WithLongId {

	private Long id;
	private String nom;
	private RegDate dateConstitution;
	private RegDate dateDissolution;
	private RegpmTypeGroupeProprietaire type;
	private Set<RegpmRattachementProprietaire> rattachementsProprietaires;

	@Id
	@Column(name = "NUMERO")
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NOM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "DA_CONSTITUTION")
	@Type(type = "RegDate")
	public RegDate getDateConstitution() {
		return dateConstitution;
	}

	public void setDateConstitution(RegDate dateConstitution) {
		this.dateConstitution = dateConstitution;
	}

	@Column(name = "DA_DISSOLUTION")
	@Type(type = "RegDate")
	public RegDate getDateDissolution() {
		return dateDissolution;
	}

	public void setDateDissolution(RegDate dateDissolution) {
		this.dateDissolution = dateDissolution;
	}

	@Column(name = "FK_TYGRPPROPNO")
	@Type(type = "TypeGroupeProprietaire")
	public RegpmTypeGroupeProprietaire getType() {
		return type;
	}

	public void setType(RegpmTypeGroupeProprietaire type) {
		this.type = type;
	}

	@OneToMany
	@JoinColumn(name = "FK_GRPPROPNO")
	public Set<RegpmRattachementProprietaire> getRattachementsProprietaires() {
		return rattachementsProprietaires;
	}

	public void setRattachementsProprietaires(Set<RegpmRattachementProprietaire> rattachementsProprietaires) {
		this.rattachementsProprietaires = rattachementsProprietaires;
	}
}
