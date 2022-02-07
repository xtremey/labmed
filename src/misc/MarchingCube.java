package misc;
import misc.Cube;
import org.jogamp.vecmath.Point3d;

public class MarchingCube {
    public int[][] table;

    public MarchingCube(){
        table = new int[256][];
        init_table();
    }

    private Cube basic_case(int i){
       switch (i) {
           case (0) :
               return new Cube(new boolean[]{false, false, false, false, false, false, false, false});
           case (1) :
               return new Cube(new boolean[]{true, false, false, false, false, false, false, false});
           case (2) :
               return new Cube(new boolean[]{true, true, false, false, false, false, false, false});
           case (3) :
               return new Cube(new boolean[]{true, false, true, false, false, false, false, false});
           case (4) :
               return new Cube(new boolean[]{true, false, false, false, false, false, true, false});
           case (5) :
               return new Cube(new boolean[]{false, true, false, false, true, true, false, false});
           case (6) :
               return new Cube(new boolean[]{true, true, false, false, false, false, true, false});
           case (7) :
               return new Cube(new boolean[]{false, true, false, true, false, false, true, false});
           case (8) :
               return new Cube(new boolean[]{true, true, false, false, true, true, false, false});
           case (9) :
               return new Cube(new boolean[]{true, false, false, false, true, true, false, true});
           case (10) :
               return new Cube(new boolean[]{true, false, false, true, false, true, true, false});
           case (11) :
               return new Cube(new boolean[]{true, false, false, false, true, true, true, false});
           case (12) :
               return new Cube(new boolean[]{false, true, false, true, true, true, false, false});
           case (13) :
               return new Cube(new boolean[]{true, false, true, false, false, true, false, true});
           case (14) :
               return new Cube(new boolean[]{false, true, false, false, true, true, false, true});
           default:
               System.out.println("Default case error");
               return null;
       }
    }

    private int[] triangle_set(int i){
        switch (i) {
            case (0) :
                return new int[]{};
            case (1) :
                return new int[]{0, 7, 3};
            case (2) :
                return new int[]{1, 4, 7, 7, 3, 1};
            case (3) :
                return new int[]{0, 7, 3, 5, 1, 2};
            case (4) :
                return new int[]{0, 7, 3, 10, 9, 5};
            case (5) :
                return new int[]{7, 0, 11, 0, 1, 11, 11, 1, 9};
            case (6) :
                return new int[]{1, 4, 7, 7, 3, 1, 10, 9, 5};
            case (7) :
                return new int[]{2, 3, 6, 1, 4, 0, 10, 9, 5};
            case (8) :
                return new int[]{11, 3, 9, 9, 3, 1};
            case (9) :
                return new int[]{6, 3, 10, 10, 3, 0, 0, 9, 10, 9, 0, 4};
            case (10) :
                return new int[]{2, 0, 7, 6, 2, 7, 10, 8, 5, 5, 8, 4};
            case (11) :
                return new int[]{11, 3, 0, 10, 11, 5, 5, 11, 0, 0, 4, 5};
            case (12) :
                return new int[]{2, 3, 6, 7, 0, 11, 0, 1, 11, 11, 1, 9};
            case (13) :
                return new int[]{0, 7, 3, 9, 8, 4, 5, 1, 2, 6, 11, 10};
            case (14) :
                return new int[]{7, 0, 6, 0, 9, 6, 9, 0, 1, 9, 10, 6};
            default:
                System.out.println("Default case error");
                return null;
        }
    }

    public void init_table(){

        for (int i = 0; i < 15; i++){
            Cube base_case = basic_case(i);
            int[] base_triangle_set = triangle_set(i);

            // rotate and insert case
            int[] inverted_triangle_set = invert_triangles(base_triangle_set);
//            handle_rotation(base_case, base_triangle_set);
            handle_rotation(base_case, inverted_triangle_set);

            //inversion
            Cube base_case_inverted = base_case.invert();
            handle_rotation(base_case_inverted, base_triangle_set);

        }
    }

    public void handle_rotation(Cube base_case, int[] triangle_set){
        //rotate one side to top
        for (int u = 0; u < 4; u++){
            Cube top_side_rotation = new Cube(base_case.get_index());
            int[] top_side_rotation_triangle_set = triangle_set.clone();
            for(int u_temp = 0; u_temp < u; u_temp++){
                top_side_rotation = top_side_rotation.rotate_up();
                top_side_rotation_triangle_set = rotate_triangles_up(top_side_rotation_triangle_set);
            }

            for (int l = 0; l < 4; l++){
                top_side_rotation = top_side_rotation.rotate_left();
                top_side_rotation_triangle_set = rotate_triangles_left(top_side_rotation_triangle_set);
                //save result
                table[top_side_rotation.get_index()] = top_side_rotation_triangle_set;
            }


        }

        //rotate two missing sides to top
        Cube top_side_rotation = new Cube(base_case.get_index());
        int[] top_side_rotation_triangle_set = triangle_set.clone();

        top_side_rotation = top_side_rotation.rotate_left();
        top_side_rotation_triangle_set = rotate_triangles_left(top_side_rotation_triangle_set);

        top_side_rotation = top_side_rotation.rotate_up();
        top_side_rotation_triangle_set = rotate_triangles_up(top_side_rotation_triangle_set);

        for (int l = 0; l < 4; l++){
            top_side_rotation = top_side_rotation.rotate_left();
            top_side_rotation_triangle_set = rotate_triangles_left(top_side_rotation_triangle_set);
            //save result
            table[top_side_rotation.get_index()] = top_side_rotation_triangle_set;
        }

        Cube top_side_rotation_2 = new Cube(base_case.get_index());
        int[] top_side_rotation_triangle_set_2 = triangle_set.clone();

        top_side_rotation_2 = top_side_rotation_2.rotate_left();
        top_side_rotation_triangle_set_2 = rotate_triangles_left(top_side_rotation_triangle_set_2);

        top_side_rotation_2 = top_side_rotation_2.rotate_up();
        top_side_rotation_2 = top_side_rotation_2.rotate_up();
        top_side_rotation_2 = top_side_rotation_2.rotate_up();
        top_side_rotation_triangle_set_2 = rotate_triangles_up(top_side_rotation_triangle_set_2);
        top_side_rotation_triangle_set_2 = rotate_triangles_up(top_side_rotation_triangle_set_2);
        top_side_rotation_triangle_set_2 = rotate_triangles_up(top_side_rotation_triangle_set_2);

        for (int l = 0; l < 4; l++){
            top_side_rotation_2 = top_side_rotation_2.rotate_left();
            top_side_rotation_triangle_set_2 = rotate_triangles_left(top_side_rotation_triangle_set_2);
            //save result
            table[top_side_rotation_2.get_index()] = top_side_rotation_triangle_set_2;
        }
    }

    public int rotate_edge_left(int edge){
        switch (edge) {
            case (0) : return 7;
            case (1) : return 3;
            case (2) : return 6;
            case (3) : return 11;
            case (4) : return 0;
            case (5) : return 2;
            case (6) : return 10;
            case (7) : return 8;
            case (8) : return 4;
            case (9) : return 1;
            case (10): return 5;
            case (11): return 9;
            default:
                System.out.println("edge must be in range [0, 11]");
                return -1;
        }
    }

    public int rotate_edge_up(int edge){
        switch (edge) {
            case (0) : return 2;
            case (1) : return 5;
            case (2) : return 10;
            case (3) : return 6;
            case (4) : return 1;
            case (5) : return 9;
            case (6) : return 11;
            case (7) : return 3;
            case (8) : return 0;
            case (9) : return 4;
            case (10): return 8;
            case (11): return 7;
            default:
                System.out.println("edge must be in range [0, 11]");
                return -1;
        }
    }

    public Point3d calculate_point_on_edge(int edge){
        switch (edge) {
            case (0) : return new Point3d(0.5, 0, 0);
            case (1) : return new Point3d(1, 0, 0.5);
            case (2) : return new Point3d(0.5, 0, 1);
            case (3) : return new Point3d(0, 0, 0.5);
            case (4) : return new Point3d(1, 0.5, 0);
            case (5) : return new Point3d(1, 0.5, 1);
            case (6) : return new Point3d(0, 0.5, 1);
            case (7) : return new Point3d(0, 0.5, 0);
            case (8) : return new Point3d(0.5, 1, 0);
            case (9) : return new Point3d(1, 1, 0.5);
            case (10): return new Point3d(0.5, 1, 1);
            case (11): return new Point3d(0, 1, 0.5);
            default:
                System.out.println("edge must be in range [0, 11]");
                return new Point3d(0,0,0);
        }
    }

    public int[] rotate_triangles_left(int[] triangles){
        int[] new_array = new int[triangles.length];
        for (int i = 0; i < triangles.length; i++){
            new_array[i] = rotate_edge_left(triangles[i]);
        }
        return new_array;
    }

    public int[] rotate_triangles_up(int[] triangles){
        int[] new_array = new int[triangles.length];
        for (int i = 0; i < triangles.length; i++){
            new_array[i] = rotate_edge_up(triangles[i]);
        }
        return new_array;
    }

    public int[] invert_triangles(int[] triangles){
        int[] result = triangles.clone();
        for (int i = 0; i < triangles.length; i++){
            result[i] = triangles[triangles.length - i - 1];
        }
        return result;
    }

    public Point3d[] get_triangle_coordinates(int[] triangle){
        Point3d[] arr = new Point3d[triangle.length];

        for (int i = 0; i < triangle.length; i++){
            int edge = triangle[i];
            arr[i] = calculate_point_on_edge(edge);
        }

        return arr;
    }

}