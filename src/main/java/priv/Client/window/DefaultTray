package priv.Client.window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import priv.common.ResourceLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *  * @author  pyuan
 *  * @date    2019/11/6 0006
 *  * @Description
 *  *
 *  
 */
public class DefaultTray {
	private static final Logger logger = LoggerFactory.getLogger(DefaultTray.class);
	public static void main(String[] args) throws Exception {
		SystemTray tray = SystemTray.getSystemTray();
		ImageIcon trayImg = new ImageIcon(ResourceLoader.getBytes("image.png"));// 托盘图标

		PopupMenu popup = new PopupMenu();
		MenuItem exitMenu = new MenuItem("exit");
		exitMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		popup.add(exitMenu);
		TrayIcon trayIcon = new TrayIcon(trayImg.getImage(), "HB", popup);
		trayIcon.setImageAutoSize(true);
		trayIcon.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() == 2) {
					try {
						Desktop.getDesktop().browse(new URL("http://127.0.0.1:9003").toURI());
					} catch (IOException | URISyntaxException exception) {
						logger.error("",exception);
					}
				}

			}

		});
		tray.add(trayIcon);
	}
}
