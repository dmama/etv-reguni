package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TaxLiabilityControlEchec {

	public static enum EchecType {
		CONTROLE_NUMERO_KO,
		AUCUN_MC_ASSOCIE_TROUVE,
		UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES,
		PLUSIEURS_MC_ASSUJETTI_TROUVES,
		CONTROLE_SUR_PARENTS_KO
	}

	private EchecType type;
	private List<Long> MenageCommunIds;
	private List<Long> parentsIds;
	private List<Long> MenageCommunParentsIds;

	public TaxLiabilityControlEchec(EchecType type) {
		this.type = type;
	}

	public List<Long> getMenageCommunIds() {
		return MenageCommunIds;
	}

	public void setMenageCommunIds(List<Long> menageCommunIds) {
		MenageCommunIds = menageCommunIds;
	}

	public List<Long> getParentsIds() {
		return parentsIds;
	}

	public void setParentsIds(List<Long> parentsIds) {
		this.parentsIds = parentsIds;
	}

	public List<Long> getMenageCommunParentsIds() {
		return MenageCommunParentsIds;
	}

	public void setMenageCommunParentsIds(List<Long> menageCommunParentsIds) {
		MenageCommunParentsIds = menageCommunParentsIds;
	}

	public EchecType getType() {
		return type;
	}

	public void setType(EchecType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
