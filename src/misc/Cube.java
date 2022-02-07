package misc;

public class Cube {
    private boolean[] corners = {false, false, false, false, false, false, false, false};

    public Cube(boolean[] corners){
        this.corners = corners;
    }

    public Cube(int index){
        if (index > 255){
            System.out.println("Error: index cannot be > 255");
            return;
        }
        int temp_value = index;
        corners = new boolean[8];
        for (int i = 7; i >= 0; i--){
            int pow = (int) Math.pow(2, i);
            if (temp_value >= pow){
                corners[i] = true;
                temp_value -= pow;
            } else {
                corners[i] = false;
            }
        }

    }

    public boolean[] get_corners(){
        return this.corners;
    }

    public int boolean_to_int(boolean bool){
        return (bool) ? 1 : 0;
    }

    public int get_index(){
        int value = 0;
        for (int i = 0; i < 8; i++){
            value += Math.pow(2, i) * boolean_to_int(this.corners[i]);
            // [1, 0, 1, 0, 1, 1, 1, 1]
        }
        return value;
    }

    public Cube rotate_left(){
        return new Cube(new boolean[]{corners[1], corners[5], corners[6], corners[2], corners[0], corners[4],
                corners[7], corners[3]});
    }

    public Cube rotate_up(){
        return new Cube(new boolean[]{corners[4], corners[5], corners[1], corners[0], corners[7], corners[6],
                corners[2], corners[3]});
    }

    public Cube invert(){
        return new Cube(new boolean[]{!corners[0], !corners[1], !corners[2], !corners[3], !corners[4], !corners[5],
                !corners[6], !corners[7]});
    }
}
