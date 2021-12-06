package main;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * GUI for making min-max based range segmentations.
 * 
 * @author Karl-Ingo Friese
 *
 */
public class ToolWindowSelector extends JPanel  {
	private static final long serialVersionUID = 1L;
	private int _window_center, _window_width;
	private JSlider _window_center_slider, _window_width_slider;
	private JLabel _min_label, _max_label;

	/**
	 * Default Constructor. Creates the GUI element for window_width
	 * and window_center selection
	 *
	 */
	public ToolWindowSelector() {

		final ImageStack slices = ImageStack.getInstance();		
		JLabel seg_sel_title = new JLabel ("Edit window center and width");
		


		// range_max needs to be calculated from the bits_stored value
		// in the current dicom series
		int range_max = (int) Math.pow(2,LabMed.get_is().getDiFile(0).getBitsStored());
		_window_center = LabMed.get_is().get_window_center();
		_window_width = LabMed.get_is().get_window_width();
		
		_min_label = new JLabel("Window Center:");
		_max_label = new JLabel("Window Width:");
		
		_window_center_slider = new JSlider(0, range_max, _window_center);
		_window_center_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (source.getValueIsAdjusting()) {
					_window_center = (int)source.getValue();
					System.out.println("window center stateChanged: "+ _window_center);
					LabMed.get_is().set_window_center(_window_center);
					LabMed.get_v2d().update_view();
				}
			}
		});		
		
		_window_width_slider = new JSlider(0, range_max, _window_width);
		_window_width_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (source.getValueIsAdjusting()) {
					_window_width = (int)source.getValue();
					System.out.println("window width stateChanged: "+ _window_width);
					LabMed.get_is().set_window_width(_window_width);
					LabMed.get_v2d().update_view();

				}
			}
		});
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 0.3;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(2,2,2,2); // top,left,bottom,right
		c.weightx = 0.1;
		c.gridx = 0; c.gridy = 0; this.add(seg_sel_title, c);

//		c.gridheight = 2;
//		c.gridx = 0; c.gridy = 1;
//		c.gridheight = 1;
//
//		c.weightx = 0.9;
//		c.gridwidth=2;
//		c.gridx = 1; c.gridy = 0; this.add(_range_sel_title, c);
//		c.gridwidth=1;

		c.weightx = 0;
		c.gridx = 0; c.gridy = 1; this.add(_min_label, c);
		c.gridx = 0; c.gridy = 3; this.add(_max_label, c);
		c.gridx = 0; c.gridy = 2; this.add(_window_center_slider, c);
		c.gridx = 0; c.gridy = 4; this.add(_window_width_slider, c);
		
		// setBackground(Color.blue);
	}	
}
