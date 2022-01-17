package main;

import java.awt.*;
import java.awt.image.BufferedImage;

import misc.ViewMode;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.behaviors.mouse.MouseRotate;
import org.jogamp.java3d.utils.behaviors.vp.OrbitBehavior;
import org.jogamp.java3d.utils.geometry.ColorCube;
import org.jogamp.java3d.utils.universe.SimpleUniverse;

import misc.MyObservable;
import misc.MyObserver;
import org.jogamp.java3d.utils.universe.ViewingPlatform;
import org.jogamp.vecmath.*;

/**
 * Three dimensional viewport for viewing the dicom images + segmentations.
 * 
 * @author  Karl-Ingo Friese
 */
public class Viewport3d extends Viewport implements MyObserver  {
	private static final long serialVersionUID = 1L;
	private int _point_distance = 1;


	public int get_point_distance() {
		return _point_distance;
	}

	public void set_point_distance(int _point_distance) {
		this._point_distance = _point_distance;
	}

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


		public void createScene() {
			if (_scene != null) {
				_scene.detach( );
			}
			_scene = new BranchGroup();
			_scene.setCapability(BranchGroup.ALLOW_DETACH);

			double x = _slices.getImageWidth();
			double y = _slices.getImageHeight();
			double z = _slices.getNumberOfImages();
			if (x == 0 || y == 0 || z == 0) {
				return;
			}
			Transform3D transform = new Transform3D();
			transform.setScale(new Vector3d(1.0 / x , 1.0 / y, 1.0 / z));
			transform.setTranslation(new Vector3d(-0.5, -0.5, 0));
			TransformGroup tg = new TransformGroup(transform);
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
			tg.setCapability(BranchGroup.ALLOW_DETACH);

			BranchGroup bg_segments = new BranchGroup();
			if(_map_name_to_seg.size() != 0) {
				for (Segment s : _map_name_to_seg.values()) {
					bg_segments.addChild(create_segment_point_cloud(s));
				}

			}
			tg.insertChild(bg_segments, 0);

			_scene.addChild(tg);
			add_ortho_slices();

			BoundingSphere bigBounds = new BoundingSphere(new Point3d(),1000);
			OrbitBehavior orbit = new OrbitBehavior(this, OrbitBehavior.REVERSE_ROTATE);
			orbit.setSchedulingBounds(bigBounds);
			orbit.setRotationCenter(new Point3d(0, 0, 0.5));

			ViewingPlatform vp = _simple_u.getViewingPlatform();
			vp.setViewPlatformBehavior(orbit);

			_simple_u.addBranchGraph(_scene);
		}

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

	public Shape3D create_ortho_slices(ViewMode mode, int pos){

		Point3d a,b,c,d; // need to be initialized
		int width = _slices.getImageWidth();
		int height = _slices.getImageHeight();
		int num_img = _slices.getNumberOfImages();
		if(mode == ViewMode.TRANSVERSAL){
			a = new Point3d(0,0,pos);
			b = new Point3d(width,0,pos);
			c = new Point3d(width, height,pos);
			d = new Point3d(0, height,pos);
		} else if(mode == ViewMode.FRONTAL){
			a = new Point3d(0, pos,0);
			b = new Point3d(width,pos,0);
			c = new Point3d(width, pos, num_img);
			d = new Point3d(0, pos, num_img);
		} else {
			a = new Point3d(pos,0,0);
			b = new Point3d(pos, height,0);
			c = new Point3d(pos, height, num_img);
			d = new Point3d(pos, 0, num_img);
		}

		QuadArray sq = new QuadArray(4, QuadArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
		sq.setCoordinate(0, a);
		sq.setCoordinate(1, b);
		sq.setCoordinate(2, c);
		sq.setCoordinate(3, d);
		sq.setTextureCoordinate(0, 3, new TexCoord2f(0.0f,0.0f));
		sq.setTextureCoordinate(0, 2, new TexCoord2f(1.0f,0.0f));
		sq.setTextureCoordinate(0, 1, new TexCoord2f(1.0f,1.0f));
		sq.setTextureCoordinate(0, 0, new TexCoord2f(0.0f,1.0f));

		BufferedImage img = LabMed.get_v2d().getBGImage(mode, pos);
		ImageComponent2D i2d = new ImageComponent2D(ImageComponent2D.FORMAT_RGBA, img);
		Texture2D tex = new Texture2D(Texture2D.BASE_LEVEL,
				Texture2D.RGBA, img.getWidth(), img.getHeight());
		tex.setImage(0, i2d);

		Appearance ap_plane = new Appearance();
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		ap_plane.setPolygonAttributes(pa);
		ap_plane.setTexture(tex);

		Shape3D square_shp = new Shape3D(sq, ap_plane);
		return square_shp;
	}

	private void add_ortho_slices(){
		TransformGroup tg = (TransformGroup) _panel3d._scene.getChild(0);

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bg.addChild(create_ortho_slices(ViewMode.TRANSVERSAL, LabMed.get_v2d().active_transversal));
		bg.addChild(create_ortho_slices(ViewMode.SAGITTAL, LabMed.get_v2d().active_saggital));
		bg.addChild(create_ortho_slices(ViewMode.FRONTAL, LabMed.get_v2d().active_frontal));

		if (tg.numChildren() == 1){
			tg.insertChild(bg, 1);
		} else {
			tg.setChild(bg, 1);
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

		if (msg._type == Message.M_NEW_ACTIVE_IMAGE) {
			if (_panel3d._scene == null || _panel3d._scene.numChildren() == 0){
				return;
			}
			add_ortho_slices();
		}
	}
}
