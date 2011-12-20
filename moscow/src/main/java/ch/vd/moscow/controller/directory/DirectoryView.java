package ch.vd.moscow.controller.directory;

import ch.vd.moscow.data.LogDirectory;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DirectoryView {

	private Long id;
	private Long envId;
	private String envName;
	private String directoryPath;
	private String pattern;

	public DirectoryView() {
	}

	public DirectoryView(LogDirectory directory) {
		this.id = directory.getId();
		this.envId = directory.getEnvironment().getId();
		this.envName = directory.getEnvironment().getName();
		this.directoryPath = directory.getDirectoryPath();
		this.pattern = directory.getPattern();
	}

	public String getName() {
		return String.format("%s (%s)", directoryPath, envName);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getEnvId() {
		return envId;
	}

	public void setEnvId(Long envId) {
		this.envId = envId;
	}

	public String getEnvName() {
		return envName;
	}

	public void setEnvName(String envName) {
		this.envName = envName;
	}

	public String getDirectoryPath() {
		return directoryPath;
	}

	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
