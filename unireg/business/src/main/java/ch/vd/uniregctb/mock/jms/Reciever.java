package ch.vd.uniregctb.mock.jms;

import javax.jms.Message;

/**
 * interface afin de recevoir des JMS messages.
 *
 * @author xcicfh (last modified by $Author: xcicfh $ @ $Date: 2007/09/25 07:58:13 $)
 * @version $Revision: 1.1 $
 */
public interface Reciever {

    /**
     * Cette méthode est appelée quand un message arrive.
     *
     * @param message un message JMS.
     */
    void onRecieve(Message message);
}
