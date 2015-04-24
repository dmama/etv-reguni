package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "COLLECTIVITE_ADM")
@TypeDefs({
		          @TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		          @TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
          })
public class RegpmCollectiviteAdministrative extends RegpmEntity implements WithLongId{

	private Long id;
	private String nomCourt;
	private String nomComplet1;
	private String nomComplet2;
	private String nomComplet3;
	private String sigle;
	private RegDate dateFinValidite;
	private String casePostale;
	private String numeroPolice;
	private String numeroTelephone;
	private String nomRue;
	private String noCCP;
	private String noAdherent;
	private String typeCommunication;
	private String numeroFax;
	private RegpmRue rue;
	private RegpmLocalitePostale localitePostale;
	private RegpmTypeCollectiviteAdministrative typeCollectivite;
	private RegpmCanton canton;

	@Id
	@Column(name = "NO_COL_ADM")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NOM_COURT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getNomCourt() {
		return nomCourt;
	}

	public void setNomCourt(String nomCourt) {
		this.nomCourt = nomCourt;
	}

	@Column(name = "NOM_COMPLET_1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getNomComplet1() {
		return nomComplet1;
	}

	public void setNomComplet1(String nomComplet1) {
		this.nomComplet1 = nomComplet1;
	}

	@Column(name = "NOM_COMPLET_2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getNomComplet2() {
		return nomComplet2;
	}

	public void setNomComplet2(String nomComplet2) {
		this.nomComplet2 = nomComplet2;
	}

	@Column(name = "NOM_COMPLET_3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "40"))
	public String getNomComplet3() {
		return nomComplet3;
	}

	public void setNomComplet3(String nomComplet3) {
		this.nomComplet3 = nomComplet3;
	}

	@Column(name = "SIGLE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "7"))
	public String getSigle() {
		return sigle;
	}

	public void setSigle(String sigle) {
		this.sigle = sigle;
	}

	@Column(name = "DAF_VALIDITE")
	@Type(type = "RegDate")
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Column(name = "CASE_POSTALE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getCasePostale() {
		return casePostale;
	}

	public void setCasePostale(String casePostale) {
		this.casePostale = casePostale;
	}

	@Column(name = "NO_POLICE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "5"))
	public String getNumeroPolice() {
		return numeroPolice;
	}

	public void setNumeroPolice(String numeroPolice) {
		this.numeroPolice = numeroPolice;
	}

	@Column(name = "NO_TELEPHONE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNumeroTelephone() {
		return numeroTelephone;
	}

	public void setNumeroTelephone(String numeroTelephone) {
		this.numeroTelephone = numeroTelephone;
	}

	@Column(name = "RUE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomRue() {
		return nomRue;
	}

	public void setNomRue(String nomRue) {
		this.nomRue = nomRue;
	}

	@Column(name = "NO_CCP")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getNoCCP() {
		return noCCP;
	}

	public void setNoCCP(String noCCP) {
		this.noCCP = noCCP;
	}

	@Column(name = "NO_ADHERENT")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "15"))
	public String getNoAdherent() {
		return noAdherent;
	}

	public void setNoAdherent(String noAdherent) {
		this.noAdherent = noAdherent;
	}

	@Column(name = "TY_COMMUNICATION")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getTypeCommunication() {
		return typeCommunication;
	}

	public void setTypeCommunication(String typeCommunication) {
		this.typeCommunication = typeCommunication;
	}

	@Column(name = "NO_FAX")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "25"))
	public String getNumeroFax() {
		return numeroFax;
	}

	public void setNumeroFax(String numeroFax) {
		this.numeroFax = numeroFax;
	}

	@ManyToOne
	@JoinColumn(name = "FK_RUENO")
	public RegpmRue getRue() {
		return rue;
	}

	public void setRue(RegpmRue rue) {
		this.rue = rue;
	}

	@ManyToOne
	@JoinColumn(name = "FK_LOC_POSTNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}

	@ManyToOne
	@JoinColumn(name = "FK_TY_COL_ADMNO")
	public RegpmTypeCollectiviteAdministrative getTypeCollectivite() {
		return typeCollectivite;
	}

	public void setTypeCollectivite(RegpmTypeCollectiviteAdministrative typeCollectivite) {
		this.typeCollectivite = typeCollectivite;
	}

	@Column(name = "FK_CANTONSIGLE")
	@Enumerated(EnumType.STRING)
	public RegpmCanton getCanton() {
		return canton;
	}

	public void setCanton(RegpmCanton canton) {
		this.canton = canton;
	}
}
