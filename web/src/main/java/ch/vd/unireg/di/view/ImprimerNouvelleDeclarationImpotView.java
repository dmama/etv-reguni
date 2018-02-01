package ch.vd.unireg.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.type.TypeAdresseRetour;
import ch.vd.unireg.type.TypeDocument;

public class ImprimerNouvelleDeclarationImpotView {

	public enum TypeContribuable {
		PP,
		PM
	}

	// Données pré-remplie de la déclaration
	private Long tiersId;
	private Integer periodeFiscale;
	private TypeContribuable typeContribuable;

	// Données renseignées si on arrive sur l'écran de création de DI depuis la liste des tâches
	private boolean depuisTache;

	// Données modifiables de la déclaration
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private TypeDocument typeDocument;
	private TypeAdresseRetour typeAdresseRetour;
	private RegDate delaiAccorde;
	private RegDate dateRetour;

	/**
	 * VRAI si la date de retour {@link #dateRetour} est non nulle en création car il existe par ailleurs une DI annulée sur le même contribuable avec exactement la même période, et qui a elle été
	 * retournée
	 */
	private boolean dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;

	// Données dépendant des droits de l'utilisateur
	private boolean isAllowedQuittancement;
	private boolean imprimable = true;
	private boolean generableNonImprimable = false;

	public ImprimerNouvelleDeclarationImpotView() {
	}

	public ImprimerNouvelleDeclarationImpotView(long tiersId, boolean depuisTache, boolean allowQuittancement) {
		this.isAllowedQuittancement = allowQuittancement;
		this.tiersId = tiersId;
		this.depuisTache = depuisTache;
	}

	public void setPeriode(PeriodeImpositionPersonnesPhysiques periode) {
		this.imprimable = true;
		this.periodeFiscale = periode.getDateFin().year();
		this.dateDebutPeriodeImposition = periode.getDateDebut();
		this.dateFinPeriodeImposition = periode.getDateFin();
		this.typeAdresseRetour = periode.getAdresseRetour();
		this.typeContribuable = TypeContribuable.PP;
	}

	public void setPeriode(PeriodeImpositionPersonnesMorales periode) {
		this.imprimable = true;
		this.periodeFiscale = periode.getDateFin().year();
		this.dateDebutPeriodeImposition = periode.getDateDebut();
		this.dateFinPeriodeImposition = periode.getDateFin();
		this.typeAdresseRetour = TypeAdresseRetour.CEDI;
		this.typeContribuable = TypeContribuable.PM;
		this.imprimable = periode.getTypeDocumentDeclaration() != null;
		this.generableNonImprimable = periode.getTypeDocumentDeclaration() == null;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setTiersId(Long tiersId) {
		this.tiersId = tiersId;
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable typeContribuable) {
		this.typeContribuable = typeContribuable;
	}

	public boolean isDepuisTache() {
		return depuisTache;
	}

	public void setDepuisTache(boolean depuisTache) {
		this.depuisTache = depuisTache;
	}

	public RegDate getDateDebutPeriodeImposition() {
		return dateDebutPeriodeImposition;
	}

	public void setDateDebutPeriodeImposition(RegDate dateDebutPeriodeImposition) {
		this.dateDebutPeriodeImposition = dateDebutPeriodeImposition;
	}

	public RegDate getDateFinPeriodeImposition() {
		return dateFinPeriodeImposition;
	}

	public void setDateFinPeriodeImposition(RegDate dateFinPeriodeImposition) {
		this.dateFinPeriodeImposition = dateFinPeriodeImposition;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public TypeAdresseRetour getTypeAdresseRetour() {
		return typeAdresseRetour;
	}

	public void setTypeAdresseRetour(TypeAdresseRetour typeAdresseRetour) {
		this.typeAdresseRetour = typeAdresseRetour;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public boolean isDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste() {
		return dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;
	}

	public void setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(boolean dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste) {
		this.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste = dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;
	}

	public boolean isAllowedQuittancement() {
		return isAllowedQuittancement;
	}

	public boolean isImprimable() {
		return imprimable;
	}

	public void setImprimable(boolean imprimable) {
		this.imprimable = imprimable;
	}

	public boolean isGenerableNonImprimable() {
		return generableNonImprimable;
	}

	public void setGenerableNonImprimable(boolean generableNonImprimable) {
		this.generableNonImprimable = generableNonImprimable;
	}

	public boolean isOuverte() {
		 return periodeFiscale == RegDate.get().year();
	}
}
