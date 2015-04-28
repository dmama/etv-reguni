package ch.vd.uniregctb.type;

import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tutelle;

/**
 * <!-- begin-user-doc --> Longueur de colonne : 20 <!-- end-user-doc -->
 */
public enum TypeRapportEntreTiers {

	TUTELLE {
		@Override
		public RapportEntreTiers newInstance() {
			return new Tutelle();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return Tutelle.class;
		}
	},

	CURATELLE {
		@Override
		public RapportEntreTiers newInstance() {
			return new Curatelle();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return Curatelle.class;
		}
	},

	CONSEIL_LEGAL {
		@Override
		public RapportEntreTiers newInstance() {
			return new ConseilLegal();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return ConseilLegal.class;
		}
	},

	PRESTATION_IMPOSABLE {
		@Override
		public RapportEntreTiers newInstance() {
			return new RapportPrestationImposable();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return RapportPrestationImposable.class;
		}
	},

	APPARTENANCE_MENAGE {
		@Override
		public RapportEntreTiers newInstance() {
			return new AppartenanceMenage();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return AppartenanceMenage.class;
		}
	},

	REPRESENTATION {
		@Override
		public RapportEntreTiers newInstance() {
			return new RepresentationConventionnelle();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return RepresentationConventionnelle.class;
		}
	},

	CONTACT_IMPOT_SOURCE {
		@Override
		public RapportEntreTiers newInstance() {
			return new ContactImpotSource();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return ContactImpotSource.class;
		}
	},

	ANNULE_ET_REMPLACE {
		@Override
		public RapportEntreTiers newInstance() {
			return new AnnuleEtRemplace();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return AnnuleEtRemplace.class;
		}
	},

	PARENTE {
		@Override
		public RapportEntreTiers newInstance() {
			return new Parente();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return Parente.class;
		}
	},

	ASSUJETTISSEMENT_PAR_SUBSTITUTION {
		@Override
		public RapportEntreTiers newInstance() {
			return new AssujettissementParSubstitution();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return AssujettissementParSubstitution.class;
		}
	},

	ACTIVITE_ECONOMIQUE {
		@Override
		public RapportEntreTiers newInstance() {
			return new ActiviteEconomique();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return ActiviteEconomique.class;
		}
	},

	MANDAT {
		@Override
		public RapportEntreTiers newInstance() {
			return new Mandat();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return Mandat.class;
		}
	},

	FUSION_ENTREPRISES {
		@Override
		public RapportEntreTiers newInstance() {
			return new FusionEntreprises();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return FusionEntreprises.class;
		}
	};

	/**
	 * Crée une nouvelle instance du rapport-entre-tiers correspondant au type courant.
	 *
	 * @return un rapport-entre-tiers du type demandé
	 */
	public abstract RapportEntreTiers newInstance();

	/**
	 * @return la classe concrète correspondant au type de rapport.
	 */
	public abstract Class<? extends RapportEntreTiers> getRapportClass();
}
