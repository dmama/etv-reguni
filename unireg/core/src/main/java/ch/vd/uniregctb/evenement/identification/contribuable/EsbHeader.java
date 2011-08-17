package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Contient l'entête spécifique à l'ESB d'un message JMS.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class EsbHeader {

	private String businessUser;

	/**
	 * Chaque demande d’identification faite par un des systèmes utilisateurs, est identifiée de manière unique par un identifiant de ce
	 * système utilisateur. Le message transitant par l’ESB, l’identifiant du message doit respecter les règles de formatage de celui-ci.
	 */
	private String businessId;

	/**
	 * Nom du service de destination de la réponse.
	 */
	private String replyTo;

	/**
	 * URL d'accès au document, si fournie par le demandeur
	 */
	private String documentUrl;

	@Column(name = "BUSINESS_USER", length = 255, nullable = false)
	public String getBusinessUser() {
		return businessUser;
	}

	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@Column(name = "BUSINESS_ID", length = 255, nullable = false)
	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	@Column(name = "REPLY_TO", length = 255, nullable = false)
	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	@Column(name="DOCUMENT_URL", length = 255, nullable = true)
	public String getDocumentUrl() {
		return documentUrl;
	}

	public void setDocumentUrl(String documentUrl) {
		this.documentUrl = documentUrl;
	}
}
