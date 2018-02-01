package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Lien entre une entreprise et ses administrateurs, s'ils sont connus dans Unireg.
 * <pre>
 *   +------------------+                           +------------------+
 *   | Entreprise       |                           | Administrateur   |
 *   +------------------+                           +------------------+
 *           ^                                                ^
 *           ¦  sujet  +----------------------------+  objet  ¦
 *           +---------|  AdministrationEntreprise  |---------+
 *                     +----------------------------+
 * </pre>
 */
@Entity
@DiscriminatorValue("AdministrationEntreprise")
public class AdministrationEntreprise extends RapportEntreTiers {

	private static final String ENTREPRISE = "entreprise";
	private static final String ADMINISTRATEUR = "administrateur";

	/**
	 * Flag activé si l'administrateur occupe également la fonction de président
	 */
	private boolean president;

	public AdministrationEntreprise() {
	}

	public AdministrationEntreprise(RegDate dateDebut, RegDate dateFin, Entreprise entreprise, PersonnePhysique administrateur, boolean president) {
		super(dateDebut, dateFin, entreprise, administrateur);
		this.president = president;
	}

	protected AdministrationEntreprise(AdministrationEntreprise src) {
		super(src);
		this.president = src.president;
	}

	@Column(name = "ADMIN_PRESIDENT")
	public boolean isPresident() {
		return president;
	}

	public void setPresident(boolean president) {
		this.president = president;
	}

	@Transient
	@Override
	public String getDescriptionTypeObjet() {
		return ADMINISTRATEUR;
	}

	@Transient
	@Override
	public String getDescriptionTypeSujet() {
		return ENTREPRISE;
	}

	@Transient
	@Override
	public TypeRapportEntreTiers getType() {
		return TypeRapportEntreTiers.ADMINISTRATION_ENTREPRISE;
	}

	@Override
	public AdministrationEntreprise duplicate() {
		return new AdministrationEntreprise(this);
	}
}
