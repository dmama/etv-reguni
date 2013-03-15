package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class AjouterEtatDeclarationView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private List<EtatDeclarationView> etats;

	// Données modifiables du formulaire
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;

	public AjouterEtatDeclarationView() {
	}

	public AjouterEtatDeclarationView(DeclarationImpotOrdinaire di, MessageSource messageSource) {
		initReadOnlyValues(di, messageSource);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di, MessageSource messageSource) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateDebut().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.etats = initEtats(di.getEtats(), messageSource);
	}

	public static TypeEtatDeclaration getDernierEtat(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etatDI = di.getDernierEtat();
		return etatDI == null ? null : etatDI.getEtat();
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, messageSource));
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
}
