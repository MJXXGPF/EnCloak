package test;

public class Sort_Bubble {
	public static void main(String[] args) {               
		int[] numbers=new int[100];
        for(int i=0;i<100;i++){
        	numbers[i] = (int)(Math.random()*(100000+1));//0-10000
        }

        bubbleSort(numbers);
/*       
        for(int i : numbers){
        	System.out.println(i);
        }
*/

    }
 
	//标记numbers
	 public static void bubbleSort(int[] numbers){
		 int temp = 0;
	     int size = numbers.length;
	     for(int i = 0 ; i < size-1; i ++){
	        for(int j = 0 ;j < size-1-i ; j++){
	            if(numbers[j] > numbers[j+1]) { //交换两数位置
	            	temp = numbers[j];
	            	numbers[j] = numbers[j+1];
	            	numbers[j+1] = temp;
	            }
	        }
	     }	        
	 }
}
