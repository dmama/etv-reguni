package ch.vd.uniregctb.registrefoncier.allegement;

import java.io.Serializable;

public class DegrevementExonerationVisuSessionData implements Serializable {

	private static final long serialVersionUID = -5184869724787189904L;
	
	private final long idContribuable;
	private final int ofsCommune;
	private final Integer noParcelle;
	private final Integer index1;
	private final Integer index2;
	private final Integer index3;

	public DegrevementExonerationVisuSessionData(long idContribuable, int ofsCommune, Integer noParcelle, Integer index1, Integer index2, Integer index3) {
		this.idContribuable = idContribuable;
		this.ofsCommune = ofsCommune;
		this.noParcelle = noParcelle;
		this.index1 = index1;
		this.index2 = index2;
		this.index3 = index3;
	}

	public long getIdContribuable() {
		return idContribuable;
	}

	public int getOfsCommune() {
		return ofsCommune;
	}

	public Integer getNoParcelle() {
		return noParcelle;
	}

	public Integer getIndex1() {
		return index1;
	}

	public Integer getIndex2() {
		return index2;
	}

	public Integer getIndex3() {
		return index3;
	}
}
