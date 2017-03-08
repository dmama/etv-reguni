package ch.vd.uniregctb.declaration.view;

import java.sql.Timestamp;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

public class DelaiDeclarationView implements Comparable<DelaiDeclarationView>, Annulable {

	private Long id;
	private RegDate dateDemande;
	private RegDate dateTraitement;
	private RegDate delaiAccordeAu;
	private RegDate oldDelaiAccorde;
	private RegDate dateExpedition;
	private Boolean confirmationEcrite;
	private String urlVisualisationExterneDocument;
	private Long idDeclaration;
	private Long tiersId;
	private int declarationPeriode;
	private DateRange declarationRange;
	private String logModifUser;
	private Timestamp logModifDate;
	private EtatDelaiDeclaration etat;
	private String etatMessage;
	private boolean sursis;
	private boolean annule;
	private boolean first;

	public DelaiDeclarationView() {
	}

	public DelaiDeclarationView(DelaiDeclaration delai, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.id = delai.getId();
		this.annule = delai.isAnnule();
		this.confirmationEcrite = StringUtils.isNotBlank(delai.getCleArchivageCourrier());
		final Declaration declaration = delai.getDeclaration();
		this.urlVisualisationExterneDocument = Optional.ofNullable(delai.getCleDocument())
				.filter(StringUtils::isNotBlank)
				.map(cle -> infraService.getUrlVisualisationDocument(declaration.getTiers().getNumero(), declaration.getPeriode().getAnnee(), cle))
				.orElse(null);
		this.dateDemande = delai.getDateDemande();
		this.dateTraitement = delai.getDateTraitement();
		this.delaiAccordeAu = delai.getDelaiAccordeAu();
		this.logModifDate = delai.getLogModifDate();
		this.logModifUser = delai.getLogModifUser();
		this.idDeclaration = declaration.getId();
		this.etat = delai.getEtat();
		this.etatMessage = messageSource.getMessage("option.etat.delai." + this.etat.name(), null, WebContextUtils.getDefaultLocale());
		this.sursis = delai.isSursis();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate delaiAccordeAu) {
		this.delaiAccordeAu = delaiAccordeAu;
	}

	public Boolean getConfirmationEcrite() {
		return confirmationEcrite;
	}

	public void setConfirmationEcrite(Boolean confirmationEcrite) {
		this.confirmationEcrite = confirmationEcrite;
	}

	public String getUrlVisualisationExterneDocument() {
		return urlVisualisationExterneDocument;
	}

	public void setUrlVisualisationExterneDocument(String urlVisualisationExterneDocument) {
		this.urlVisualisationExterneDocument = urlVisualisationExterneDocument;
	}

	public Long getIdDeclaration() {
		return idDeclaration;
	}

	public void setIdDeclaration(Long idDeclaration) {
		this.idDeclaration = idDeclaration;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public int getDeclarationPeriode() {
		return declarationPeriode;
	}

	public void setDeclarationPeriode(int declarationPeriode) {
		this.declarationPeriode = declarationPeriode;
	}

	public DateRange getDeclarationRange() {
		return declarationRange;
	}

	public void setDeclarationRange(DateRange declarationRange) {
		this.declarationRange = declarationRange;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public RegDate getDateExpedition() {
		return dateExpedition;
	}

	public void setDateExpedition(RegDate dateExpedition) {
		this.dateExpedition = dateExpedition;
	}

	public RegDate getOldDelaiAccorde() {
		return oldDelaiAccorde;
	}

	public void setOldDelaiAccorde(RegDate oldDelaiAccorde) {
		this.oldDelaiAccorde = oldDelaiAccorde;
	}

	public String getLogModifUser() {
		return logModifUser;
	}

	public void setLogModifUser(String logModifUser) {
		this.logModifUser = logModifUser;
	}

	public Timestamp getLogModifDate() {
		return logModifDate;
	}

	public void setLogModifDate(Timestamp logModifDate) {
		this.logModifDate = logModifDate;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public EtatDelaiDeclaration getEtat() {
		return etat;
	}

	public String getEtatMessage() {
		return etatMessage;
	}

	public boolean isSursis() {
		return sursis;
	}

	/**
	 * Compare d'apres la date de DelaiDeclarationView
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(@NotNull DelaiDeclarationView delaiDeclarationView) {
		int comparison = - dateTraitement.compareTo(delaiDeclarationView.dateTraitement);
		if (comparison == 0) {
			comparison = - Long.compare(id, delaiDeclarationView.id);
		}
		return comparison;
	}
}
