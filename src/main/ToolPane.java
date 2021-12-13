package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * Container class for all tools in the lower section of the main window.
 *
 * @author Karl-Ingo Friese
 */
public class ToolPane extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTabbedPane _tab_pane;
	private JPanel _range_selector = null;
	private JPanel _region_selector = null;
	private JPanel _window_selector = null;

	/**
	 * Default Constructor. Creates an empty ToolPane with no active panel.
	 *
	 */
	public ToolPane() {
		this.setPreferredSize(new Dimension(800,200));
		setBorder( new LineBorder(Color.black, 1));
		setLayout(new BorderLayout(0,0));
		_tab_pane = new JTabbedPane(JTabbedPane.TOP);
		this.add(_tab_pane);
	}

	/**
	 * Shows a new tool in the ToolPane.
	 * 
	 * @param panel the new panel to show
	 */
	public void showTool(JPanel panel) {
		if (panel instanceof ToolRangeSelector){
			if (_range_selector == null){
				_tab_pane.addTab("Range", panel);
				_range_selector = panel;
			} else {
				this.remove(_range_selector);
				_tab_pane.remove(_range_selector);
				_tab_pane.addTab("Range", panel);
				_range_selector = panel;
			}
		} else if (panel instanceof ToolRegionSelector){
			if (_region_selector == null){
				_tab_pane.addTab("Region", panel);
				_region_selector = panel;
			} else {
				this.remove(_region_selector);
				_tab_pane.remove(_region_selector);
				_tab_pane.addTab("Region", panel);
				_region_selector = panel;
			}

		} else if (panel instanceof ToolWindowSelector) {
			if (_window_selector == null) {
				_tab_pane.addTab("Window", panel);
				_window_selector = panel;
			}
		}

		this.validate();
		this.repaint();
	}

}
