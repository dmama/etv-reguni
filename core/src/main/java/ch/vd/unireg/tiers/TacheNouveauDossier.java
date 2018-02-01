package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Entity
@DiscriminatorValue("NOUVEAU_DOSSIER")
public class TacheNouveauDossier extends Tache {

	// Ce constructeur est requis par Hibernate
	protected TacheNouveauDossier() {
	}

	public TacheNouveauDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheNouveauDossier;
	}
}
