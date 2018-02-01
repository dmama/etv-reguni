package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.unireg.type.GenreImpot;

@Entity
@DiscriminatorValue("ForDebiteurPrestationImposable")
public class ForDebiteurPrestationImposable extends ForFiscalAvecMotifs {

	public ForDebiteurPrestationImposable() {
		setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
	}

	public ForDebiteurPrestationImposable(ForDebiteurPrestationImposable fdpi) {
		super(fdpi);
	}

	@Transient
	@Override
	public boolean isDebiteur() {
		return true;
	}

	@Override
	public ForFiscal duplicate() {
		return new ForDebiteurPrestationImposable(this);
	}
}
