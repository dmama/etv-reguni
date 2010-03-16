package ch.vd.uniregctb.admin;

/**
 * Bean correspondant a l'exécution d'un script DBUnit.
 * @author Ludovic Bertin
 *
 */
public class ScriptBean {

	public static enum DBUnitMode { CLEAN_INSERT, DELETE_ALL, INSERT_APPEND }
	
	private DBUnitMode mode = DBUnitMode.CLEAN_INSERT;
	private byte[] scriptData = null;
	private String scriptFileName = null;

	/**
	 * Renvoie le mode de lancement du script.
	 * @return le mode de lancement
	 * @see ch.vd.uniregctb.admin.ScriptBean.DBUnitMode
	 */
	public DBUnitMode getMode() {
		return mode;
	}

	/**
	 * Positionne le mode de lancement du script.
	 * @param 	mode	le mode de lancement
	 * @see ch.vd.uniregctb.admin.ScriptBean.DBUnitMode
	 */
	public void setMode(DBUnitMode mode) {
		this.mode = mode;
	}

	/**
	 * Renvoie le contenu du fichier uploadé sous forme de tableau de byte.
	 * @return un tableau de byte
	 */
	public byte[] getScriptData() {
		return scriptData;
	}

	/**
	 * Positionne le contenu du fichier uploadé sous forme de tableau de byte.
	 * @param scriptData      le contenu du fichier uploadé
	 */
	public void setScriptData(byte[] scriptData) {
		this.scriptData = scriptData;
	}

	/**
	 * Renvoie le nom du script DBUnit "connu"
	 * @return le nom du fichier (en relatif par rapport au dossier contenant tous les scripts)
	 */
	public String getScriptFileName() {
		return scriptFileName;
	}

	/**
	 * Positionne le nom du script DBUnit "connu"
	 * @param scriptFileName	le nom du fichier (en relatif par rapport au dossier contenant tous les scripts)
	 */
	public void setScriptFileName(String scriptFileName) {
		this.scriptFileName = scriptFileName;
	}
}
