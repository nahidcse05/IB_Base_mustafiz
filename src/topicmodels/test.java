package topicmodels;

import java.util.Arrays;

public class test {

	public void mod2(int local[])
	{
		
		Arrays.fill(local,5);
		for(int i=0; i<local.length; i++)
		{
			local[i] *= 2;
		}
	}
	
	public void mod1(int local[][])
	{
		for(int i=0; i<local.length; i++)
		{
			mod2(local[i]);
		}
	}
	
	public test()
	{
		int a[][] = new int[2][2];
		mod1(a);
		for(int i=0; i<a.length; i++)
		{
			for(int j=0; j<a[i].length; j++)
			{
				System.out.print(a[i][j]+" ");
			}
			System.out.print("\n");
		}
	}
	
	public static void main(String[] args)
	{
		test t= new test();
	}
	
	
}
