package misc;

import java.awt.image.DataBufferInt;
import java.util.*;
import misc.DiFileInputStream;

/**
 * Implements the internal representation of a DICOM file.
 * Stores all DataElements and makes them accessable via getDataElement(TagName).
 * Also stores the pixel data & important information for displaying the contained image in 
 * seperate variables with special access functions.
 * 
 * @author Karl-Ingo Friese
 */
public class DiFile {
	private int _w;
	private int _h;
	private int _bits_stored;
	private int _bits_allocated;
	private Hashtable<Integer, DiDataElement> _data_elements;
	private int _image_number;
	String _file_name;
	private int _window_center;
	private boolean _window_center_given = false;
	private int _window_width;
	private boolean _window_width_given = false;
	private int _slope = 1;
	private int _intercept = 0;
	private int _high_bit;
	private String _format;
	private int[][] _intensity_arr; // width * height

	/**
	 * Default Construtor - creates an empty DicomFile.
	 */
	public DiFile () {
		_w = _h = _bits_stored = _bits_allocated = _image_number = 0;
		_data_elements = new Hashtable<Integer, DiDataElement>();
		_file_name = null;
	}

	/**
	 * Initializes the DicomFile from a file. Might throw an exception (unexpected
	 * end of file, wrong data etc).
	 * This method will be implemented in exercise 1.
	 * 
	 * @param file_name	a string containing the name of a valid dicom file
	 * @throws Exception
	 */
	public void initFromFile(String file_name) throws Exception {
		// exercise 1
		_file_name = file_name;
		DiFileInputStream is = new DiFileInputStream(file_name);
		is.skipHeader();

		while (true) {
			DiDataElement dde = new DiDataElement();
			dde.readNext(is);
			_data_elements.put(dde.getTag(), dde);
			int tag = dde.getTag();
			if (tag == 0x00280011) { //columns / width
				_w = dde.getValueAsInt();
			} else if (tag == 0x00280010) { //rows / height
				_h = dde.getValueAsInt();
			} else if (tag == 0x00280100) { //bits allocated
				_bits_allocated = dde.getValueAsInt();
			} else if (tag == 0x00280101) { // bits stored
				_bits_stored = dde.getValueAsInt();
			} else if (tag == 0x00280004) { //format
				_format = dde.getValueAsString().replaceAll("\\s+","");
				if (!_format.equals("MONOCHROME2")){
					throw new Exception("The only supported data format is MONOCHROME2, but got: |" + _format + "|");
				}
			} else if (tag == 0x00280102) { //high bit
				_high_bit = dde.getValueAsInt();
				if (_high_bit != _bits_stored - 1) {
					throw new Exception("Expected high bit to be " + (_bits_stored - 1) + ", but got: " + _high_bit);
				}
			} else if (tag == 0x00281050) { //window center
				_window_center = dde.getValueAsInt();
				_window_center_given = true;
			} else if (tag == 0x00281051) { //window width
				_window_width = dde.getValueAsInt();
				_window_width_given = true;
			} else if (tag == 0x00281052) { //intercept
				_intercept = dde.getValueAsInt();
			} else if (tag == 0x00281053) { //slope
				_slope = dde.getValueAsInt();
			} else if (tag == 0x7FE00010) { //pixel data, last element
				//byte data
				byte[] pixel_bytes = dde.getValues();

				//sanity check for pixel array length
				int expected_byte_length = _w * _h * _bits_allocated / 8;
				if (pixel_bytes.length != expected_byte_length){
					throw new Exception("Expected pixel array to be of length "
							+ expected_byte_length + ", but got: " + pixel_bytes.length);
				}

				//init array
				_intensity_arr = new int[_w][_h];

				//integer format
				int last_important_byte = (int) Math.ceil(_bits_stored / 8.0); //last byte with information, ignore all above
				int last_important_bit = last_important_byte * 8 - _bits_stored; //last bit with information
				int highest_last_byte_val = (int) Math.pow(2, last_important_bit) - 1;

				//pixel data constants
				int byte_per_pixel = _bits_allocated / 8;
				int num_pixels = _w * _h;

				//iterate over pixel values
				for (int i = 0; i < num_pixels; i++) {
					int val = 0;
					//iterate over bytes per pixel
					for (int j = 0; j < byte_per_pixel; j++) {
						//check if byte can be skipped, for given dataset not relevant since all bytes relevant
						if (j + 1 > last_important_byte) continue;

						byte raw = pixel_bytes[i * byte_per_pixel + j];
						int unsigned = (int) (raw & 0xff);

						//ignore bits with no information, for given dataset not relevant: all zeros anyway
						if (unsigned > highest_last_byte_val) unsigned = highest_last_byte_val;

						int shifted = unsigned << (j * 8); //accumulate values of all bytes per pixel
						val += shifted;
					}

					//compute x and y position
					int x = i % _w;
					int y = i / _w;

					//set value
					_intensity_arr[x][y] = val;
				}

				//pixel data is last element, stop
				break;
			}
		}
	}

	public int[][] get_intensity_arr(){
		return _intensity_arr;
	}

	public int get_intensity(int x, int y){ // x is width, y is height
		return _intensity_arr[x][y];
	}

	public int get_greyscale(int x, int y){ // x is width, y is height
		return intensity_to_greyscale(get_intensity(x, y));
	}

	/**
	 * Normalizes intensity to 0-255 greyscale
	 * @param intensity intensity
	 * @return The scaled and normalized Intensity
	 */
	public int intensity_to_greyscale(int intensity){
		// get byte storage format
		int bits_stored = getBitsStored();

		// init center and width if not given
		if (!_window_center_given) _window_center = (int) Math.pow(2, (float)(bits_stored - 1));
		if (!_window_width_given) _window_width = (int) Math.pow(2, bits_stored);

		//apply scaling
		int scaled = intensity * _slope + _intercept;

		//normalize to 0-255
		int normalized;
		int scaled_center = _window_center * _slope + _intercept;
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
	 * Converts a dicom file into a human readable string info. Might be long.
	 * Useful for debugging.
	 * 
	 * @return		a human readable string representation
	 */
	public String toString() {
		String str = new String();
		
		str+=_file_name+"\n";
		Enumeration<Integer> e = _data_elements.keys();
   	  	List<String> l = new ArrayList<String>();

   	  	while(e.hasMoreElements()) {
		  Integer tag  = e.nextElement();
		  DiDataElement el = (DiDataElement)_data_elements.get(tag);
		  l.add(el.toString());
		}
		
        Collections.sort(l);
        Iterator<String> it = l.iterator();
        while (it.hasNext()) {
        	str += it.next();
        }

        return str;
	}

	public int get_window_center() {
		return _window_center;
	}

	public int get_window_width() {
		return _window_width;
	}

	public int get_slope() {
		return _slope;
	}

	public int get_intercept() {
		return _intercept;
	}

	public int get_high_bit() {
		return _high_bit;
	}

	public String get_format() {
		return _format; // whitespace is already removed
	}


	/**
	 * Returns the number of allocated bits per pixel.
	 * @return the number of allocated bits.
	 */
	public int getBitsAllocated() {
		return _bits_allocated;
	}

	/**
	 * Returns the number of bits per pixel that are actually used for color info.
	 * @return the number of stored bits.
	 */
	public int getBitsStored() {
		return _bits_stored;
	}

	/**
	 * Allows access to the internal data element HashTable.
	 * @return a reference to the data element HashTable
	 */
	public Hashtable<Integer, DiDataElement> getDataElements() {
		return _data_elements;
	}

	/**
	 * Returns the DiDataElement with the given id. Can return null.
	 * @param id
	 * @return
	 */
	public DiDataElement getElement(int id) {
		return _data_elements.get(id);
	}

	/**
	 * Returns the image width of the contained dicom image.
	 * @return the image width
	 */
	public int getImageWidth() {
		return _w;
	}

	/**
	 * Returns the image height of the contained dicom image.
	 * @return the image height
	 */
	public int getImageHeight() {
		return _h;
	}

	/**
	 * Returns the file name of the current file.
	 * 
	 * @return the file name
	 */
	public String getFileName() {
		return _file_name;
	}
	
	/**
	 * Returns the image number in the current dicom series.
	 * 
	 * @return the image number
	 */
	public int getImageNumber() {
		return _image_number;
	}
}
