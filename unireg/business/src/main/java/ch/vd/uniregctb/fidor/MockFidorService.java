package ch.vd.uniregctb.fidor;

public class MockFidorService implements FidorService {

	public String getUrlTaoPP(Long numero) {
		return "https://blabla/_NOCTB_/_OID_";
	}

	public String getUrlTaoBA(Long numero) {
		return "https://blabla/_NOCTB_/_OID_";
	}

	public String getUrlTaoIS(Long numero) {
		return "https://blabla/_NOCTB_/_OID_";
	}

	public String getUrlSipf(Long numero) {
		return "https://blabla/_NOCTB_/_OID_";
	}
}
