package ch.vd.uniregctb.indexer.jdbc;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.jdbc.JdbcDirectory;
import org.apache.lucene.store.jdbc.JdbcDirectorySettings;
import org.apache.lucene.store.jdbc.JdbcFileEntrySettings;
import org.apache.lucene.store.jdbc.JdbcStoreException;
import org.apache.lucene.store.jdbc.dialect.Dialect;
import org.apache.lucene.store.jdbc.handler.FileEntryHandler;
import org.apache.lucene.store.jdbc.support.JdbcTable;
import org.apache.lucene.store.jdbc.support.JdbcTemplate;

public class MyJdbcDirectory extends JdbcDirectory {

	public MyJdbcDirectory(DataSource dataSource, String tableName) throws JdbcStoreException {
		super(dataSource, tableName);
	}

	@Override
	public void close() throws IOException {
		super.close();
	}

	@Override
	public void create() throws IOException {
		super.create();
	}

	@Override
	protected Lock createLock() throws IOException {
		return super.createLock();
	}

	@Override
	public IndexOutput createOutput(String name) throws IOException {
		return super.createOutput(name);
	}

	@Override
	public void delete() throws IOException {
		super.delete();
	}

	@Override
	public void deleteContent() throws IOException {
		super.deleteContent();
	}

	@Override
	public void deleteFile(String name) throws IOException {
		super.deleteFile(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List deleteFiles(List arg0) throws IOException {
		return super.deleteFiles(arg0);
	}

	@Override
	public void deleteMarkDeleted() throws IOException {
		super.deleteMarkDeleted();
	}

	@Override
	public void deleteMarkDeleted(long arg0) throws IOException {
		super.deleteMarkDeleted(arg0);
	}

	@Override
	public boolean fileExists(String name) throws IOException {
		return super.fileExists(name);
	}

	@Override
	public long fileLength(String name) throws IOException {
		return super.fileLength(name);
	}

	@Override
	public long fileModified(String name) throws IOException {
		return super.fileModified(name);
	}

	@Override
	public void forceDeleteFile(String name) throws IOException {
		super.forceDeleteFile(name);
	}

	@Override
	public DataSource getDataSource() {
		return super.getDataSource();
	}

	@Override
	public Dialect getDialect() {
		return super.getDialect();
	}

	@Override
	protected FileEntryHandler getFileEntryHandler(String name) {
		return super.getFileEntryHandler(name);
	}

	@Override
	public JdbcTemplate getJdbcTemplate() {
		return super.getJdbcTemplate();
	}

	@Override
	public JdbcDirectorySettings getSettings() {
		return super.getSettings();
	}

	@Override
	public JdbcTable getTable() {
		return super.getTable();
	}

	@Override
	public String[] list() throws IOException {
		return super.list();
	}

	@Override
	public Lock makeLock(String arg0) {
		return super.makeLock(arg0);
	}

	@Override
	public IndexInput openInput(String name, int bufferSize) throws IOException {

        return super.openInput(name);
	}

	@Override
	public IndexInput openInput(String name) throws IOException {

		// Set la taille du buffer : 5Mb
		int bufferSize = 5*1000*1000;

		JdbcFileEntrySettings settings = getSettings().getFileEntrySettings(name);
        String setting = "indexInput.bufferSize";
        //String s = settings.getSetting(setting);
        settings.setIntSetting(setting, bufferSize);

		return super.openInput(name);
	}

	@Override
	public void renameFile(String from, String to) throws IOException {
		super.renameFile(from, to);
	}

	@Override
	public boolean tableExists() throws IOException, UnsupportedOperationException {
		return super.tableExists();
	}

	@Override
	public void touchFile(String name) throws IOException {
		super.touchFile(name);
	}

	@Override
	public void clearLock(String name) throws IOException {
		Lock lock = makeLock(name);
		lock.release();
	}

	@Override
	public LockFactory getLockFactory() {
		return super.getLockFactory();
	}

	@Override
	public String getLockID() {
		return super.getLockID();
	}

	@Override
	public void setLockFactory(LockFactory lockFactory) {
		super.setLockFactory(lockFactory);
	}
}
