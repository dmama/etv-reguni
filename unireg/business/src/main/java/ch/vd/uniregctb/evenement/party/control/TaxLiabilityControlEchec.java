package ch.vd.uniregctb.evenement.party.control;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TaxLiabilityControlEchec {

	public enum EchecType {
		CONTROLE_NUMERO_KO,
		AUCUN_MC_ASSOCIE_TROUVE,
		UN_PLUSIEURS_MC_NON_ASSUJETTI_TROUVES,
		PLUSIEURS_MC_ASSUJETTI_TROUVES,
		CONTROLE_SUR_PARENTS_KO,
		DATE_OU_PF_DANS_FUTUR
	}

	private EchecType type;
	private List<Long> menageCommunIds;
	private List<Long> parentsIds;
	private List<Long> menageCommunParentsIds;
	private boolean assujetissementNonConforme;

	public TaxLiabilityControlEchec(EchecType type) {
		this.type = type;
	}

	public List<Long> getMenageCommunIds() {
		return menageCommunIds;
	}

	public void setMenageCommunIds(List<Long> menageCommunIds) {
		this.menageCommunIds = menageCommunIds;
	}

	public List<Long> getParentsIds() {
		return parentsIds;
	}

	public void setParentsIds(List<Long> parentsIds) {
		this.parentsIds = parentsIds;
	}

	public List<Long> getMenageCommunParentsIds() {
		return menageCommunParentsIds;
	}

	public void setMenageCommunParentsIds(List<Long> menageCommunParentsIds) {
		this.menageCommunParentsIds = menageCommunParentsIds;
	}

	public EchecType getType() {
		return type;
	}

	public void setType(EchecType type) {
		this.type = type;
	}

	public boolean isAssujetissementNonConforme() {
		return assujetissementNonConforme;
	}

	public void setAssujetissementNonConforme(boolean assujetissementNonConforme) {
		this.assujetissementNonConforme = assujetissementNonConforme;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
