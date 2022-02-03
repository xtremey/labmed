package main;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ToolStepSizeSelector extends JPanel{
    private static final long serialVersionUID = 1L;
    private int _step_size;
    private JSlider _step_size_slider;
    private JLabel _step_label;

    /**
     * Default Constructor. Creates the GUI element for window_width
     * and window_center selection
     *
     */
    public ToolStepSizeSelector() {

        final ImageStack slices = ImageStack.getInstance();
        JLabel seg_sel_title = new JLabel("Edit cube marching step size");


        // range_max needs to be calculated from the bits_stored value
        // in the current dicom series
        _step_size = LabMed.get_v3d().get_step_size();

        _step_label = new JLabel("Step size:");

        _step_size_slider = new JSlider(1, 4, _step_size);
        _step_size_slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (source.getValueIsAdjusting()) {
                    _step_size = (int) source.getValue();
                    System.out.println("step size slider stateChanged: " + _step_size);
                    LabMed.get_v3d().set_step_size(_step_size);
                    LabMed.get_v3d().update_mc_rendering();
                }
            }
        });


        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0.3;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2); // top,left,bottom,right
        c.weightx = 0.1;
        c.gridx = 0;
        c.gridy = 0;
        this.add(seg_sel_title, c);


        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        this.add(_step_label, c);
        c.gridx = 0;
        c.gridy = 2;
        this.add(_step_size_slider, c);
    }
}
