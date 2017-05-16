package ch.vd.uniregctb.embedded;

import java.io.File;

import org.apache.commons.lang3.CharEncoding;

import ch.vd.registre.embedded.tomcat.TomcatRunner;
import ch.vd.registre.embedded.tomcat.TomcatRunnerParameters;

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

	// private static final Logger log = LoggerFactory.getLogger(UniregTomcatRunner.class);

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
		System.setProperty("project.version", "7.1.3-SNAPSHOT");
		// END_LVERSION

		System.setProperty("project.name", "UniregCTB - Web");
		System.setProperty("project.description", "UniregCTB, Le registre unifie de l'Etat de Vaud - Web");

		System.setProperty("unireg-web.appname", "unireg-web");
		System.setProperty("interfaces-mode", "direct");
		System.setProperty("unireg.log4j.location", "file:${ch.vd.appDir}/${unireg-web.appname}/config/unireg-log4j.xml");

		final String propertiesPath = String.format("%s/%s/config/unireg.properties", appDir, System.getProperty("unireg-web.appname"));
		System.setProperty("unireg.properties.path", propertiesPath);

		final String credentialsPath = String.format("%s/%s/config/credentials.properties", appDir, System.getProperty("unireg-web.appname"));
		System.setProperty("credentials.properties.path", credentialsPath);

		System.setProperty("host-interfaces.version-short", "1.9");
		System.setProperty("host-interfaces.factory", "weblogic.jndi.WLInitialContextFactory");
		System.setProperty("host-interfaces.ejb-proxy-class", "org.springframework.ejb.access.SimpleRemoteStatelessSessionProxyFactoryBean");

		System.setProperty("editique.locale.sync.attente.timeout", "30");
		System.setProperty("editique.locale.async.attente.delai", "10");

		System.setProperty("oracle.hibernate.dialect", "ch.vd.uniregctb.hibernate.dialect.Oracle10gDialectWithNVarChar");
		System.setProperty("oracle.hibernate.query.substitutions", "true 1, false 0");

		System.setProperty("extprop.hibernate.hbm2ddl.mode", "validate");

		final String userDbPath;
		final File userDb = new File("src/test/resources/ch/vd/uniregctb/embedded/tomcat-users.xml");
		if (userDb.exists()) {
			userDbPath = userDb.getPath();
		}
		else {
			userDbPath = null;
		}

		final TomcatRunnerParameters params = new TomcatRunnerParameters();
		params.setContextPath("/fiscalite/unireg/web");
		params.setPort(8080);
		params.setWebappDir("src/main/webapp");
		params.setUserDatabasePath(userDbPath);
		params.setURIEncoding(CharEncoding.UTF_8);

		final TomcatRunner runner = new TomcatRunner(params);
		runner.start();
	}
}
