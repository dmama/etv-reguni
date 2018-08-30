package ch.vd.unireg.documentfiscal;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;

public class AutreDocumentFiscalAvecSuiviView extends AutreDocumentFiscalView {

	private final RegDate dateRetour;
	private final RegDate delaiRetour;
	private final RegDate dateRappel;
	private final boolean avecCopieConformeRappel;
	private final String urlVisualisationExterneRappel;

	public AutreDocumentFiscalAvecSuiviView(AutreDocumentFiscalAvecSuivi doc, ServiceInfrastructureService infraService, MessageHelper messageHelper, String typeKey, String subtypeKey) {
		super(doc, infraService, messageHelper, typeKey, subtypeKey);
		this.dateRetour = doc.getDateRetour();
		this.dateRappel = doc.getDateRappel();
		this.delaiRetour = doc.getDelaiRetour();
		this.avecCopieConformeRappel = StringUtils.isNotBlank(doc.getCleArchivageRappel()) && dateRappel != null;
		this.urlVisualisationExterneRappel = Optional.ofNullable(doc.getCleDocumentRappel())
				.filter(StringUtils::isNotBlank)
				.filter(cle -> dateRappel != null)
				.map(cle -> infraService.getUrlVisualisationDocument(getTiersId(), doc.getPeriodeFiscale(), cle))
				.orElse(null);
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public RegDate getDateRappel() {
		return dateRappel;
	}

	public boolean isAvecCopieConformeRappel() {
		return avecCopieConformeRappel;
	}

	public String getUrlVisualisationExterneRappel() {
		return urlVisualisationExterneRappel;
	}
}
