package ch.vd.uniregctb.lr.view;

import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.NomCourrierViewPart;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.di.view.DelaiDeclarationView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapDetailView implements Comparable<ListeRecapDetailView> {

	private TiersGeneralView dpi;

	private Long id;

	private CategorieImpotSource categorie;

	private ModeCommunication modeCommunication;

	private PeriodiciteDecompte periodicite;

	private String personneContact;

	private String numeroTelephone;

	private RegDate dateDebutPeriode;

	private RegDate dateFinPeriode;

	private RegDate dateRetour;

	private List<DelaiDeclarationView> delais;

	private List<EtatDeclaration> etats;

	private Boolean sansSommation;

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

	private RegDate delaiAccorde;

	private TypeEtatDeclaration etat;

	private boolean annule;

	private boolean isAllowedDelai;

	private boolean imprimable;

	public CategorieImpotSource getCategorie() {
		return categorie;
	}

	public void setCategorie(CategorieImpotSource categorie) {
		this.categorie = categorie;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	public PeriodiciteDecompte getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(PeriodiciteDecompte periodicite) {
		this.periodicite = periodicite;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getNumeroTelephone() {
		return numeroTelephone;
	}

	public void setNumeroTelephone(String numeroTelephone) {
		this.numeroTelephone = numeroTelephone;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getSansSommation() {
		return sansSommation;
	}

	public void setSansSommation(Boolean sansSommation) {
		this.sansSommation = sansSommation;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public void setDelais(List<DelaiDeclarationView> delais) {
		this.delais = delais;
	}

	public TiersGeneralView getDpi() {
		return dpi;
	}

	public void setDpi(TiersGeneralView dpi) {
		this.dpi = dpi;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
	}


	public RegDate getRegDateDebutPeriode() {
		return dateDebutPeriode;
	}

	public void setDateDebutPeriode(RegDate dateDebutPeriode) {
		this.dateDebutPeriode = dateDebutPeriode;
	}

	public RegDate getRegDateFinPeriode() {
		return dateFinPeriode;
	}

	public void setDateFinPeriode(RegDate dateFinPeriode) {
		this.dateFinPeriode = dateFinPeriode;
	}

	public RegDate getRegDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public RegDate getRegDelaiAccorde() {
		return delaiAccorde;
	}

	public void setDelaiAccorde(RegDate delaiAccorde) {
		this.delaiAccorde = delaiAccorde;
	}

	public Date getDateDebutPeriode() {
		return RegDate.asJavaDate(dateDebutPeriode);
	}

	public void setDateDebutPeriode(Date dateDebutPeriode) {
		this.dateDebutPeriode = RegDate.get(dateDebutPeriode);
	}

	public Date getDateFinPeriode() {
		return RegDate.asJavaDate(dateFinPeriode);
	}

	public void setDateFinPeriode(Date dateFinPeriode) {
		this.dateFinPeriode = RegDate.get(dateFinPeriode);
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

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public void setEtat(TypeEtatDeclaration etat) {
		this.etat = etat;
	}

	public List<EtatDeclaration> getEtats() {
		return etats;
	}

	public void setEtats(List<EtatDeclaration> etats) {
		this.etats = etats;
	}

	public boolean isAllowedDelai() {
		return isAllowedDelai;
	}

	public void setAllowedDelai(boolean isAllowedDelai) {
		this.isAllowedDelai = isAllowedDelai;
	}

	public boolean isImprimable() {
		return imprimable;
	}

	public void setImprimable(boolean imprimable) {
		this.imprimable = imprimable;
	}

	@Override
	public int compareTo(ListeRecapDetailView o) {
		int value = -  getDateDebutPeriode().compareTo(o.getDateDebutPeriode());
		return value;
	}

}
