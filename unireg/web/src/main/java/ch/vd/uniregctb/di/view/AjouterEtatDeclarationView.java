package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.view.EtatDeclarationView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class AjouterEtatDeclarationView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private List<EtatDeclarationView> etats;
	private boolean typeDocumentEditable;

	// Données modifiables du formulaire
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;

	public AjouterEtatDeclarationView() {
	}

	public AjouterEtatDeclarationView(DeclarationImpotOrdinairePP di, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this(di, true, infraService, messageSource);
	}

	public AjouterEtatDeclarationView(DeclarationImpotOrdinairePM di, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this(di, false, infraService, messageSource);
	}

	private AjouterEtatDeclarationView(DeclarationImpotOrdinaire di, boolean typeDocumentEditable, ServiceInfrastructureService infraService, MessageSource messageSource) {
		initReadOnlyValues(di, typeDocumentEditable, infraService, messageSource);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di, boolean typeDocumentEditable, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateFin().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.etats = initEtats(di.getEtats(), infraService, messageSource);
		this.typeDocumentEditable = typeDocumentEditable;
	}

	public static TypeEtatDeclaration getDernierEtat(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etatDI = di.getDernierEtat();
		return etatDI == null ? null : etatDI.getEtat();
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, infraService, messageSource));
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

	public List<EtatDeclarationView> getEtats() {
		return etats;
	}

	public boolean isTypeDocumentEditable() {
		return typeDocumentEditable;
	}

	public void setTypeDocumentEditable(boolean typeDocumentEditable) {
		this.typeDocumentEditable = typeDocumentEditable;
	}
}
