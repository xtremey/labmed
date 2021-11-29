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

import misc.DiFile;
import misc.MyObservable;
import misc.MyObserver;
import misc.ViewMode;

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

	// store the current viewmode
	private ViewMode _view_mode = ViewMode.TRANSVERSAL;

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
		if (_view_mode == ViewMode.TRANSVERSAL){
			_w = _slices.getImageWidth();
			_h = _slices.getImageHeight();
		} else if (_view_mode == ViewMode.SAGITTAL){
			_w = _slices.getImageHeight();
			_h = _slices.getNumberOfImages();
		} else if (_view_mode == ViewMode.FRONTAL){
			_w = _slices.getImageWidth();
			_h = _slices.getNumberOfImages();
		}
		
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
		reallocate();
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



			if(_view_mode == ViewMode.TRANSVERSAL){
				//get active file
				int z = _slices.getActiveImageID();

				//iterate over pixel values
				for (int y = 0; y < _w; y++){
					for (int x = 0; x < _h; x++){
						int greyscale = _slices.get_greyscale(x, y, z);
						int argb = (0xff<<24) + (greyscale<<16) + (greyscale<<8) + greyscale;
						_bg_img.setRGB(x, y, argb);
					}
				}
			}
			else if(_view_mode == ViewMode.SAGITTAL){
				//get active file
				int x = _slices.getActiveImageID();

				//iterate over pixel values
				for (int y = 0; y < _w; y++){
					for (int z = 0; z < _h; z++){
						int greyscale = _slices.get_greyscale(x, y, z);
						int argb = (0xff<<24) + (greyscale<<16) + (greyscale<<8) + greyscale;
						_bg_img.setRGB(y, z, argb);
					}
				}
			}
			else if(_view_mode == ViewMode.FRONTAL){
				//get active file
				int y = _slices.getActiveImageID();

				//iterate over pixel values
				for (int x = 0; x < _w; x++){
					for (int z = 0; z < _h; z++){
						int greyscale = _slices.get_greyscale(x, y, z);
						int argb = (0xff<<24) + (greyscale<<16) + (greyscale<<8) + greyscale;
						_bg_img.setRGB(x, z, argb);
					}
				}
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
		switch (mode){
			case 1: {
				_view_mode = ViewMode.SAGITTAL;
				update(_slices, new Message(Message.M_CLEAR));
				for(int i = 0; i < _slices.getImageWidth(); i++) {
					update(_slices, new Message(Message.M_NEW_IMAGE_LOADED));
				}
				break;
			}
			case 2: {
				_view_mode = ViewMode.FRONTAL;
				update(_slices, new Message(Message.M_CLEAR));
				for(int i = 0; i < _slices.getImageHeight(); i++) {
					update(_slices, new Message(Message.M_NEW_IMAGE_LOADED));
				}
				break;
			}
			default: {
				_view_mode = ViewMode.TRANSVERSAL;
				update(_slices, new Message(Message.M_CLEAR));
				for(int i = 0; i < _slices.getNumberOfImages(); i++) {
					update(_slices, new Message(Message.M_NEW_IMAGE_LOADED));
				}
				break;
			}
		}
		update_view();
	}
}
