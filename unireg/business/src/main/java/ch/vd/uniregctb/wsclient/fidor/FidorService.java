package ch.vd.uniregctb.wsclient.fidor;

import java.util.List;

import ch.vd.uniregctb.wsclient.model.Logiciel;

public interface FidorService {

	String getUrlTaoPP(Long numero);

	String getUrlTaoBA(Long numero);

	String getUrlTaoIS(Long numero);

	String getUrlSipf(Long numero);

	Logiciel getLogiciel(long idLogiciel);

	List<Logiciel> getTousLesLogiciels();
}
