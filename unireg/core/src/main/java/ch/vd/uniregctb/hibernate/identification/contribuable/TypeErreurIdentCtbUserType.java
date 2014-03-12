package ch.vd.uniregctb.hibernate.identification.contribuable;

import ch.vd.uniregctb.evenement.identification.contribuable.Erreur;
import ch.vd.uniregctb.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : Erreur.TypeErreur <--> varchar
 */
public class TypeErreurIdentCtbUserType extends EnumUserType<Erreur.TypeErreur> {

	public TypeErreurIdentCtbUserType() {
		super(Erreur.TypeErreur.class);
	}
}
