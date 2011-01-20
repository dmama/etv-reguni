package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Interface pour la résolution du nouveau mode d'imposition pour un contribuable.
 * 
 * @author Pavel BLANCO
 *
 */
public interface ModeImpositionResolver {
	
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
	
	/**
	 * Calcule du nouveau mode d'imposition pour le contribuable à la date et en prenant en compte son ancien mode d'imposition.
	 * 
	 * @param contribuable le contribuable
	 * @param date date à partir de laquelle le mode d'imposition sera appliqué
	 * @param imposition l'ancien mode d'imposition
	 * @return le nouveau mode d'imposition
	 * 
	 * @throws ModeImpositionResolverException
	 */
	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition imposition) throws ModeImpositionResolverException;
	
}

