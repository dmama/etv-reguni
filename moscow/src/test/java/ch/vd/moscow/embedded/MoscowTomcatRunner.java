package ch.vd.moscow.embedded;

import ch.vd.registre.embedded.tomcat.TomcatRunner;

import java.io.File;

/**
 * @author msi
 */
public class MoscowTomcatRunner {

	// private static final Logger log = Logger.getLogger(MoscowTomcatRunner.class);

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
		System.setProperty("pom.version", "1.0-SNAPSHOT");
		// END_LVERSION

		System.setProperty("pom.name", "Moscow");
		System.setProperty("pom.description", "Moscow, une application de monitoring des logs d'Unireg");

		System.setProperty("web.log4j.location", "file:${ch.vd.appDir}/moscow/config/log4j.xml");
        System.setProperty("application.properties.path", String.format("%s/moscow/config/application.properties", appDir));
        System.setProperty("credentials.properties.path", String.format("%s/moscow/config/credentials.properties", appDir));

		System.setProperty("jdbc.driverClassName", "org.postgresql.Driver");

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
		final TomcatRunner runner = new TomcatRunner("/moscow", 8080, "src/main/webapp", null, null);
		runner.start();

        while (true) {
            Thread.sleep(1000);
        }
	}
}
