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
public class ToolPointDistanceSelector extends JPanel  {
	private static final long serialVersionUID = 1L;
	private int _point_distance;
	private JSlider _point_distance_slider;
	private JLabel _min_label;

	/**
	 * Default Constructor. Creates the GUI element for window_width
	 * and window_center selection
	 *
	 */
	public ToolPointDistanceSelector() {

		final ImageStack slices = ImageStack.getInstance();		
		JLabel seg_sel_title = new JLabel ("Edit window center and width");
		


		// range_max needs to be calculated from the bits_stored value
		// in the current dicom series
		_point_distance = LabMed.get_v3d().get_point_distance();

		_min_label = new JLabel("Point Distance:");

		_point_distance_slider = new JSlider(1, 20, _point_distance);
		_point_distance_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (source.getValueIsAdjusting()) {
					_point_distance = (int)source.getValue();
					System.out.println("point distance slider stateChanged: "+ _point_distance);
					LabMed.get_v3d().set_point_distance(_point_distance);
					LabMed.get_v3d().update_view();
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


		c.weightx = 0;
		c.gridx = 0; c.gridy = 1; this.add(_min_label, c);
		c.gridx = 0; c.gridy = 2; this.add(_point_distance_slider, c);
		
		// setBackground(Color.blue);
	}	
}
