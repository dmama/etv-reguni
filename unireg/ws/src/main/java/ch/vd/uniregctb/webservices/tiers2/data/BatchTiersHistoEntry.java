package ch.vd.uniregctb.webservices.tiers2.data;

import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import org.springframework.util.Assert;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Classe regroupant un numéro de tiers et le tiers lui-même.
 * <p>
 * Cette classe est instanciée par la méthode {@link ch.vd.uniregctb.webservices.tiers2.TiersWebService#getBatchTiersHisto(ch.vd.uniregctb.webservices.tiers.params.GetBatchTiersHisto)}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BatchTiersHistoEntry", propOrder = {
		"number", "tiers", "exceptionMessage", "exceptionType"
})
public class BatchTiersHistoEntry {

	/**
	 * Le numéro de tiers demandé.
	 */
	@XmlElement(required = true)
	public Long number;

	/**
	 * Le tiers demandé, ou <b>null</b> si le tiers n'a pas pu être retourné.
	 * <p>
	 * S'il le tiers est <b>null</b>, il y a deux possibilités :
	 * <ul>
	 * <li>{@link #exceptionMessage} est <b>null</b>: le tiers n'existe pas.</li>
	 * <li>{@link #exceptionMessage} est différent de <b>null</b>: le tiers existe mais le web-service n'a pu pas le retourner. Dans ce cas,
	 * la raison est stockée dans {@link #exceptionMessage}.</li>
	 * </ul>
	 */
	@XmlElement(required = false)
	public TiersHisto tiers;

	/**
	 * Le message de l'exception levée si le tiers n'a pas pu être retourné.
	 */
	@XmlElement(required = false)
	public String exceptionMessage;

	/**
	 * Le type de l'exception levée si le tiers n'a pas pu être retourné.
	 */
	@XmlElement(required = false)
	public WebServiceExceptionType exceptionType;

	public BatchTiersHistoEntry() {
	}

	public BatchTiersHistoEntry(Long number, TiersHisto tiers) {
		this.number = number;
		this.tiers = tiers;
		this.exceptionMessage = null;
		this.exceptionType = null;
	}

	public BatchTiersHistoEntry(Long number, Object value) {
		this.number = number;
		if (value == null) {
			this.tiers = null;
			this.exceptionMessage = null;
			this.exceptionType = null;
		}
		else if (value instanceof TiersHisto) {
			this.tiers = (TiersHisto) value;
			this.exceptionMessage = null;
			this.exceptionType = null;
		}
		else {
			Assert.isTrue(value instanceof WebServiceException);
			this.tiers = null;
			this.exceptionMessage = ((WebServiceException) value).getMessage();
			this.exceptionType = ((WebServiceException) value).getType();
		}
	}
}