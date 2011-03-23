package ch.vd.uniregctb.mock.jms;


import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * messages implementation of the {@link javax.jms.TextMessage} interface.
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.1 $
 */
public final class MockTextMessage extends MockMessage implements TextMessage {


    /**
     * contient le texte du message.
     */
    private String text;

    /**
     *default Contructor
     *
     */
    public MockTextMessage() {
    }

    /**
     * Default construction
     * @param destination une destination.
     */
    public MockTextMessage(Destination destination) {
        super(destination);
    }

    /**
     * {@inheritDoc}
     */
    public String getText() throws JMSException {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    public void setText(String text) throws JMSException {
        this.text = text;
    }






}
