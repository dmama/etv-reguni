package ch.vd.unireg.interfaces.efacture.data;

import org.jetbrains.annotations.Nullable;

public class ResultatQuittancement {
	String businessId;
	TypeResultatQuittancement typeResultat;
	public ResultatQuittancement(TypeResultatQuittancement typeResultat) {
		this(null, typeResultat);
	}

	public ResultatQuittancement(@Nullable String businessId, TypeResultatQuittancement typeResultat) {
		this.businessId = businessId;
		this.typeResultat = typeResultat;
	}

	public String getBusinessId() {
		return businessId;
	}

	public TypeResultatQuittancement getType() {
		return typeResultat;
	}

	public String getDescription(long ctbId) {
		return typeResultat.getDescription(ctbId);
	}

	public boolean isOk() {
		return typeResultat.isOk();
	}
}
