package test;

public class FastPower {
    public static void main(String[] args) {
    	int base = 2;
    	int index = 20;
    	int ans = fastPower(base, index);
    	System.out.println(ans);
    }
    public static int fastPower(int base, int index) {
		int tmp = 1;
		while(index > 0){
			if ((index % 2) == 1) {
				tmp = base * tmp;
			}
			base *= base;
			index /= 2;
		}
		return tmp;
	}

}
