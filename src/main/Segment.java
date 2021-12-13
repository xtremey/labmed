package main;

import misc.BitMask;

import java.util.*;


enum SegmentType {
	RANGE,
	REGION
}


/**
 * This class represents a segment. Simply spoken, a segment has a unique name,
 * a color for displaying in the 2d/3d viewport and contains n bitmasks where n is the
 * number of images in the image stack.
 * 
 * @author  Karl-Ingo Friese
 */
public class Segment {
	private String _name;		// the segment name
	private int _color;			// the segment color
	private int _w;				// Bitmask width
	private int _h;				// Bitmask height
	private BitMask[] _layers;	// each segment contains an array of n bitmasks
	private SegmentType _segment_type = null;
	
	/**
	 * Constructor for new segment objects.
	 * 
	 * @param name			the name of the new segment
	 * @param w				the width of the bitmasks
	 * @param h				the height of the bitmasks
	 * @param layer_num		the total number of bitmasks
	 */
	public Segment(String name, int w, int h, int layer_num, SegmentType type) {
		this._name = name;
		this._w = w;
		this._h = h;
		this._segment_type = type;
		_color = 0xff00ff;		
		_layers = new BitMask[layer_num];
		
		for (int i=0; i<layer_num; i++) {
			_layers[i] = new BitMask(_w,_h);
		}
	}

	public void create_range_seg(int min, int max, ImageStack slices){
		for( int i = 0; i < slices.getNumberOfImages(); i++){
			BitMask mask = _layers[i];
			for( int y = 0; y < _h; y++){
				for( int x = 0; x < _w; x++){
					mask.set(x,y, slices.getDiFile(i).is_in_range(min, max, x, y));
				}
			}
		}
	}

	private boolean _is_valid_pixel(int[] pixel, ImageStack slices){
		if(pixel[0] < 0 || pixel[0] >= _w) return false;
		if(pixel[1] < 0 || pixel[1] >= _h) return false;
		if(pixel[2] < 0 || pixel[2] >= slices.getNumberOfImages()) return false;
		return true;
	}

	public boolean is_in_region(int[] pixel, int min, int max, BitMask[] tested, ImageStack slices){
		if(!_is_valid_pixel(pixel, slices)) return false;
		if(tested[pixel[2]].get(pixel[0], pixel[1])) return false;

		return slices.getDiFile(pixel[2]).is_in_range(min, max, pixel[0], pixel[1]);
	}



	public void create_region_segment(int[] seed_pixel, int variance, ImageStack slices){
		//clear old segmentation
		for (int i = 0; i < slices.getNumberOfImages(); i++){
			_layers[i].clear();
		}

		Queue<int[]> queue = new LinkedList<>();
		queue.add(seed_pixel);
		_layers[seed_pixel[2]].set(seed_pixel[0], seed_pixel[1], true);
		BitMask[] tested = new BitMask[slices.getNumberOfImages()];
		for (int i=0; i < slices.getNumberOfImages(); i++) {
			tested[i] = new BitMask(_w,_h);
		}

        int seed_intensity = LabMed.get_is().get_intensity(seed_pixel[0], seed_pixel[1], seed_pixel[2]);
		int min_val = (int) (seed_intensity - seed_intensity * variance / 100.0);
		int max_val = (int) (seed_intensity + seed_intensity * variance / 100.0);

		while (queue.size() != 0){
			int[] current_pixel = queue.remove();
			// get N6 neighbourhood
			int[][] neighbours = new int[6][];
			for (int i = 0; i < 6; i++){
				neighbours[i] = Arrays.copyOf(current_pixel, 3);
			}

			neighbours[0][0] = current_pixel[0] + 1;
			neighbours[1][0] = current_pixel[0] - 1;
			neighbours[2][1] = current_pixel[1] + 1;
			neighbours[3][1] = current_pixel[1] - 1;
			neighbours[4][2] = current_pixel[2] + 1;
			neighbours[5][2] = current_pixel[2] - 1;

			for(int i = 0; i < 6; i++){
				int[] pixel = neighbours[i];
				if (!_is_valid_pixel(pixel, slices)) continue;
				if(is_in_region(pixel, min_val, max_val, tested, slices)){
					queue.add(pixel);
					_layers[pixel[2]].set(pixel[0], pixel[1], true);
				}
				tested[pixel[2]].set(pixel[0], pixel[1], true);
			}


		}

	}

	public boolean is_in_mask(int x, int y, int z){
		BitMask mask = _layers[z];
		return mask.get(x,y);
	}

	/**
	 * Returns the number of bitmasks contained in this segment.
	 * 
	 * @return  the number of layers.
	 */
	public int getMaskNum() {
		return _layers.length;
	}

	/**
	 * Returns the Bitmask of a single layer.
	 * 
	 * @param i	the layer number
	 * @return	the coresponding bitmask
	 */
	public BitMask getMask(int i) {
		return _layers[i];
	}

	/**
	 * Returns the name of the segment.
	 * 
	 * @return  the segment name.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Sets the name of the segment.
	 * 
	 * @param name	the new segment name
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * Returns the segment color as the usual rgb int value.
	 * 
	 * @return the color
	 */
	public int getColor() {
		return _color;
	}

	/**
	 * Sets the segment color.
	 * 
	 * @param color the segment color (used when displaying in 2d/3d viewport)
	 */
	public void setColor(int color) {
		_color = color;
	}

	public SegmentType get_segment_type() {
		return _segment_type;
	}
}
