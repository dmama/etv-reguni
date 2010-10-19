package ch.vd.uniregctb.wsclient.fidor;

public interface FidorService {

	String getUrlTaoPP(Long numero);

	String getUrlTaoBA(Long numero);

	String getUrlTaoIS(Long numero);

	String getUrlSipf(Long numero);
}
