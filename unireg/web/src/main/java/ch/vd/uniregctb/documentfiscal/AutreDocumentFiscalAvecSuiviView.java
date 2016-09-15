package ch.vd.uniregctb.documentfiscal;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;

public class AutreDocumentFiscalAvecSuiviView extends AutreDocumentFiscalView {

	private final RegDate dateRetour;
	private final RegDate delaiRetour;
	private final RegDate dateRappel;
	private final boolean avecCopieConformeRappel;

	public AutreDocumentFiscalAvecSuiviView(AutreDocumentFiscalAvecSuivi doc, MessageSource messageSource, String typeKey, String subtypeKey) {
		super(doc, messageSource, typeKey, subtypeKey);
		this.dateRetour = doc.getDateRetour();
		this.dateRappel = doc.getDateRappel();
		this.delaiRetour = doc.getDelaiRetour();
		this.avecCopieConformeRappel = StringUtils.isNotBlank(doc.getCleArchivageRappel()) && dateRappel != null;
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
}
