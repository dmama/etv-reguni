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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * Modèle de communauté qui permet de regrouper les communautés RF constituées des mêmes membres.
 */
@Entity
@Table(name = "RF_MODELE_COMMUNAUTE")
public class ModeleCommunauteRF extends HibernateEntity implements LinkedEntity {

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
	@JoinTable(name = "RF_MEMBRE_COMMUNAUTE",
			joinColumns = @JoinColumn(name = "MODEL_COMMUNAUTE_ID"),
			inverseJoinColumns = @JoinColumn(name = "AYANT_DROIT_ID"))
	public Set<AyantDroitRF> getMembres() {
		return membres;
	}

	public void setMembres(Set<AyantDroitRF> membres) {
		this.membres = membres;
	}

	public void addMembre(AyantDroitRF ayantDroit) {
		if (membres == null) {
			membres = new HashSet<>();
		}
		membres.add(ayantDroit);
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

	public void addPrincipal(PrincipalCommunauteRF principal) {
		if (principaux == null) {
			principaux = new HashSet<>();
		}
		principaux.add(principal);
	}

	// configuration hibernate : le modèle de communauté ne possède pas les regroupements
	@OneToMany(mappedBy = "modele", fetch = FetchType.LAZY)
	public Set<RegroupementCommunauteRF> getRegroupements() {
		return regroupements;
	}

	public void setRegroupements(Set<RegroupementCommunauteRF> regroupements) {
		this.regroupements = regroupements;
	}

	@Transient
	public void addRegroupement(RegroupementCommunauteRF regroupement) {
		if (regroupements == null) {
			regroupements = new HashSet<>();
		}
		regroupements.add(regroupement);
	}

	public static int hashCode(@Nullable Collection<? extends AyantDroitRF> ayantsDroits) {
		if (ayantsDroits == null) {
			return 0;
		}
		// on retourne un hashcode des ids des ayants-droits

		final List<? extends AyantDroitRF> sorted = new ArrayList<>(ayantsDroits);
		sorted.sort(Comparator.comparing(AyantDroitRF::getId)); // pour avoir un hashcode indépendant de l'ordre d'itération des éléments

		// le code ci-dessus est adapté de Arrays::hasCode() pour se prémunir de toutes modifications de l'algorithme
		// (comme le hashCode est persisté, il est vraiment critique que l'algorithme soit constant dans le temps)
		int result = 1;
		for (AyantDroitRF a : sorted) {
			result = 31 * result + (a == null ? 0 : a.getId().hashCode());
		}

		return result;
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

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntity.Context context, boolean includeAnnuled) {
		// si le modèle de communauté change, on veut notifier que les regroupements concernés ont changé
		return regroupements == null ? null : new ArrayList<Object>(regroupements);
	}

	/**
	 * @return le principal de communauté courant s'il a été explicitement désigné; <i>null</i> si aucun principal n'a été désigné.
	 */
	@Transient
	@Nullable
	public AyantDroitRF getPrincipalCourant() {
		return principaux == null ? null : principaux.stream()
				.filter(p -> p.isValidAt(null))
				.findFirst()
				.map(PrincipalCommunauteRF::getPrincipal)
				.orElse(null);
	}
}
