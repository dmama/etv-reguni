package ch.vd.uniregctb.di.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeDocument;

public class ImprimerNouvelleDeclarationImpotView {

	// Données pré-remplie de la déclaration
	private Long tiersId;
	private Integer periodeFiscale;

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
	private final boolean isAllowedQuittancement = SecurityProvider.isGranted(Role.DI_QUIT_PP);
	private boolean imprimable = true;
	private boolean valid = true;

	public ImprimerNouvelleDeclarationImpotView() {
	}

	public ImprimerNouvelleDeclarationImpotView(long tiersId) {
		this.tiersId = tiersId;
	}

	public ImprimerNouvelleDeclarationImpotView(long tiersId, boolean depuisTache) {
		this.tiersId = tiersId;
		this.depuisTache = depuisTache;
	}

	public void setPeriode(PeriodeImposition periode) {
		this.imprimable = true;
		this.periodeFiscale = periode.getDateDebut().year();
		this.dateDebutPeriodeImposition = periode.getDateDebut();
		this.dateFinPeriodeImposition = periode.getDateFin();
		this.typeAdresseRetour = periode.getAdresseRetour();
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

	public boolean isOuverte() {
		return dateDebutPeriodeImposition != null && dateDebutPeriodeImposition.year() == RegDate.get().year();
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isValid() {
		return valid;
	}
}
