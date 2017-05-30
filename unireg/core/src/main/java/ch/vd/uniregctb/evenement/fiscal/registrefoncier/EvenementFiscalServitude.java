package ch.vd.uniregctb.evenement.fiscal.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "SERVITUDE_RF")
public class EvenementFiscalServitude extends EvenementFiscalDroit {

	public enum TypeEvenementServitude {
		USUFRUIT,
		DROIT_HABITATION
	}

	private Set<AyantDroitRF> ayantDroits;
	private Set<ImmeubleRF> immeubles;
	private TypeEvenementServitude typeServitude;

	public EvenementFiscalServitude() {
	}

	public EvenementFiscalServitude(@Nullable RegDate dateValeur, @NotNull Set<AyantDroitRF> ayantDroits, @NotNull Set<ImmeubleRF> immeubles, @NotNull EvenementFiscalServitude.TypeEvenementFiscalDroitPropriete type,
	                                @NotNull EvenementFiscalServitude.TypeEvenementServitude typeServitude) {
		super(dateValeur, type);
		this.ayantDroits = new HashSet<>(ayantDroits);
		this.immeubles = new HashSet<>(immeubles);
		this.typeServitude = typeServitude;
	}

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "EVT_FISCAL_SERVITUDE_AYANTDRT",
			joinColumns = @JoinColumn(name = "EVENEMENT_ID"),
			inverseJoinColumns = @JoinColumn(name = "AYANT_DROIT_ID"))
	public Set<AyantDroitRF> getAyantDroits() {
		return ayantDroits;
	}

	public void setAyantDroits(Set<AyantDroitRF> ayantDroits) {
		this.ayantDroits = ayantDroits;
	}

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(name = "EVT_FISCAL_SERVITUDE_IMMEUBLE",
			joinColumns = @JoinColumn(name = "EVENEMENT_ID"),
			inverseJoinColumns = @JoinColumn(name = "IMMEUBLE_ID"))
	public Set<ImmeubleRF> getImmeubles() {
		return immeubles;
	}

	public void setImmeubles(Set<ImmeubleRF> immeubles) {
		this.immeubles = immeubles;
	}

	@Column(name = "TYPE_EVT_SERVITUDE", length = LengthConstants.EVTFISCAL_TYPE_EVT_SERVITUDE)
	@Enumerated(EnumType.STRING)
	public TypeEvenementServitude getTypeServitude() {
		return typeServitude;
	}

	public void setTypeServitude(TypeEvenementServitude typeServitude) {
		this.typeServitude = typeServitude;
	}
}
