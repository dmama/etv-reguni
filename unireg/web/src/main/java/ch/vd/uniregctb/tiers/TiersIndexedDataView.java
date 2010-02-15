package ch.vd.uniregctb.tiers;

import org.apache.lucene.document.Document;

import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;

public class TiersIndexedDataView extends TiersIndexedData {

	public TiersIndexedDataView(Document doc) {
		super(doc);
	}

	/**
	 * URL TAO
	 */
	private String urlTaoPP;

	/**
	 * URL TAO BA
	 */
	private String urlTaoBA;

	/**
	 * URL TAO IS
	 */
	private String urlTaoIS;

	/**
	 * URL SIPF
	 */
	private String urlSipf;



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
	/**
	 * @return the urlTaoIS
	 */
	public String getUrlTaoIS() {
		return urlTaoIS;
	}
	/**
	 * @param urlTaoIS the urlTaoIS to set
	 */
	public void setUrlTaoIS(String urlTaoIS) {
		this.urlTaoIS = urlTaoIS;
	}

}
