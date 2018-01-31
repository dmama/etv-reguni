package ch.vd.uniregctb.di.view;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.view.DeclarationView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Vue d'une déclaration d'impôt (ordinaire)
 */
@SuppressWarnings("UnusedDeclaration")
public class DeclarationImpotView extends DeclarationView {

	private final String codeControle;
	private final TypeDocument typeDocument;
	private final String typeDocumentMessage;
	private final boolean diPP;
	private final boolean diPM;
	private final RegDate dateDebutExercice;
	private final RegDate dateFinExercice;

	public DeclarationImpotView(DeclarationImpotOrdinaire di, ServiceInfrastructureService infraService, MessageSource messageSource) {
		super(di, infraService, messageSource);

		this.codeControle = di.getCodeControle();
		this.typeDocument = di.getTypeDeclaration();
		if (this.typeDocument != null) {
			this.typeDocumentMessage = messageSource.getMessage("option.type.document." + this.typeDocument.name(), null, WebContextUtils.getDefaultLocale());
		}
		else {
			this.typeDocumentMessage = null;
		}

		if (di instanceof DeclarationImpotOrdinairePM) {
			final DeclarationImpotOrdinairePM dipm = (DeclarationImpotOrdinairePM) di;
			this.dateDebutExercice = dipm.getDateDebutExerciceCommercial();
			this.dateFinExercice = dipm.getDateFinExerciceCommercial();
		}
		else {
			this.dateDebutExercice = null;
			this.dateFinExercice = null;
		}

		this.diPP = di instanceof DeclarationImpotOrdinairePP;
		this.diPM = di instanceof DeclarationImpotOrdinairePM;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public RegDate getDateDebutExercice() {
		return dateDebutExercice;
	}

	public RegDate getDateFinExercice() {
		return dateFinExercice;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getTypeDocumentMessage() {
		return typeDocumentMessage;
	}

	public boolean isDiPP() {
		return diPP;
	}

	public boolean isDiPM() {
		return diPM;
	}
}
