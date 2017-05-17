package ch.vd.uniregctb.type;

import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AssujettissementParSubstitution;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.ScissionEntreprise;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.TransfertPatrimoine;
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

	/**
	 * @since 6.0
	 */
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

	/**
	 * @since 6.0
	 */
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

	/**
	 * @since 6.0
	 */
	FUSION_ENTREPRISES {
		@Override
		public RapportEntreTiers newInstance() {
			return new FusionEntreprises();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return FusionEntreprises.class;
		}
	},

	/**
	 * @since 6.0
	 */
	SCISSION_ENTREPRISE {
		@Override
		public RapportEntreTiers newInstance() {
			return new ScissionEntreprise();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return ScissionEntreprise.class;
		}
	},

	/**
	 * @since 6.0
	 */
	TRANSFERT_PATRIMOINE {
		@Override
		public RapportEntreTiers newInstance() {
			return new TransfertPatrimoine();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return TransfertPatrimoine.class;
		}
	},

	/**
	 * @since 6.0
	 */
	ADMINISTRATION_ENTREPRISE {
		@Override
		public RapportEntreTiers newInstance() {
			return new AdministrationEntreprise();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return AdministrationEntreprise.class;
		}
	},

	/**
	 * @since 6.0
	 */
	SOCIETE_DIRECTION {
		@Override
		public RapportEntreTiers newInstance() {
			return new SocieteDirection();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return SocieteDirection.class;
		}
	},

	/**
	 * @since 7.2.0
	 */
	HERITAGE {
		@Override
		public RapportEntreTiers newInstance() {
			return new Heritage();
		}

		@Override
		public Class<? extends RapportEntreTiers> getRapportClass() {
			return Heritage.class;
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
