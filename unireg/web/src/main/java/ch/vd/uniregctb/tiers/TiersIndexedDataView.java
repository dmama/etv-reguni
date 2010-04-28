package ch.vd.uniregctb.tiers;

import java.util.Date;

import org.apache.lucene.document.Document;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public class TiersIndexedDataView {

	private TiersIndexedData data;
	private String urlTaoPP;
	private String urlTaoBA;
	private String urlTaoIS;
	private String urlSipf;

	public TiersIndexedDataView(TiersIndexedData data) {
		this.data = data;
	}

	public Long getNumero() {
		return data.getNumero();
	}

	public boolean isAnnule() {
		return data.isAnnule();
	}

	public String getRoleLigne1() {
		return data.getRoleLigne1();
	}

	public String getRoleLigne2() {
		return data.getRoleLigne2();
	}

	public String getNom1() {
		return data.getNom1();
	}

	public String getNom2() {
		return data.getNom2();
	}

	public String getDateNaissance() {
		return data.getDateNaissance();
	}

	public String getNpa() {
		return data.getNpa();
	}

	public String getLocaliteOuPays() {
		return data.getLocaliteOuPays();
	}

	public String getForPrincipal() {
		return data.getForPrincipal();
	}

	public Date getDateOuvertureFor() {
		return data.getDateOuvertureFor();
	}

	public Date getDateFermetureFor() {
		return data.getDateFermetureFor();
	}

	public boolean isDebiteurInactif() {
		return data.isDebiteurInactif();
	}

	public String getUrlTaoPP() {
		return urlTaoPP;
	}

	public void setUrlTaoPP(String urlTaoPP) {
		this.urlTaoPP = urlTaoPP;
	}

	public String getUrlTaoBA() {
		return urlTaoBA;
	}

	public void setUrlTaoBA(String urlTaoBA) {
		this.urlTaoBA = urlTaoBA;
	}

	public String getUrlSipf() {
		return urlSipf;
	}

	public void setUrlSipf(String urlSipf) {
		this.urlSipf = urlSipf;
	}

	public String getUrlTaoIS() {
		return urlTaoIS;
	}

	public void setUrlTaoIS(String urlTaoIS) {
		this.urlTaoIS = urlTaoIS;
	}

}
