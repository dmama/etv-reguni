package ch.vd.unireg.regimefiscal;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;

/**
 * @author Raphaël Marmier, 2017-05-03, <raphael.marmier@vd.ch>
 */
public interface RegimeFiscalServiceConfiguration {

	/**
	 * Contient les informations de mapping d'une forme juridique vers un type de régime fiscal valide pendant une certaine période.
	 */
	class FormeJuridiqueMapping implements DateRange {

		@Nullable
		private final RegDate dateDebut;
		@Nullable
		private final RegDate dateFin;
		@NotNull
		private final FormeJuridiqueEntreprise formeJuridique;
		@NotNull
		private final String codeRegime;

		public FormeJuridiqueMapping(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @NotNull FormeJuridiqueEntreprise formeJuridique, @NotNull String codeRegime) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.formeJuridique = formeJuridique;
			this.codeRegime = codeRegime;
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
		public String getCodeRegime() {
			return codeRegime;
		}
	}

	/**
	 * Retourne le code du type de régime fiscal correspondant par configuration à la forme juridique.
	 * @param formeJuridique la forme juridique pour laquelle on cherche un code.
	 * @return le code configuré pour la forme juridique, ou <code>null</code> si aucun.
	 */
	@NotNull
	List<FormeJuridiqueMapping> getMapping(FormeJuridiqueEntreprise formeJuridique);

	/**
	 * Détermine si le type de régime fiscal entraine une DI vaudoise optionnelle
	 */
	boolean isRegimeFiscalDiOptionnelleVd(String codeTypeRegimeFiscal);
}
