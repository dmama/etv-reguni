package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.uniregctb.common.ComparisonHelper;

@Entity
@DiscriminatorValue("CollectiviteAdministrative")
public class CollectiviteAdministrative extends Contribuable {

	private Integer numeroCollectiviteAdministrative;
	private Integer identifiantDistrictFiscal;
	private Integer identifiantRegionFiscale;

	public CollectiviteAdministrative() {
	}

	public CollectiviteAdministrative(Long id, Integer numeroCollectiviteAdministrative, Integer identifiantDistrictFiscal, Integer identifiantRegionFiscale) {
		super(id);
		this.numeroCollectiviteAdministrative = numeroCollectiviteAdministrative;
		this.identifiantDistrictFiscal = identifiantDistrictFiscal;
		this.identifiantRegionFiscale = identifiantRegionFiscale;
	}

	@Column(name = "NUMERO_CA", unique = true)
	//@Index(name = "IDX_NUMERO_CA") -> impliqué par le 'unique = true'
	public Integer getNumeroCollectiviteAdministrative() {
		return numeroCollectiviteAdministrative;
	}

	public void setNumeroCollectiviteAdministrative(Integer theNumeroCollectiviteAdministrative) {
		numeroCollectiviteAdministrative = theNumeroCollectiviteAdministrative;
	}

	@Transient
	@Override
	public String getRoleLigne1() {
		return "Collectivité admin.";
	}

	@Transient
	@Override
	public NatureTiers getNatureTiers() {
		return NatureTiers.CollectiviteAdministrative;
	}

	@Transient
	@Override
	public TypeTiers getType() {
		return TypeTiers.COLLECTIVITE_ADMINISTRATIVE;
	}

	@Column(name = "DISTRICT_FISCAL_ID", nullable = true)
	public Integer getIdentifiantDistrictFiscal() {
		return identifiantDistrictFiscal;
	}

	public void setIdentifiantDistrictFiscal(Integer identifiantDistrictFiscal) {
		this.identifiantDistrictFiscal = identifiantDistrictFiscal;
	}

	@Column(name = "REGION_FISCALE_ID", nullable = true)
	public Integer getIdentifiantRegionFiscale() {
		return identifiantRegionFiscale;
	}

	public void setIdentifiantRegionFiscale(Integer identifiantRegionFiscale) {
		this.identifiantRegionFiscale = identifiantRegionFiscale;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equalsTo(Tiers obj) {
		if (this == obj)
			return true;
		if (!super.equalsTo(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;

		final CollectiviteAdministrative other = (CollectiviteAdministrative) obj;
		return ComparisonHelper.areEqual(numeroCollectiviteAdministrative, other.numeroCollectiviteAdministrative);
	}
}
