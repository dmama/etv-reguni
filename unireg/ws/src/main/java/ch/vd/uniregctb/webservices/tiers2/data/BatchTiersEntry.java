package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.springframework.util.Assert;

import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;

/**
 * Classe regroupant un numéro de tiers et le tiers lui-même.
 * <p>
 * Cette classe est instanciée par la méthode {@link TiersWebService#getBatchTiers(ch.vd.uniregctb.webservices.tiers.params.GetBatchTiers)}.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>batchPartyEntryType</i> (xml) / <i>BatchPartyEntry</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BatchTiersEntry", propOrder = {
		"number", "tiers", "exceptionMessage", "exceptionType"
})
public class BatchTiersEntry {

	/**
	 * Le numéro de tiers demandé.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>number</i>.
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
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>party</i>.
	 */
	@XmlElement(required = false)
	public Tiers tiers;

	/**
	 * Le message de l'exception levée si le tiers n'a pas pu être retourné.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>exceptionInfo</i>.
	 */
	@XmlElement(required = false)
	public String exceptionMessage;

	/**
	 * Le type de l'exception levée si le tiers n'a pas pu être retourné.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>exceptionInfo</i>.
	 */
	@XmlElement(required = false)
	public WebServiceExceptionType exceptionType;

	public BatchTiersEntry() {
	}

	public BatchTiersEntry(Long number, Tiers tiers) {
		this.number = number;
		this.tiers = tiers;
		this.exceptionMessage = null;
		this.exceptionType = null;
	}

	public BatchTiersEntry(Long number, Object value) {
		this.number = number;
		if (value == null) {
			this.tiers = null;
			this.exceptionMessage = null;
			this.exceptionType = null;
		}
		else if (value instanceof Tiers) {
			this.tiers = (Tiers) value;
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
