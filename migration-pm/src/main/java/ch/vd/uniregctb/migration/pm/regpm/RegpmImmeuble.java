package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "IMMEUBLE")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class)
})
public class RegpmImmeuble extends RegpmEntity implements WithLongId {

	private Long id;
	private String noParcelle;
	private String codeRegistreFoncier;
	private BigDecimal partPPE;
	private String lotPPE;
	private String descriptionLotPPE1;
	private String descriptionLotPPE2;
	private String descriptionLotPPE3;
	private RegDate dateRadiationPPE;
	private RegpmImmeuble parcelle;
	private RegpmCommune commune;

	@Id
	@Column(name = "NUMERO")
	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NO_PARCELLE_COMM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(String noParcelle) {
		this.noParcelle = noParcelle;
	}

	@Column(name = "CO_REG_FONCIER")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getCodeRegistreFoncier() {
		return codeRegistreFoncier;
	}

	public void setCodeRegistreFoncier(String codeRegistreFoncier) {
		this.codeRegistreFoncier = codeRegistreFoncier;
	}

	@Column(name = "PART_PPE", length = 6, precision = 2)
	public BigDecimal getPartPPE() {
		return partPPE;
	}

	public void setPartPPE(BigDecimal partPPE) {
		this.partPPE = partPPE;
	}

	@Column(name = "NO_LOT_PPE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getLotPPE() {
		return lotPPE;
	}

	public void setLotPPE(String lotPPE) {
		this.lotPPE = lotPPE;
	}

	@Column(name = "DESC_LOT_PPE_1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getDescriptionLotPPE1() {
		return descriptionLotPPE1;
	}

	public void setDescriptionLotPPE1(String descriptionLotPPE1) {
		this.descriptionLotPPE1 = descriptionLotPPE1;
	}

	@Column(name = "DESC_LOT_PPE_2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getDescriptionLotPPE2() {
		return descriptionLotPPE2;
	}

	public void setDescriptionLotPPE2(String descriptionLotPPE2) {
		this.descriptionLotPPE2 = descriptionLotPPE2;
	}

	@Column(name = "DESC_LOT_PPE_3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getDescriptionLotPPE3() {
		return descriptionLotPPE3;
	}

	public void setDescriptionLotPPE3(String descriptionLotPPE3) {
		this.descriptionLotPPE3 = descriptionLotPPE3;
	}

	@Column(name = "DA_RADIATION_PPE")
	@Type(type = "RegDate")
	public RegDate getDateRadiationPPE() {
		return dateRadiationPPE;
	}

	public void setDateRadiationPPE(RegDate dateRadiationPPE) {
		this.dateRadiationPPE = dateRadiationPPE;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_IMMNO")
	public RegpmImmeuble getParcelle() {
		return parcelle;
	}

	public void setParcelle(RegpmImmeuble parcelle) {
		this.parcelle = parcelle;
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
