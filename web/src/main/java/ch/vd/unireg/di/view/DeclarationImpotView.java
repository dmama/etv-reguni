package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.view.CodeControlable;
import ch.vd.unireg.declaration.view.DeclarationView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.utils.WebContextUtils;

/**
 * Vue d'une déclaration d'impôt (ordinaire)
 */
@SuppressWarnings("UnusedDeclaration")
public class DeclarationImpotView extends DeclarationView implements CodeControlable{

	private final String codeControle;
	private final TypeDocument typeDocument;
	private final String typeDocumentMessage;
	private final boolean diPP;
	private final boolean diPM;
	private final RegDate dateDebutExercice;
	private final RegDate dateFinExercice;

	public DeclarationImpotView(DeclarationImpotOrdinaire di, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		super(di, infraService, messageHelper);

		this.codeControle = di.getCodeControle();
		this.typeDocument = di.getTypeDeclaration();
		if (this.typeDocument != null) {
			this.typeDocumentMessage = messageHelper.getMessage("option.type.document." + this.typeDocument.name());
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
