package ch.vd.unireg.tiers;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.type.TypeEtatTache;

@Entity
public abstract class TacheAnnulationDeclaration<T extends Declaration> extends Tache {

	private T declaration;

	// Ce constructeur est requis par Hibernate
	protected TacheAnnulationDeclaration() {
	}

	public TacheAnnulationDeclaration(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, T declaration,
	                                  CollectiviteAdministrative collectiviteAdministrativeAssignee) {
		super(etat, dateEcheance, contribuable, collectiviteAdministrativeAssignee);
		this.declaration = declaration;
	}

	@ManyToOne(targetEntity = Declaration.class)
	@JoinColumn(name = "DECLARATION_ID")
	@ForeignKey(name = "FK_TACH_DECL_ID")
	public T getDeclaration() {
		return declaration;
	}

	public void setDeclaration(T declaration) {
		this.declaration = declaration;
	}
}
