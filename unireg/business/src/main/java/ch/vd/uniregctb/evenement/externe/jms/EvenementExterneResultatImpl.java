package ch.vd.uniregctb.evenement.externe.jms;


import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceType;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.evenement.externe.EmmetteurType;
import ch.vd.uniregctb.evenement.externe.EvenementExterneResultat;
import ch.vd.uniregctb.evenement.externe.IEvenementExterne;


/**
* Default implementation de l'interface {@link EditiqueResultat}.
* @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:55:02 $)
* @version $Revision: 1.1 $
*/
public final class EvenementExterneResultatImpl implements EvenementExterneResultat {

    /**
     * contient l'exception levé lors de la reception d'un message
     */
    private Exception error = null;

    /**
     * contient l'évenement externe de la réponse.
     */
    private IEvenementExterne evenement;

    /**
     * Contient le type de l'emmetteur du message
     */
    private EmmetteurType emmetteurType;

    /**
     * contient le texte de la réponse.
     */
    private String text;

    private String correlationId;

    /**
     * {@inheritDoc}
     */
    public Exception getError() {
        return error;
    }

    /**
     * Définit le message de l'erreur survenue lors de la réception de l'événement.
     * @param error le message de l'erreur survenue lors de la réception de l'événement.
     */
    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    public IEvenementExterne getEvenement() {
        return evenement;
    }

    /**
     * Définit l'évenement externe de la réponse.
     * @param evenement l'évenement externe de la réponse.
     */
    public void setEvenement(IEvenementExterne evenement) {
        this.evenement = evenement;
        if (evenement instanceof EvenementImpotSourceType) {
        	this.emmetteurType = EmmetteurType.ImpotSource;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * {@inheritDoc}
     */
    public String getText() {
        return text;
    }

    /**
     * Définit le texte de la réponse.
     * @param text le texte de la réponse.
     */
    public void setText(String text) {
        this.text = text;
    }

	/**
	 * @return the emmetteurType
	 */
	public EmmetteurType getEmmetteur() {
		return emmetteurType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getCorrelationId() {
		return correlationId;
	}

	/**
	 * @param correlationId the correlationId to set
	 */
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}




}
