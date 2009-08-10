package com.spicyhorse.qa.remotemonitor;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

//TODO: find a way to unit test the functions of the class, especially for multi-threading.

//TODO: find a profile tool to perfect myself

//TODO: thread kill itself after throwing exception. make sure handling gracefully! Sync resources

//TODO: add logging

//TODO: add shortcut keys
public class GuiStatusMonitor implements Observer {

	private static final String STR_INTERNAL_WEBAPPTASK = "WEBAPPTASK";
	private static final String STR_INTERNAL_PINGTASK = "PINGTASK";
	private static final String STR_INTERNAL_BUILDBOT = "BUILDBOT";

	// to gather all the down threadinfo, remeber to clean if one thread reports
	// ok in update()
	private static Map<Long, StatusMessage> downThreads = new TreeMap<Long, StatusMessage>();

	// no need to make this class multithread!

	static Logger logger = Logger.getLogger(GuiStatusMonitor.class);

	private static List<MonitorableTask> tasks = new ArrayList<MonitorableTask>();

	private static String old_status;
	private static StatusMessage new_status;

	private static PopupMenu popup;
	private static TrayIcon trayIcon;
	private static MenuItem aboutItem;
	private static MenuItem exitItem;
	private static Menu newTaskItem;
	private static MenuItem newPingTaskItem;
	private static MenuItem newWebappTaskItem;
	private static MenuItem newBuildbotBuildTaskItem;

	private static Menu allstatus;

	// not in use
	private static Image errorImage;
	private static Image successImage;
	private static Image failImage;
	private static Image warnImage;
	private static Image exceptionImage;

	private static Image greenImage;
	private static Image redImage;

	private final static String PAUSE_LABEL = "-_-i   Cease fire!";
	private final static String REMOVE_LABEL = "#_#   Kill me!";
	private static final String RESUME_LABEL = "^_^   Pick me up!";

	private final static String REMEMBER_TARGET_LABEL = "@_@  Always Monitoring!";
	private final static String PING_STATUS_MONITORING_LABEL = "Ping Status Monitoring";
	private final static String WEB_APPLICATION_STATUS_MONITORING_LABEL = "Web application Status Monitoring";
	private final static String BUILDBOT_STATUS_MONITOR_LABEL = "Buildbot Builder's Status Monitoring";

	final static SystemTray tray = SystemTray.getSystemTray();
	private static final String LAST_MONITOR_TARGETS_INI = "last_monitor_targets.ini";

	private static GuiStatusMonitor self; // singleton impl.

	ActionListener subtasksListener; // listening for 'pause, kill, remember
										// target' buttons actions.

	// ATTE: do not confused with 'newTaskListener', which is listening for 'new
	// XXXX monitoring' actions.

	// private static Menu tasksPlaceHolder ; // no running monitoring task, it
	// shows empty, otherwise, show info.

	// weak singleton implementation
	public static GuiStatusMonitor getGUIStatusMonitor() throws Exception {
		if (self == null) {
			self = new GuiStatusMonitor();
		}
		return self;
	}

	private GuiStatusMonitor() throws Exception {
		// old_status = "" ;
		// new_status = "" ;
		loadResources();
		createAndShowGUI();
		loadLastMonitoringTarget();
	}

	// / Load necessary images into cache
	static void loadResources() throws IOException {
		errorImage = createImage("/images/buildbot.gif", "errorImage");
		successImage = createImage("/images/buildbot-good.gif", "successImage");
		failImage = createImage("/images/buildbot-bad.gif", "failImage");
		warnImage = createImage("/images/buildbot-warn.gif", "warnImage");
		exceptionImage = createImage("/images/buildbot-exception.gif",
				"exceptionImage");

		greenImage = createImage("/images/buildbot-good.gif", "failImage"); // createImage(
																			// "/images/green.jpg"
																			// ,
																			// "tray icon green");
		redImage = createImage("/images/buildbot-bad.gif", "errorImage"); // createImage(
																			// "/images/red.jpg"
																			// ,
																			// "tray icon red");
	}

	public static long extractThreadIDFromMenuLabel(String info) {
		Pattern p = Pattern.compile("id:(\\d+)\\s+");
		Matcher m = p.matcher(info);
		if (m.find()) {
			String item = info.substring(m.start() + 3, m.end());
			logger.debug("!!! extracting thread id: " + item.trim());
			return Long.parseLong(item.trim());
		} else {
			return 0L;
		}
	}

	public void removeParentMenu(MenuItem childMenu) {
		if (childMenu.getParent() instanceof Menu) {
			logger.debug("...... remove Parent menu, " + childMenu.getParent());
			logger.debug("...... popup.getItem(0), " + popup.getItem(0));
			logger.debug("...... is parent in popup, "
					+ (popup.getItem(0) == childMenu.getParent()));
			Menu grandpa = (Menu) ((Menu) childMenu.getParent()).getParent();
			Menu father = (Menu) childMenu.getParent();
			grandpa.remove(father);
		}
	}

	public void removeFromThreadPool(long threadID) {
		Iterator<MonitorableTask> iter = tasks.iterator();
		while (iter.hasNext()) {
			MonitorableTask pt2remove = (MonitorableTask) iter.next();
			Thread thread = pt2remove.getThisThread();
			if (thread.getId() == threadID) {
				pt2remove.stop(); // ESP: can use thread.destroy() which is
									// deprecated.
				iter.remove();
				logger.debug("threadID:" + threadID + " was killed! ");
				logger
						.debug("Tasks collection has "
								+ tasks.size()
								+ " items of thread(s)! If this succeed, see GUI will update");
				return;
			}
		}
		logger.debug("!!!!!!! threadID:" + threadID + " was NOT FOUND! ");
		logger.debug("Tasks collection has " + tasks.size()
				+ " items of thread(s)!");
	}

	private void createAndShowGUI() throws Exception {
		// Check the SystemTray support
		if (!SystemTray.isSupported()) {
			logger.error("SystemTray is not supported");
			throw new Exception(
					"createAndShowGUI():SystemTray is not supported!");
		}
		popup = new PopupMenu();
		URL u = getClass().getProtectionDomain().getCodeSource().getLocation();
		File f = new File(u.toURI());
		logger.debug(f.getParent());
		logger.debug("=================root directory is " + u.toString());

		trayIcon = new TrayIcon(greenImage);

		allstatus = new Menu("All Status");
		newTaskItem = new Menu("New Task");
		newPingTaskItem = new MenuItem(PING_STATUS_MONITORING_LABEL);
		newWebappTaskItem = new MenuItem(
				WEB_APPLICATION_STATUS_MONITORING_LABEL);
		newBuildbotBuildTaskItem = new MenuItem(BUILDBOT_STATUS_MONITOR_LABEL);
		aboutItem = new MenuItem("About");
		exitItem = new MenuItem("Exit");

		// tasksPlaceHolder = new Menu("");
		// NOTES:
		// * I try to make a status info in the 1st level of pop-up Menu,
		// but it seems default api has no easy way of using position mechanism,
		// so at first, I just make status menu as second level, which depend on
		// 1st level menu, the 'allstatus' Menu

		// Add components to popup menu
		popup.add(allstatus);
		popup.addSeparator();
		popup.add(newTaskItem);
		newTaskItem.add(newPingTaskItem);
		newTaskItem.add(newWebappTaskItem);
		newTaskItem.add(newBuildbotBuildTaskItem);
		popup.addSeparator();
		popup.add(aboutItem);
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			logger.error("TrayIcon could not be added.");
			throw new Exception(
					"createAndShowGUI():SystemTray is not supported!");
		}

		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane
						.showMessageDialog(null,
								"IP & Webapp Availability Monitor v0.1 for SpicyHorse QA.");
			}
		});

		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane
						.showMessageDialog(null,
								"IP & Webapp Availability Monitor, maintainer: realalien@gmail.com");
			}
		});
		subtasksListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String subtaskMenuItemLabel = null;
				if (e.getSource() instanceof MenuItem) {
					subtaskMenuItemLabel = ((MenuItem) e.getSource())
							.getLabel();
				}

				Map<String, Integer> label_int = new HashMap<String, Integer>();
				label_int.put(PAUSE_LABEL, 1);
				label_int.put(RESUME_LABEL, 2);
				label_int.put(REMOVE_LABEL, 3);
				label_int.put(REMEMBER_TARGET_LABEL, 4);

				String threadLabel = null;
				Long thread_id = null;
				logger.debug("********************" + e.getActionCommand());
				switch (label_int.get(subtaskMenuItemLabel)) {
				// TODO: use enum as string label entry?

				case 1: // PAUSE_LABEL pressed

					if (e.getActionCommand().equals(PAUSE_LABEL)) { // pause,
																	// resume
																	// share one
																	// menuitem
						// change label
						((MenuItem) e.getSource()).setLabel(RESUME_LABEL);
						// change thread property, get thread_id from father's
						// label
						threadLabel = ((Menu) ((MenuItem) e.getSource())
								.getParent()).getLabel();
						thread_id = extractThreadIDFromMenuLabel(threadLabel);
						pause_thread(thread_id);
						logger.error(PAUSE_LABEL + " was pressed!");

					}
					break;
				case 2: // RESUME_LABEL pressed
					if (e.getActionCommand().equals(RESUME_LABEL)) { // pause,
																		// resume
																		// share
																		// one
																		// menuitem
						((MenuItem) e.getSource()).setLabel(PAUSE_LABEL);
						// change thread property, get thread_id from father's
						// label
						threadLabel = ((Menu) ((MenuItem) e.getSource())
								.getParent()).getLabel();
						thread_id = extractThreadIDFromMenuLabel(threadLabel);
						resume_thread(thread_id);
						logger.error(RESUME_LABEL + " was pressed!");
					}
					break;
				case 3:
					logger.error(REMOVE_LABEL + " was pressed!");
					removeParentMenu((MenuItem) e.getSource());
					long threadID = extractThreadIDFromMenuLabel(((Menu) ((MenuItem) e
							.getSource()).getParent()).getLabel());
					removeFromThreadPool(threadID);
					break;
				case 4:
					logger.debug(REMEMBER_TARGET_LABEL + " was pressed!");
					if (((CheckboxMenuItem) e.getSource()).getState() == true) {
						((CheckboxMenuItem) e.getSource()).setState(false);
					} else {
						((CheckboxMenuItem) e.getSource()).setState(true);
					}
				}

				// if (((MenuItem) e.getSource()).getLabel() == PAUSE_LABEL &&
				// e.getActionCommand().equals(PAUSE_LABEL)) {
				// //change label
				// ((MenuItem) e.getSource()).setLabel(RESUME_LABEL);
				// // change thread property, get thread_id from father's label,
				// or what else methods?
				//						
				// threadLabel = ((Menu)((MenuItem)
				// e.getSource()).getParent()).getLabel() ;
				// thread_id = extractThreadIDFromMenuLabel(threadLabel);
				// pause_thread(thread_id);
				// logger.debug(PAUSE_LABEL + " was pressed!");
				//						
				//						
				// }else if (((MenuItem) e.getSource()).getLabel() ==
				// RESUME_LABEL && e.getActionCommand().equals(RESUME_LABEL)) {
				// //change label
				// ((MenuItem) e.getSource()).setLabel(PAUSE_LABEL);
				// // change thread property
				// threadLabel = ((Menu)((MenuItem)
				// e.getSource()).getParent()).getLabel() ;
				// thread_id = extractThreadIDFromMenuLabel(threadLabel);
				// resume_thread(thread_id);
				// logger.debug(RESUME_LABEL + " was pressed!");
				// }else if (((MenuItem) e.getSource()).getLabel() ==
				// REMOVE_LABEL) {
				// logger.debug(REMOVE_LABEL + " was pressed!");
				// removeParentMenu((MenuItem) e.getSource());
				// long threadID = extractThreadIDFromMenuLabel(((Menu)
				// ((MenuItem) e
				// .getSource()).getParent()).getLabel());
				// removeFromThreadPool(threadID);
				// } else if (((MenuItem) e.getSource()).getLabel() ==
				// REMOVE_LABEL) {
				// logger.debug(REMEMBER_TARGET_LABEL + " was pressed!");
				// if (((CheckboxMenuItem) e.getSource()).getState() == true){
				// ((CheckboxMenuItem) e.getSource()).setState(false);
				// }else{
				// ((CheckboxMenuItem) e.getSource()).setState(true);
				// }
				// }
				// Q: shall I create a thread status listener as each updating
				// invokes a lot of jobs to do?!
				logger
						.debug("=====Here we are in the listener of subtasks for MenuItem of different thread!");
			}
		};

		class NewTaskListener implements ActionListener {

			/**
			 * Notes: The instance property of the inner class was originally
			 * introduced as clause 'produce_task()' should allow a new task
			 * accepting an Observer(or Observer's subclasses). As we used this
			 * class as inner class due to references of GUI component, thus no
			 * need for this property 'observer'.
			 * 
			 * public Observer observer = null;
			 * 
			 * NewTaskListener(Observer obs) { this.observer = obs; }
			 */

			// TODO: with each new kind of task introduced, this method becomes
			// rather fat,
			// see how to smell good
			@Override
			public void actionPerformed(ActionEvent e) {
				MenuItem item = (MenuItem) e.getSource();
				// TrayIcon.MessageType type = null;
				logger.debug(item.getLabel());
				String pressedItemLabel = item.getLabel();
				if (pressedItemLabel.contains("Monitoring")) {
					// add a thread and update GUI
					String ip = JOptionPane
							.showInputDialog("Please input a ip");
					String freqInput = JOptionPane
							.showInputDialog("Please input a timeout(in second)");
					String default_website_port = "80";
					String default_buildbot_port = "9911";
					String port = null;
					String builder = null;
					if (pressedItemLabel
							.contains(WEB_APPLICATION_STATUS_MONITORING_LABEL)) {
						port = JOptionPane
								.showInputDialog("Please input a web application listening port");
						if (!isNumeric(port)) {
							port = default_website_port;
						}
					}
					if (pressedItemLabel
							.contains(BUILDBOT_STATUS_MONITOR_LABEL)) {
						port = JOptionPane
								.showInputDialog("Please input a web application listening port");
						if (!isNumeric(port)) {
							port = default_buildbot_port;
						}
						builder = JOptionPane
								.showInputDialog("Please input the builder name you want to monitor");
					}

					// construct basic info to create corresponding thread.
					long time_out = 5000; // if no assign
					try {
						time_out = Long.parseLong(freqInput) * 1000;
					} catch (NumberFormatException exp) {
						time_out = 15000;
					}
					if (ip != null) { // TODO:validate IP
						StringBuffer data = new StringBuffer();
						if (item.getLabel()
								.equals(PING_STATUS_MONITORING_LABEL)) {
							data.append(STR_INTERNAL_PINGTASK);
						} else if (item.getLabel().equals(
								WEB_APPLICATION_STATUS_MONITORING_LABEL)) {
							data.append(STR_INTERNAL_WEBAPPTASK);
						} else if (item.getLabel().equals(
								BUILDBOT_STATUS_MONITOR_LABEL)) {
							data.append(STR_INTERNAL_BUILDBOT);
						}
						data.append(":::");
						data.append(ip);
						data.append(":::");
						if (port != null) {
							data.append(port);
							data.append(":::");
						}
						data.append(time_out);
						if (builder != null) {
							data.append(":::");
							data.append(builder);
						}

						Thread thread = produce_task(data.toString());
						if (thread != null) {
							thread.start();
							String threadInfo = thread.getName() + " id:"
									+ thread.getId() + " => reaching:" + ip;
							if (port != null) {
								threadInfo = threadInfo + ":" + port;
							}
							threadInfo = threadInfo + " freq.:" + freqInput;
							if (builder != null) {
								threadInfo = threadInfo + " builder name:"
										+ builder;
							}

							trayIcon.displayMessage("Created!", threadInfo,
									TrayIcon.MessageType.INFO);
						} else {
							trayIcon.displayMessage(
									"Failed to create a thread! ",
									"Unable to create a thread with data "
											+ data.toString(),
									TrayIcon.MessageType.ERROR);
						}
					} else {
						trayIcon.displayMessage("Failed to create a thread! ",
								"IP address is invalid",
								TrayIcon.MessageType.ERROR);
					}
				} else {
					trayIcon
							.displayMessage(
									"Failed to create a thread! ",
									"MenuItem pressed does not contain keyword 'Monitoring'",
									TrayIcon.MessageType.ERROR);
				}
			}
		}

		NewTaskListener newTaskListener = new NewTaskListener();

		newPingTaskItem.addActionListener(newTaskListener);
		newWebappTaskItem.addActionListener(newTaskListener);
		newBuildbotBuildTaskItem.addActionListener(newTaskListener);

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] options = { "Yes, quit the program!",
						"No, keep it working!" };

				int ret = JOptionPane.showOptionDialog(null,
						"Do you want to quit?", "Please confirm",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[0]);

				if (ret == 0) { // choose Yes
					saveMonitorTarget();
					tray.remove(trayIcon);
					System.exit(0);
				}
			}
		});
	}

	// make it private after
	public void pause_thread(Long thread_id) {
		Iterator<MonitorableTask> iter = tasks.iterator();
		while (iter.hasNext()) {
			MonitorableTask pt2remove = (MonitorableTask) iter.next();
			Thread thread = pt2remove.getThisThread();
			if (thread.getId() == thread_id) {
				pt2remove.suspend(); // ESP: can use thread.destroy(), test if
										// null
				// ok

				logger.debug("threadID:" + thread_id + " was suspened! ");
				logger.debug("Tasks collection has " + tasks.size()
						+ " items of thread(s)! In pause thread...");
				return;
			}
		}
		logger.debug("!!!!!!! try to suspend threadID:" + thread_id
				+ " was NOT FOUND! ");
	}

	public void resume_thread(Long thread_id) {
		Iterator<MonitorableTask> iter = tasks.iterator();
		while (iter.hasNext()) {
			MonitorableTask pt2remove = (MonitorableTask) iter.next();
			Thread thread = pt2remove.getThisThread();

			if (thread.getId() == thread_id) {
				pt2remove.resume();
				logger.debug("threadID:" + thread_id + " was resumed! ");
				logger.debug("Tasks collection has " + tasks.size()
						+ " items of thread(s)! In pause thread...");
				return;
			}
		}
		logger.debug("!!!!!!! try to resume threadID:" + thread_id
				+ " was NOT FOUND! ");
	}

	private boolean isNumeric(String port) {
		if (port == null) {
			return false;
		}
		return java.util.regex.Pattern.matches("\\d+", port);
	}

	protected static Image createImage(String url, String description) {
		URL imageURL = GuiStatusMonitor.class.getResource(url);

		if (imageURL == null) {
			logger.debug("Resource not found: " + url);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}

	@Override
	public void update(Observable target, Object status) {
		if (status instanceof StatusMessage) {
			new_status = (StatusMessage) status; // variable for reuse

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					logger
							.debug("------update: invoke runnalbe for update GUI!");
					if (new_status != null && new_status.isDown()) {
						trayIcon.displayMessage("Detected an exception.! ",
								new_status.getData(),
								TrayIcon.MessageType.WARNING);
						trayIcon.setImage(redImage);
						downThreads.put(new_status.getId(), new_status);
					}
					if (new_status != null && new_status.getId() > 0
							&& new_status.isUp()) {
						if (downThreads.containsKey(new_status.getId())) {
							downThreads.remove(new_status.getId());
						}
					}
					logger.debug("down threads : " + downThreads.toString());
					if (tasks.size() == 0 || downThreads.size() == 0) {
						trayIcon.setImage(greenImage);
					}
				}

			});
			// old_status = new_status;
		} else {
			logger.debug("no status got!");
			// logger.debug("from observer: " + old_status
			// + " no change.");
		}
	}

	// if (status instanceof String) {
	// new_status = (String) status; // variable for reuse
	// // if (new_status != old_status) {
	// // logger.debug("------update: changing status to new : "
	// // + new_status + " from old: " + old_status);
	// //
	// //// if (SwingUtilities.isEventDispatchThread()) {
	// //// logger.debug("******* it's isEventDispatchThread");
	// //// } else {
	// //// logger.debug("******* it's not isEventDispatchThread"); // this
	// //// }
	//
	//
	// SwingUtilities.invokeLater(new Runnable() {
	// public void run() {
	// logger.debug("------update: invoke runnalbe for update GUI!");
	// if (new_status != null && new_status.contains("DOWN")) {
	//							
	// trayIcon.displayMessage("Detected an exception.! ",
	// new_status,
	// TrayIcon.MessageType.WARNING);
	// trayIcon.setImage(redImage);
	// }
	// if (tasks.size() == 0) {
	// trayIcon.setImage(greenImage);
	// }
	// }
	//					
	// });
	// // old_status = new_status;
	// } else {
	// logger.debug("no status got!");
	// // logger.debug("from observer: " + old_status
	// // + " no change.");
	// }
	// }

	// logger.debug("from observer: " + new_status);
	// }

	// I forgot why I was trying to implement GUIstatusMonitor a multithread,
	// I also find that there are no need to make task multithread, just put
	// those tasks in a sleep mode and reading a suspend switch!
	// @Override
	// public void run() {
	// while (true) {
	//
	// System.out
	// .println("------run: Detecting every 10 sec. ....detecting: staus="
	// + new_status + "!");
	//
	// try {
	// Thread.sleep(5000);
	// } catch (InterruptedException e) {
	// logger.debug("observer intrrupted!");
	// }
	// if (tasks.size() == 0 ){ // make sure its color is right
	// trayIcon.setImage(createImage("/images/green.jpg",
	// new_status));
	// }
	// }
	// }

	/**
	 * To write tasks to a file for reloading next time Note: * I think just
	 * follow GUI info, as those tasks do not care about the logging
	 * */
	public void saveMonitorTarget() {
		int numOfTask = allstatus.getItemCount();
		logger.debug("Total number of tasks: " + numOfTask);

		StringBuffer content = new StringBuffer();
		for (int i = 0; i < numOfTask; i++) {
			Menu taskMenu = (Menu) allstatus.getItem(i);
			for (int j = 0; j < taskMenu.getItemCount(); j++) {
				MenuItem item = taskMenu.getItem(j);
				if (item.getLabel().equals(REMEMBER_TARGET_LABEL)) {
					if ((((CheckboxMenuItem) item).getState()) == true) {
						logger.debug("ready to write file with info: "
								+ taskMenu.getLabel());
						String parsedEntry = makeEntryLog(taskMenu.getLabel());
						logger.debug("parsed entry : " + parsedEntry);
						content.append(System.getProperty("line.separator"));
						content.append(parsedEntry);
					}
				}
			}
		}
		try {
			String fileName = LAST_MONITOR_TARGETS_INI;
			logger.debug("total content  : " + content);
			FileWriter fw = new FileWriter(fileName);
			BufferedWriter bos = new BufferedWriter(fw);
			bos.write(content.toString());
			bos.close();
			fw.close();
		} catch (IOException e) {
			logger.error("saveMonitorTarget(): can't save message:" + content);
			logger.error(e.getMessage());
		}
	}

	/**
	 * Parse menu label to make a entry like "PINGTASK:::192.168.6.10:::5000"
	 * TODO: make it object attached to a menu, some kind of MVC.
	 * */
	private String makeEntryLog(String label) {
		StringBuffer entry = new StringBuffer();
		String seperator = ":::";
		if (label != null) {
			// append TASK_identity
			if (label.contains("PingTask")) {
				entry.append(STR_INTERNAL_PINGTASK);
				entry.append(seperator);
			} else if (label.contains("WebApplicationMonitor")) {
				entry.append(STR_INTERNAL_WEBAPPTASK);
				entry.append(seperator);
			} else if (label.contains(STR_INTERNAL_BUILDBOT)) {
				entry.append(STR_INTERNAL_BUILDBOT);
				entry.append(seperator);
			}

			// append ip:::port:::
			Pattern pat = Pattern.compile("reaching:[\\d+|\\.|\\:]+\\s+");
			Matcher m = pat.matcher(label);
			if (m.find()) {
				String item = label.substring(m.start() + 9, m.end()).trim();
				if (item.contains(":")) {
					String tcpip[] = item.split(":");
					entry.append(tcpip[0]);
					entry.append(seperator);
					entry.append(tcpip[1]);
				} else {
					entry.append(item);
				}
				entry.append(seperator);
			}

			// append freq.
			pat = Pattern.compile("freq.:\\d+[\\s+]{0,}");
			m = pat.matcher(label);
			if (m.find()) {
				String freq = label.substring(m.start() + 6, m.end()).trim();
				entry.append(freq);
				entry.append(seperator);
			}

			// append builder.
			pat = Pattern.compile("builder:.*");
			m = pat.matcher(label);
			if (m.find()) {
				String buidler = label.substring(m.start() + 8, m.end()).trim();
				entry.append(buidler);
			}

		}
		logger.debug("Before writting to file, we got an entry: " + entry);
		return entry.toString();
	}

	private void loadLastMonitoringTarget() {
		List<String> entries = new ArrayList<String>();
		try {
			FileReader fr = new FileReader(LAST_MONITOR_TARGETS_INI);
			BufferedReader bw = new BufferedReader(fr);
			String line;
			while ((line = bw.readLine()) != null) {
				if (line.contains(":::")) {
					logger.debug("Found a entry: " + line);
					entries.add(line);
				}
			}
			fr.close();
			bw.close();
			logger.info("Loaded last monitor target from:"
					+ LAST_MONITOR_TARGETS_INI);
		} catch (FileNotFoundException e) {
			e.printStackTrace(); // TODO: do not use GUI, pass silently, or use
									// logging.
		} catch (IOException e) {
			e.printStackTrace();
		}

		// String pingTaskRec = "PINGTASK:::192.168.6.111:::5000";
		// String webappTaskRec = "WEBAPPTASK:::192.168.6.111:::80:::5000";
		//
		// logger.debug("Reading last saved monitoring target: " + pingTaskRec);
		// logger.debug("Reading last saved monitoring target: " +
		// webappTaskRec);
		//	
		// Thread thread = produce_task(pingTaskRec);
		// if (thread != null ) {thread.start();}
		//		
		// Thread thread2 = produce_task(webappTaskRec);
		// if (thread2 != null ) {thread2.start();}

		// start all threads
		for (int i = 0; i < entries.size(); i++) {
			Thread newThread = produce_task(entries.get(i));
			newThread.start();
		}
	}

	/**
	 * Because creating a task involves many steps, such as "create threads",
	 * "add to thread pool", "add status change listener", "create gui",
	 * "add sub-menu and their listeners", thus use factory pattern here, Q:
	 * question about the exposure of instance property!
	 * */
	private Thread produce_task(String data) {
		String info[] = data.split(":::");
		logger.debug("produce_task():Reading last saved monitoring target: "
				+ data.toString());

		String task_category = info[0];
		String ip = null;
		Long freq = null;
		String port = null; // only applicable to WEBAPPTASK
		String builder = null; // only applicable to BUILDBOT task
		// create threads
		MonitorableTask task = null;
		if (task_category.equals(STR_INTERNAL_PINGTASK)) {
			ip = info[1];
			freq = Long.parseLong(info[2]);

			if (ip == null || freq == null) {
				return null;
			}
			task = new PingTask(freq, ip);

		} else if (task_category.equals(STR_INTERNAL_WEBAPPTASK)) {
			ip = info[1];
			port = info[2];
			freq = Long.parseLong(info[3]);
			if (ip == null || freq == null || port == null) {
				return null;
			}
			task = new WebApplicationMonitor(freq, ip, port);
		} else if (task_category.equals(STR_INTERNAL_BUILDBOT)) {
			ip = info[1];
			port = info[2];
			freq = Long.parseLong(info[3]);
			builder = info[4];
			if (ip == null || freq == null || port == null || builder == null) {
				return null;
			}
			task = new BuildbotStatusMonitor(freq, ip, port, builder);
		}

		if (task != null) {
			Thread thread = task.getThisThread();
			// ESP: do NOT change the thread info for label, as it is used to
			// extract info.
			String threadInfo = task_category + " id:"
					+ task.getThisThread().getId() + "  " + " => reaching:"
					+ ip;
			if (port != null) {
				threadInfo = threadInfo + ":" + port;
			}
			threadInfo = threadInfo + " freq.:" + freq;
			if (builder != null) {
				threadInfo = threadInfo + " builder:" + builder;
			}

			// hidden info: task.getThisThread().getName() + " id:" +
			// task.getThisThread().getId()
			// add to thread pool
			tasks.add(task);

			// add status change listener
			task.addObserver(this);

			// create gui
			Menu new_status_info = new Menu(threadInfo);
			allstatus.add(new_status_info);

			// add sub-menu for manipulation of each task
			MenuItem pauseItem = new MenuItem(PAUSE_LABEL);
			MenuItem killItem = new MenuItem(REMOVE_LABEL);
			CheckboxMenuItem memorizeItem = new CheckboxMenuItem(
					REMEMBER_TARGET_LABEL);
			memorizeItem.setState(true);
			pauseItem.addActionListener(subtasksListener);
			killItem.addActionListener(subtasksListener);
			memorizeItem.addActionListener(subtasksListener);

			new_status_info.add(pauseItem);
			new_status_info.add(killItem);
			new_status_info.addSeparator();
			new_status_info.add(memorizeItem);
			pauseItem.addActionListener(subtasksListener);

			logger
					.debug("produce_task()...OK with a new thread fully armed with menuitems!");
			return thread;
		} else {
			logger
					.debug("produce_task()...return null rather than a thread instance.");
			return null;
		}
	}
}

/**
 * ***************************************************************************
 * NOT IN USE!
 * ***************************************************************************
 * */
// see if we can add position mechanism for the Menu, or background color, if
// possible!
// * java.awt.Menu has position implementation
// * TODO: add a property of thread info, instead of parsing menu label.
class MyMenuButton extends Menu {

	private static final long serialVersionUID = -2072856829961148670L;

	private Color backgroundColor;

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public MyMenuButton(String label) {
		super(label);
	}
}