package ch.vd.uniregctb.documentfiscal;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.view.DocumentFiscalView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.utils.WebContextUtils;

public class AutreDocumentFiscalView extends DocumentFiscalView {

	private final RegDate dateEnvoi;
	private final String libelleTypeDocument;
	private final String libelleSousType;
	private final boolean avecCopieConformeEnvoi;
	private final String urlVisualisationExterneDocument;

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
	}

	private String getLibelle(MessageSource messageSource, String typeKey) {
		return typeKey != null ? messageSource.getMessage(typeKey, null, WebContextUtils.getDefaultLocale()) : StringUtils.EMPTY;
	}

	public RegDate getDateEnvoi() {
		return dateEnvoi;
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
}
