package main;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

/**
 * GUI for making min-max based range segmentations.
 * 
 * @author Karl-Ingo Friese
 *
 */
public class ToolRegionSelector extends JPanel  {
	private static final long serialVersionUID = 1L;
	private int _variance;
	private int[] _seed_pixel;
	private Segment _seg;
	private JList<String> _seg_list;
	private JSlider _variance_slider;
	private JButton _seg_start_button;
	private JLabel _range_sel_title, _variance_label;

	/**
	 * Default Constructor. Creates the GUI element and connects it to a
	 * segmentation.
	 *
	 * @param seg		the segmentation to be modified
	 */
	public ToolRegionSelector(Segment seg) {
		_seg = seg;

		final ImageStack slices = ImageStack.getInstance();		
		JLabel seg_sel_title = new JLabel ("Edit Segmentation");
		
		_seg_list = new JList<String>(slices.getSegNamesByType(SegmentType.REGION));
		_seg_list.setSelectedIndex(slices.getSegNamesByType(SegmentType.REGION).indexOf(seg.getName()));
		_seg_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		_seg_list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int seg_index = _seg_list.getSelectedIndex();
				String name = (String)(slices.getSegNamesByType(SegmentType.REGION).getElementAt(seg_index));
				if (!_seg.getName().equals(name)) {
					_seg = slices.getSegment(name);
					_range_sel_title.setText("Range Selector - "+_seg.getName());
					// ...
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(_seg_list);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		_range_sel_title = new JLabel("Range Selector - "+_seg.getName());

		// range_max needs to be calculated from the bits_stored value
		// in the current dicom series
		int range_max = 100;
		_variance = 10;
		
		_variance_label = new JLabel("Variance:");
		
		_variance_slider = new JSlider(0, range_max, _variance);

		_seg_start_button = new JButton("Create Segmentation");

		_seg_start_button.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JButton source = (JButton) e.getSource();
				if (source.getModel().isPressed()) {
					_seed_pixel = LabMed.get_v2d().get_seed_pixel();
					System.out.println("_variance_slider stateChanged: "+_variance);
					_seg.create_region_segment(_seed_pixel, _variance, slices);
					LabMed.get_v2d().update_view();
				}
			}
		});


		_variance_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (source.getValueIsAdjusting()) {
//					_seed_pixel = LabMed.get_v2d().get_seed_pixel();
					_variance = (int)source.getValue();
					System.out.println("_variance_slider stateChanged: "+_variance);
//					_seg.create_region_segment(_seed_pixel, _variance, slices);
//					LabMed.get_v2d().update_view();
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

		c.gridheight = 2;
		c.gridx = 0; c.gridy = 1; this.add(scrollPane, c);
		c.gridheight = 1;

		c.weightx = 0.9;
		c.gridwidth=2;
		c.gridx = 1; c.gridy = 0; this.add(_range_sel_title, c);
		c.gridwidth=1;

		c.weightx = 0;
		c.gridx = 1; c.gridy = 1; this.add(_seg_start_button, c);
		c.gridx = 1; c.gridy = 2; this.add(_variance_label, c);
		c.gridx = 1; c.gridy = 3; this.add(_variance_slider, c);

		// setBackground(Color.blue);
	}	
}
