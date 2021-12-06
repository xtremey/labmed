package main;

import java.util.*;
import java.io.*;
import javax.swing.*;

import misc.DiFile;
import misc.DiFileInputStream;
import misc.MyObservable;

/**
 * The ImageStack class represents all DicomFiles of a series and its segments.
 * It is the global data structure in YaDiV.
 * This class is implemented as singleton, meaning the constructor is private.
 * Use getInstance() instead.
 * 
 * @author  Karl-Ingo Friese
 */
public class ImageStack extends MyObservable {
	private static ImageStack _instance = null;
	private DiFile[] _dicom_files;
	private DefaultListModel<String> _seg_names = new DefaultListModel<>();
	private HashMap<String, Segment> _segment_map = new HashMap<>();
	private String _dir_name = "";
	private int _w, _h, _active = 0;
	private int _max_val;
	private int _window_width, _window_center;

	/**
	 * Default Constructor.
	 */
	private ImageStack() {
	}

	public static ImageStack getInstance() {
    	if (_instance==null) {
    		_instance = new ImageStack();
    	}
    	return _instance;
	}


	/**
	 * * Normalizes intensity to 0-255 greyscale
	 * @param intensity intensity
	 * @return The scaled and normalized Intensity
	 */
	public int intensity_to_greyscale(int intensity, int z){
		DiFile df = getDiFile(z);

		// get byte storage format
		int bits_stored = df.getBitsStored();

		// init center and width if not given
//		if (!df.is_window_center_given()) _window_center = (int) Math.pow(2, (float)(bits_stored - 1));
//		if (!df.is_window_width_given()) _window_width = (int) Math.pow(2, bits_stored);

		//apply scaling
		int scaled = intensity * df.get_slope() + df.get_intercept();

		//normalize to 0-255
		int normalized;
		int scaled_center = _window_center * df.get_slope() + df.get_intercept();
		double lower_bound = scaled_center - _window_width / 2.0;
		double upper_bound = scaled_center + _window_width / 2.0;
		if (scaled <= lower_bound){
			normalized = 0;
		} else if (scaled > upper_bound){
			normalized = 255;
		} else {
			normalized = (int) Math.round((scaled - lower_bound)  * 255 / (upper_bound - lower_bound));
		}

		return normalized;
	}

	/**
	 * Reads all DICOM files from the given directory. All files are checked
	 * for correctness before loading. The load process is implemented as a thread.
	 * 
	 * @param dir_name	string contaning the directory name.
	 */
	public void initFromDirectory(String dir_name) {
		_dir_name = dir_name;
		_w = 0;
		_h = 0;

		// loading thread
		Thread t = new Thread() {
			JProgressBar progress_bar;
			
			// returns the image number of a dicom file or -1 if something wents wrong
			private int check_file(File file) {
				int result = -1;
				
				if (!file.isDirectory()) {
		        	try {
		        		DiFileInputStream candidate = new DiFileInputStream(file);
		        		
			    		if (candidate.skipHeader()) {
			    			result = candidate.quickscan_for_image_number();
			    		}				    	
						candidate.close();
		    		} catch (Exception ex) {
						System.out.println("this will work after exercise 1");
		    			result = -1;
		    		}
				}
				
	            return result;
			}
			
			// checks the DICOM files, retrieves their image number and loads them in the right order.
			public void run() {
				Hashtable<Integer, String> map_number_to_difile_name = new Hashtable<Integer, String>();
				DiFile df;

			    notifyObservers(new Message(Message.M_CLEAR));

				JFrame progress_win = new JFrame("checking ...");
				progress_win.setResizable(false);
				progress_win.setAlwaysOnTop(true);
				
				File dir = new File(_dir_name);
			    File[] files_unchecked = dir.listFiles();

				progress_bar = new JProgressBar(0, files_unchecked.length);
				progress_bar.setValue(0);
				progress_bar.setStringPainted(true);
				progress_win.add(progress_bar);
				progress_win.pack();
				// progress_bar.setIndeterminate(true);
				int main_width = (int)(LabMed.get_window().getSize().getWidth());
				int main_height = (int)(LabMed.get_window().getSize().getHeight());
				progress_win.setLocation((main_width-progress_win.getSize().width)/2, (main_height-progress_win.getSize().height)/2);
				progress_win.setVisible(true);		
								
			    for (int i=0; i<files_unchecked.length; i++) {
			    	int num = check_file(files_unchecked[i]);
			    	if (num >= 0) {
			    		map_number_to_difile_name.put(Integer.valueOf(num), files_unchecked[i].getAbsolutePath());			    					        		
		        	}
			    	progress_bar.setValue(i+1);
			    }
				
			    progress_win.setTitle("loading ...");
			    
				Enumeration<Integer> e = map_number_to_difile_name.keys();
		   	  	List<Integer> l = new ArrayList<Integer>();
		   	  	while(e.hasMoreElements()) {
		   	  		l.add((Integer)e.nextElement());
				}
				
			    String[] file_names = new String[l.size()];
		        Collections.sort(l);
		        Iterator<Integer> it = l.iterator();
		        int file_counter = 0;
		        while (it.hasNext()) {
		        	file_names[file_counter++] =  map_number_to_difile_name.get(it.next());
		        }
		        
				progress_bar.setMaximum(file_names.length);
				progress_bar.setValue(0);

				_dicom_files = new DiFile[file_names.length];
				
				for (int i=0; i<file_names.length; i++) {
			    	df = new DiFile();
			    	try {
			    		df.initFromFile(file_names[i]);
			    	} catch (Exception ex) {
			    		System.out.println(getClass()+"::initFromDirectory -> failed to open "+file_names[i]);
			    		System.out.println(ex);
			    		System.exit(0);
			    	}
			    	progress_bar.setValue(i+1);
			    	_dicom_files[i] = df;

					// initialize default image width and heigth from the first image read
					if (_w==0) _w = df.getImageWidth();
					if (_h==0) _h = df.getImageHeight();
					if (_max_val == 0) _max_val = df.get_max_val();

					if (df.is_window_center_given()) {
						_window_center = df.get_window_center();
					} else {
						_window_center = (int) Math.pow(2, (float)(df.getBitsStored() - 1));
					}
					if (df.is_window_width_given()) {
						_window_width = df.get_window_width();
					} else {
						_window_width = (int) Math.pow(2, df.getBitsStored());
					}


					notifyObservers(new Message(Message.M_NEW_IMAGE_LOADED));
				}
			    
			    progress_win.setVisible(false);
			}
		};
		
		t.start();	    
	}

	/**
	 * Adds a new segment with the given name.
	 * 
	 * @param name	the name of the new segment (must be unique)
	 * @return		the new segment or null if the name was not unique
	 */
	public Segment createSegment(String name) {
		Segment seg;

		if (_segment_map.containsKey(name)) {
			seg = null;
		} else {
			int[] def_colors = {0xff0000, 0x00ff00, 0x0000ff};
			seg = new Segment(name, _w, _h, _dicom_files.length);
			seg.setColor(def_colors[_segment_map.size()]);
			_segment_map.put(name, seg);
			_seg_names.addElement(name);
			notifyObservers(new Message(Message.M_NEW_SEGMENTATION, seg));
		}
		
		return seg;
	}

	public int get_intensity(int x, int y, int z){ // x is width, y is height, z is image number
		return getDiFile(z).get_intensity(x, y);
	}

	public int get_max_val(){
		return _max_val;
	}


	public int get_greyscale(int x, int y, int z){ // x is width, y is height, z is image number
		int intensity = get_intensity(x, y, z);
		return intensity_to_greyscale(intensity, z);
	}
	
	/**
	 * Returns the DicomFile from the series with image number i; 
	 * 
	 * @param i	image number
	 * @return the DIOCM file
	 */
	public DiFile getDiFile(int i) {
		return (DiFile)(_dicom_files[i]);
	}
	
	/**
	 * Returns the segment with the given name.
	 * 
	 * @param name	the name of a segment
	 * @return		the segment
	 */
	public Segment getSegment(String name) {
		return (Segment)(_segment_map.get(name));
	}

	/**
	 * Returns the number of segments.
	 * 
	 * @return		the number of segments
	 */
	public int getSegmentNumber() {
		return _segment_map.size();
	}

	/**
	 * Returns the Number of DicomFiles in the ImageStack.
	 *   
	 * @return the number of files
	 */
	public int getNumberOfImages() {
		return (_dicom_files==null? 0 : _dicom_files.length);
	}
	
	/**
	 * Returns the DefaultListModel containing the segment names.
	 *   
	 * @return guess what
	 */
	public DefaultListModel<String> getSegNames() {
		return _seg_names;
	}

	/**
	 * Returns the width of the images in the image stack.
	 *   
	 * @return the image width
	 */
	public int getImageWidth() {
		return _w;
	}
	
	/**
	 * Returns the height of the images in the image stack.
	 *   
	 * @return the image height
	 */
	public int getImageHeight() {
		return _h;
	}
	
	/**
	 * Returns the currently active image.
	 * 
	 * @return the currently active image
	 */
	public int getActiveImageID() {
		return _active;
	}

	/**
	 * Sets the currently active image in the viewmode.
	 * Notifys Observers with M_NEW_ACTIVE_IMAGE Message. Object is the new active image value;
	 * 
	 * @param i	the active image
	 */
	public void setActiveImage(int i) {
		_active = i;

	    notifyObservers(new Message(Message.M_NEW_ACTIVE_IMAGE, Integer.valueOf(i)));
	}

	public int get_window_center() {
		return _window_center;
	}

	public void set_window_center(int _window_center) {
		this._window_center = _window_center;
	}

	public int get_window_width() {
		return _window_width;
	}

	public void set_window_width(int _window_width) {
		this._window_width = _window_width;
	}
}

