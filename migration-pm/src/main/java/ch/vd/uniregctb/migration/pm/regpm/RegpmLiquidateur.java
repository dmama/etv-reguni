package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "LIQUIDATEUR")
@TypeDefs({
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		          @TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
          })
public class RegpmLiquidateur extends RegpmEntity implements WithLongId {

	private Long id;
	private RegDate dateAttributionMandat;
	private boolean rectifiee;
	private RegpmCollectiviteAdministrative collectiviteAdministrative;
	private RegpmIndividu individu;
	private RegpmEntreprise entreprise;
	private RegpmEtablissement etablissement;

	@Id
	@Column(name = "NO_LIQUIDATEUR")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DA_ATTRIB_MANDAT")
	@Type(type = "RegDate")
	public RegDate getDateAttributionMandat() {
		return dateAttributionMandat;
	}

	public void setDateAttributionMandat(RegDate dateAttributionMandat) {
		this.dateAttributionMandat = dateAttributionMandat;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}

	@ManyToOne
	@JoinColumn(name = "FK_COLADMNO")
	public RegpmCollectiviteAdministrative getCollectiviteAdministrative() {
		return collectiviteAdministrative;
	}

	public void setCollectiviteAdministrative(RegpmCollectiviteAdministrative collectiviteAdministrative) {
		this.collectiviteAdministrative = collectiviteAdministrative;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INDNO")
	public RegpmIndividu getIndividu() {
		return individu;
	}

	public void setIndividu(RegpmIndividu individu) {
		this.individu = individu;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ENTPRNO")
	public RegpmEntreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(RegpmEntreprise entreprise) {
		this.entreprise = entreprise;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ETABNO")
	public RegpmEtablissement getEtablissement() {
		return etablissement;
	}

	public void setEtablissement(RegpmEtablissement etablissement) {
		this.etablissement = etablissement;
	}
}
