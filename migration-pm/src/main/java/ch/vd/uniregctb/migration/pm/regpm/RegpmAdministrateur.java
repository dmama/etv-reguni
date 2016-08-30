package ch.vd.uniregctb.migration.pm.regpm;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.usertype.BooleanYesNoUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.FonctionUserType;
import ch.vd.uniregctb.migration.pm.regpm.usertype.RegDateUserType;

@Entity
@Table(name = "ADMINISTRATEUR")
@TypeDefs({
		@TypeDef(name = "RegDate", typeClass = RegDateUserType.class),
		@TypeDef(name = "BooleanYesNo", typeClass = BooleanYesNoUserType.class),
		@TypeDef(name = "Fonction", typeClass = FonctionUserType.class)
})
public class RegpmAdministrateur extends RegpmEntity {

	/**
	 * Ils ont fait une clé primaire avec le numéro de l'entreprise et un numéro de séquence
	 */
	@Embeddable
	public static class PK implements Serializable, Comparable<PK> {

		private Integer seqNo;
		private Long idIndividu;

		public PK() {
		}

		public PK(Integer seqNo, Long idIndividu) {
			this.seqNo = seqNo;
			this.idIndividu = idIndividu;
		}

		@Override
		public int compareTo(@NotNull PK o) {
			int comparison = Long.compare(idIndividu, o.idIndividu);
			if (comparison == 0) {
				comparison = seqNo - o.seqNo;
			}
			return comparison;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final PK pk = (PK) o;

			if (seqNo != null ? !seqNo.equals(pk.seqNo) : pk.seqNo != null) return false;
			return idIndividu != null ? idIndividu.equals(pk.idIndividu) : pk.idIndividu == null;
		}

		@Override
		public int hashCode() {
			int result = seqNo != null ? seqNo.hashCode() : 0;
			result = 31 * result + (idIndividu != null ? idIndividu.hashCode() : 0);
			return result;
		}

		@Column(name = "NO_SEQUENCE")
		public Integer getSeqNo() {
			return seqNo;
		}

		public void setSeqNo(Integer seqNo) {
			this.seqNo = seqNo;
		}

		@Column(name = "FK_INDNO")
		public Long getIdIndividu() {
			return idIndividu;
		}

		public void setIdIndividu(Long idIndividu) {
			this.idIndividu = idIndividu;
		}
	}

	private PK id;
	private RegDate dateEntreeFonction;
	private boolean rectifiee;
	private RegpmIndividu administrateur;
	private RegpmFonction fonction;
	private Set<RegpmFinMandatAdministrateur> fins;

	/**
	 * Valeur <strong>transiente</strong> (au sens de la sérialisation java) pour ne pas être exposée dans le graphe final
	 */
	private transient RegpmEntreprise entreprise;

	/**
	 * Les régimes fiscaux VD de l'entreprise (c'est cette information qui restera dans le graphe une fois sérialisé/dé-sérialisé
	 */
	private SortedSet<RegpmRegimeFiscalVD> regimesFiscauxVD;

	/**
	 * L'identifiant de l'entreprise administrée (cette information restera dans le graphe une fois sérialisé/dé-sérialisé
	 */
	private Long entrepriseId;

	@EmbeddedId
	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

	@Column(name = "DAD_FONCTION")
	@Type(type = "RegDate")
	public RegDate getDateEntreeFonction() {
		return dateEntreeFonction;
	}

	public void setDateEntreeFonction(RegDate dateEntreeFonction) {
		this.dateEntreeFonction = dateEntreeFonction;
	}

	@Column(name = "RECTIFIEE")
	@Type(type = "BooleanYesNo", parameters = @Parameter(name = "default", value = "false"))
	public boolean isRectifiee() {
		return rectifiee;
	}

	public void setRectifiee(boolean rectifiee) {
		this.rectifiee = rectifiee;
	}

	@ManyToOne
	@JoinColumn(name = "FK_INDNO", updatable = false, insertable = false)
	public RegpmIndividu getAdministrateur() {
		return administrateur;
	}

	public void setAdministrateur(RegpmIndividu administrateur) {
		this.administrateur = administrateur;
	}

	/**
	 * <strong>Attention</strong> l'entreprise est un membre "transient" (au sens de la sérialisation java).
	 * Il n'est donc pas censé être utilisé au moment de l'utilisation des données du graphe !!
	 */
	@ManyToOne
	@JoinColumn(name = "FK_ENTPRNO")
	public RegpmEntreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(RegpmEntreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Column(name = "FK_FCTCO")
	@Type(type = "Fonction")
	public RegpmFonction getFonction() {
		return fonction;
	}

	public void setFonction(RegpmFonction fonction) {
		this.fonction = fonction;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "FK_ADMEURFKIND", referencedColumnName = "FK_INDNO"),
			@JoinColumn(name = "FK_ADMEURNO", referencedColumnName = "NO_SEQUENCE")
	})
	public Set<RegpmFinMandatAdministrateur> getFins() {
		return fins;
	}

	public void setFins(Set<RegpmFinMandatAdministrateur> fins) {
		this.fins = fins;
	}

	@Transient
	public SortedSet<RegpmRegimeFiscalVD> getRegimesFiscauxVD() {
		return entreprise != null ? entreprise.getRegimesFiscauxVD() : regimesFiscauxVD;
	}

	@Transient
	public Long getEntrepriseId() {
		return entreprise != null ? entreprise.getId() : entrepriseId;
	}

	public void initEntrepriseData(@NotNull RegpmEntreprise e) {
		initEntrepriseData(e.getRegimesFiscauxVD(), e.getId());
	}

	public void initEntrepriseData(@NotNull SortedSet<RegpmRegimeFiscalVD> regimes, long entrepriseId) {
		this.regimesFiscauxVD = new TreeSet<>(regimes);     // on force le chargement de la collection!
		this.entrepriseId = entrepriseId;
	}

	/**
	 * Utilisable lors de la constitution du graphe afin de récupérer dans les données de l'entreprise
	 * (qui ne survivra pas à un passage par l'état sérialisé) ce qui nous servira par la suite
	 * (= dans des champs qui survivront, eux, au passage par l'état sérialisé)
	 */
	public void fetchEntrepriseData() {
		if (entreprise != null) {
			initEntrepriseData(entreprise);
		}
	}
}
