package ch.vd.uniregctb.tiers.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.Sexe;

public class NonHabitantCivilView {

	private String nom;
	private String prenom;
	private String numeroAssureSocial;
	private final IdentificationPersonneView identificationPersonne;
	private Sexe sexe;
	private RegDate dateNaissance;
	private String sDateNaissance;
	private RegDate dateDeces;
	private CategorieEtranger categorieEtranger;
	private RegDate dateDebutValiditeAutorisation;
	private String sDateDebutValiditeAutorisation;
	private Integer numeroOfsNationalite;
	private String libelleOfsPaysOrigine;
	private String libelleCommuneOrigine;
	private String prenomsPere;
	private String nomPere;
	private String prenomsMere;
	private String nomMere;

	@SuppressWarnings("UnusedDeclaration")
	public NonHabitantCivilView() {
		this.identificationPersonne = new IdentificationPersonneView();
	}

	public NonHabitantCivilView(ServiceInfrastructureService infraService, PersonnePhysique pp) {
		this.nom = pp.getNom();
		this.prenom = pp.getPrenom();
		this.numeroAssureSocial = pp.getNumeroAssureSocial();
		this.identificationPersonne = new IdentificationPersonneView(pp);
		this.sexe = pp.getSexe();
		this.dateNaissance = pp.getDateNaissance();
		this.sDateNaissance = RegDateHelper.dateToDisplayString(pp.getDateNaissance());
		this.dateDeces = pp.getDateDeces();
		this.categorieEtranger = pp.getCategorieEtranger();
		this.dateDebutValiditeAutorisation = pp.getDateDebutValiditeAutorisation();
		this.sDateDebutValiditeAutorisation = RegDateHelper.dateToDisplayString(pp.getDateDebutValiditeAutorisation());
		this.numeroOfsNationalite = pp.getNumeroOfsNationalite();
		if (pp.getNumeroOfsNationalite() != null) {
			final Pays pays = infraService.getPays(pp.getNumeroOfsNationalite(), pp.getDateNaissance());
			if (pays != null) {
				this.libelleOfsPaysOrigine = pays.getNomCourt();
			}
		}
		this.libelleCommuneOrigine = pp.getLibelleCommuneOrigine();
		this.prenomsPere = pp.getPrenomsPere();
		this.nomPere = pp.getNomPere();
		this.prenomsMere = pp.getPrenomsMere();
		this.nomMere = pp.getNomMere();
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public IdentificationPersonneView getIdentificationPersonne() {
		return identificationPersonne;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getsDateNaissance() {
		return sDateNaissance;
	}

	public void setsDateNaissance(String sDateNaissance) {
		this.sDateNaissance = sDateNaissance;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	public CategorieEtranger getCategorieEtranger() {
		return categorieEtranger;
	}

	public void setCategorieEtranger(CategorieEtranger categorieEtranger) {
		this.categorieEtranger = categorieEtranger;
	}

	public RegDate getDateDebutValiditeAutorisation() {
		return dateDebutValiditeAutorisation;
	}

	public void setDateDebutValiditeAutorisation(RegDate dateDebutValiditeAutorisation) {
		this.dateDebutValiditeAutorisation = dateDebutValiditeAutorisation;
	}

	public String getsDateDebutValiditeAutorisation() {
		return sDateDebutValiditeAutorisation;
	}

	public void setsDateDebutValiditeAutorisation(String sDateDebutValiditeAutorisation) {
		this.sDateDebutValiditeAutorisation = sDateDebutValiditeAutorisation;
	}

	public Integer getNumeroOfsNationalite() {
		return numeroOfsNationalite;
	}

	public void setNumeroOfsNationalite(Integer numeroOfsNationalite) {
		this.numeroOfsNationalite = numeroOfsNationalite;
	}

	public String getLibelleOfsPaysOrigine() {
		return libelleOfsPaysOrigine;
	}

	public void setLibelleOfsPaysOrigine(String libelleOfsPaysOrigine) {
		this.libelleOfsPaysOrigine = libelleOfsPaysOrigine;
	}

	public String getLibelleCommuneOrigine() {
		return libelleCommuneOrigine;
	}

	public void setLibelleCommuneOrigine(String libelleCommuneOrigine) {
		this.libelleCommuneOrigine = libelleCommuneOrigine;
	}

	public String getPrenomsPere() {
		return prenomsPere;
	}

	public void setPrenomsPere(String prenomsPere) {
		this.prenomsPere = prenomsPere;
	}

	public String getNomPere() {
		return nomPere;
	}

	public void setNomPere(String nomPere) {
		this.nomPere = nomPere;
	}

	public String getPrenomsMere() {
		return prenomsMere;
	}

	public void setPrenomsMere(String prenomsMere) {
		this.prenomsMere = prenomsMere;
	}

	public String getNomMere() {
		return nomMere;
	}

	public void setNomMere(String nomMere) {
		this.nomMere = nomMere;
	}
}
