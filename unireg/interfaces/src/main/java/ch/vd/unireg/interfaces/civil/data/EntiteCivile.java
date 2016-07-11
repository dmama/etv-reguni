package ch.vd.unireg.interfaces.civil.data;

import java.util.Collection;

import ch.vd.unireg.interfaces.common.Adresse;

public interface EntiteCivile {

    /**
     * @return la liste des adresses de l'entité civil.
     */
    Collection<Adresse> getAdresses();
}
