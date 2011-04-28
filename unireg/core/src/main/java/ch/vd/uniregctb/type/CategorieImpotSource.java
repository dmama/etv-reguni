/**
 *
 */
package ch.vd.uniregctb.type;

/** 
 * <!-- begin-user-doc -->
 * Longueur de colonne : 32
 * <!-- end-user-doc -->
 * Catégorie de l'impôt à la source selon les art. 130 ss. LI.
 * Valeurs possibles :
 * - Travailleurs
 * - Artistes, sportifs et conférenciers
 * - Administrateurs
 * - Créanciers hypothécaires
 * - Bénéficiaires de prestations de prévoyance découlant de rapports de travail de droit public
 * - Bénéficiaires de prestations de prévoyance découlant du droit privé
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Ka_zoGHuEdydo47IZ53QMw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Ka_zoGHuEdydo47IZ53QMw"
 */
public enum CategorieImpotSource {
	/** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uck5kGHwEdydo47IZ53QMw"
	 */
	ADMINISTRATEURS("Administrateurs"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0Brg4GHwEdydo47IZ53QMw"
	 */
	CONFERENCIERS_ARTISTES_SPORTIFS("Conférenciers, artistes, sportifs"), /**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0nrw8GHwEdydo47IZ53QMw"
	 */
	CREANCIERS_HYPOTHECAIRES("Créanciers hypothécaires"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_s2bbIGHwEdydo47IZ53QMw"
	 */
	PRESTATIONS_PREVOYANCE("Prestations de prévoyance"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uA9UkGHwEdydo47IZ53QMw"
	 */
	REGULIERS("Réguliers"), /** 
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_aSRwEFesEd2mebda6X9XDw"
	 */
	LOI_TRAVAIL_AU_NOIR("Loi sur le travail au noir");

	private final String texte;

	private CategorieImpotSource(String texte) {
		this.texte = texte;
	}

	public String texte() {
		return texte;
	}

}
