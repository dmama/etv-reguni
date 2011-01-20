package ch.vd.uniregctb.embedded;

import java.io.File;

import ch.vd.registre.warren.TomcatRunner;

/**
 * Cette application permet de démarrer l'application Unireg avec un Tomcat embeddé à l'intérieur d'Eclipse.
 * <p>
 * De cette manière, Tomcat utilise directement les ressources d'Eclipse et les modifications du code source (dans le mesure des
 * possibilités de la JVM) et des jsps sont directement reportées sans redéploiement de l'application.
 * <p>
 * Il est nécessaire de spécifier le répertoire de l'application ${ch.vd.appDir} en paramètre. Par exemple :
 * <pre>
 * UniregTomcatRunner ~/appDir
 * </pre>
 */
public class UniregTomcatRunner {

	// private static final Logger log = Logger.getLogger(UniregTomcatRunner.class);

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
		System.setProperty("pom.version", "4.9-SNAPSHOT");
		// END_LVERSION

		System.setProperty("pom.name", "UniregCTB - Web");
		System.setProperty("pom.description", "UniregCTB, Le registre unifie de l'Etat de Vaud - Web");

		System.setProperty("unireg.appname", "unireg");
		System.setProperty("unireg.standalone", "false");
		System.setProperty("unireg.log4j.location", "file:${ch.vd.appDir}/${unireg.appname}/config/unireg-log4j.xml");

		final String propertiesPath = String.format("%s/%s/config/unireg.properties", appDir, System.getProperty("unireg.appname"));
		System.setProperty("unireg.properties.path", propertiesPath);

		final String credentialsPath = String.format("%s/%s/config/credentials.properties", appDir, System.getProperty("unireg.appname"));
		System.setProperty("credentials.properties.path", credentialsPath);

		System.setProperty("host-interfaces.version-short", "1.9");
		System.setProperty("host-interfaces.factory", "weblogic.jndi.WLInitialContextFactory");
		System.setProperty("host-interfaces.ejb-proxy-class", "org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean");

		System.setProperty("editique.locale.attente.timeout", "30");

		System.setProperty("oracle.hibernate.dialect", "ch.vd.uniregctb.hibernate.dialect.Oracle10gDialectWithNVarChar");
		System.setProperty("oracle.hibernate.query.substitutions", "true 1, false 0");

		System.setProperty("extprop.hibernate.hbm2ddl.mode", "update");
		
		/**
		 * Paramètres:
		 * <ul>
		 * <li>1. Le context path de l'application (Ex: "/registre/regch")</li>
		 * <li>2. Le repertoire de la webapp explosée (sans les classes et les libs) relative au repertoire courant Ex Reg-CH: "webapp" =>
		 * ..../04-Impl/regch/web/webapp Ex Unireg: "src/main/webapp" => ..../04-Impl/unireg/web/src/main/webapp</li>
		 * <li>3. Le fichier context.xml pour la définition des data sources</li>
		 * </ul>
		 */
		TomcatRunner runner = new TomcatRunner("/fiscalite/unireg/web", "src/main/webapp", null);
		runner.start();
	}
}
