package ch.vd.unireg.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class QuittancerDeclarationView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private List<EtatDocumentFiscalView> etats;
	private boolean typeDocumentEditable;

	// Données modifiables du formulaire
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;

	public QuittancerDeclarationView() {
	}

	public QuittancerDeclarationView(DeclarationImpotOrdinairePP di, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this(di, true, infraService, messageHelper);
	}

	public QuittancerDeclarationView(DeclarationImpotOrdinairePM di, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this(di, false, infraService, messageHelper);
	}

	private QuittancerDeclarationView(DeclarationImpotOrdinaire di, boolean typeDocumentEditable, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		initReadOnlyValues(di, typeDocumentEditable, infraService, messageHelper);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di, boolean typeDocumentEditable, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateFin().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.etats = initEtats(di.getEtatsDeclaration(), infraService, messageHelper);
		this.typeDocumentEditable = typeDocumentEditable;
	}

	public static TypeEtatDocumentFiscal getDernierEtat(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etatDI = di.getDernierEtatDeclaration();
		return etatDI == null ? null : etatDI.getEtat();
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDeclaration> etats, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageHelper));
		}
		Collections.sort(list);
		return list;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebutPeriodeImposition() {
		return dateDebutPeriodeImposition;
	}

	public RegDate getDateFinPeriodeImposition() {
		return dateFinPeriodeImposition;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public List<EtatDocumentFiscalView> getEtats() {
		return etats;
	}

	public boolean isTypeDocumentEditable() {
		return typeDocumentEditable;
	}

	public void setTypeDocumentEditable(boolean typeDocumentEditable) {
		this.typeDocumentEditable = typeDocumentEditable;
	}
}
