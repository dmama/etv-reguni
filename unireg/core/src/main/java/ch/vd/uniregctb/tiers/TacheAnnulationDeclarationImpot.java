package ch.vd.uniregctb.tiers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@Entity
@DiscriminatorValue("ANNUL_DI")
public class TacheAnnulationDeclarationImpot extends Tache {

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationDeclarationImpot() {
	}

	public TacheAnnulationDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, DeclarationImpotOrdinaire declarationImpotOrdinaire,
	                                       CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
		this.declarationImpotOrdinaire = declarationImpotOrdinaire;
	}

	private DeclarationImpotOrdinaire declarationImpotOrdinaire;

	@ManyToOne
	@JoinColumn(name = "DECLARATION_ID")
	@ForeignKey(name = "FK_TACH_DECL_ID")
	public DeclarationImpotOrdinaire getDeclarationImpotOrdinaire() {
		return declarationImpotOrdinaire;
	}

	public void setDeclarationImpotOrdinaire(DeclarationImpotOrdinaire theDeclarationImpotOrdinaire) {
		declarationImpotOrdinaire = theDeclarationImpotOrdinaire;
	}

	@Transient
	@Override
	public TypeTache getTypeTache() {
		return TypeTache.TacheAnnulationDeclarationImpot;
	}
}
