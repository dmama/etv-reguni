package ch.vd.uniregctb.evenement.fiscal.registrefoncier;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;

@Entity
@DiscriminatorValue(value = "DROIT_PROPRIETE_RF")
public class EvenementFiscalDroitPropriete extends EvenementFiscalDroit {

	private AyantDroitRF ayantDroit;
	private ImmeubleRF immeuble;
	@Nullable
	private CommunauteRF communaute;

	public EvenementFiscalDroitPropriete() {
	}

	public EvenementFiscalDroitPropriete(@Nullable RegDate dateValeur, @NotNull AyantDroitRF ayantDroit, @NotNull ImmeubleRF immeuble, @Nullable CommunauteRF communaute, @NotNull EvenementFiscalDroitPropriete.TypeEvenementFiscalDroitPropriete type) {
		super(dateValeur, type);
		this.ayantDroit = ayantDroit;
		this.immeuble = immeuble;
	}

	@JoinColumn(name = "AYANT_DROIT_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_AYANTDROIT_ID")
	public AyantDroitRF getAyantDroit() {
		return ayantDroit;
	}

	public void setAyantDroit(AyantDroitRF ayantDroit) {
		this.ayantDroit = ayantDroit;
	}

	@JoinColumn(name = "IMMEUBLE_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	@Nullable
	@JoinColumn(name = "COMMUNAUTE_ID")
	@ManyToOne(fetch = FetchType.EAGER)
	@ForeignKey(name = "FK_EVTFISC_COMMUNAUTE_ID")
	public CommunauteRF getCommunaute() {
		return communaute;
	}

	public void setCommunaute(@Nullable CommunauteRF communaute) {
		this.communaute = communaute;
	}
}
