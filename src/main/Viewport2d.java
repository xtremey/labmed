package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import misc.DiDataElement;
import misc.DiFile;
import misc.MyObservable;
import misc.MyObserver;

/**
 * Two dimensional viewport for viewing the DICOM images + segmentations.
 * 
 * @author  Karl-Ingo Friese
 */
public class Viewport2d extends Viewport implements MyObserver {
	private static final long serialVersionUID = 1L;
	// the background image needs a pixel array, an image object and a MemoryImageSource
	private BufferedImage _bg_img;

	// each segmentation image needs the same, those are stored in a hashtable
	// and referenced by the segmentation name
	private Hashtable<String, BufferedImage> _map_seg_name_to_img;
	
	// this is the gui element where we actualy draw the images	
	private Panel2d _panel2d;
	
	// the gui element that lets us choose which image we want to show and
	// its data source (DefaultListModel)
	private ImageSelector _img_sel;
	private DefaultListModel<String> _slice_names;
	
	// width and heigth of our images. dont mix those with
	// Viewport2D width / height or Panel2d width / height!
	private int _w, _h;

	/**
	 * Private class, implementing the GUI element for displaying the 2d data.
	 * Implements the MouseListener Interface.
	 */
	public class Panel2d extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;
		public Panel2d() {
			super();
			setMinimumSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setMaximumSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setPreferredSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setBackground(Color.black);
			this.addMouseListener( this );
		}

		public void mouseClicked ( java.awt.event.MouseEvent e ) { 
			System.out.println("Panel2d::mouseClicked: x="+e.getX()+" y="+e.getY());
		}
		public void mousePressed ( java.awt.event.MouseEvent e ) {}
		public void mouseReleased( java.awt.event.MouseEvent e ) {}
		public void mouseEntered ( java.awt.event.MouseEvent e ) {}
		public void mouseExited  ( java.awt.event.MouseEvent e ) {}
	
		/**
		 * paint should never be called directly but via the repaint() method.
		 */
		public void paint(Graphics g) {
			g.drawImage(_bg_img, 0, 0, this.getWidth(), this.getHeight(), this);
			
			Enumeration<BufferedImage> segs = _map_seg_name_to_img.elements();	
			while (segs.hasMoreElements()) {
				g.drawImage(segs.nextElement(), 0, 0,  this.getWidth(), this.getHeight(), this);
			}
		}
	}
	
	/**
	 * Private class: The GUI element for selecting single DicomFiles in the View2D.
	 * Stores two references: the ImageStack (containing the DicomFiles)
	 * and the View2D which is used to show them.
	 * 
	 * @author kif
	 */
	private class ImageSelector extends JPanel {
		private static final long serialVersionUID = 1L;
		private JList<String> _jl_slices;
		private JScrollPane _jsp_scroll;
		
		/**
		 * Constructor with View2D and ImageStack reference.  
		 * The ImageSelector needs to know where to find the images and where to display them
		 */
		public ImageSelector() {
			_jl_slices = new JList<String>(_slice_names);

			_jl_slices.setSelectedIndex(0);
			_jl_slices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			_jl_slices.addListSelectionListener(new ListSelectionListener(){
				/**
				 * valueChanged is called when the list selection changes.   
				 */
			    public void valueChanged(ListSelectionEvent e) {
			      	int slice_index = _jl_slices.getSelectedIndex();
			      	 
			       	if (slice_index>=0){
			       		_slices.setActiveImage(slice_index);
			       	}
				 }
			});
			
			_jsp_scroll = new JScrollPane(_jl_slices);			
			_jsp_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			_jsp_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			
			setLayout(new BorderLayout());
			add(_jsp_scroll, BorderLayout.CENTER);
		}
	}
		
	/**
	 * Constructor, with a reference to the global image stack as argument
	 */
	public Viewport2d() {
		super();
		
		_slice_names = new DefaultListModel<String>();
		_slice_names.addElement(" ----- ");

		// create an empty 10x10 image as default
		_bg_img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		final int[] bg_pixels = ((DataBufferInt) _bg_img.getRaster().getDataBuffer()).getData();
		for (int i=0; i<bg_pixels.length; i++) {
			bg_pixels[i] = 0xff000000;
		}

		_map_seg_name_to_img = new Hashtable<String, BufferedImage>();

		// The image selector needs to know which images are to select
		_img_sel = new ImageSelector();

		setLayout( new BorderLayout() );
		_panel2d = new Panel2d();		
        add(_panel2d, BorderLayout.CENTER );        
        add(_img_sel, BorderLayout.EAST );
		setPreferredSize(new Dimension(DEF_WIDTH+50,DEF_HEIGHT));
	}


	/**
	 * This is private method is called when the current image width + height don't
	 * fit anymore (can happen after loading new DICOM series or switching viewmode).
	 * (see e.g. exercise 2)
	 */
	private void reallocate() {
		_w = _slices.getImageWidth();
		_h = _slices.getImageHeight();
		
		// create background image
		_bg_img = new BufferedImage(_w, _h, BufferedImage.TYPE_INT_ARGB);

		// create image for segment layers
		for (String seg_name : _map_name_to_seg.keySet()) {
			BufferedImage seg_img = new BufferedImage(_w, _h, BufferedImage.TYPE_INT_ARGB);

			_map_seg_name_to_img.put(seg_name, seg_img);
		}
	}
	
	/*
	 * Calculates the background image and segmentation layer images and forces a repaint.
	 * This function will be needed for several exercises after the first one.
	 * @see Viewport#update_view()
	 */
	public void update_view() {
		if (_slices.getNumberOfImages() == 0) {
			return;
		}
		
		// these are two variables you might need in exercise #2
		// int active_img_id = _slices.getActiveImageID();
		// DiFile active_file = _slices.getDiFile(active_img_id);

		// _w and _h need to be initialized BEFORE filling the image array !
		if (_bg_img==null || _bg_img.getWidth(null)!=_w || _bg_img.getHeight(null)!=_h) {
			reallocate();
		}

		// rendering the background picture
		if (_show_bg) {
			// this is the place for the code displaying a single DICOM image in the 2d viewport (exercise 2)
			//
			// the easiest way to set a pixel of an image is the setRGB method
			// example: _bg_img.setRGB(x,y, 0xff00ff00)
			//                                AARRGGBB
			// the resulting image will be used in the Panel2d::paint() method

			//get active file
			int active_img_id = _slices.getActiveImageID();
			DiFile active_file = _slices.getDiFile(active_img_id);

			//check data format: has to be monochrome2
			DiDataElement dde_format = active_file.getDataElements().get(0x00280004);
			String format = dde_format.getValueAsString().replaceAll("\\s+",""); // remove whitespace
			if (!format.equals("MONOCHROME2")){
				System.out.println("The only supported data format is MONOCHROME2, but got: |" + format + "|");
				return;
			}

			// get byte storage format
			int bits_allocated = active_file.getBitsAllocated();
			int bits_stored = active_file.getBitsStored();
			int high_bit = bits_stored - 1;

			//sanity check for high bit if it is given
			DiDataElement dde_high_bit = active_file.getDataElements().get(0x00280102);
			if (dde_high_bit != null ) {
				int actual_high_bit = dde_high_bit.getValueAsInt();
				if (actual_high_bit != high_bit) {
					System.out.println("Expected high bit to be " + high_bit + ", but got: " + actual_high_bit);
					return;
				}
			}

			//get pixel data
			DiDataElement dde_pixel_data = active_file.getDataElements().get(0x7FE00010);
			byte[] pixel_bytes = dde_pixel_data.getValues();

			//sanity check for pixel array length
			int expected_byte_length = _w * _h * bits_allocated / 8;
			if (pixel_bytes.length != expected_byte_length){
				System.out.println("Expected pixel array to be of length "
						+ expected_byte_length + ", but got: " + pixel_bytes.length);
				return;
			}

			//get intercept, slope, window-center, window-width
			int window_center = (int) Math.pow(2, (float)(bits_stored - 1)); //default center is half of stored bits
			int window_width = (int) Math.pow(2, bits_stored); //default window is whole range
			int intercept = 0;
			int slope = 1;

			DiDataElement dde_window_center = active_file.getDataElements().get(0x00281050);
			if (dde_window_center != null) window_center = dde_window_center.getValueAsInt();

			DiDataElement dde_window_width = active_file.getDataElements().get(0x00281051);
			if (dde_window_width != null) window_width = dde_window_width.getValueAsInt();

			DiDataElement dde_intercept = active_file.getDataElements().get(0x00281052);
			if (dde_intercept != null) intercept = dde_intercept.getValueAsInt();

			DiDataElement dde_slope = active_file.getDataElements().get(0x00281053);
			if (dde_slope != null) slope = dde_slope.getValueAsInt();


			//pixel data and important constants
			int byte_per_pixel = bits_allocated / 8;
			int num_pixels = pixel_bytes.length / byte_per_pixel;
			final int[] bg_pixels = ((DataBufferInt) _bg_img.getRaster().getDataBuffer()).getData();

			//integer format
			int last_important_byte = (int) Math.ceil(bits_stored / 8.0); //last byte with information, ignore all above
			int last_important_bit = last_important_byte * 8 - bits_stored; //last bit with information
			int highest_last_byte_val = (int) Math.pow(2, last_important_bit) - 1;

			//iterate over pixel values
			for (int i = 0; i < num_pixels; i++){
				int val = 0;
				//iterate over bytes per pixel
				for (int j = 0; j < byte_per_pixel; j++) {
					//check if byte can be skipped, for given dataset not relevant since all bytes relevant
					if (j + 1 > last_important_byte) continue;

					byte raw = pixel_bytes[i * byte_per_pixel + j];
					int unsigned = (int)(raw & 0xff);

					//ignore bits with no information, for given dataset not relevant: all zeros anyway
					if (unsigned > highest_last_byte_val) unsigned = highest_last_byte_val;

					int shifted = unsigned << (j * 8); //accumulate values of all bytes per pixel
					val += shifted;
				}

				//apply scaling
				float scaled = slope * val + intercept;

				//normalize to 0-255
				int normalized;
				double scaled_center = window_center * slope + intercept;
				double lower_bound = scaled_center - window_width / 2.0;
				double upper_bound = scaled_center + window_width / 2.0;
				if (scaled <= lower_bound){
					normalized = 0;
				} else if (scaled > upper_bound){
					normalized = 255;
				} else {
					normalized = (int) Math.round((scaled - lower_bound)  * 255 / (upper_bound - lower_bound));
				}

				int argb = (0xff<<24) + (normalized<<16) + (normalized<<8) + normalized;

				bg_pixels[i] = argb;
			}


		} else {
			// faster: access the data array directly (see below)
			final int[] bg_pixels = ((DataBufferInt) _bg_img.getRaster().getDataBuffer()).getData();
			for (int i = 0; i<bg_pixels.length; i++) {
				bg_pixels[i] = 0xff000000;
			}
		}

		/*
		// rendering the segmentations. each segmentation is rendered in a different image.
		for (String seg_name : _map_name_to_seg.keySet()) {
			// here should be the code for displaying the segmentation data
			// (exercise 3)

			BufferedImage seg_img = _map_seg_name_to_img.get(seg_name);
			int[] seg_pixels = ((DataBufferInt)seg_img.getRaster().getDataBuffer()).getData();

			// to drawn a segmentation image, fill the pixel array seg_pixels
			// with ARGB values similar to exercise 2
		}
		*/

		repaint();
	}
	

	/**
	 * Implements the observer function update. Updates can be triggered by the global
	 * image stack.
	 */
	@Override
	public void update(final MyObservable mo, final Message msg) {
		if (!EventQueue.isDispatchThread()) {
			// all swing thingies must be done in the AWT-EventQueue 
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					update(mo,msg);
				}
			});
			return;
		}

		if (msg._type == Message.M_CLEAR) {
			// clear all slice info
			_slice_names.clear();
		}
		
		if (msg._type == Message.M_NEW_IMAGE_LOADED) {
			// a new image was loaded and needs an entry in the ImageSelector's
			// DefaultListModel _slice_names
			String name = new String();
			int num = _slice_names.getSize();				
	    	name = ""+num;
			if (num<10) name = " "+name;				
			if (num<100) name = " "+name;		
			_slice_names.addElement(name);
			
			if (num==0) {
				// if the new image was the first image in the stack, make it active
				// (display it).
				reallocate();
				_slices.setActiveImage(0);
			}			
		}
		
		if (msg._type == Message.M_NEW_ACTIVE_IMAGE) {
			update_view();			
		}
		
		if (msg._type == Message.M_SEG_CHANGED) {
			String seg_name = ((Segment)msg._obj).getName();
			boolean update_needed = _map_name_to_seg.containsKey(seg_name);
			if (update_needed) {
				update_view();
			}
		}
	  }

    /**
	 * Returns the current file.
	 * 
	 * @return the currently displayed dicom file
	 */
	public DiFile currentFile() {
		return _slices.getDiFile(_slices.getActiveImageID());
	}

	/**
	 * Toggles if a segmentation is shown or not.
	 */
	public boolean toggleSeg(Segment seg) {
		String name = seg.getName();
		boolean gotcha = _map_name_to_seg.containsKey(name);
		
		if (!gotcha) {
			// if a segmentation is shown, we need to allocate memory for pixels
			BufferedImage seg_img = new BufferedImage(_w, _h, BufferedImage.TYPE_INT_ARGB);
			_map_seg_name_to_img.put(name, seg_img);
		} else {
			_map_seg_name_to_img.remove(name);
		}
		
		// most of the buerocracy is done by the parent viewport class
		super.toggleSeg(seg);
		
		return gotcha;
	}
	
	/**
	 * Sets the view mode (transversal, sagittal, frontal).
	 * This method will be implemented in exercise 2.
	 * 
	 * @param mode the new viewmode
	 */
	public void setViewMode(int mode) {
		// you should do something with the new viewmode here
		System.out.println("Viewmode "+mode);
	}
}
