package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import misc.*;
import org.jogamp.java3d.*;
import org.jogamp.java3d.utils.behaviors.vp.OrbitBehavior;
import org.jogamp.java3d.utils.geometry.GeometryInfo;
import org.jogamp.java3d.utils.geometry.NormalGenerator;
import org.jogamp.java3d.utils.universe.SimpleUniverse;

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
	private int _step_size = 1;

	public boolean show_ortho_slices = false;
	public boolean show_volume_render = false;
	public boolean show_point_cloud = false;
	public boolean show_marching_cube = false;

	public int get_point_distance() {
		return _point_distance;
	}

	public int get_step_size(){
		return _step_size;
	}

	public void set_point_distance(int _point_distance) {
		this._point_distance = _point_distance;
	}

	public void set_step_size(int step_size){
		this._step_size = step_size;
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
			transform.setScale(new Vector3d(1.0 / x , -1.0 / y, 1.0 / z));
			transform.setTranslation(new Vector3d(-0.5, 0.5, 0));
			TransformGroup tg = new TransformGroup(transform);
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
			tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
			tg.setCapability(BranchGroup.ALLOW_DETACH);


			_scene.addChild(tg);
			update_point_cloud();
			update_ortho_slices();
			update_volume_rendering();
			update_mc_rendering();

			BoundingSphere bigBounds = new BoundingSphere(new Point3d(),1000);
			OrbitBehavior orbit = new OrbitBehavior(this, OrbitBehavior.REVERSE_ROTATE);
			orbit.setSchedulingBounds(bigBounds);
			orbit.setRotationCenter(new Point3d(0, 0, 0.5));

			ViewingPlatform vp = _simple_u.getViewingPlatform();
			vp.setViewPlatformBehavior(orbit);

			//light
			BoundingSphere bounds;
			bounds = new BoundingSphere (new Point3d(0.0d,0.0d,0.0d),
					Double.MAX_VALUE);
			// ambient light
			AmbientLight a_light = new AmbientLight();
			a_light.setInfluencingBounds(bounds);
			a_light.setColor(new Color3f(0.1f,0.1f,0.1f));
			_scene.addChild(a_light);

			// directional light
//			DirectionalLight d_light = new DirectionalLight();
//			d_light.setInfluencingBounds(bounds);
//			d_light.setColor(new Color3f(0.1f,0.1f,0.1f));
//			d_light.setDirection(new Vector3f(-0.5f, 0, -1));
//			_scene.addChild(d_light);

			//point light 1
			PointLight p_light = new PointLight();
			p_light.setInfluencingBounds(bounds);
			p_light.setColor(new Color3f(0.003f,0.003f,0.003f));
			p_light.setPosition(new Point3f(100f, 100f, 100f));
			_scene.addChild(p_light);

			//point light 1
			PointLight p_light2 = new PointLight();
			p_light2.setInfluencingBounds(bounds);
			p_light2.setColor(new Color3f(0.0025f,0.0025f,0.0025f));
			p_light2.setPosition(new Point3f(-100f, -100f, 100f));
			_scene.addChild(p_light2);


			_simple_u.addBranchGraph(_scene);
		}

	}

	public Color3f int_to_color(int color){
		int r = (color & 0x00ff0000) >> 16;
		int g = (color & 0x0000ff00) >> 8;
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

	public void update_point_cloud(){
		TransformGroup tg = (TransformGroup) _panel3d._scene.getChild(0);

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		if (_map_name_to_seg.size() != 0 && show_point_cloud) {
			for (Segment s : _map_name_to_seg.values()) {
				bg.addChild(create_segment_point_cloud(s));
			}
		}

		if (tg.numChildren() == 0){
			tg.insertChild(bg, 0);
		} else {
			tg.setChild(bg, 0);
		}
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

		BufferedImage img = LabMed.get_v2d().getBGImage(mode, pos, 128);
		ImageComponent2D i2d = new ImageComponent2D(ImageComponent2D.FORMAT_RGBA, img);
		Texture2D tex = new Texture2D(Texture2D.BASE_LEVEL,
				Texture2D.RGBA, img.getWidth(), img.getHeight());
		tex.setImage(0, i2d);

		TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NICEST, 0.7f);

		Appearance ap_plane = new Appearance();
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		ap_plane.setPolygonAttributes(pa);
		ap_plane.setTransparencyAttributes(ta);
		ap_plane.setTexture(tex);

		Shape3D square_shp = new Shape3D(sq, ap_plane);
		return square_shp;
	}

	private BranchGroup get_volume_rendering(){
		BranchGroup bg = new BranchGroup();

		for (int x = 0; x < _slices.getImageWidth(); x++) {
			Shape3D shape = create_ortho_slices(ViewMode.SAGITTAL, x);
			bg.addChild(shape);
		}
		for (int y = 0; y < _slices.getImageHeight(); y++) {
			Shape3D shape = create_ortho_slices(ViewMode.FRONTAL, y);
			bg.addChild(shape);
		}
		for (int z = 0; z < _slices.getNumberOfImages(); z++) {
			Shape3D shape = create_ortho_slices(ViewMode.TRANSVERSAL, z);
			bg.addChild(shape);
		}

		return bg;
	}

	public void update_volume_rendering(){
		TransformGroup tg = (TransformGroup) _panel3d._scene.getChild(0);
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		if(show_volume_render){
			bg.addChild(get_volume_rendering());
		}
		if (tg.numChildren() == 2){
			tg.insertChild(bg, 2);
		} else {
			tg.setChild(bg, 2);
		}
	}

	public void update_ortho_slices(){
		TransformGroup tg = (TransformGroup) _panel3d._scene.getChild(0);

		BranchGroup bg = new BranchGroup();

		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		if (show_ortho_slices){
			bg.addChild(create_ortho_slices(ViewMode.TRANSVERSAL, LabMed.get_v2d().active_transversal));
			bg.addChild(create_ortho_slices(ViewMode.SAGITTAL, LabMed.get_v2d().active_saggital));
			bg.addChild(create_ortho_slices(ViewMode.FRONTAL, LabMed.get_v2d().active_frontal));
		}

		if (tg.numChildren() == 1){
			tg.insertChild(bg, 1);
		} else {
			tg.setChild(bg, 1);
		}
	}


	public Shape3D create_marching_cube_render(Segment segment){
		int step_size = _step_size;
		Map<Point3d, Integer> map = new HashMap<>();
		ArrayList<Point3d> vertex_list = new ArrayList<Point3d>();
		ArrayList<Integer> index_list = new ArrayList<Integer>();


		for (int x=0; x < _slices.getImageWidth() - step_size; x += step_size){
			for (int y=0; y < _slices.getImageHeight() - step_size; y += step_size){
				for (int z=0; z < _slices.getNumberOfImages() - step_size; z += step_size){
					// get all adjacent voxel
					boolean[] arr = new boolean[8];
					arr[0] = segment.is_in_mask(x, y, z);
					arr[1] = segment.is_in_mask(x+step_size, y, z);
					arr[2] = segment.is_in_mask(x+step_size, y, z+step_size);
					arr[3] = segment.is_in_mask(x, y, z+step_size);
					arr[4] = segment.is_in_mask(x, y+step_size, z);
					arr[5] = segment.is_in_mask(x+step_size, y+step_size, z);
					arr[6] = segment.is_in_mask(x+step_size, y+step_size, z+step_size);
					arr[7] = segment.is_in_mask(x, y+step_size, z+step_size);

					//calculate index in mc-table
					Cube cube = new Cube(arr);
					int cube_index = cube.get_index();

					//compute triangle array
					int[] triangles = LabMed.get_mc().table[cube_index];
					Point3d[] triangle_points = LabMed.get_mc().get_triangle_coordinates(triangles);

					//scaling and translation
					Point3d offset = new Point3d(x, y, z);
					for (int i = 0; i < triangle_points.length; i++){
						triangle_points[i].scale(step_size);
						triangle_points[i].add(offset);

						if (!map.containsKey(triangle_points[i])){
							map.put(triangle_points[i],vertex_list.size());
							index_list.add(vertex_list.size());
							vertex_list.add(triangle_points[i]);
						} else {
							int vertex_index = map.get(triangle_points[i]);
							index_list.add(vertex_index);
						}

					}
				}
			}
		}

		IndexedTriangleArray geometry = new IndexedTriangleArray(vertex_list.size(),
				IndexedTriangleArray.COORDINATES | IndexedTriangleArray.NORMALS, index_list.size());

		for(int i = 0; i < index_list.size(); i++){
			geometry.setCoordinateIndex(i, index_list.get(i));
		}
		for(int i = 0; i < vertex_list.size(); i++) {
			geometry.setCoordinate(i, vertex_list.get(i));
		}
		NormalGenerator normalGenerator = new NormalGenerator();
		GeometryInfo info = new GeometryInfo(geometry);
		normalGenerator.generateNormals(info);
		GeometryArray geom_result = info.getGeometryArray();

		Appearance app = new Appearance();
		Material material = new Material();
//		material.setAmbientColor(new Color3f(0.2f, 0.2f, 0.2f));
		Color3f color = int_to_color(segment.getColor());
		material.setDiffuseColor(new Color3f(color.x, color.y, color.z));
		material.setSpecularColor(new Color3f(1, 1, 1));
//		material.setShininess(64);
		app.setMaterial(material);
		app.setColoringAttributes(new ColoringAttributes(color, ColoringAttributes.NICEST));
		Shape3D cube_march_result = new Shape3D();
		cube_march_result.setAppearance(app);
		cube_march_result.setGeometry(geom_result);
//		cube_march_result.setGeometry(geometry);

		return cube_march_result;
	}

	public void update_mc_rendering(){
		TransformGroup tg = (TransformGroup) _panel3d._scene.getChild(0);

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		if (_map_name_to_seg.size() != 0 && show_marching_cube) {
			for (Segment s : _map_name_to_seg.values()) {
				bg.addChild(create_marching_cube_render(s));
			}
		}

		if (tg.numChildren() == 3){
			tg.insertChild(bg, 3);
		} else {
			tg.setChild(bg, 3);
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
				update_point_cloud();
				update_mc_rendering();
			}
		}

		if (msg._type == Message.M_NEW_ACTIVE_IMAGE) {
			if (_panel3d._scene == null || _panel3d._scene.numChildren() == 0){
				return;
			}
			update_ortho_slices();
		}

		if (msg._type == Message.M_NEW_IMAGE_LOADED) {
			update_view();
		}
	}
}
