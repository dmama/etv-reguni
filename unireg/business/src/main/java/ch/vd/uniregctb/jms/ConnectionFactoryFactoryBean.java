package ch.vd.uniregctb.jms;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.springframework.beans.factory.FactoryBean;

/**
 * Classe fournie par Giampaolo Tranchida pour éviter une NPE au
 * démarrage de l'application, see {@link #getConnectionFactory()}
 */
public class ConnectionFactoryFactoryBean implements FactoryBean {

    private ManagedConnectionFactory managedConnectionFactory;
    private ConnectionManager connectionManager;
    private Object connectionFactory;

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public Object getObject() throws Exception {
        return getConnectionFactory();
    }

    public Class<?> getObjectType() {
        try {
            Object connectionFactory = getConnectionFactory();
            if (connectionFactory != null) {
                return connectionFactory.getClass();
            }
        } catch (ResourceException e) {
        }
        return null;
    }

    public boolean isSingleton() {
        return true;
    }

    public Object getConnectionFactory() throws ResourceException {
        // we must initialize the connection factory outside of the getObject method since spring needs the
        // connection factory type for autowiring before we have created the bean
        if (connectionFactory == null) {
            // START SNIPPET: cf
            if (managedConnectionFactory == null) {
                return null;
            }
            if (connectionManager != null) {
                connectionFactory = managedConnectionFactory.createConnectionFactory(connectionManager);
            } else {
                connectionFactory = managedConnectionFactory.createConnectionFactory();
            }
            // END SNIPPET: cf
        }
        return connectionFactory;
    }
}