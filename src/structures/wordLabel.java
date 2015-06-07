package structures;

public class wordLabel {
	
	private double value;
	private int poslabel;
	private int neglabel;
	
	public int poscount;
	public int negcount;
	
	public wordLabel()
	{
		this.value = 0;
		this.poslabel = -1;
		this.neglabel = -1;
		this.poscount = 0;
		this.negcount = 0;
	}
	
	public wordLabel(double count, int poslabel, int neglabel)
	{
		this.value = count;
		this.poslabel = poslabel;
		this.neglabel = neglabel;
	}
	
	public double getValue()
	{
		return value;
	}
	
	public void setValue(double value)
	{
		this.value = value;
	}
	
	public void setposLabel(int poslabel)
	{
		this.poslabel = poslabel;
	}
	
	public int getposLabel()
	{
		return this.poslabel;
	}
	
	public int getnegLabel()
	{
		return this.neglabel;
	}
	
	public void setnegLabel(int neglabel)
	{
		this.neglabel = neglabel;
	}
	

	
}
