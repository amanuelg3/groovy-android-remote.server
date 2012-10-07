package de.banbury.remoteserver.handler

import java.awt.Robot

class MouseRobotHandler implements IMouseHandler {
	private Robot robot;

	public MouseRobotHandler() {
		robot = new Robot();
	}

	public void move(x, y) {
		robot.mouseMove(x, y)
	}

	public void wheel(n) {
		robot.mouseWheel(n)
	}

	public void click(int button) {
		robot.mousePress(button)
		robot.mouseRelease(button)
	}

	public void down(int button) {
		robot.mousePress(button)
	}

	public void up(int button) {
		robot.mouseRelease(button)
	}
}
