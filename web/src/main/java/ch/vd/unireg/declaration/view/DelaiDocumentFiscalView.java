package ch.vd.unireg.declaration.view;

import java.sql.Timestamp;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDelaiDeclaration;

public class DelaiDocumentFiscalView implements Comparable<DelaiDocumentFiscalView>, Annulable {

	private Long id;
	private RegDate dateDemande;
	private RegDate dateTraitement;
	private RegDate delaiAccordeAu;
	private RegDate oldDelaiAccorde;
	private RegDate dateExpedition;
	private Boolean confirmationEcrite;
	private String urlVisualisationExterneDocument;
	private Long idDocumentFiscal;
	private Long tiersId;
	private int periode;
	private DateRange declarationRange;
	private String logModifUser;
	private Timestamp logModifDate;
	private EtatDelaiDocumentFiscal etat;
	private String etatMessage;
	private boolean sursis;
	private boolean annule;
	private boolean first;
	private boolean lastOfState;
	@Nullable
	private Long demandeDelaisMandataireId;
	private TypeDelaiDeclaration typeDelai;

	public DelaiDocumentFiscalView() {
	}

	public DelaiDocumentFiscalView(DelaiDocumentFiscal delai, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this.id = delai.getId();
		this.annule = delai.isAnnule();
		this.confirmationEcrite = StringUtils.isNotBlank(delai.getCleArchivageCourrier());
		final DocumentFiscal documentFiscal = delai.getDocumentFiscal();
		this.urlVisualisationExterneDocument = Optional.ofNullable(delai.getCleDocument())
				.filter(StringUtils::isNotBlank)
				.map(cle -> infraService.getUrlVisualisationDocument(documentFiscal.getTiers().getNumero(), documentFiscal.getAnneePeriodeFiscale(), cle))
				.orElse(null);
		this.dateDemande = delai.getDateDemande();
		this.dateTraitement = delai.getDateTraitement();
		this.delaiAccordeAu = delai.getDelaiAccordeAu();
		this.logModifDate = delai.getLogModifDate();
		this.logModifUser = delai.getLogModifUser();
		this.idDocumentFiscal = documentFiscal.getId();
		this.etat = delai.getEtat();
		this.etatMessage = messageHelper.getMessage("option.etat.delai." + this.etat.name());
		this.sursis = delai.isSursis();
		this.demandeDelaisMandataireId = delai.getDemandeMandataire() == null ? null : delai.getDemandeMandataire().getId();
		this.typeDelai = delai instanceof DelaiDeclaration ? ((DelaiDeclaration) delai).getTypeDelai() : null;
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

	public Long getIdDocumentFiscal() {
		return idDocumentFiscal;
	}

	public void setIdDocumentFiscal(Long idDocumentFiscal) {
		this.idDocumentFiscal = idDocumentFiscal;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public int getPeriode() {
		return periode;
	}

	public void setPeriode(int periode) {
		this.periode = periode;
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

	public EtatDelaiDocumentFiscal getEtat() {
		return etat;
	}

	public String getEtatMessage() {
		return etatMessage;
	}

	public boolean isSursis() {
		return sursis;
	}

	public boolean isLastOfState() {
		return lastOfState;
	}

	public void setLastOfState(boolean lastOfState) {
		this.lastOfState = lastOfState;
	}

	@Nullable
	public Long getDemandeDelaisMandataireId() {
		return demandeDelaisMandataireId;
	}

	public TypeDelaiDeclaration getTypeDelai() {
		return typeDelai;
	}

	public void setTypeDelai(TypeDelaiDeclaration typeDelai) {
		this.typeDelai = typeDelai;
	}

	/**
	 * Compare d'apres la date de DelaiDeclarationView
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(@NotNull DelaiDocumentFiscalView delaiDocumentFiscalView) {
		int comparison = -dateTraitement.compareTo(delaiDocumentFiscalView.dateTraitement);
		if (comparison == 0) {
			comparison = -Long.compare(id, delaiDocumentFiscalView.id);
		}
		return comparison;
	}
}
