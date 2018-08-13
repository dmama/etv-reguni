package ch.vd.unireg.regimefiscal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * Contient les informations de mapping d'une forme juridique vers un type de régime fiscal valide pendant une certaine période.
 */
public class FormeJuridiqueVersTypeRegimeFiscalMapping implements DateRange {

	@Nullable
	private final RegDate dateDebut;
	@Nullable
	private final RegDate dateFin;
	@NotNull
	private final FormeJuridiqueEntreprise formeJuridique;
	@NotNull
	private final TypeRegimeFiscal typeRegimeFiscal;

	public FormeJuridiqueVersTypeRegimeFiscalMapping(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @NotNull FormeJuridiqueEntreprise formeJuridique, @NotNull TypeRegimeFiscal typeRegimeFiscal) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.formeJuridique = formeJuridique;
		this.typeRegimeFiscal = typeRegimeFiscal;
	}

	@Nullable
	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Nullable
	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@NotNull
	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	@NotNull
	public TypeRegimeFiscal getTypeRegimeFiscal() {
		return typeRegimeFiscal;
	}
}
