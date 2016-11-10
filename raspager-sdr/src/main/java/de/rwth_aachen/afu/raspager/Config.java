package de.rwth_aachen.afu.raspager;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo.BoardType;

import de.rwth_aachen.afu.raspager.sdr.SerialPortComm;

public class Config {
	private static final Logger log = Logger.getLogger(Config.class.getName());

	// default
	private final String DEFAULT_NAME = "[SDRPager v1.2-SCP-#2345678]";
	private final int DEFAULT_PORT = 1337;

	// default serial
	private final int DEFAULT_SERIAL_PIN = SerialPortComm.DTR;

	// current
	private String name = DEFAULT_NAME;
	private int port = 0;
	private String[] master = null;

	private int serialPin = DEFAULT_SERIAL_PIN;
	private String serialPort = "";
	private BoardType raspi = null;
	private Pin gpioPin = null;
	private boolean useSerial = true;
	private boolean invert = false;
	private int delay = 0;
	private Mixer.Info soundDevice = AudioSystem.getMixerInfo()[0];

	public void load(String filename) {
		Scanner sc;

		try {
			sc = new Scanner(new File(filename));

			// check if this is a valid config file
			if (!sc.hasNextLine() || !sc.nextLine().trim().equals("#[slave config]")) {
				sc.close();
			}

			while (sc.hasNextLine()) {
				String s = sc.nextLine();

				if (s.charAt(0) == '#') {
					// log("Kommentar: " + s, Log.INFO);
					continue;
				}

				if (s.indexOf("=") < 1) {
					// log("Zeile ignoriert (kein = ): " + s, Log.INFO);
					continue;
				}

				String[] p = s.split("=");

				if (p[0].equals("name")) {
					if (p.length > 1) {

						name = p[1];

					}
				} else if (p[0].equals("port")) {

					if (p.length > 1) {

						try {
							this.port = Integer.parseInt(p[1]);
						} catch (NumberFormatException e) {

							this.port = DEFAULT_PORT;

							// log("Port ist auf keinen gueltigen Wert
							// gesetzt!", Log.ERROR);
							// log("Verwende Default-Port (" + this.DEFAULT_PORT
							// + ") ...", Log.INFO);

						}

					}

				} else if (p[0].equals("master")) {
					if (p.length > 1) {
						setMaster(p[1]);
					} else {

						// log("Keine Master angegeben!", Log.INFO);
					}

				} else if (p[0].equals("correction")) {
					// correction factor for audio
					if (p.length > 1) {
						try {
							// TODO impl
							// AudioEncoder.correction = Float.parseFloat(p[1]);
						} catch (NumberFormatException e) {
							// default value
							// TODO impl
							// AudioEncoder.correction =
							// AudioEncoder.DEFAULT_CORRECTION;

							// log("Korrekturfaktor ist auf keinen gueltigen
							// Wert gesetzt!", Log.ERROR);
							// log("Verwende Default-Faktor (" +
							// AudioEncoder.DEFAULT_CORRECTION + ") ...",
							// Log.INFO);

						}
					}
				} else if (p[0].equals("serial")) {
					if (p.length > 1) {
						String[] pp = p[1].split(" ");
						if (pp.length < 2) {
							// log("serial ist nicht gueltig.", Log.ERROR);
							// log("Verwende Default-Serial...", Log.INFO);
							continue;
						}

						this.serialPort = !pp[0].equals("-") ? pp[0] : "";

						if (pp[1].equals("DTR")) {
							this.serialPin = SerialPortComm.DTR;
						} else if (pp[1].equals("RTS")) {
							this.serialPin = SerialPortComm.RTS;
						} else {
							this.serialPin = DEFAULT_SERIAL_PIN;

							// log("serialPin ist auf keinen gueltigen Wert
							// gesetzt!", Log.ERROR);
							// log("Verwende Default-SerialPin...", Log.INFO);
						}
					}
				} else if (p[0].equals("gpio")) {
					String[] pp = p[1].split(" / ");
					if (p.length > 1 || pp.length < 2) {
						if (pp[0].equals("-") || pp[0].equals("-")) {
							setRaspi(null);
							setGpio(null);
							// log("Kein Raspi / GPIO gewünscht! Funktion
							// deaktiviert...", Log.INFO);
						} else {
							setRaspi(BoardType.valueOf(pp[0]));
							setGpio(RaspiPin.getPinByName(pp[1]));
						}
					} else {
						// log("Kein Raspi-Typ / GPIO-Pin angegeben!",
						// Log.INFO);
					}
				} else if (p[0].equals("use")) {
					if (p.length > 1) {
						this.useSerial = p[1].equals("serial");
					}
				} else if (p[0].equals("invert")) {
					if (p.length > 1) {
						this.invert = (p[1].equals("true") || p[1].equals("1"));
					}
				} else if (p[0].equals("delay")) {
					if (p.length > 1) {
						try {
							this.delay = Integer.parseInt(p[1]);
						} catch (NumberFormatException e) {
							// log("Delay ist auf keinen gueltigen Wert
							// gesetzt!", Log.ERROR);
							// log("Verwende Default-Delay (0 ms)...",
							// Log.INFO);
						}
					}
				} else if (p[0].equals("sounddevice")) {
					if (p.length > 1) {
						Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
						boolean found = false;
						for (Mixer.Info device : soundDevices) {
							if (device.getName().equals(p[1])) {
								setSoundDevice(device);
								found = true;
								break;
							}
						}

						if (!found) {
							// log("Angegebenes Sound Device nicht gefunden!",
							// Log.INFO);
						}
					} else {
						// log("Kein Sound Device angegeben!", Log.INFO);
					}
				}
			}

			sc.close();

		} catch (FileNotFoundException e) {
			// log(filename + " konnte nicht gefunden/geoeffnet werden!",
			// Log.ERROR);
			// log("Verwende Default-Werte ...", Log.INFO);
			// log("Diese Konfiguration filtert keine Master!", Log.INFO);
		}

		if (this.name == null) {
			this.name = this.DEFAULT_NAME;

			// log("Kein Name angegeben!", Log.ERROR);
			// log("Verwende Default-Name ([SDR-Pager v1.2-SCP-#2345678]) ...",
			// Log.INFO);
		}

		if (this.port == 0) {
			this.port = this.DEFAULT_PORT;

			// log("Kein Port angegeben!", Log.ERROR);
			// log("Verwende Default-Port (1337) ...", Log.INFO);
		}

		// log(this.toString(), Log.INFO);
	}

	public void loadDefault() {
		loadDefault(false);
	}

	public void loadDefault(boolean resetMaster) {
		this.name = DEFAULT_NAME;
		this.port = DEFAULT_PORT;

		if (resetMaster) {
			this.master = null;
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSerial(String port, int pin) {
		this.serialPort = (port != null ? port : "");
		this.serialPin = pin;
	}

	public void setRaspi(BoardType raspi) {
		this.raspi = raspi;
	}

	public void setGpio(Pin pin) {
		this.gpioPin = pin;
	}

	public void setUseSerial(boolean useSerial) {
		this.useSerial = useSerial;
	}

	public void setInvert(boolean invert) {
		this.invert = invert;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public void setSoundDevice(Mixer.Info mixer) {
		this.soundDevice = mixer;
	}

	public String getSerialPort() {
		return this.serialPort;
	}

	public int getSerialPin() {
		return this.serialPin;
	}

	public BoardType getRaspi() {
		return this.raspi;
	}

	public Pin getGpioPin() {
		return this.gpioPin;
	}

	public boolean useSerial() {
		return this.useSerial;
	}

	public boolean useGpio() {
		return !this.useSerial;
	}

	public boolean getInvert() {
		return this.invert;
	}

	public int getDelay() {
		return this.delay;
	}

	public Mixer.Info getSoundDevice() {
		return this.soundDevice;
	}

	public void setMaster(String masterStr) {
		String[] p = masterStr.split(" +");

		int len = 0;

		for (int i = 0; i < p.length; i++) {

			try {

				String ip = InetAddress.getByName(p[i]).getHostAddress();

				if (i > Arrays.asList(p).indexOf(p[i])) {

					// log("Doppelter Master: " + p[i], Log.INFO);
					p[i] = "";

				} else {

					p[i] = ip;
					len++;

				}

			} catch (UnknownHostException e) {

				// log("Unbekannter Host: " + p[i], Log.ERROR);
				p[i] = "";

			}
		}

		this.master = new String[len];

		for (int i = 0, j = 0; i < p.length; i++) {
			if (!p[i].equals("")) {
				this.master[j] = p[i];
				// log("Master eingetragen: " + this.master[j], Log.INFO);
				j++;
			}
		}

	}

	public boolean isMaster(String ip) {
		if (this.master == null) {
			return true;
		}

		return Arrays.asList(master).contains(ip);
	}

	public String masterToString() {
		if (this.master == null || this.master.length == 0)
			return "";

		String s = this.master[0];

		for (int i = 1; i < this.master.length; i++) {
			s += " " + this.master[i];
		}

		return s;
	}

	public String[] getMaster() {
		if (this.master == null) {
			return null;
		}

		String[] tmp = new String[this.master.length];

		for (int i = 0; i < this.master.length; i++) {
			tmp[i] = this.master[i];
		}

		return tmp;
	}

}
