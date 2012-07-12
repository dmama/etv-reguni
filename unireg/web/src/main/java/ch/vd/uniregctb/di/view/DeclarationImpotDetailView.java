package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Vue pour le détail d'une DI
 * 
 * @author xcifde
 * 
 */
public class DeclarationImpotDetailView implements Comparable<DeclarationImpotDetailView>, DeclarationImpotView {

	private TiersGeneralView contribuable;

	private Long id;

	private Long idDelai;

	private Integer periodeFiscale;

	private String codeControle;

	private RegDate dateDebutPeriodeImposition;

	private RegDate dateFinPeriodeImposition;

	private RegDate dateRetour;
	private String sourceRetour;

	/**
	 * VRAI si la date de retour {@link #dateRetour} est non nulle en création
	 * car il existe par ailleurs une DI annulée sur le même contribuable avec
	 * exactement la même période, et qui a elle été retournée
	 */
	private boolean dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;

	private RegDate delaiAccorde;

	private List<DelaiDeclarationView> delais;

	private List<EtatDeclaration> etats;

	private TypeEtatDeclaration etat;

	private String nomCourrier1;

	private String nomCourrier2;

	private Boolean sommation;

	private TypeDocument typeDeclarationImpot;

	private Integer numeroForGestion;

	private String officeImpot;

	private boolean annule;

	private boolean isAllowedSommation;

	private boolean isAllowedQuittancement;

	private boolean isAllowedDelai;

	private boolean isAllowedDuplic;

	private boolean isAllowedImpressionTO;

	private boolean isSommable;

	/**
	 * VRAI si la DI a été sommée (même si elle est maintenant retournée ou échue)
	 */
	private boolean wasSommee;

	private boolean imprimable;

	private TypeAdresseRetour typeAdresseRetour;

	private String errorMessage;
	
	private boolean ouverte;

	public DeclarationImpotDetailView() {

	}

	public DeclarationImpotDetailView(DeclarationImpotOrdinaire di) {
		fill(di);
	}

	public void fill(DeclarationImpotOrdinaire di) {
		
		this.id = di.getId();
		this.periodeFiscale = di.getPeriode() == null ? null : di.getPeriode().getAnnee();
		this.codeControle = di.getCodeControle();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.typeDeclarationImpot = di.getTypeDeclaration();
		this.delaiAccorde = di.getDelaiAccordeAu();
		this.dateRetour = di.getDateRetour();
		this.annule = di.isAnnule();

		// les états
		final ArrayList<EtatDeclaration> etats = new ArrayList<EtatDeclaration>(di.getEtats());
		Collections.sort(etats);
		this.setEtats(etats);

		final EtatDeclaration etat = di.getDernierEtat();
		this.etat = (etat == null ? null : etat.getEtat());

		if (etat instanceof EtatDeclarationRetournee) {
			this.sourceRetour = ((EtatDeclarationRetournee) etat).getSource();
		}

		// les délais
		final List<DelaiDeclarationView> delaisView = new ArrayList<DelaiDeclarationView>();
		for (DelaiDeclaration delai : di.getDelais()) {
			final DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(di.getPremierDelai() == delai.getDelaiAccordeAu());
			delaisView.add(delaiView);
		}
		Collections.sort(delaisView);
		this.delais = delaisView;
	}


	public boolean isAllowedSommation() {
		return isAllowedSommation;
	}

	public void setAllowedSommation(boolean isAllowedSommation) {
		this.isAllowedSommation = isAllowedSommation;
	}

	public boolean isAllowedImpressionTO() {
		return isAllowedImpressionTO;
	}

	public void setAllowedImpressionTO(boolean isAllowedImpressionTO) {
		this.isAllowedImpressionTO = isAllowedImpressionTO;
	}

	public boolean isAllowedQuittancement() {
		return isAllowedQuittancement;
	}

	public void setAllowedQuittancement(boolean isAllowedQuittancement) {
		this.isAllowedQuittancement = isAllowedQuittancement;
	}

	public boolean isAllowedDelai() {
		return isAllowedDelai;
	}

	public void setAllowedDelai(boolean isAllowedDelai) {
		this.isAllowedDelai = isAllowedDelai;
	}

	public boolean isAllowedDuplic() {
		return isAllowedDuplic;
	}

	public void setAllowedDuplic(boolean isAllowedDuplic) {
		this.isAllowedDuplic = isAllowedDuplic;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RegDate getRegDateDebutPeriodeImposition() {
		return dateDebutPeriodeImposition;
	}

	public void setDateDebutPeriodeImposition(RegDate dateDebutPeriodeImposition) {
		this.dateDebutPeriodeImposition = dateDebutPeriodeImposition;
	}

	public RegDate getRegDateFinPeriodeImposition() {
		return dateFinPeriodeImposition;
	}

	public void setDateFinPeriodeImposition(RegDate dateFinPeriodeImposition) {
		this.dateFinPeriodeImposition = dateFinPeriodeImposition;
	}

	public RegDate getRegDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public String getSourceRetour() {
		return sourceRetour;
	}

	public boolean isDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste() {
		return dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;
	}

	public void setDateRetourProposeeCarDeclarationRetourneeAnnuleeExiste(boolean dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste) {
		this.dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste = dateRetourProposeeCarDeclarationRetourneeAnnuleeExiste;
	}

	public RegDate getRegDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}

	public Date getDateDebutPeriodeImposition() {
		return RegDate.asJavaDate(dateDebutPeriodeImposition);
	}

	public void setDateDebutPeriodeImposition(Date dateDebutPeriodeImposition) {
		this.dateDebutPeriodeImposition = RegDate
				.get(dateDebutPeriodeImposition);
	}

	public Date getDateFinPeriodeImposition() {
		return RegDate.asJavaDate(dateFinPeriodeImposition);
	}

	public void setDateFinPeriodeImposition(Date dateFinPeriodeImposition) {
		this.dateFinPeriodeImposition = RegDate.get(dateFinPeriodeImposition);
	}

	public Date getDateRetour() {
		return RegDate.asJavaDate(dateRetour);
	}

	public void setDateRetour(Date dateRetour) {
		this.dateRetour = RegDate.get(dateRetour);
	}

	public Date getDelaiAccorde() {
		return RegDate.asJavaDate(delaiAccorde);
	}

	public void setDelaiAccorde(Date delaiAccorde) {
		this.delaiAccorde = RegDate.get(delaiAccorde);
	}

	public Boolean getSommation() {
		return sommation;
	}

	public void setSommation(Boolean sommation) {
		this.sommation = sommation;
	}

	public TypeDocument getTypeDeclarationImpot() {
		return typeDeclarationImpot;
	}

	public void setTypeDeclarationImpot(TypeDocument typeDeclarationImpot) {
		this.typeDeclarationImpot = typeDeclarationImpot;
	}

	public Integer getPeriodeFiscale() {
		return periodeFiscale;
	}

	public void setPeriodeFiscale(Integer periodeFiscale) {
		this.periodeFiscale = periodeFiscale;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public void setCodeControle(String codeControle) {
		this.codeControle = codeControle;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public void setDelais(List<DelaiDeclarationView> delais) {
		this.delais = delais;
	}

	public List<EtatDeclaration> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDeclaration> etats) {
		this.etats = etats;
	}

	public String getNomCourrier1() {
		return nomCourrier1;
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier1 = nomCourrier1;
	}

	public String getNomCourrier2() {
		return nomCourrier2;
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier2 = nomCourrier2;
	}

	public Integer getNumeroForGestion() {
		return numeroForGestion;
	}

	public void setNumeroForGestion(Integer numeroForGestion) {
		this.numeroForGestion = numeroForGestion;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}

	@Override
	public TiersGeneralView getContribuable() {
		return contribuable;
	}

	public void setContribuable(TiersGeneralView contribuable) {
		this.contribuable = contribuable;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public void setEtat(TypeEtatDeclaration etat) {
		this.etat = etat;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		Assert.isTrue(!nomCourrier.isEmpty());
		nomCourrier1 = nomCourrier.get(0);
		if (nomCourrier.size() > 1) {
			nomCourrier2 = nomCourrier.get(1);
		}
	}

	@Override
	public int compareTo(DeclarationImpotDetailView o) {
		return -1 * (periodeFiscale - o.periodeFiscale);
	}

	public boolean isSommable() {
		return isSommable;
	}

	public void setSommable(boolean isSommable) {
		this.isSommable = isSommable;
	}

	public TypeAdresseRetour getTypeAdresseRetour() {
		return typeAdresseRetour;
	}

	public void setTypeAdresseRetour(TypeAdresseRetour typeAdresseRetour) {
		this.typeAdresseRetour = typeAdresseRetour;
	}

	public boolean isImprimable() {
		return imprimable;
	}

	public void setImprimable(boolean imprimable) {
		this.imprimable = imprimable;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public void setIdDelai(Long idDelai) {
		this.idDelai = idDelai;
	}

	public void setOuverte(boolean ouverte) {
		this.ouverte = ouverte;
	}

	public boolean isOuverte() {
		return ouverte;
	}

	public boolean isWasSommee() {
		return wasSommee;
	}

	public void setWasSommee(boolean wasSommee) {
		this.wasSommee = wasSommee;
	}
}
