package de.banbury.remoteserver

import org.apache.log4j.Logger

class CustomCommands {
	private ConfigObject config

	CustomCommands(File f) {
		config = new ConfigSlurper().parse(f.toURI().toURL())
	}

	CustomCommands(InputStream stream) {
		String cfg = stream.getText()
		stream.close()
		config = new ConfigSlurper().parse(cfg)
	}

	CustomCommands(String filename) {
		this(new File(filename))
	}

	boolean hasCommand(String cmd) {
		return config.containsKey(cmd.split()[0])
	}

	void execute(String cmd) {
		def cmds = cmd.split()
		if (config.containsKey(cmds[0])) {
			try {
				String cmdstr = config.get(cmds[0])
				def arr = [cmdstr]
				arr.add(cmds[1..-1].toList())
				String cmdline = 'cmd /c "' + arr.flatten().join(" ") + '"'
				Logger.getLogger(this.getClass()).debug("Executing command '$cmdline'")
				Process p = Runtime.runtime.exec(cmdline)
			} catch (Exception ex) {
				Logger.getLogger(this.getClass()).error("Error", ex)
			}
		} else {
			Logger.getLogger(this.getClass()).error("Unknown command '" + cmd + "'")
		}
	}
}
