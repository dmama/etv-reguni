package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;

/**
 * Modèle de communauté qui permet de regrouper les communautés RF constituées des mêmes membres.
 */
@Entity
@Table(name = "MODELE_COMMUNAUTE_RF")
public class ModeleCommunauteRF extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La clé de hashage des membres du modèle de communauté.
	 */
	private int membresHashCode;

	/**
	 * Les membres du modèle de communauté.
	 */
	private Set<AyantDroitRF> membres;

	/**
	 * L'historique des principaux du modèle de communauté.
	 */
	private Set<PrincipalCommunauteRF> principaux;

	/**
	 * Les regroupements qui pointent vers ce modèle de communauté.
	 */
	private Set<RegroupementCommunauteRF> regroupements;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "MEMBRES_HASH_CODE")
	@Index(name = "IDX_MODCOMM_HASHCODE", columnNames = "MEMBRES_HASH_CODE")
	public int getMembresHashCode() {
		return membresHashCode;
	}

	public void setMembresHashCode(int membresHashCode) {
		this.membresHashCode = membresHashCode;
	}

	// configuration hibernate : le modèle de communauté gère les membres
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "MEMBRE_COMMUNAUTE_RF",
			joinColumns = @JoinColumn(name = "MODEL_COMMUNAUTE_ID"),
			inverseJoinColumns = @JoinColumn(name = "AYANT_DROIT_ID"))
	public Set<AyantDroitRF> getMembres() {
		return membres;
	}

	public void setMembres(Set<AyantDroitRF> membres) {
		this.membres = membres;
	}

	// configuration hibernate : le modèle de communauté possède les principaux
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "MODEL_COMMUNAUTE_ID", nullable = false)
	@ForeignKey(name = "FK_PRINC_MODCOMM_ID")
	public Set<PrincipalCommunauteRF> getPrincipaux() {
		return principaux;
	}

	public void setPrincipaux(Set<PrincipalCommunauteRF> principaux) {
		this.principaux = principaux;
	}

	// configuration hibernate : le modèle de communauté ne possède pas les regroupements
	@OneToMany(mappedBy = "modele", fetch = FetchType.LAZY)
	public Set<RegroupementCommunauteRF> getRegroupements() {
		return regroupements;
	}

	public void setRegroupements(Set<RegroupementCommunauteRF> regroupements) {
		this.regroupements = regroupements;
	}

	public static int hashCode(@Nullable Collection<? extends AyantDroitRF> ayantsDroits) {
		if (ayantsDroits == null) {
			return 0;
		}
		// on retourne un hashcode des ids des ayants-droits
		return Objects.hash(ayantsDroits.stream()
				                    .map(AyantDroitRF::getId)
				                    .sorted(Comparator.naturalOrder())  // pour avoir un hashcode indépendant de l'ordre d'itération des éléments
				                    .toArray());
	}

	/**
	 * @return <i>vrai</i> si les membres de la communauté sont les mêmes (= mêmes ids) que ceux passes en paramètre
	 */
	public boolean matches(Set<? extends AyantDroitRF> membres) {

		final Set<Long> thisIds = this.membres.stream()
				.map(AyantDroitRF::getId)
				.collect(Collectors.toSet());

		final Set<Long> othersIds = membres.stream()
				.map(AyantDroitRF::getId)
				.collect(Collectors.toSet());

		return thisIds.equals(othersIds);
	}
}
