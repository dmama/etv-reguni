package ch.vd.unireg.interfaces.efacture.data;

import org.jetbrains.annotations.Nullable;

public class ResultatQuittancement {
	String idDemande;
	TypeResultatQuittancement typeResultat;
	public ResultatQuittancement(TypeResultatQuittancement typeResultat) {
		this(null, typeResultat);
	}

	public ResultatQuittancement(@Nullable String idDemande, TypeResultatQuittancement typeResultat) {
		this.idDemande = idDemande;
		this.typeResultat = typeResultat;
	}

	public String getIdDemande() {
		return idDemande;
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
