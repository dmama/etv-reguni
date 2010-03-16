package ch.vd.uniregctb.evenement.fiscal;

import ch.vd.uniregctb.evenement.EvenementFiscal;



/**
 * Facade des événements fiscal. Cette facade est dédiée à la communication d'applications publiant
 * des événement.
 * @author xcicfh (last modified by $Author: xcipdt $ @ $Date: 2008/03/28 15:55:58 $)
 * @version $Revision: 1.3 $
 */
public interface EvenementFiscalFacade {




    /**
     * publie l'événement.
     *
     * @param evenement un événement fiscal
     * @throws EvenementFiscalException si un problème survient durant la génération du XML ou durant la transmission
     *             du message au serveur JMS.
     */
    void publierEvenement(EvenementFiscal evenement) throws EvenementFiscalException;


}
