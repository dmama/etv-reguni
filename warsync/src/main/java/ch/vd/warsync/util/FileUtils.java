package ch.vd.warsync.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class FileUtils {

	private static final Logger LOGGER = Logger.getLogger(FileUtils.class);

	/**
	 * Deletes all files and subdirectories under dir.
	 * Returns true if all deletions were successful.
	 * If a deletion fails, the method stops attempting to delete and returns
	 * false.
	 */
	public static boolean deleteDir(String d, boolean deleteSelf) {

		File dir = new File(d);
		if (dir.isDirectory()) {
			LOGGER.debug("Deleting WHOLE directory: "+dir);
			boolean ret = recursiveDelete(dir, deleteSelf);
			if (ret && deleteSelf && dir.exists()) {
				LOGGER.error("The directory "+dir+" still exists after delete!");
			}
			return ret;
		}
		LOGGER.error("Not a directory!: "+dir);
		return false;
	}

	private static boolean recursiveDelete(File dirOrFile, boolean deleteSelf) {

		if (dirOrFile.isDirectory()) {
			String[] children = dirOrFile.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = recursiveDelete(new File(dirOrFile, children[i]), true);
				if (!success) {
					return false;
				}
			}
		}

		// The directory is now empty so delete it
		boolean ret = true;
		if (deleteSelf) {
			LOGGER.debug("Deleting: "+dirOrFile);
			ret = dirOrFile.delete();
		}
		return ret;
	}

	public static List<String> findFilesInDir(String d) {

		File dirOrFile = new File(d);
		assertExists(dirOrFile);

		ArrayList<String> files = new ArrayList<String>();
		recursiveFind(d, "", files);
		return files;
	}

	private static void recursiveFind(String base, String ext, ArrayList<String> files) {

		String slashPlusExt = "";
		String extPlusSlash = "";
		if (!ext.equals("")) {
			slashPlusExt = "/"+ext;
			extPlusSlash = ext+"/";
		}

		LOGGER.debug("recursiveFind: Base="+base+" Ext="+slashPlusExt);
		String completeDir = base+slashPlusExt;
		File dir = new File(completeDir);
		assertTrue(dir.isDirectory(), "Pas un directory: "+completeDir);

		String[] children = dir.list();
		for (int i = 0; i < children.length; i++) {
			String subFile = children[i];
			String relFile = extPlusSlash+subFile;
			String completeSubFile = base+"/"+relFile;

			LOGGER.debug("File: Base="+base+" RelFile="+relFile+" ComplFile="+completeSubFile);
			files.add(relFile);

			File subDir = new File(completeSubFile);
			if (subDir.isDirectory()) {
				recursiveFind(base, relFile, files);
			}
		}
	}

	public static void createDir(String dir) {

		File d = new File(dir);
		createDir(d);
	}
	public static void createDir(File dir) {
		if (!dir.exists()) {
			LOGGER.debug("Creating directory: "+dir);
			dir.mkdirs();
		}
		FileUtils.assertExists(dir);
	}

	public static void copyFile(String inS, String outS) throws IOException {
		File in = new File(inS);
		File out = new File(outS);
		copyFile(in, out);
	}
	public static void copyFile(File in, File out) throws IOException {

		FileChannel inChannel = new FileInputStream(in).getChannel();
	    FileChannel outChannel = new FileOutputStream(out).getChannel();
	    try {
	        inChannel.transferTo(0, inChannel.size(), outChannel);
	        LOGGER.debug("File "+in+" copied to "+out);
	    }
	    catch (IOException e) {
	        throw e;
	    }
	    finally {
	        if (inChannel != null) {
	        	inChannel.close();
	        }
	        if (outChannel != null) {
	        	outChannel.close();
	        }
	    }

	    assertTrue(out.exists() && out.isFile(), "Le fichier de destination n'existe pas!");
	}

	public static void assertExists(File dir) {
		assertTrue(dir.isDirectory(), "Directory "+dir.getName()+" doesn't exists");
	}

	public static void assertTrue(boolean cond, String msg) {
		if (!cond) {
			throw new RuntimeException(msg);
		}
	}

}
