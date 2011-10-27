/**
 *
 */
package ch.vd.uniregctb.type;

import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tutelle;

/**
 * <!-- begin-user-doc --> Longueur de colonne : 20 <!-- end-user-doc -->
 *
 * @author jec
 * @uml.annotations derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Ng5gYJQ9EdyqCO_31WzPOw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Ng5gYJQ9EdyqCO_31WzPOw"
 */
public enum TypeRapportEntreTiers {
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_wKYpoKfVEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_2hdgcKfVEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_5ufdIKfVEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_AApXsKfWEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_M4B50KfWEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_gad-sKfWEdy6qP7Nc3dO8g"
	 */
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
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 */
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
