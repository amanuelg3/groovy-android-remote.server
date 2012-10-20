package de.banbury.remoteserver;

// MouseKeysRemoteServer.java
//
// MouseKeysRemoteServer v1.0.3 for use with MouseKeysRemote v1.0-v1.03.7
//
// Author: Magnus Uppman - magnus.uppman@gmail.com
// (c) 2011 Linuxfunkar - www.linuxfunkar.se
// License: GPL
//
// Usage: java MouseKeysRemoteServer [password]
// ChangeLog:
// 2011-04-19 Version 1.0
// 2011-08-10 Version 1.0.1
//	Changed language Swedish to generic name Custom.
//	Added MPZ command for Mouse Pitch Zoom gesture
// 2011-09-23 Version 1.0.2
//	Added the MMC command for centering the mouse  
// 2011-09-27 Version 1.0.3
//	Added commands for North,South,West etc..  

// Notes:
// Special characters and zoom:
// Must enable Compose and Meta key in KDE.
// System settings -> Keyboard layout -> Advanced

// file:///usr/share/X11/locale/en_US.UTF-8/Compose

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import de.banbury.remoteserver.handler.IKeyboardHandler;
import de.banbury.remoteserver.handler.IMouseHandler;
import de.banbury.remoteserver.handler.KeyboardRobotHandler;
import de.banbury.remoteserver.handler.MouseRobotHandler;

public class MouseKeysRemoteServer extends UDPServer {

	public final static int DEFAULT_PORT = 5555;
	public final static int X_TIMER = 0;
	public final static int Y_TIMER = 1;
	public final static int MOVE_LEFT = 0;
	public final static int MOVE_RIGHT = 1;
	public final static int MOVE_UP = 2;
	public final static int MOVE_DOWN = 3;
	public final static int STOP = 4;
	public final static int STEP = 3;

	public final static int KEY_PRESSED = 0;
	public final static int KEY_RELEASED = 1;

	public final static int KB_CUSTOM = 0;
	public final static int KB_ENGLISH = 1;

	int mouseMoveXdir = STOP, mouseMoveYdir = STOP;
	TimerThread mouseMoveX = new TimerThread(50, X_TIMER, this);
	TimerThread mouseMoveY = new TimerThread(50, Y_TIMER, this);
	static GraphicsEnvironment ge;
	static GraphicsDevice[] gs;
	static GraphicsDevice gd;
	static DisplayMode dm;
	// static int keyb_lang = KB_CUSTOM;
	static int keyb_lang = KB_ENGLISH; // Set default

	private Logger log;

	private IKeyboardHandler keyb;
	private IMouseHandler mouse;

	private int x = 200, y = 200, accX, accY;
	private int w, h;

	private String passwd = "";

	public MouseKeysRemoteServer() throws SocketException, UnknownHostException {
		super(DEFAULT_PORT);

		log = Logger.getLogger(this.getClass());
		mouseMoveX.start();
		mouseMoveY.start();
	}

	public MouseKeysRemoteServer(String passwd) throws SocketException,
			UnknownHostException {
		this();
		this.passwd = passwd;
	}

	public void setKeyboardHandler(IKeyboardHandler keyb) {
		this.keyb = keyb;
	}

	public void setMouseHandler(IMouseHandler mouse) {
		this.mouse = mouse;
	}

	public void respond(DatagramPacket packet) {
		log.trace("respond");
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
		try {
			String s = new String(data, "ASCII");
			if (s.equals("ping")) {
				log.info("Pinged from " + packet.getAddress() + ":"
						+ packet.getPort());
				packet.setData("pong".getBytes("UTF-8"));
				socket.send(packet);
			} else if (s.regionMatches(0, passwd, 0, passwd.length())) {
				String new_s = s.substring(passwd.length());
				decodeMsg(new_s);
			} else {
				log.info(packet.getAddress() + " at port " + packet.getPort()
						+ " says " + s + ".");
				log.info("Wrong password.");
			}

		} catch (Exception e) {
			log.error("Exception", e);
		}

	}

	public boolean callback(int id) {
		log.trace("callback");

		if (id == X_TIMER) { // MouseMoveX
			if (mouseMoveXdir == MOVE_LEFT) {
				accX += STEP;
				x = x - accX;
				if (x < 0)
					x = 1;
				mouse.move(x, y);
				return true;
			}
			if (mouseMoveXdir == MOVE_RIGHT) {
				accX += STEP;
				x = x + accX;
				if (x > w)
					x = w - 1;
				mouse.move(x, y);
				return true;
			}
		} else if (id == Y_TIMER) {
			if (mouseMoveYdir == MOVE_UP) {
				accY += STEP;
				y = y - accY;
				if (y < 0)
					y = 1;
				mouse.move(x, y);
				return true;
			}
			if (mouseMoveYdir == MOVE_DOWN) {
				accY += STEP;
				y = y + accY;
				if (y > h)
					y = h - 1;
				mouse.move(x, y);
				return true;
			}
		}
		return false;
	}

	private int decodeMsg(String s) {
		log.trace("decodeMsg");
		log.debug("Decoding message '" + s + "'");

		if (s.regionMatches(0, "XMM", 0, 3)) {
			handleXMM(s);
		} else if (s.regionMatches(0, "YMM", 0, 3)) {
			handleYMM(s);
		} else if (s.regionMatches(0, "MWS", 0, 3)) { // Mouse wheel move quick
			handleMWS(s);
		} else if (s.regionMatches(0, "KBP", 0, 3)) {
			handleKBP(s);
		} else if (s.regionMatches(0, "KBR", 0, 3)) {
			handleKBR(s);
		} else if (s.regionMatches(0, "MLC", 0, 3)) {
			mouse.down(InputEvent.BUTTON1_MASK);
		} else if (s.regionMatches(0, "MLR", 0, 3)) {
			mouse.up(InputEvent.BUTTON1_MASK);
		} else if (s.regionMatches(0, "MRC", 0, 3)) {
			mouse.down(InputEvent.BUTTON3_MASK);
		} else if (s.regionMatches(0, "MRR", 0, 3)) {
			mouse.up(InputEvent.BUTTON3_MASK);
		} else if (s.regionMatches(0, "MWU", 0, 3)) { // Mouse wheel up
			mouse.wheel(-1);
		} else if (s.regionMatches(0, "MWD", 0, 3)) { // Mouse wheel down
			mouse.wheel(1);
		} else if (s.regionMatches(0, "MML", 0, 3)) {
			handleMML();
		} else if (s.regionMatches(0, "MSL", 0, 3)) { // Stop moving left
			handleMSL();
		} else if (s.regionMatches(0, "MMR", 0, 3)) {
			handleMMR();
		} else if (s.regionMatches(0, "MSR", 0, 3)) { // Stop moving right
			handleMSR();
		} else if (s.regionMatches(0, "MMU", 0, 3)) {
			handleMMU();
		} else if (s.regionMatches(0, "MSU", 0, 3)) {
			handleMSU();
		} else if (s.regionMatches(0, "MMD", 0, 3)) {
			handleMMD();
		} else if (s.regionMatches(0, "MSD", 0, 3)) {
			handleMSD();
		} else if (s.regionMatches(0, "MMC", 0, 3)) {
			handleMMC();
		} else if (s.regionMatches(0, "KBS", 0, 3)) {
			handleKBS(s);
		} else if (s.regionMatches(0, "LNG", 0, 3)) {
			handleLNG(s);
		} else if (s.regionMatches(0, "MPZ", 0, 3)) { // Mouse Pinch Zoom
			handleMPZ(s);
		} else if (s.regionMatches(0, "MZR", 0, 3)) { // Mouse Pinch Zoom Reset
			handleMZR();
		} else if (s.regionMatches(0, "NOP", 0, 3)) {
			return 0;
		} else {
			log.info("Unknown command!" + s);
			return -1;
		}
		return 0;
	}

	/**
	 * Keyboard language
	 * 
	 * @param msg
	 */
	private void handleLNG(String msg) {
		String data = msg.substring(3);
		int i = Integer.parseInt(data);
		keyb_lang = i;
	}

	/**
	 * Mouse movement x-axis.
	 * 
	 * @param msg
	 */
	private void handleXMM(String msg) {
		String data = msg.substring(3);

		int i = Integer.parseInt(data);
		x += i;
		if (x < 0)
			x = 1;
		else if (x > w)
			x = w - 1;
		mouse.move(x, y);
	}

	/**
	 * Mouse movement y-axis
	 * 
	 * @param msg
	 */
	private void handleYMM(String msg) {
		String data = msg.substring(3);
		int i = Integer.parseInt(data);
		y += i;
		if (y < 0)
			y = 1;
		else if (y > h)
			y = h - 1;
		mouse.move(x, y);
	}

	/**
	 * Mouse wheel
	 * 
	 * @param msg
	 */
	private void handleMWS(String msg) {
		String data = msg.substring(3);
		int i = Integer.parseInt(data);

		mouse.wheel(i);
	}

	/**
	 * Mouse pinch zoom
	 * 
	 * @param msg
	 */
	private void handleMPZ(String msg) {
		String data = msg.substring(3);
		int i = Integer.parseInt(data);

		if (i < 0) // Zoom out
		{
			i = -(i);
			keyb.press(KeyEvent.VK_META);

			for (int j = 0; j < i; j++) {
				keyb.press(KeyEvent.VK_MINUS);
				keyb.release(KeyEvent.VK_MINUS);

			}
			keyb.release(KeyEvent.VK_META);

		} else { // Zoom in
			keyb.press(KeyEvent.VK_META);

			for (int j = 0; j < i; j++) {
				if (keyb_lang == KB_CUSTOM) {
					keyb.press(KeyEvent.VK_PLUS);
					keyb.release(KeyEvent.VK_PLUS);

				} else {
					keyb.press(KeyEvent.VK_EQUALS);
					keyb.release(KeyEvent.VK_EQUALS);
				}
			}
			keyb.release(KeyEvent.VK_META);
		}
	}

	/**
	 * Reset zoom
	 */
	private void handleMZR() {
		keyb.press(KeyEvent.VK_META);
		keyb.press(KeyEvent.VK_0);
		keyb.release(KeyEvent.VK_0);
		keyb.release(KeyEvent.VK_META);
	}

	/**
	 * Move mouse left (via button)
	 */
	private void handleMML() {
		accX = 1;
		x -= accX;
		if (x < 0)
			x = 1;
		mouse.move(x, y);
		mouseMoveXdir = MOVE_LEFT;
		mouseMoveX.active = true;
	}

	/**
	 * Stop moving mouse left (via button)
	 */
	private void handleMSL() {
		if (mouseMoveXdir == MOVE_LEFT) {
			mouseMoveXdir = STOP;
			mouseMoveX.active = false;
		}
	}

	/**
	 * Move mouse right (via button)
	 */
	private void handleMMR() {
		accX = 1;
		x += accX;
		if (x > w)
			x = w - 1;
		mouseMoveXdir = MOVE_RIGHT;
		mouseMoveX.active = true;
		mouse.move(x, y);
	}

	/**
	 * Stop moving mouse right (via button)
	 */
	private void handleMSR() {
		if (mouseMoveXdir == MOVE_RIGHT) {
			mouseMoveXdir = STOP;
			mouseMoveX.active = false;
		}
	}

	/**
	 * Move mouse up (via button)
	 */
	private void handleMMU() {
		accY = 1;
		y -= accY;
		if (y < 0)
			y = 1;
		mouse.move(x, y);
		mouseMoveYdir = MOVE_UP;
		mouseMoveY.active = true;
	}

	/**
	 * Stop moving mouse up (via button)
	 */
	private void handleMSU() {
		if (mouseMoveYdir == MOVE_UP) {
			mouseMoveYdir = STOP;
			mouseMoveY.active = false;
		}
	}

	/**
	 * Move mouse down (via button)
	 */
	private void handleMMD() {
		accY = 1;
		y += accY;
		if (y > h)
			y = h - 1;
		mouse.move(x, y);
		mouseMoveYdir = MOVE_DOWN;
		mouseMoveY.active = true;
	}

	/**
	 * Stop moving mouse down (via button)
	 */
	private void handleMSD() {
		if (mouseMoveYdir == MOVE_DOWN) {
			mouseMoveYdir = STOP;
			mouseMoveY.active = false;
		}
	}

	/**
	 * Center mouse (stop movement)
	 */
	private void handleMMC() {
		if (mouseMoveYdir == MOVE_DOWN || mouseMoveYdir == MOVE_UP) {
			mouseMoveYdir = STOP;
			mouseMoveY.active = false;
		}
		if (mouseMoveXdir == MOVE_RIGHT || mouseMoveXdir == MOVE_LEFT) {
			mouseMoveXdir = STOP;
			mouseMoveX.active = false;
		}
		x = w / 2;
		y = h / 2;
		mouse.move(x, y);
	}

	/**
	 * Key pressed
	 * 
	 * @param msg
	 */
	private void handleKBP(String msg) {
		int keyCode = Integer.parseInt(msg.substring(3));
		// Add key to list (if new) and increment keysPressed

		handleKey(keyCode, KEY_PRESSED);
	}

	/**
	 * Key released
	 * 
	 * @param msg
	 */
	private void handleKBR(String msg) {
		int keyCode = Integer.parseInt(msg.substring(3));
		handleKey(keyCode, KEY_RELEASED);
	}

	/**
	 * Key clicked (pressed & released)
	 * 
	 * @param msg
	 */
	private void handleKBS(String msg) {
		String data = msg.substring(3);

		char ch;

		for (int i = 0; i < data.length(); i++) {
			ch = data.charAt(i);

			handleKey(ch, KEY_PRESSED);
			handleKey(ch, KEY_RELEASED);
		}
	}

	void handleKey(int keyCode, int state)
	// Alt-Graph doesn't work... don't bother, use compose instead!
	{
		log.trace("handleKey");
		log.debug("Keycode: " + keyCode + " State: "
				+ ((state == KEY_PRESSED) ? "pressed" : "released"));

		if (keyCode > 64 && keyCode < 91) { // Capital letters
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_SHIFT);
				keyb.press(keyCode);
			} else {
				keyb.release(keyCode);
				keyb.release(KeyEvent.VK_SHIFT);
			}
		} else if (keyCode > 96 && keyCode < 123) { // Small letters
			keyb.activate(keyCode - 32, state);
		} else if (keyCode == 1300) { // North
			keyb.activate(KeyEvent.VK_UP, state);
		} else if (keyCode == 1301) { // South
			keyb.activate(KeyEvent.VK_DOWN, state);
		} else if (keyCode == 1302) { // West
			keyb.activate(KeyEvent.VK_LEFT, state);
		} else if (keyCode == 1303) { // East
			keyb.activate(KeyEvent.VK_RIGHT, state);
		} else if (keyCode == 1304) { // North-West
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_UP);
				keyb.press(KeyEvent.VK_LEFT);
			} else {
				keyb.release(KeyEvent.VK_UP);
				keyb.release(KeyEvent.VK_LEFT);
			}
		} else if (keyCode == 1305) { // North-East
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_UP);
				keyb.press(KeyEvent.VK_RIGHT);
			} else {
				keyb.release(KeyEvent.VK_UP);
				keyb.release(KeyEvent.VK_RIGHT);
			}
		} else if (keyCode == 1306) { // South-West
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_DOWN);
				keyb.press(KeyEvent.VK_LEFT);
			} else {
				keyb.release(KeyEvent.VK_DOWN);
				keyb.release(KeyEvent.VK_LEFT);
			}
		} else if (keyCode == 1307) { // South-East
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_DOWN);
				keyb.press(KeyEvent.VK_RIGHT);
			} else {
				keyb.release(KeyEvent.VK_DOWN);
				keyb.release(KeyEvent.VK_RIGHT);
			}
		} else if (keyCode == 1208 || keyCode == 8) {
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_BACK_SPACE);
			} else {
				keyb.release(KeyEvent.VK_BACK_SPACE);
			}
		} else if (keyCode == 1207 || keyCode == 9) {
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_TAB);
			} else {
				keyb.release(KeyEvent.VK_TAB);
			}
		} else if (keyCode == 10) {
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_ENTER);
			} else {
				keyb.release(KeyEvent.VK_ENTER);
			}
		} else if (keyCode == 13) {
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_ENTER);
			} else {
				keyb.release(KeyEvent.VK_ENTER);
			}
		} else if (keyCode == 32) {
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_SPACE);
			} else {
				keyb.release(KeyEvent.VK_SPACE);
			}
		} else if (keyCode == 33) // !
		{
			if (keyb_lang == KB_CUSTOM || keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_EXCLAMATION_MARK);
					keyb.release(KeyEvent.VK_EXCLAMATION_MARK);
					keyb.release(KeyEvent.VK_SHIFT);
				} else {}
			}
		} else if (keyCode == 34) // "
		{
			if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_2);
					keyb.release(KeyEvent.VK_2);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			} else if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_QUOTE);
					keyb.release(KeyEvent.VK_QUOTE);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 35) // #
		{
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_SHIFT);
				keyb.press(KeyEvent.VK_3);
				keyb.release(KeyEvent.VK_3);
				keyb.release(KeyEvent.VK_SHIFT);
			}
		} else if (keyCode == 36) // $ ¤
		{
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_COMPOSE);
				keyb.release(KeyEvent.VK_COMPOSE);
				keyb.press(KeyEvent.VK_O);
				keyb.release(KeyEvent.VK_O);
				keyb.press(KeyEvent.VK_X);
				keyb.release(KeyEvent.VK_X);
			}
		}

		else if (keyCode == 1205 || keyCode == 43) {
			if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_PLUS);
				} else {
					keyb.release(KeyEvent.VK_PLUS);
				}
			} else if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_EQUALS);
					keyb.release(KeyEvent.VK_EQUALS);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1205 || keyCode == 45) // -
		{
			if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_MINUS);
				} else {
					keyb.release(KeyEvent.VK_MINUS);
				}
			} else if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_UNDERSCORE);
					keyb.release(KeyEvent.VK_UNDERSCORE);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1222 || keyCode == 47) // /
		{
			if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SLASH);
				} else {
					keyb.release(KeyEvent.VK_SLASH);
				}
			} else if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_SLASH);
					keyb.release(KeyEvent.VK_SLASH);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1214 || keyCode == 58) // :
		{
			if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_PERIOD);
					keyb.release(KeyEvent.VK_PERIOD);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			} else if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_SEMICOLON);
					keyb.release(KeyEvent.VK_SEMICOLON);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1220 || keyCode == 59) // ;
		{
			if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SEMICOLON);
				} else {
					keyb.release(KeyEvent.VK_SEMICOLON);
				}
			} else if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_COMMA);
					keyb.release(KeyEvent.VK_COMMA);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1218 || keyCode == 61) // =
		{
			if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_EQUALS);
				} else {
					keyb.release(KeyEvent.VK_EQUALS);
				}
			} else if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_0);
					keyb.release(KeyEvent.VK_0);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 63) // ?
		{
			if (keyb_lang == KB_CUSTOM) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_PLUS);
					keyb.release(KeyEvent.VK_PLUS);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			} else if (keyb_lang == KB_ENGLISH) {
				if (state == KEY_PRESSED) {
					keyb.press(KeyEvent.VK_SHIFT);
					keyb.press(KeyEvent.VK_SLASH);
					keyb.release(KeyEvent.VK_SLASH);
					keyb.release(KeyEvent.VK_SHIFT);
				}
			}
		} else if (keyCode == 1212 || keyCode == 64) // @
		{
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_COMPOSE);
				keyb.release(KeyEvent.VK_COMPOSE);
				keyb.press(KeyEvent.VK_SHIFT);
				keyb.press(KeyEvent.VK_A);
				keyb.release(KeyEvent.VK_A);
				keyb.press(KeyEvent.VK_T);
				keyb.release(KeyEvent.VK_T);
				keyb.release(KeyEvent.VK_SHIFT);
			} else {

			}
		} else if (keyCode == 1213 || keyCode == 92) // \
		{
			if (state == KEY_PRESSED) {
				keyb.press(KeyEvent.VK_COMPOSE);
				keyb.release(KeyEvent.VK_COMPOSE);

				// US
				// keyb.press(KeyEvent.VK_PLUS);
				// keyb.release(KeyEvent.VK_PLUS);
				//
				// keyb.press(KeyEvent.VK_PLUS);
				// keyb.release(KeyEvent.VK_PLUS);
				//
				// keyb.press(KeyEvent.VK_SLASH);
				// keyb.release(KeyEvent.VK_SLASH);
				//
				// keyb.press(KeyEvent.VK_SLASH);
				// keyb.release(KeyEvent.VK_SLASH);

				// Swedish
				keyb.press(KeyEvent.VK_SHIFT);
				keyb.press(55);
				keyb.release(55);
				keyb.press(55);
				keyb.release(55);
				keyb.release(KeyEvent.VK_SHIFT);
			} else {

			}
		} else if (keyCode == 1015) // NUM_KP_0
		{
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD0);
			else
				keyb.release(KeyEvent.VK_NUMPAD0);
		} else if (keyCode == 1016) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD1);
			else
				keyb.release(KeyEvent.VK_NUMPAD1);
		} else if (keyCode == 1017) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD2);
			else
				keyb.release(KeyEvent.VK_NUMPAD2);
		} else if (keyCode == 1018) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD3);
			else
				keyb.release(KeyEvent.VK_NUMPAD3);

		} else if (keyCode == 1019) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD4);
			else
				keyb.release(KeyEvent.VK_NUMPAD4);

		} else if (keyCode == 1020) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD5);
			else
				keyb.release(KeyEvent.VK_NUMPAD5);

		} else if (keyCode == 1021) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD6);
			else
				keyb.release(KeyEvent.VK_NUMPAD6);

		} else if (keyCode == 1022) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD7);
			else
				keyb.release(KeyEvent.VK_NUMPAD7);
		} else if (keyCode == 1023) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD8);
			else
				keyb.release(KeyEvent.VK_NUMPAD8);
		} else if (keyCode == 1024) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_NUMPAD9);
			else
				keyb.release(KeyEvent.VK_NUMPAD9);
		} else if (keyCode == 1101) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F1);
			else
				keyb.release(KeyEvent.VK_F1);

		} else if (keyCode == 1102) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F2);
			else
				keyb.release(KeyEvent.VK_F2);

		} else if (keyCode == 1103) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F3);
			else
				keyb.release(KeyEvent.VK_F3);

		} else if (keyCode == 1104) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F4);
			else
				keyb.release(KeyEvent.VK_F4);

		} else if (keyCode == 1105) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F5);
			else
				keyb.release(KeyEvent.VK_F5);

		} else if (keyCode == 1106) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F6);
			else
				keyb.release(KeyEvent.VK_F6);

		} else if (keyCode == 1107) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F7);
			else
				keyb.release(KeyEvent.VK_F7);

		} else if (keyCode == 1108) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F8);
			else
				keyb.release(KeyEvent.VK_F8);

		} else if (keyCode == 1109) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F9);
			else
				keyb.release(KeyEvent.VK_F9);

		} else if (keyCode == 1110) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F10);
			else
				keyb.release(KeyEvent.VK_F10);

		} else if (keyCode == 1111) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F11);
			else
				keyb.release(KeyEvent.VK_F11);

		} else if (keyCode == 1112) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_F12);
			else
				keyb.release(KeyEvent.VK_F12);

		} else if (keyCode == 1200) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_CONTROL);
			else
				keyb.release(KeyEvent.VK_CONTROL);

		} else if (keyCode == 1201) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_DELETE);
			else
				keyb.release(KeyEvent.VK_DELETE);

		} else if (keyCode == 1202) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_SHIFT);
			else
				keyb.release(KeyEvent.VK_SHIFT);

		} else if (keyCode == 1203) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_ALT);
			else
				keyb.release(KeyEvent.VK_ALT);

		} else if (keyCode == 1204) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_INSERT);
			else
				keyb.release(KeyEvent.VK_INSERT);

		} else if (keyCode == 1206) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_MINUS);
			else
				keyb.release(KeyEvent.VK_MINUS);
		}

		else if (keyCode == 1209) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_PAGE_UP);
			else
				keyb.release(KeyEvent.VK_PAGE_UP);

		} else if (keyCode == 1210) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_PAGE_DOWN);
			else
				keyb.release(KeyEvent.VK_PAGE_DOWN);

		} else if (keyCode == 1211) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_ASTERISK);
			else
				keyb.release(KeyEvent.VK_ASTERISK);

		} else if (keyCode == 1215) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_COMMA);
			else
				keyb.release(KeyEvent.VK_COMMA);

		} else if (keyCode == 1216) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_PERIOD);
			else
				keyb.release(KeyEvent.VK_PERIOD);

		} else if (keyCode == 1217) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_END);
			else
				keyb.release(KeyEvent.VK_END);

		}

		else if (keyCode == 1219) {
			if (state == KEY_PRESSED)
				keyb.press(KeyEvent.VK_HOME);
			else
				keyb.release(KeyEvent.VK_HOME);

		}

		// else if(keyCode == 1221)
		// {
		// if (state == KEY_PRESSED) keyb.press(KeyEvent.VK_NUM_LOCK);
		// else keyb.release(KeyEvent.VK_NUM_LOCK);

		// }
		// else if(keyCode == 1223)
		// {System.out.println("ALT_GR");
		// if (state == KEY_PRESSED) keyb.press(KeyEvent.VK_ALT_GRAPH);
		// else keyb.release(KeyEvent.VK_ALT_GRAPH);
		// }
		else {
			log.warn("Unknown key:" + keyCode);
			if (state == KEY_PRESSED) {
				keyb.press(keyCode);
			} else {
				keyb.release(keyCode);
			}
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		log.info("Server started.");
		log.info("Screen: w: " + w + " h:" + h);
	}

	public static void main(String[] args) {
		Logger log = Logger.getLogger("Main");
		log.trace("Application started.");

		try {
			String passwd = "";
			if (args.length > 0)
				passwd = args[0];

			MouseKeysRemoteServer server = new MouseKeysRemoteServer(passwd);
			server.setKeyboardHandler(new KeyboardRobotHandler());
			server.setMouseHandler(new MouseRobotHandler());

			log.info("Starting server...");
			server.start();
		} catch (Exception e) {
			log.error("Exception", e);
		}

	}
}
