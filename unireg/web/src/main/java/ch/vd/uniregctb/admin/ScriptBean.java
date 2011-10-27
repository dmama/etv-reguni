package ch.vd.uniregctb.admin;

import org.springframework.web.multipart.MultipartFile;

/**
 * Bean correspondant a l'exécution d'un script DBUnit.
 * @author Ludovic Bertin
 *
 */
public class ScriptBean {

	public static enum DBUnitMode { CLEAN_INSERT, DELETE_ALL, INSERT_APPEND }

	private DBUnitMode mode = DBUnitMode.CLEAN_INSERT;
	private MultipartFile scriptData = null;

	public DBUnitMode getMode() {
		return mode;
	}

	public void setMode(DBUnitMode mode) {
		this.mode = mode;
	}

	public MultipartFile getScriptData() {
		return scriptData;
	}

	public void setScriptData(MultipartFile scriptData) {
		this.scriptData = scriptData;
	}
}
