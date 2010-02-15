package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author msi
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_8kjeEG7EEd2HlNPAVeri9w"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_8kjeEG7EEd2HlNPAVeri9w"
 */
@Entity
@DiscriminatorValue("NOUVEAU_DOSSIER")
public class TacheNouveauDossier extends Tache {

	private static final long serialVersionUID = 6392155109611263105L;

	// Ce constructeur est requis par Hibernate
	protected TacheNouveauDossier() {
	}

	public TacheNouveauDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable) {
		super(etat, dateEcheance, contribuable);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheNouveauDossier;
	}
}
