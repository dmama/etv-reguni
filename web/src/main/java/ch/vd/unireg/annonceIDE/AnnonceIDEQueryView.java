package ch.vd.unireg.annonceIDE;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;

/**
 * Vue web qui contient les paramètres de recherche pour les annonces à l'IDE.
 */
public class AnnonceIDEQueryView {

	// les critères de recherche métier
	private Long noticeId;
	private TypeAnnonce type;
	private List<StatutAnnonce> status;
	private Long cantonalId;
	private String userId;
	private String name;
	private RegDate dateFrom;
	private RegDate dateTo;
	private Boolean containsForName;

	private Long tiersId;

	// les options de recherche
	private int resultsPerPage;

	public Long getNoticeId() {
		return noticeId;
	}

	public void setNoticeId(Long noticeId) {
		this.noticeId = noticeId;
	}

	public TypeAnnonce getType() {
		return type;
	}

	public void setType(TypeAnnonce type) {
		this.type = type;
	}

	public List<StatutAnnonce> getStatus() {
		return status;
	}

	public void setStatus(List<StatutAnnonce> status) {
		this.status = status;
	}

	public Long getCantonalId() {
		return cantonalId;
	}

	public void setCantonalId(Long cantonalId) {
		this.cantonalId = cantonalId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RegDate getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(RegDate dateFrom) {
		this.dateFrom = dateFrom;
	}

	public RegDate getDateTo() {
		return dateTo;
	}

	public void setDateTo(RegDate dateTo) {
		this.dateTo = dateTo;
	}

	public Boolean getContainsForName() {
		return containsForName;
	}

	public void setContainsForName(Boolean containsForName) {
		this.containsForName = containsForName;
	}

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public void setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public AnnonceIDEQuery toQuery() {
		final AnnonceIDEQuery query = new AnnonceIDEQuery();
		query.setNoticeId(noticeId);
		query.setType(type);
		query.setStatus(status == null || status.isEmpty() ? null : status.toArray(new StatutAnnonce[status.size()]));
		query.setCantonalId(cantonalId);
		query.setUserId(userId);
		query.setName(name);
		query.setDateFrom(dateFrom);
		query.setDateTo(dateTo);
		query.setContainsForName(containsForName);
		query.setTiersId(tiersId);
		return query;
	}
}
