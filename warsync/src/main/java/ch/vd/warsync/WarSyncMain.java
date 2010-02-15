package ch.vd.warsync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import ch.vd.warsync.util.FileUtils;

public class WarSyncMain implements JNotifyListener {

	private static final Logger LOGGER = Logger.getLogger(WarSyncMain.class);

	private static final String version = "2.2-SNAPSHOT";

	private static final String warName = "uniregctb-web-" + version;

	private static final String uniregDir = "../unireg";
	private static final String coreDir = uniregDir + "/core/target/classes";
	private static final String bizDir = uniregDir + "/business/target/classes";
	private static final String norentesDir = uniregDir + "/norentes/target/classes";
	private static final String webDir = uniregDir + "/web";
	// Src
	private static final String web_srcDir = webDir + "/src";
	private static final String web_webappDir = web_srcDir + "/main/webapp";
	private static final String web_jspDir = web_webappDir + "/WEB-INF/jsp";
	// Target
	private static final String web_targetDir = webDir + "/target";
	private static final String web_classesDir = web_targetDir + "/classes";
	private static final String web_warDir = web_targetDir + "/uniregctb-web-" + version;
	private static final String web_metaInfDir = web_warDir + "/META-INF";
	private static final String web_webInfDir = web_warDir + "/WEB-INF";
	private static final String web_libDir = web_webInfDir + "/lib";

	private static final String destDir = uniregDir + "/war-sync";
	private static final String dMetaInfDir = destDir + "/META-INF";
	private static final String dWebinfDir = destDir + "/WEB-INF";
	private static final String dClassesDir = dWebinfDir + "/classes";
	private static final String dLibDir = dWebinfDir + "/lib";

	private final ArrayList<WatchedDir> folders = new ArrayList<WatchedDir>();

	// CONFIG A CHOIX
	private static final String FULL = "FULL";
	private static final String WEB = "WEB";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		DOMConfigurator.configure("src/main/resources/log4j.xml");

		// test();

		new WarSyncMain().execute(args[0]);
	}

	private void execute(String configName) throws Exception {

		if (!new File(destDir).isDirectory()) {
			LOGGER.error("Le repertoire " + destDir + " doit exister");
			int nbTimes = 10;
			while (nbTimes > 0) {
				LOGGER.error(nbTimes + " [sec]");
				Thread.sleep(1000);
				nbTimes--;
			}
		}
		LOGGER.info("Sync Unireg version " + version);
		LOGGER.info("Destination de synchro: " + destDir);

		FileUtils.deleteDir(destDir, false);
		FileUtils.createDir(destDir);

		if (configName.equals(FULL)) {
			configFull();
			initialCopy();
		}
		else if (configName.equals(FULL)) {
			configJspHtmlCssOnly();
		}
		else {
			throw new AssertionError("Config inconnue: " + configName);
		}

		watchDirs();

		// int nbTimes = 6; // 60 secs
		int nbTimes = 10000; // Beaucoup...
		LOGGER.info("Attente de fichiers a synchroniser pendant " + (nbTimes * 10) + " [secs] ...");
		while (nbTimes > 0) {
			Thread.sleep(30 * 1000); // 30 sec
			// Re-watch les repertoires, si ils ont été supprimé/recréés pendant une compile
			// watchDirs();
			LOGGER.info("Still waiting for files to sync...");
		}

		LOGGER.info("Terminé.");
	}

	private void configJspHtmlCssOnly() {

		final String baseSourceDir = "../unireg/web/src/main/webapp";
		final String baseDestDir = "../unireg/web/target/" + warName;

		// css/js/images
		addFolder(baseSourceDir + "/css", baseDestDir + "/css");
		addFolder(baseSourceDir + "/htm", baseDestDir + "/htm");
		addFolder(baseSourceDir + "/images", baseDestDir + "/images");
		addFolder(baseSourceDir + "/js", baseDestDir + "/js");
		addFolder(baseSourceDir + "/META-INF", baseDestDir + "/META-INF");

		final String sWebInf = baseSourceDir + "/WEB-INF";
		final String dWebInf = baseDestDir + "/WEB-INF";
		addFolder(sWebInf + "/jsp", dWebInf + "/jsp");
		addFolder(sWebInf + "/tlds", dWebInf + "/tlds");
		addFolder(sWebInf + "/ws", dWebInf + "/ws");
		addFolder(sWebInf, dWebInf, false);
	}

	private void configFull() {

		// Core
		addFolder(coreDir, dClassesDir);
		// Biz
		addFolder(bizDir, dClassesDir);
		// Norentes
		addFolder(norentesDir, dClassesDir);
		// css/js/images
		addFolder(web_warDir + "/css", destDir + "/css");
		addFolder(web_warDir + "/js", destDir + "/js");
		addFolder(web_warDir + "/images", destDir + "/images");
		// war/META-INF
		addFolder(web_metaInfDir, dMetaInfDir);

		// WEB-INF
		// web classes
		addFolder(web_classesDir, dClassesDir);
		// web libs
		addFolder(web_libDir, dLibDir);
		// JSP
		addFolder(web_jspDir, dWebinfDir + "/jsp");
		// TLD
		addFolder(web_webInfDir + "/tld", dWebinfDir + "/tld");
		addFolder(web_webInfDir + "/tlds", dWebinfDir + "/tlds");
		// App context files
		addFolder(web_classesDir + "/WEB-INF", dWebinfDir);
		/*
		 * addFolder(webinfDir, dWebinfDir); addFolder(webappDir, destDir); // Classes de web addFolder(warDir+"/WEB-INF/tlds",
		 * dWebinfDir+"/tlds");
		 */
	}

	private void addFolder(String s, String d) {
		addFolder(s, d, true);
	}

	private void addFolder(String s, String d, boolean recursive) {
		LOGGER.info("Watching " + s + " to " + d + " (rec=" + recursive + "");
		WatchedDir wd = new WatchedDir(s, d, recursive);
		folders.add(wd);

		FileUtils.createDir(d);
	}

	private void initialCopy() throws Exception {

		int nbFiles = 0;
		for (WatchedDir wd : folders) {
			List<String> files = FileUtils.findFilesInDir(wd.source);

			LOGGER.info("Initial copy of " + wd.source);

			// Copy each file
			for (String f : files) {

				if (f.contains(".svn")) {
					continue;
				}

				File s = new File(wd.source + "/" + f);
				File d = new File(wd.destination + "/" + f);
				if (s.isDirectory()) {
					FileUtils.createDir(d);
				}
				else {
					FileUtils.copyFile(s, d);
				}
				nbFiles++;
			}
		}

		LOGGER.info("Initial copy of " + nbFiles + " files");
	}

	private void watchDirs() throws Exception {

		for (WatchedDir wd : folders) {

			// Remove previous watch
			if (wd.watchId >= 0) {
				boolean res = JNotify.removeWatch(wd.watchId);
			}

			// add actual watch
			LOGGER.info("Watching " + wd.source);
			int watchID = JNotify.addWatch(wd.source, JNotify.FILE_ANY, wd.recursive, this);
			wd.watchId = watchID;
		}
	}

	private void syncFile(String rootPath, String name) {

		if (name.contains(".svn")) {
			return;
		}

		try {
			// Find the WD
			WatchedDir current = null;
			for (WatchedDir wd : folders) {
				if (wd.source.equals(rootPath)) {
					current = wd;
					LOGGER.debug("Dest: " + wd.destination);
					break;
				}
			}

			if (current != null) {

				// Source
				String completeSource = current.source + "/" + name;
				File sourceFile = new File(completeSource);
				// Dest
				String completeDest = current.destination + "/" + name;
				File destFile = new File(completeDest);

				LOGGER.debug("Syncing file " + completeSource + " to " + completeDest);

				// Dir?
				if (sourceFile.isDirectory()) {
					LOGGER.info("Creating dir: " + completeDest);
					FileUtils.createDir(completeDest);
				}
				else if (sourceFile.isFile()) { // File
					LOGGER.info("Copying file " + sourceFile + " to " + destFile);
					FileUtils.copyFile(sourceFile, destFile);
				}
				else { // Deleted
					LOGGER.info("Deleting: " + destFile);
					destFile.delete();
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void fileCreated(int wd, String rootPath, String name) {
		log("CREATED", rootPath, name, null);
		syncFile(rootPath, name);
	}

	public void fileDeleted(int wd, String rootPath, String name) {
		log("DELETED", rootPath, name, null);
		syncFile(rootPath, name);
	}

	public void fileModified(int wd, String rootPath, String name) {
		log("MODIFIED", rootPath, name, null);
		syncFile(rootPath, name);
	}

	public void fileRenamed(int wd, String rootPath, String name, String newName) {
		log("RENAMED", rootPath, name, newName);
		syncFile(rootPath, name);
	}

	private void log(String prefix, String rootPath, String name, String newName) {
		/*
		 * LOGGER.debug("["+prefix+"] Root: "+rootPath+" Name: "+name); if (newName != null) { LOGGER.debug(" Old:"+newName); }
		 */
	}

	// *********************************************************
	public static void test(String[] args) throws Exception {

		// to add a watch :
		String path = System.getProperty("user.home") + "/tmp";
		String libPath = System.getProperty("java.library.path");

		LOGGER.info("Watching " + path);

		// watch mask
		// int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;

		// watch subtree?
		boolean watchSubtree = true;

		// add actual watch
		int watchID = JNotify.addWatch(path, JNotify.FILE_ANY, watchSubtree, new WarSyncMain());

		// sleep a little, the application will exit if you
		// don't (watching is asynchrnous), depending on your
		// application, this may not be required
		int secs = 3;
		LOGGER.debug("WatchID=" + watchID);
		try {
			while (secs > 0) {
				LOGGER.info("Waiting " + (secs * 10) + " secondes, watchID=" + watchID);
				Thread.sleep(10 * 1000);
				secs--;
			}
		}
		catch (InterruptedException e) {
			LOGGER.error("Exception: " + e.getMessage(), e);
		}

		// to remove watch the watch
		boolean res = JNotify.removeWatch(watchID);
		if (!res) {
			LOGGER.error("Error in removing watch ID");
		}
		LOGGER.info("Terminé.");
	}

}
