package ch.vd.unireg.tiers.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.Sexe;

public class NonHabitantCivilView {

	private String nom;
	private String nomNaissance;
	private String prenomUsuel;
	private String tousPrenoms;
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

	private Integer ofsCommuneOrigine;
	private String newLibelleCommuneOrigine;
	private String oldLibelleCommuneOrigine;

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
		this.nomNaissance = pp.getNomNaissance();
		this.prenomUsuel = pp.getPrenomUsuel();
		this.tousPrenoms = pp.getTousPrenoms();
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
			final List<Pays> candidates = infraService.getPaysHisto(pp.getNumeroOfsNationalite());
			if (candidates != null && !candidates.isEmpty()) {
				this.libelleOfsPaysOrigine = candidates.get(candidates.size() - 1).getNomCourt();
			}
		}
		if (pp.getOrigine() != null) {
			this.oldLibelleCommuneOrigine = pp.getOrigine().getLibelle();
			this.newLibelleCommuneOrigine = pp.getOrigine().getLibelle();
		}
		else {
			this.oldLibelleCommuneOrigine = null;
			this.newLibelleCommuneOrigine = null;
		}
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

	public String getNomNaissance() {
		return nomNaissance;
	}

	public void setNomNaissance(String nomNaissance) {
		this.nomNaissance = nomNaissance;
	}

	public String getPrenomUsuel() {
		return prenomUsuel;
	}

	public void setPrenomUsuel(String prenomUsuel) {
		this.prenomUsuel = prenomUsuel;
	}

	public String getTousPrenoms() {
		return tousPrenoms;
	}

	public void setTousPrenoms(String tousPrenoms) {
		this.tousPrenoms = tousPrenoms;
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

	public Integer getOfsCommuneOrigine() {
		return ofsCommuneOrigine;
	}

	public void setOfsCommuneOrigine(Integer ofsCommuneOrigine) {
		this.ofsCommuneOrigine = ofsCommuneOrigine;
	}

	public String getNewLibelleCommuneOrigine() {
		return newLibelleCommuneOrigine;
	}

	public void setNewLibelleCommuneOrigine(String newLibelleCommuneOrigine) {
		this.newLibelleCommuneOrigine = newLibelleCommuneOrigine;
	}

	public String getOldLibelleCommuneOrigine() {
		return oldLibelleCommuneOrigine;
	}

	public void setOldLibelleCommuneOrigine(String oldLibelleCommuneOrigine) {
		this.oldLibelleCommuneOrigine = oldLibelleCommuneOrigine;
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
