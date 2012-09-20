unireg-bridges
==============

unireg-bridges est un composant ESB destiné à être déployé dans Fuse ESB.

Ce composant permet de faire un bridge JMS/JMS entre ActiveMQ et WeblogicJMS pour l'application unireg

		<camel:route>
			<camel:from
				uri="jmsIntegrationDurable:topic:ch.vd.fiscalite.integration.jms.EvtRegCivilTopic" />
			<camel:to uri="activemq:ch.vd.registre.evtCivil" />
		</camel:route>

		<camel:route>
			<camel:from uri="activemq:ch.vd.registre.evtFiscal" />
			<camel:to
				uri="jmsIntegration:topic:ch.vd.fiscalite.integration.jms.EvtRegFiscalTopic" />
		</camel:route>

		<camel:route>
			<camel:from uri="activemq:ch.vd.editique.output" />
			<camel:to uri="jmsIntegration:topic:ch.vd.editique.jms.InputDestination" />
		</camel:route>

		<camel:route>
			<camel:from
				uri="jmsIntegration:ch.vd.editique.jms.OutputQueue?selector=documentType LIKE 'RG%25' and printMode='D'" />
			<camel:to uri="activemq:ch.vd.editique.input" />
		</camel:route>

		<camel:route>
			<camel:from
				uri="jmsTaoIs:ch.vd.dfin.tao.is.evenement.externe.jms.OutputQueue" />
			<camel:to uri="activemq:ch.vd.unireg.evtExterne" />
		</camel:route>


Configuration
-------------

la configuration se fait grâce au fichier unireg-bridges.properties

taoIs.server.jndi		Url de connection jndi weblogic Tao IS (format t3://HOST:PORT)
integration.server.jndi	Url de connection jndi weblogic Integration (format t3://HOST:PORT)

Installation
------------

copier unireg-bridges.properties dans le repertoire appDir de FuseESB et adapter les valeurs hostname et port
copier unireg-bridges-sa-1.0-SNAPSHOT.zip dans le répertoire hotdeploy de fuseESB


