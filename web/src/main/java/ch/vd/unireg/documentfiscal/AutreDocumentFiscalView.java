package ch.vd.unireg.documentfiscal;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.DocumentFiscalView;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.utils.WebContextUtils;

public class AutreDocumentFiscalView extends DocumentFiscalView {

	private final RegDate dateEnvoi;
	private final RegDate dateRappel;
	private final String libelleTypeDocument;
	private final String libelleSousType;
	private final boolean avecCopieConformeEnvoi;
	private final String urlVisualisationExterneDocument;
	private final String urlVisualisationExterneRappel;

	private int periodeFiscale;

	// Demande de bilan finale
	private RegDate dateRequisitionRadiation;

	// Autorisation de radiaton RC
	private RegDate dateDemande;

	public AutreDocumentFiscalView(AutreDocumentFiscal doc, ServiceInfrastructureService infraService, MessageSource messageSource, String typeKey, String subtypeKey) {
		super(doc, infraService, messageSource);
		this.dateEnvoi = doc.getDateEnvoi();
		this.libelleTypeDocument = getLibelle(messageSource, typeKey);
		this.libelleSousType = getLibelle(messageSource, subtypeKey);
		this.avecCopieConformeEnvoi = StringUtils.isNotBlank(doc.getCleArchivage()) || StringUtils.isNotBlank(doc.getCleDocument());
		this.urlVisualisationExterneDocument = Optional.ofNullable(doc.getCleDocument())
				.filter(StringUtils::isNotBlank)
				.map(cle -> infraService.getUrlVisualisationDocument(this.getTiersId(), doc.getPeriodeFiscale(), cle))
				.orElse(null);
		if (doc instanceof AutreDocumentFiscalAvecSuivi) {
			final AutreDocumentFiscalAvecSuivi docFisc = (AutreDocumentFiscalAvecSuivi) doc;
			this.dateRappel = docFisc.getDateRappel();
			this.urlVisualisationExterneRappel = Optional.ofNullable(docFisc.getCleDocumentRappel())
					.filter(StringUtils::isNotBlank)
					.map(cle -> infraService.getUrlVisualisationDocument(this.getTiersId(), doc.getPeriodeFiscale(), cle))
					.orElse(null);
		}
		else {
			this.dateRappel = null;
			this.urlVisualisationExterneRappel = null;
		}
		if (doc instanceof DemandeBilanFinal) {
			this.dateRequisitionRadiation = ((DemandeBilanFinal) doc).getDateRequisitionRadiation();
			this.periodeFiscale = doc.getPeriodeFiscale();
		}
		else if (doc instanceof AutorisationRadiationRC) {
			this.dateDemande = ((AutorisationRadiationRC) doc).getDateDemande();
		}
		else if (doc instanceof DemandeDegrevementICI) {
			this.periodeFiscale = doc.getPeriodeFiscale();
		}
	}

	private String getLibelle(MessageSource messageSource, String typeKey) {
		return typeKey != null ? messageSource.getMessage(typeKey, null, WebContextUtils.getDefaultLocale()) : StringUtils.EMPTY;
	}

	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public RegDate getDateRappel() {
		return dateRappel;
	}

	public String getLibelleTypeDocument() {
		return libelleTypeDocument;
	}

	public String getLibelleSousType() {
		return libelleSousType;
	}

	public boolean isAvecCopieConformeEnvoi() {
		return avecCopieConformeEnvoi;
	}

	public String getUrlVisualisationExterneDocument() {
		return urlVisualisationExterneDocument;
	}

	public String getUrlVisualisationExterneRappel() {
		return urlVisualisationExterneRappel;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(int periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public RegDate getDateRequisitionRadiation() {
		return dateRequisitionRadiation;
	}

	public void setDateRequisitionRadiation(RegDate dateRequisitionRadiation) {
		this.dateRequisitionRadiation = dateRequisitionRadiation;
	}

	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate dateDemande) {
		this.dateDemande = dateDemande;
	}
}
