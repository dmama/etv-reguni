
package ch.vd.uniregctb.mock.jms;

import java.util.ArrayList;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Topic;

/**
 * Stub implementation of the {@link javax.jms.Topic} interface.
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.1 $
 */
public final class MockTopic implements Topic, Destination {

    /**
     * default nom de la queue
     */
    public static final String DEFAULT_TOPIC_NAME = "jms.TopicQueue";



    /**
     *
     */
    private final ArrayList<Reciever> receivers = new ArrayList<Reciever>();

    /**
     *
     */
    private String topicName = DEFAULT_TOPIC_NAME;

    /**
     * default constructor
     *
     */
    public MockTopic() {
    }

    /**
     * default constructor
     * @param topicName nom de la queue
     */
    public MockTopic(String topicName) {
        this.topicName = topicName;
    }

    /**
     * {@inheritDoc}
     */
    public String getTopicName() {
        return this.topicName;
    }

    /**
     * ajoute un recierver de message.
     * @param reciever un reciever.
     */
    public void addReciever(Reciever reciever) {
        receivers.add(reciever);
    }

    /**
     * appeller quand un message arrive
     * @param message un message JMS
     */
    public void onMessage(Message message) {
	    for (Reciever reciever : receivers) {
		    reciever.onRecieve(message);
	    }
    }

    @Override
    public String toString() {
    	return topicName;
    }
}
