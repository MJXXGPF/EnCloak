package test;

public class Sort_Quick {
	public static void main(String[] args) 
    {	       
		int[] numbers=new int[4000];
		for(int i=0;i<numbers.length;i++)
			numbers[i] = (int)(Math.random()*(100000+1));//0-10000
  
        quickSort(numbers, 0, numbers.length-1);
//        for(int i=0;i<numbers.length;i++){
//        	System.out
//					.print(numbers[i]+" ");
//        }
       
        
    }
	 

	public static void quickSort(int arr[],int lo,int hi)
    {
        if(hi<=lo)
            return;
        int j=partition(arr,lo,hi);//j左边的元素不大于a[j],j右边的元素不小于a[j]
        quickSort(arr,lo,j-1);
        quickSort(arr,j+1,hi);
    }
	public static int partition(int arr[],int lo,int hi)
    {
        int i=lo;
        int j=hi+1;
        int v=arr[lo];//切分的元素
        while (true)
        {
            //保证切分元素左边的元素都比它小，右边的元素都比它大
            while (arr[++i]<v)//查找比切分元素大的元素a
                if(i==hi)
                    break;
            while (arr[--j]>v)//查找比切分元素小的元素b
                if(j==lo)
                    break;
            if(i>=j)//左右扫描指针相等或左指针跑到右指针前面，说明扫描结束
                break;
            int tmp=arr[i];
            arr[i]=arr[j];
            arr[j]=tmp;
            //交换前面找到的a b

        }
        int tmp=arr[lo];
        arr[lo]=arr[j];
        arr[j]=tmp;
        //最后交换切分元素，将切分元素放到指定的位置
        return j;
    }

}
