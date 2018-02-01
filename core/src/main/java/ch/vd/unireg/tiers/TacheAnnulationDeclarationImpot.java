package ch.vd.unireg.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

@Entity
@DiscriminatorValue("ANNUL_DI")
public class TacheAnnulationDeclarationImpot extends TacheAnnulationDeclaration<DeclarationImpotOrdinaire> {

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationDeclarationImpot() {
	}

	public TacheAnnulationDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, DeclarationImpotOrdinaire declarationImpotOrdinaire,
	                                       CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, declarationImpotOrdinaire, collectiviteAdministrativeAssignee);
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheAnnulationDeclarationImpot;
	}
}
