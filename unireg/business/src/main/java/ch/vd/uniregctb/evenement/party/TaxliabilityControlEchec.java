package ch.vd.uniregctb.evenement.party;

import java.util.List;

public class TaxliabilityControlEchec {
	private TaxliabilityControlEchecType type;
	private List<Long> MenageCommunIds;
	private List<Long> parentsIds;
	private List<Long> MenageCommunParentsIds;


	public TaxliabilityControlEchec(TaxliabilityControlEchecType type) {
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

	public TaxliabilityControlEchecType getType() {
		return type;
	}

	public void setType(TaxliabilityControlEchecType type) {
		this.type = type;
	}
}
