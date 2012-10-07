package de.banbury.remoteserver.handler

import java.awt.Robot

class KeyboardRobotHandler implements IKeyboardHandler {
	private Robot robot;

	public KeyboardRobotHandler() {
		robot = new Robot();
	}

	public void press(int code) {
		robot.keyPress(code)
	}

	public void release(int code) {
		robot.keyRelease(code)
	}

	public void activate(int code, release) {
		if (release)
			release(code)
		else
			press(code)
	}

	public void click(int code) {
		press(code)
		release(code)
	}
}
