package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.Contribuable;

/**
 * Une communauté représente un groupement de tiers qui possèdent ensemble un droit sur un immeuble.
 */
@Entity
@DiscriminatorValue("Communaute")
public class CommunauteRF extends AyantDroitRF {

	private TypeCommunaute type;

	/**
	 * Les droits de propriété des membres de la communauté.
	 */
	private Set<DroitProprietePersonneRF> membres;

	/**
	 * Historique des regroupements de cette communauté vers des modèles de communautés.
	 */
	private Set<RegroupementCommunauteRF> regroupements;

	@Column(name = "TYPE_COMMUNAUTE", length = LengthConstants.RF_TYPE_COMMUNAUTE)
	@Enumerated(EnumType.STRING)
	public TypeCommunaute getType() {
		return type;
	}

	public void setType(TypeCommunaute type) {
		this.type = type;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final CommunauteRF commRight = (CommunauteRF) right;
		commRight.type = this.type;
	}

	// configuration hibernate : la comunauté ne possède pas les membres
	@OneToMany(mappedBy = "communaute")
	public Set<DroitProprietePersonneRF> getMembres() {
		return membres;
	}

	@Transient
	public void addMembre(DroitProprietePersonneRF droit) {
		if (membres == null) {
			membres = new HashSet<>();
		}
		membres.add(droit);
	}

	public void setMembres(Set<DroitProprietePersonneRF> membres) {
		this.membres = membres;
	}

	// configuration hibernate : la communauté possède les regroupements
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "COMMUNAUTE_ID", nullable = false)
	@ForeignKey(name = "FK_REGRCOMM_RF_COMMUNAUTE_ID")
	public Set<RegroupementCommunauteRF> getRegroupements() {
		return regroupements;
	}

	public void setRegroupements(Set<RegroupementCommunauteRF> regroupements) {
		this.regroupements = regroupements;
	}

	public void addRegroupement(@NotNull RegroupementCommunauteRF regroupement) {
		if (regroupements == null) {
			regroupements = new HashSet<>();
		}
		regroupement.setCommunaute(this);
		regroupements.add(regroupement);
	}

	/**
	 * @return les infos des membres de la communauté <i>non-triés</i>.
	 */
	@NotNull
	public CommunauteRFMembreInfo buildMembreInfoNonTries() {

		// on extrait la liste des tiers RF
		final List<TiersRF> tiersRF = membres.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DroitProprieteRF::getAyantDroit)
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.collect(Collectors.toList());

		// on extrait la liste des numéros de contribuables rapprochés
		final List<Long> ctbIds = new ArrayList<>();
		for (int i = tiersRF.size() - 1; i >= 0; --i) {
			final TiersRF tiers = tiersRF.get(i);
			final Contribuable ctb = tiers.getCtbRapproche();
			if (ctb != null) {
				ctbIds.add(ctb.getId());
				tiersRF.remove(i);  // on supprime le tiers de la liste des tiers non-rapprochés
			}
		}

		// [SIFISC-28067] on crée l'historique d'appartenance des membres
		final Collection<CommunauteRFAppartenanceInfo> membresHisto = membres.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(CommunauteRFAppartenanceInfo::new)
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toList());

		return new CommunauteRFMembreInfo(ctbIds, tiersRF, membresHisto);
	}

	/**
	 * @return le principal de communauté courant s'il a été explicitement désigné; <i>null</i> si aucun principal n'a été désigné.
	 */
	@Transient
	@Nullable
	public AyantDroitRF getPrincipalCommunauteDesigne() {
		return regroupements.stream()
				.filter(r -> r.isValidAt(null))
				.findFirst()
				.map(RegroupementCommunauteRF::getModele)
				.map(ModeleCommunauteRF::getPrincipalCourant)
				.orElse(null);

	}
}
