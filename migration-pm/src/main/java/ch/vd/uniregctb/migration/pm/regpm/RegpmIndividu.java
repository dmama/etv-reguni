package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;
import java.util.SortedSet;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.SexeUserType;
import ch.vd.uniregctb.type.Sexe;

@Entity
@Table(name = "INDIVIDU")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "Sexe", typeClass = SexeUserType.class)
})
public class RegpmIndividu extends RegpmEntity implements WithLongId {

	private static final long serialVersionUID = 5699922874760523793L;

	private Long id;
	private Long navs13;
	private String noContribuableIT;
	private String nom;
	private String prenom;
	private Sexe sexe;
	private RegDate dateNaissance;
	private RegDate dateDeces;
	private SortedSet<RegpmCaracteristiquesIndividu> caracteristiques;
	private Set<RegpmAdresseIndividu> adresses;
	private Set<RegpmMandat> mandants;

	@Id
	@Column(name = "NO_INDIVIDU")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "NAVS13")
	public Long getNavs13() {
		return navs13;
	}

	public void setNavs13(Long navs13) {
		this.navs13 = navs13;
	}

	@Column(name = "NO_CANTONAL")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "9"))
	public String getNoContribuableIT() {
		return noContribuableIT;
	}

	public void setNoContribuableIT(String noContribuableIT) {
		this.noContribuableIT = noContribuableIT;
	}

	@Column(name = "NOM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "50"))
	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@Column(name = "PRENOM")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	@Column(name = "CO_SEXE")
	@Type(type = "Sexe")
	public Sexe getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		this.sexe = sexe;
	}

	@Column(name = "DATE_NAISSANCE")
	@Type(type = "RegDate")
	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	@Column(name = "DATE_DECES")
	@Type(type = "RegDate")
	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_INDNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmCaracteristiquesIndividu> getCaracteristiques() {
		return caracteristiques;
	}

	public void setCaracteristiques(SortedSet<RegpmCaracteristiquesIndividu> caracteristiques) {
		this.caracteristiques = caracteristiques;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_INDNO")
	public Set<RegpmAdresseIndividu> getAdresses() {
		return adresses;
	}

	public void setAdresses(Set<RegpmAdresseIndividu> adresses) {
		this.adresses = adresses;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_INDNO")
	public Set<RegpmMandat> getMandants() {
		return mandants;
	}

	public void setMandants(Set<RegpmMandat> mandants) {
		this.mandants = mandants;
	}
}
