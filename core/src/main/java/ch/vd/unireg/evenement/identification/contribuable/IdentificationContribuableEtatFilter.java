package ch.vd.unireg.evenement.identification.contribuable;

import java.util.EnumSet;
import java.util.Set;

public enum IdentificationContribuableEtatFilter {

	SEULEMENT_TRAITES {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return ETATS_TRAITES.contains(etat);
		}
	},
	SEULEMENT_NON_TRAITES {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return ETATS_NON_TRAITES.contains(etat);
		}
	},
	SEULEMENT_SUSPENDUS {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return ETATS_SUSPENDUS.contains(etat);
		}
	},

	SEULEMENT_A_TRAITER_MANUELLEMENT {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT == etat;
		}
	},

	SEULEMENT_NON_TRAITES_ET_EN_EXEPTION {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return ETATS_NON_TRAITES_ET_EN_EXCEPTION.contains(etat);
		}
	},

	SEULEMENT_NON_FINAUX {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return ETATS_NON_FINAUX.contains(etat);
		}
	},

	TOUS {
		@Override
		public boolean isIncluded(IdentificationContribuable.Etat etat) {
			return true;
		}
	};

	private static final Set<IdentificationContribuable.Etat> ETATS_TRAITES = EnumSet.of(IdentificationContribuable.Etat.TRAITE_AUTOMATIQUEMENT,
	                                                                                     IdentificationContribuable.Etat.TRAITE_MANUELLEMENT,
	                                                                                     IdentificationContribuable.Etat.TRAITE_MAN_EXPERT,
	                                                                                     IdentificationContribuable.Etat.NON_IDENTIFIE);

	private static final Set<IdentificationContribuable.Etat> ETATS_NON_TRAITES = EnumSet.of(IdentificationContribuable.Etat.A_EXPERTISER,
	                                                                                         IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT);

	private static final Set<IdentificationContribuable.Etat> ETATS_NON_TRAITES_ET_EN_EXCEPTION = EnumSet.of(IdentificationContribuable.Etat.A_EXPERTISER,
	                                                                                                         IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT,
	                                                                                                         IdentificationContribuable.Etat.EXCEPTION);

	private static final Set<IdentificationContribuable.Etat> ETATS_SUSPENDUS = EnumSet.of(IdentificationContribuable.Etat.A_EXPERTISER_SUSPENDU,
	                                                                                       IdentificationContribuable.Etat.A_TRAITER_MAN_SUSPENDU);

	private static final Set<IdentificationContribuable.Etat> ETATS_NON_FINAUX = EnumSet.of(IdentificationContribuable.Etat.A_EXPERTISER,
	                                                                                        IdentificationContribuable.Etat.A_EXPERTISER_SUSPENDU,
	                                                                                        IdentificationContribuable.Etat.A_TRAITER_MAN_SUSPENDU,
	                                                                                        IdentificationContribuable.Etat.A_TRAITER_MANUELLEMENT,
	                                                                                        IdentificationContribuable.Etat.EXCEPTION);

	public abstract boolean isIncluded(IdentificationContribuable.Etat etat);
}
