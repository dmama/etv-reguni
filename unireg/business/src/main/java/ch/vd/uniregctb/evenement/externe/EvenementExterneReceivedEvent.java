package ch.vd.uniregctb.evenement.externe;

import org.springframework.context.ApplicationEvent;

public class EvenementExterneReceivedEvent extends ApplicationEvent {


	/**
	 *
	 */
	private static final long serialVersionUID = 5900589968001209134L;

	public EvenementExterneReceivedEvent( EvenementExterneResultat resultat) {
		super(resultat);
	}

	 /**
     * Obtient l'évenement externe de la réponse.
     *
     * @return Retourne l'évenement externe de la réponse.
     */
    public IEvenementExterne getEvenement() {
    	return getResult().getEvenement();
    }


    /**
     * Obtient l'emmetteur de l'événement.
     * @return Retourne l'emmetteur de l'événement.
     */
    public EmmetteurType getEmmetteur() {
    	return getResult().getEmmetteur();
    }

	 /**
     * Obtient l'indication si une erreur est survenue.
     *
     * @return <code>true</code> si une erreur est survenue, autrement <code>false</code>.
     * @see #getError()
     */
    public boolean hasError() {
    	return getResult().hasError();
    }

    /**
     * Obtient le message de l'erreur survenue lors de la réception de l'événement.
     *
     * @return Retourn l'exception en cas d'erreur, sinon <codeb>null</code>.
     */
    public Exception getError() {
    	return getResult().getError();
    }

    /**
     * Obtient le texte de la réponse.
     *
     * @return Retourne le texte de la réponse.
     */
    public String getText() {
    	return getResult().getText();
    }

    /**
     * Obtient l'identifiant unique de l'événement
     * @return Retourne  l'identifiant unique de l'événement
     */
    public String getCorrelationId() {
    	return getResult().getCorrelationId();
    }

    /**
     * Obtient le resulat
     * @return Retourne le resultat.
     */
	private EvenementExterneResultat getResult() {
		return (EvenementExterneResultat) this.getSource();
	}
}
