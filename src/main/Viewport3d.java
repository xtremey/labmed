package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;

import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.geometry.ColorCube;
import org.jogamp.java3d.utils.universe.SimpleUniverse;

import misc.MyObservable;
import misc.MyObserver;
import org.jogamp.vecmath.*;

/**
 * Three dimensional viewport for viewing the dicom images + segmentations.
 * 
 * @author  Karl-Ingo Friese
 */
public class Viewport3d extends Viewport implements MyObserver  {
	private static final long serialVersionUID = 1L;
	private int _point_distance = 1;

	/**
	 * Private class, implementing the GUI element for displaying the 3d data.
	 */
	public class Panel3d extends Canvas3D {
		private static final long serialVersionUID = 1L;
		public SimpleUniverse _simple_u;
		public BranchGroup _scene;
		
		
		public Panel3d(GraphicsConfiguration config){	
			super(config);
			setMinimumSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setMaximumSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setPreferredSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
			setBackground(Color.black);

			_simple_u = new SimpleUniverse(this);
		    _simple_u.getViewingPlatform().setNominalViewingTransform();
		    _scene = null;

	        createScene();
	        super.getView().setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);          
		}

		public Color3f int_to_color(int color){
			int r = color & 0x00ff0000;
			int g = color & 0x0000ff00;
			int b = color & 0x000000ff;
			return new Color3f(r, g, b);
		}

		public Shape3D create_segment_point_cloud(Segment segment){
			double x = _slices.getImageWidth();
			double y = _slices.getImageHeight();
			double z = _slices.getNumberOfImages();
			int num_points = (int) (x * y * z);
			PointArray arr = new PointArray(num_points, PointArray.COORDINATES);
			int idx = 0;
			for(int i = 0; i < x; i++){
				for(int j = 0; j < y; j++){
					for(int k = 0; k < z; k++){
						if (k % _point_distance == 0 && j % _point_distance == 0 && i % _point_distance == 0){
							if(segment.is_in_mask(i, j, k)){
								Point3d p = new Point3d(i, j, k);
								arr.setCoordinate(idx, p);
								idx++;
							}
						}
					}
				}
			}
			Color3f color = int_to_color(segment.getColor());
			ColoringAttributes color_ca = new ColoringAttributes(color, 1);
			Appearance ap = new Appearance();
			ap.setColoringAttributes(color_ca);

			return new Shape3D(arr, ap);
		}
	 
		public void createScene() {
			if (_scene != null) {
				_scene.detach( );
			}
			_scene = new BranchGroup();
			_scene.setCapability( BranchGroup.ALLOW_DETACH );
			
			if(_map_name_to_seg.size() == 0){
				// create a ColorCube object of size 0.5
				ColorCube c = new ColorCube(0.5f);
				_scene.addChild(c);
			} else {
				double x = _slices.getImageWidth();
				double y = _slices.getImageHeight();
				double z = _slices.getNumberOfImages();
				Transform3D transform = new Transform3D();
				transform.setScale(new Vector3d(1.0 / x , 1.0 / y, 1.0 / z));
				transform.setTranslation(new Vector3d(-0.5, -0.5, 0));
				TransformGroup tg = new TransformGroup(transform);

				for (Segment s : _map_name_to_seg.values()){
					tg.addChild(create_segment_point_cloud(s));
				}

				_scene.addChild(tg);
			}

			
			_simple_u.addBranchGraph(_scene);
		}
			
	}		


	private Panel3d _panel3d;

	/**
	 * Constructor, with a reference to the global image stack as argument.
	 *
	 */
	public Viewport3d() {
		super();
		
		this.setPreferredSize(new Dimension(DEF_WIDTH,DEF_HEIGHT));
		this.setLayout( new BorderLayout() );
		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
		_panel3d = new Panel3d( config );		
        this.add(_panel3d, BorderLayout.CENTER );        
	}

	/**
	 * calculates the 3d data structurs.
	 */
	public void update_view() {
		_panel3d.createScene();
	}
	
	/**
	 * Implements the observer function update. Updates can be triggered by the global
	 * image stack.
	 */
	public void update(final MyObservable mo, final Message msg) {
		if (!EventQueue.isDispatchThread()) {
			// all swing thingies must be done in the AWT-EventQueue 
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					update(mo, msg);
				}
			});
			return;
		}

		if (msg._type == Message.M_SEG_CHANGED) {
			String seg_name = ((Segment)(msg._obj)).getName();
			boolean update_needed = _map_name_to_seg.containsKey(seg_name);
			if (update_needed) {
				update_view();
			}
		}
	}
}
