package ch.vd.unireg.wsclient.rcent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.NoticeRequestStatus;
import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.registre.base.date.RegDate;

/**
 * Requête pour rechercher des demandes d'annonce (entreprises).
 */
public class RcEntNoticeQuery {

	private Long noticeId;
	private TypeOfNoticeRequest type;
	private NoticeRequestStatus[] status;
	private Long cantonalId;
	private String userId;
	private String name;
	private RegDate dateFrom;
	private RegDate dateTo;
	private Boolean containsForName;

	/**
	 * @return le critère de recherche sur l'identifiant unique de la demande d’annonce.
	 */
	public Long getNoticeId() {
		return noticeId;
	}

	/**
	 * @return le critère de recherche sur le type de demande d'annonce.
	 */
	public TypeOfNoticeRequest getType() {
		return type;
	}

	/**
	 * @return le ou les critères de recherche sur le statut de la demande d'annonce
	 */
	@Nullable
	public NoticeRequestStatus[] getStatus() {
		return status == null ? null : status.clone();
	}

	/**
	 * @return le critère de recherche sur le numéro cantonal de l'établissement ou de l'entreprise de rattachement de la demande d'annonce.
	 */
	public Long getCantonalId() {
		return cantonalId;
	}

	/**
	 * @return le critère de recherche sur l'identification IAM de l’émetteur de l’annonce, dans le cas d’une annonce reçue depuis SiTi.
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @return le critère de recherche sur le nom de l’établissement objet de la demande d’annonce.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return le critère de recherche sur le début de la plage de date d’envoi de la demande d’annonce à RCEnt.
	 */
	public RegDate getDateFrom() {
		return dateFrom;
	}

	/**
	 * @return le critère de recherche sur la fin de la plage de date d’envoi de la demande d’annonce à RCEnt.
	 */
	public RegDate getDateTo() {
		return dateTo;
	}

	/**
	 * @return <b>vrai</b> si le critère <i>name</i> fonctionne en mode <i>contains</i>; par défaut : <b>faux</b>.
	 */
	public Boolean getContainsForName() {
		return containsForName;
	}

	public void setNoticeId(Long noticeId) {
		this.noticeId = noticeId;
	}

	public void setType(TypeOfNoticeRequest type) {
		this.type = type;
	}

	public void setStatus(NoticeRequestStatus[] status) {
		this.status = status == null ? null : status.clone();
	}

	public void setCantonalId(Long cantonalId) {
		this.cantonalId = cantonalId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDateFrom(RegDate dateFrom) {
		this.dateFrom = dateFrom;
	}

	public void setDateTo(RegDate dateTo) {
		this.dateTo = dateTo;
	}

	public void setContainsForName(Boolean containsForName) {
		this.containsForName = containsForName;
	}

	@Override
	public String toString() {
		final List<String> values = new ArrayList<>();
		if (noticeId != null) {
			values.add("noticeId=" + noticeId);
		}
		if (type != null) {
			values.add("type=" + type);
		}
		if (status != null) {
			values.add("status=" + Arrays.toString(status));
		}
		if (cantonalId != null) {
			values.add("cantonalId=" + cantonalId);
		}
		if (userId != null) {
			values.add("userId='" + userId + '\'');
		}
		if (name != null) {
			values.add("name='" + name + '\'');
		}
		if (dateFrom != null) {
			values.add("dateFrom=" + dateFrom);
		}
		if (dateTo != null) {
			values.add("dateTo=" + dateTo);
		}
		if (containsForName != null) {
			values.add("containsForName=" + containsForName);
		}
		return "NoticeQuery{" + String.join(", ", values) + "}";
	}
}
