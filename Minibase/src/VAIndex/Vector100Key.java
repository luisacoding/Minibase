package VAIndex;

import java.util.Arrays;

import global.Vector100Dtype;
import btree.KeyClass;

public class Vector100Key extends KeyClass{
	
	public int getDataLength() {
		if (this.dataLength == 0)
			System.out.println("erro datalength is 0");
		return dataLength;
	}

	private int _b;// bits per dimension
	private int dataLength;// length of key in bytes
	private double regionsize;// size of one region in a dimension, same for all dimension
	private int totalregionnum;// number of region in a dimension
	private byte []data;
	private Vector100Dtype _vector;
	private int []_regionnum ;
	private boolean isAllRegionSet;
	public static int keyCompare(Vector100Key v1,Vector100Key v2){
		int i;
		v1.setAllRegionNumber();
		v2.setAllRegionNumber();
		for (i=0;i<100;i++){
			if (v1.get_regionnumAt(i)>v2.get_regionnumAt(i))
				return 1;
			else if (v1.get_regionnumAt(i)<v2.get_regionnumAt(i))
				return -1;
		}
		return 0;
	}
	public int get_b() {
		if (_b == 0)
			System.out.println("error b is 0 in Vector100Key");
		return _b;
	}
	public void set_b(int _b) {
		this._b = _b;
	}
	
	public boolean isAllRegionSet() {
		return isAllRegionSet;
	}
	public Vector100Dtype get_vector() {
		if (_vector == null)
			System.out.println("error vector null in Vector100Key");
		return _vector;
	}
	public static int getVAKeyLength(int b){
		int len=b*100/8;
		return len;
	}
	public Vector100Key(int b) throws VAException{
		_b = b;
		
		//Check if the bit number is even.
		if (b%2 == 0)
		{ 
			dataLength = b*100/8;// convert to byte
			data = new byte [dataLength];
			totalregionnum = 1<<b;
			regionsize = VAFile.MAXRANGE / (double)totalregionnum; //divide one dimention into 2^b parts
		}
		else
			throw new VAException(null, "bit number should be even not "+b);
	}
	public Vector100Key(int datalen,byte[] da){
		this.dataLength = datalen;
		this.setDataBytes(da, 0);
		this._b = datalen*8/100;
		this.setAllRegionNumber();
		this.totalregionnum = 1<<this._b;
		this.regionsize = VAFile.MAXRANGE / (double)totalregionnum;
	}
	
//	public Vector100Key getKey(){
//		return null;
//	}

	public Vector100Key(Vector100Dtype v, int b) throws VAException{
		_b = b;
		_vector = v;
		//System.out.println("in Vector100Key");//debug
		if (b%2 == 0)
		{ 
			dataLength = b*100/8;// convert to byte
			data = new byte [dataLength];
			totalregionnum = 1<<b;
			regionsize = VAFile.MAXRANGE / (double)totalregionnum; //divide one dimention into 2^b parts
		}
		else
			throw new VAException(null, "bit number should be even");
		_regionnum = new int [Vector100Dtype.Max];
		short [] vecvalue = v.getVectorValue();
		int tmpregionnum;
		StringBuffer binarydata = new StringBuffer();
		// set bits in string
		for (int i=0;i<100;i++){
			if (vecvalue[i] > VAFile.UPPERBOUND)
				throw new VAException(null, "vector value larger than upper bound "+vecvalue[i]);
			else if (vecvalue[i] < VAFile.LOWERBOUND)
				throw new VAException(null, "vector value lower than lower bound");
			tmpregionnum = (int)((vecvalue[i] - VAFile.LOWERBOUND)/regionsize);
			if (tmpregionnum == totalregionnum)//last region should be 2^b-1, change 2^b to 2^b-1
				tmpregionnum -= 1;
			_regionnum[i] = tmpregionnum;
			String binaryregion = Integer.toBinaryString(tmpregionnum);// binary number of region
			StringBuffer tmpsb = new StringBuffer();
			if (binaryregion.length() < _b){// padding zero
				
				for (int j=binaryregion.length();j<_b;j++){
					tmpsb.append("0");
				}
			}
			tmpsb.append(binaryregion);
			binarydata.append(tmpsb);		
		}
		// convert string to bytes
		for (int i=0;i<dataLength;i++){
			String tmpstr = new String(binarydata.substring(i*8, i*8+8));
			byte numberbyte = (byte) Integer.parseInt(tmpstr,2);
			byte []numberbytearray = new byte [1];
			numberbytearray[0] = numberbyte;
			System.arraycopy (numberbytearray, 0, data, i, 1);
		}
		//System.out.println("in Vector100Key 2");//debug
		//System.out.println(Arrays.toString(data));//debug
		//System.out.println(binarydata);//debug
		
	}
	
	public byte [] returnKeyByteArray(){
		return data;
	}
	public void setDataBytes(byte [] data, int position){
		System.arraycopy(data, position, this.data, 0, this.dataLength);
	}
	public void setAllRegionNumber(){

		if (this._b == 0)
		{
			System.out.println("************* error bit number is 0 in setAllRegionNumber");
			return;
		}
		StringBuffer sb = new StringBuffer();
		//concate all bits to one string
		for (int i=0;i<this.getDataLength();i++)
		{
			String s1 = String.format("%8s", Integer.toBinaryString(data[i] & 0xFF)).replace(' ', '0');
			//System.out.println("in setAllRegionNumber1 "+i+" "+s1);//debug
			sb.append(s1);
		}
		//System.out.println("in setAllRegionNumber2 "+sb);//debug
		_regionnum = new int [Vector100Dtype.Max];
		for (int i=0;i<Vector100Dtype.Max;i++){
//			System.out.println("in setRegionNumber3 b=" + _b+" i="+i);//debug
			int rgnum = Integer.parseInt(sb.substring(i*_b, i*_b+_b),2);
			this._regionnum[i] = rgnum;

		}
		isAllRegionSet = true;
	}
	public void printAllRegionNumber(){
		for (int i=0;i<100;i++){
			System.out.print(this._regionnum[i]+" ");
		}
		System.out.println("");
	}
	public int get_regionnumAt(int i){
		return this._regionnum[i];
	}
//	public short getPartionPointAt(int idx){
//		short p = (short)((this.regionsize * this._regionnum[idx]) + VAFile.LOWERBOUND) ;
//		return p;
//	}
	public short getPartionPointAt(int idx){
		short p;
		p = (short)((this.regionsize * this._regionnum[idx]) + VAFile.LOWERBOUND) ;
		return p;
		}
	public short getPartionPointPlusOneAt(int idx){
		short p;
		p = (short)((this.regionsize * (this._regionnum[idx] + 1)) + VAFile.LOWERBOUND) ;
		if (p > VAFile.UPPERBOUND)
			p = VAFile.UPPERBOUND;
		return p;
		}
	public int getLowerBoundDistance(Vector100Dtype target) throws VAException{
		double sum=0;
		Vector100Key tarkey = new Vector100Key(target,_b);//for region number calculation
		short tmpval=0;
		for (int i=0;i<Vector100Dtype.Max;i++){
//			if (i==0)
//				System.out.print("$$$$$in getLowerBoundDistance "+
//			tarkey.get_regionnumAt(i)+" "+this.get_regionnumAt(i));//debug
			if (tarkey.get_regionnumAt(i) > this.get_regionnumAt(i)){
				tmpval = 
						(short)(target.getVectorValueAt(i) - (this.getPartionPointPlusOneAt(i)));
			}
			else if (tarkey.get_regionnumAt(i) == this.get_regionnumAt(i)){
				tmpval = 0;
			}
			else if (tarkey.get_regionnumAt(i) < this.get_regionnumAt(i)){
				tmpval =
						(short)((this.getPartionPointAt(i)) - target.getVectorValueAt(i));
//				if (i==0) 
//					System.out.print("&&&& getPartionPointAt "+this.getPartionPointAt(i));
					
						
			}
//			if (i==0) System.out.println(" tmpval "+ tmpval);//debug
			sum += tmpval * tmpval;
		}
		sum = Math.sqrt(sum);

		return (int)sum;
		
	}
 
}