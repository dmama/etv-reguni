package ch.vd.unireg.hibernate.identification.contribuable;

import ch.vd.unireg.evenement.identification.contribuable.Erreur;
import ch.vd.unireg.hibernate.EnumUserType;

/**
 * Classe de transtypage pour Hibernate : Erreur.TypeErreur <--> varchar
 */
public class TypeErreurIdentCtbUserType extends EnumUserType<Erreur.TypeErreur> {

	public TypeErreurIdentCtbUserType() {
		super(Erreur.TypeErreur.class);
	}
}
