package ch.vd.uniregctb.wsclient.fidor;

import java.util.List;

import ch.vd.uniregctb.wsclient.model.Logiciel;

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

	public Logiciel getLogiciel(Long idLogiciel) {
		return null;
	}

	public List<Logiciel> getTousLesLogiciels() {
		return null;
	}
}
