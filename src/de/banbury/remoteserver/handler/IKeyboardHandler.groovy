package de.banbury.remoteserver.handler

interface IKeyboardHandler {
	void press(int code)
	void release(int code)
	void activate(int code, release)
	void click(int code)
}
