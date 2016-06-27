package ch.vd.unireg.interfaces.infra.data;

public enum TypeCollectivite {
	SIGLE_A_COM("A.COM")
	,SIGLE_A_MIL("A.MIL")
	,SIGLE_A_PEN("A.PEN")
	,SIGLE_ACI("ACI")
	,SIGLE_ACIA("ACIA")
	,SIGLE_ACIFD("ACIFD")
	,SIGLE_ACIPP("ACIPP")
	,SIGLE_AFC("AFC")
	,SIGLE_ARCH("ARCH")
	,SIGLE_B_COM("B.COM")
	,SIGLE_C_INF("C.INF")
	,SIGLE_CADAS("CADAS")
	,SIGLE_CAVS("CAVS")
	,SIGLE_CFIN("CFIN")
	,SIGLE_CIR("CIR")
	,SIGLE_CRAGR("CRAGR")
	,SIGLE_DFIN("DFIN")
	,SIGLE_JPAIX("JPAIX")
	,SIGLE_JUST("JUST")
	,SIGLE_LOGMT("LOGMT")
	,SIGLE_NOTAI("NOTAI")
	,SIGLE_O_SOC("O.SOC")
	,SIGLE_OCBE("OCBE")
	,SIGLE_OCC("OCC")
	,SIGLE_OMSV("OMSV")
	,SIGLE_OPF("OPF")
	,SIGLE_PINF("PINF")
	,SIGLE_PSEN("PSEN")
	,SIGLE_RC("RC")
	,SIGLE_RENSP("RENSP")
	,SIGLE_RF("RF")
	,SIGLE_S_ACI("S/ACI")
	,SIGLE_S_AGR("S.AGR")
	,SIGLE_TIERS("TIERS")
	,SIGLE_TUTEL("TUTEL")
	,SIGLE_TVA("TVA")
	,SIGLE_STAT("STAT")
	,SIGLE_RDU("RDU")
	,SIGLE_RDUC("RDUC");
	private final String code;

	TypeCollectivite(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
