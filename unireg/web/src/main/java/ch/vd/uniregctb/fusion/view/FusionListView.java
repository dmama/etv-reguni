package ch.vd.uniregctb.fusion.view;

import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class FusionListView extends TiersCriteriaView{

		/**
		 * Serial ID
		 */
		private static final long serialVersionUID = 5427261326083899967L;

		TiersGeneralView nonHabitant;

		TiersGeneralView habitant;

		public TiersGeneralView getNonHabitant() {
			return nonHabitant;
		}

		public void setNonHabitant(TiersGeneralView nonHabitant) {
			this.nonHabitant = nonHabitant;
		}

		public TiersGeneralView getHabitant() {
			return habitant;
		}

		public void setHabitant(TiersGeneralView habitant) {
			this.habitant = habitant;
		}

}
