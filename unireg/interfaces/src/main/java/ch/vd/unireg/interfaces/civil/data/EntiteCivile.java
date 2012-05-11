package ch.vd.unireg.interfaces.civil.data;

import java.util.Collection;

public interface EntiteCivile {

    /**
     * @return la liste des adresses de l'entité civil.
     */
    Collection<Adresse> getAdresses();
}
