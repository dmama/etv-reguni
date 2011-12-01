package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Classe de base pour les resolvers de mode d'imposition
 */
public abstract class ModeImpositionResolver {

	public class Imposition {

		private RegDate dateDebut;
		private ModeImposition modeImposition;

		public Imposition() {
		}

		public Imposition(RegDate dateDebut, ModeImposition modeImposition) {
			this.dateDebut = dateDebut;
			this.modeImposition = modeImposition;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public void setDateDebut(RegDate dateDebut) {
			this.dateDebut = dateDebut;
		}

		public ModeImposition getModeImposition() {
			return modeImposition;
		}

		public void setModeImposition(ModeImposition modeImposition) {
			this.modeImposition = modeImposition;
		}
	}

	private final TiersService tiersService;

	public ModeImpositionResolver(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	protected TiersService getTiersService() {
		return tiersService;
	}
}
