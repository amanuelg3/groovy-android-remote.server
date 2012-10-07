package de.banbury.remoteserver.handler

interface IMouseHandler {
	void move(x, y)
	void wheel(n)
	void click(int button)
	void down(int button)
	void up(int button)
}
