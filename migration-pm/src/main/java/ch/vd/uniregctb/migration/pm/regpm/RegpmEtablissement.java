package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Set;
import java.util.SortedSet;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FixedCharUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.LongZeroIsNullUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ETABLISSEMENT")
@TypeDefs({
		@TypeDef(name = "FixedChar", typeClass = FixedCharUserType.class),
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "LongZeroIsNull", typeClass = LongZeroIsNullUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class)
})
public class RegpmEtablissement extends RegpmEntity implements WithLongId {

	private static final long serialVersionUID = -7209488522260407956L;

	private Long id;
	private String raisonSociale1;
	private String raisonSociale2;
	private String raisonSociale3;
	private RegDate dateInscriptionRC;
	private RegDate dateRadiationRC;
	private String enseigne;
	private RegDate dateInformation;        // Date à partir de laquelle les divers services communaux signalent à la Chancellerie PM la présence d'un établissement sur leur territoire.
	private String noTelephone;
	private String noFax;
	private String chez;
	private String nomRue;
	private String noPolice;
	private RegpmCoordonneesFinancieres coordonneesFinancieres;
	private NumeroIDE numeroIDE;
	private Long numeroCantonal;
	private SortedSet<InscriptionRC> inscriptionsRC;
	private SortedSet<RadiationRC> radiationsRC;
	private RegpmEntreprise entreprise;
	private RegpmIndividu individu;
	private RegpmLocalitePostale localitePostale;
	private RegpmRue rue;
	private Set<RegpmEtablissementStable> etablissementsStables;
	private Set<RegpmEtablissement> succursales;
	private SortedSet<RegpmDomicileEtablissement> domicilesEtablissements;
	private Set<RegpmRattachementProprietaire> rattachementsProprietaires;
	private Set<RegpmAppartenanceGroupeProprietaire> appartenancesGroupeProprietaire;
	private Set<RegpmMandat> mandants;

	// no institution ?, mandat contribuable ?, ...

	/**
	 * Une façade de l'adresse portée par l'établissement qui peut entrer dans le StreetDataMigrator
	 */
	private class Adresse implements AdresseAvecRue {

		@Override
		public RegDate getDateDebut() {
			return etablissementsStables.stream()
					.map(RegpmEtablissementStable::getDateDebut)
					.min(NullDateBehavior.EARLIEST::compare)
					.orElse(null);
		}

		@Override
		public RegDate getDateFin() {
			// si l'un des établissements stables a une date de fin vide, la date de fin est vide
			final boolean finVideExiste = etablissementsStables.stream()
					.filter(e -> e.getDateFin() == null)
					.findAny()
					.isPresent();
			if (finVideExiste) {
				return null;
			}

			// aucune date de fin vide -> on prend la plus récente
			return etablissementsStables.stream()
					.map(RegpmEtablissementStable::getDateFin)
					.max(NullDateBehavior.LATEST::compare)
					.get();
		}

		@Override
		public String getNomRue() {
			return nomRue;
		}

		@Override
		public String getNoPolice() {
			return noPolice;
		}

		@Override
		public String getLieu() {
			return null;
		}

		@Override
		public RegpmLocalitePostale getLocalitePostale() {
			return localitePostale;
		}

		@Override
		public RegpmRue getRue() {
			return rue;
		}

		@Override
		public Integer getOfsPays() {
			return ServiceInfrastructureService.noOfsSuisse;
		}
	}

	@Transient
	public AdresseAvecRue getAdresse() {
		// l'adresse est valide aux dates des établissements stables... donc pas d'établissement stable = pas d'adresse !
		return etablissementsStables.isEmpty() ? null : new Adresse();
	}

	@Id
	@Column(name = "NO_ETABLISSEMENT")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "RAISON_SOC_LGN1")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale1() {
		return raisonSociale1;
	}

	public void setRaisonSociale1(String raisonSociale1) {
		this.raisonSociale1 = raisonSociale1;
	}

	@Column(name = "RAISON_SOC_LGN2")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale2() {
		return raisonSociale2;
	}

	public void setRaisonSociale2(String raisonSociale2) {
		this.raisonSociale2 = raisonSociale2;
	}

	@Column(name = "RAISON_SOC_LGN3")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getRaisonSociale3() {
		return raisonSociale3;
	}

	public void setRaisonSociale3(String raisonSociale3) {
		this.raisonSociale3 = raisonSociale3;
	}

	@Column(name = "DA_INSC_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateInscriptionRC() {
		return dateInscriptionRC;
	}

	public void setDateInscriptionRC(RegDate dateInscriptionRC) {
		this.dateInscriptionRC = dateInscriptionRC;
	}

	@Column(name = "DA_RADIATION_RC_VD")
	@Type(type = "RegDate")
	public RegDate getDateRadiationRC() {
		return dateRadiationRC;
	}

	public void setDateRadiationRC(RegDate dateRadiationRC) {
		this.dateRadiationRC = dateRadiationRC;
	}

	@Column(name = "ENSEIGNE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getEnseigne() {
		return enseigne;
	}

	public void setEnseigne(String enseigne) {
		this.enseigne = enseigne;
	}

	@Column(name = "DA_INFORMATION")
	@Type(type = "RegDate")
	public RegDate getDateInformation() {
		return dateInformation;
	}

	public void setDateInformation(RegDate dateInformation) {
		this.dateInformation = dateInformation;
	}

	@Column(name = "NO_TELEPHONE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoTelephone() {
		return noTelephone;
	}

	public void setNoTelephone(String noTelephone) {
		this.noTelephone = noTelephone;
	}

	@Column(name = "NO_FAX")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "16"))
	public String getNoFax() {
		return noFax;
	}

	public void setNoFax(String noFax) {
		this.noFax = noFax;
	}

	@Column(name = "CHEZ")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getChez() {
		return chez;
	}

	public void setChez(String chez) {
		this.chez = chez;
	}

	@Column(name = "RUE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "30"))
	public String getNomRue() {
		return nomRue;
	}

	public void setNomRue(String nomRue) {
		this.nomRue = nomRue;
	}

	@Column(name = "NO_POLICE")
	@Type(type = "FixedChar", parameters = @Parameter(name = "length", value = "10"))
	public String getNoPolice() {
		return noPolice;
	}

	public void setNoPolice(String noPolice) {
		this.noPolice = noPolice;
	}

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "noCCP", column = @Column(name = "NO_CCP")),
			@AttributeOverride(name = "noCompteBancaire", column = @Column(name = "NO_CPTE_BANCAIRE")),
			@AttributeOverride(name = "iban", column = @Column(name = "IBAN")),
			@AttributeOverride(name = "bicSwift", column = @Column(name = "BIC_SWIFT")),
			@AttributeOverride(name = "nomInstitutionFinanciere", column = @Column(name = "NOM_INSTIT_FIN"))
	})
	@AssociationOverride(name = "institutionFinanciere", joinColumns = @JoinColumn(name = "FK_INSNO"))
	public RegpmCoordonneesFinancieres getCoordonneesFinancieres() {
		return coordonneesFinancieres;
	}

	public void setCoordonneesFinancieres(RegpmCoordonneesFinancieres coordonneesFinancieres) {
		this.coordonneesFinancieres = coordonneesFinancieres;
	}

	@Embedded
	public NumeroIDE getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(NumeroIDE numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	@Column(name = "NO_CANTONAL")
	@Type(type = "LongZeroIsNull")
	public Long getNumeroCantonal() {
		return numeroCantonal;
	}

	public void setNumeroCantonal(Long numeroCantonal) {
		this.numeroCantonal = numeroCantonal;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<InscriptionRC> getInscriptionsRC() {
		return inscriptionsRC;
	}

	public void setInscriptionsRC(SortedSet<InscriptionRC> inscriptionsRC) {
		this.inscriptionsRC = inscriptionsRC;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RadiationRC> getRadiationsRC() {
		return radiationsRC;
	}

	public void setRadiationsRC(SortedSet<RadiationRC> radiationsRC) {
		this.radiationsRC = radiationsRC;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INDNO")
	public RegpmIndividu getIndividu() {
		return individu;
	}

	public void setIndividu(RegpmIndividu individu) {
		this.individu = individu;
	}

	@ManyToOne
	@JoinColumn(name = "FK_ENTPRNO")
	public RegpmEntreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(RegpmEntreprise entreprise) {
		this.entreprise = entreprise;
	}

	@ManyToOne
	@JoinColumn(name = "FK_LOC_POSTNO")
	public RegpmLocalitePostale getLocalitePostale() {
		return localitePostale;
	}

	public void setLocalitePostale(RegpmLocalitePostale localitePostale) {
		this.localitePostale = localitePostale;
	}

	@ManyToOne
	@JoinColumn(name = "FK_RUENO")
	public RegpmRue getRue() {
		return rue;
	}

	public void setRue(RegpmRue rue) {
		this.rue = rue;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "FK_ETABNO")
	public Set<RegpmEtablissementStable> getEtablissementsStables() {
		return etablissementsStables;
	}

	public void setEtablissementsStables(Set<RegpmEtablissementStable> etablissementsStables) {
		this.etablissementsStables = etablissementsStables;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABLISSEMENNO")
	public Set<RegpmEtablissement> getSuccursales() {
		return succursales;
	}

	public void setSuccursales(Set<RegpmEtablissement> succursales) {
		this.succursales = succursales;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	@Sort(type = SortType.NATURAL)
	public SortedSet<RegpmDomicileEtablissement> getDomicilesEtablissements() {
		return domicilesEtablissements;
	}

	public void setDomicilesEtablissements(SortedSet<RegpmDomicileEtablissement> domicilesEtablissements) {
		this.domicilesEtablissements = domicilesEtablissements;
	}

	@OneToMany
	@JoinColumn(name = "FK_ETABNO")
	public Set<RegpmRattachementProprietaire> getRattachementsProprietaires() {
		return rattachementsProprietaires;
	}

	public void setRattachementsProprietaires(Set<RegpmRattachementProprietaire> rattachementsProprietaires) {
		this.rattachementsProprietaires = rattachementsProprietaires;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	public Set<RegpmAppartenanceGroupeProprietaire> getAppartenancesGroupeProprietaire() {
		return appartenancesGroupeProprietaire;
	}

	public void setAppartenancesGroupeProprietaire(Set<RegpmAppartenanceGroupeProprietaire> appartenancesGroupeProprietaire) {
		this.appartenancesGroupeProprietaire = appartenancesGroupeProprietaire;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "FK_ETABNO")
	public Set<RegpmMandat> getMandants() {
		return mandants;
	}

	public void setMandants(Set<RegpmMandat> mandants) {
		this.mandants = mandants;
	}
}
