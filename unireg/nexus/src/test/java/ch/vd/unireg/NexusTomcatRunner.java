package ch.vd.unireg;

import java.io.File;

import ch.vd.registre.embedded.tomcat.TomcatRunner;

/**
 * Cette application permet de démarrer l'application Nexus de Unireg avec un Tomcat embeddé à l'intérieur d'Eclipse.
 * <p>
 * De cette manière, Tomcat utilise directement les ressources d'Eclipse et les modifications du code source (dans le mesure des
 * possibilités de la JVM) et des jsps sont directement reportées sans redéploiement de l'application.
 * <p>
 * Il est nécessaire de spécifier le répertoire de l'application ${ch.vd.appDir} en paramètre. Par exemple :
 * <pre>
 * NexusTomcatRunner ~/appDir
 * </pre>
 */
public class NexusTomcatRunner {

	// private static final Logger log = Logger.getLogger(NexusTomcatRunner.class);

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("ERROR: le répertoire de l'application doit être spécifié comme premier paramètre");
			return;
		}

		final String appDir = args[0];
		final File f = new File(appDir);
		if (!f.isDirectory()) {
			System.err.println("ERROR: le chemin " + appDir + " n'est pas un répertoire ou n'est pas accessible");
			return;
		}

		System.out.println("Démarrage avec appDir=" + appDir);

		// appDir
		System.setProperty("ch.vd.appDir", appDir);

		// Propriétés normallement configurées par Maven et qu'on spécifie à la main parce qu'on pointe vers le répertoire sources (et non pas target).

		// BEGIN_LVERSION
		System.setProperty("pom.version", "5.1.2-SNAPSHOT");
		// END_LVERSION

		System.setProperty("pom.name", "Unireg - Nexus");
		System.setProperty("pom.description", "Nexus : le cache unifié d'Unireg");

		System.setProperty("unireg-nexus.appname", "unireg-nexus");
		System.setProperty("unireg.standalone", "false");
		System.setProperty("unireg-nexus.log4j.location", "file:${ch.vd.appDir}/${unireg-nexus.appname}/config/unireg-log4j.xml");

		final String propertiesPath = String.format("%s/%s/config/unireg.properties", appDir, System.getProperty("unireg-nexus.appname"));
		System.setProperty("unireg-nexus.properties.path", propertiesPath);

		final String credentialsPath = String.format("%s/%s/config/credentials.properties", appDir, System.getProperty("unireg-nexus.appname"));
		System.setProperty("unireg-nexus.credentials.path", credentialsPath);

		System.setProperty("host-interfaces.version-short", "1.9");
		System.setProperty("host-interfaces.factory", "weblogic.jndi.WLInitialContextFactory");
		System.setProperty("host-interfaces.ejb-proxy-class", "org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean");

		/**
		 * Paramètres:
		 * <ul>
		 * <li>1. Le context path de l'application (Ex: "/registre/regch")</li>
		 * <li>2. Le port TCP sur lequel l'application écoute</li>
		 * <li>3. Le repertoire de la webapp explosée (sans les classes et les libs) relative au repertoire courant Ex Reg-CH: "webapp" =>
		 * ..../04-Impl/regch/web/webapp Ex Unireg: "src/main/webapp" => ..../04-Impl/unireg/web/src/main/webapp</li>
		 * <li>4. Le fichier context.xml pour la définition des data sources</li>
		 * <li>5. Le fichier tomcat-users.xml pour la définition des utilisateurs</li>
		 * </ul>
		 */
		final TomcatRunner runner = new TomcatRunner("/fiscalite/unireg/nexus", 8080, "src/main/webapp", null, null);
		runner.start();
	}
}
