package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v3.NoticeRequestStatusCode;
import ch.vd.evd0022.v3.TypeOfNoticeRequest;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.wsclient.rcent.RcEntNoticeQuery;

/**
 * Critères de recherche sur les demandes d'annonce à l'IDE.
 */
public class AnnonceIDEQuery implements Serializable {

	private static final long serialVersionUID = -4449007097340120358L;

	private Long noticeId;
	private TypeAnnonce type;
	private StatutAnnonce[] status;
	private Long cantonalId;
	private String userId;
	private String name;
	private RegDate dateFrom;
	private RegDate dateTo;
	private Boolean containsForName;

	private Long tiersId;

	public Long getNoticeId() {
		return this.noticeId;
	}

	public TypeAnnonce getType() {
		return this.type;
	}

	@Nullable
	public StatutAnnonce[] getStatus() {
		return this.status == null ? null : this.status.clone();
	}

	public Long getCantonalId() {
		return this.cantonalId;
	}

	public String getUserId() {
		return this.userId;
	}

	public String getName() {
		return this.name;
	}

	public RegDate getDateFrom() {
		return this.dateFrom;
	}

	public RegDate getDateTo() {
		return this.dateTo;
	}

	public Boolean getContainsForName() {
		return this.containsForName;
	}

	public void setNoticeId(Long noticeId) {
		this.noticeId = noticeId;
	}

	public void setType(TypeAnnonce type) {
		this.type = type;
	}

	public void setStatus(StatutAnnonce[] status) {
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

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public String toString() {
		final List<String> values = new ArrayList<>();
		if (this.noticeId != null) {
			values.add("noticeId=" + this.noticeId);
		}

		if (this.type != null) {
			values.add("type=" + this.type);
		}

		if (this.status != null) {
			values.add("status=" + Arrays.toString(this.status));
		}

		if (this.cantonalId != null) {
			values.add("cantonalId=" + this.cantonalId);
		}

		if (this.userId != null) {
			values.add("userId=\'" + this.userId + '\'');
		}

		if (this.name != null) {
			values.add("name=\'" + this.name + '\'');
		}

		if (this.dateFrom != null) {
			values.add("dateFrom=" + this.dateFrom);
		}

		if (this.dateTo != null) {
			values.add("dateTo=" + this.dateTo);
		}

		if (this.containsForName != null) {
			values.add("containsForName=" + this.containsForName);
		}

		if (this.tiersId != null) {
			values.add("tiersId=" + this.tiersId);
		}

		return "AnnonceIDEQuery{" + StringUtils.join(", ", values) + "}";
	}

	@NotNull
	public RcEntNoticeQuery toFindNoticeQuery() {
		final RcEntNoticeQuery query = new RcEntNoticeQuery();
		query.setNoticeId(noticeId);
		query.setType(toRCEntType(type));
		query.setStatus(toRCEntStatus(status));
		query.setCantonalId(cantonalId);
		query.setUserId(userId);
		query.setName(name);
		query.setDateFrom(dateFrom);
		query.setDateTo(dateTo);
		query.setContainsForName(containsForName);
		return query;
	}

	@Nullable
	private static NoticeRequestStatusCode[] toRCEntStatus(@Nullable StatutAnnonce[] status) {
		if (status == null) {
			return null;
		}
		final NoticeRequestStatusCode[] res = new NoticeRequestStatusCode[status.length];
		for (int i = 0; i < status.length; i++) {
			res[i] = RCEntAnnonceIDEHelper.STATUS_ANNONCE_CONVERTER.convert(status[i]);
		}
		return res;
	}

	@Nullable
	private static TypeOfNoticeRequest toRCEntType(@Nullable TypeAnnonce type) {
		return RCEntAnnonceIDEHelper.TYPE_ANNONCE_CONVERTER.apply(type);
	}
}
